package com.roncoo.education.course.job;

import cn.hutool.core.collection.CollUtil;
import com.roncoo.education.common.core.enums.StatusIdEnum;
import com.roncoo.education.common.tools.IdWorker;
import com.roncoo.education.course.dao.CourseDao;
import com.roncoo.education.course.dao.impl.mapper.UserCourseAssignMapper;
import com.roncoo.education.course.dao.impl.mapper.entity.Course;
import com.roncoo.education.course.dao.impl.mapper.entity.CourseExample;
import com.roncoo.education.course.dao.impl.mapper.entity.UserCourseAssign;
import com.roncoo.education.system.feign.interfaces.IFeignSysConfig;
import com.roncoo.education.user.feign.interfaces.IFeignUsers;
import com.roncoo.education.user.feign.interfaces.vo.UserRosterVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 按入职天数自动排课（二开新增）
 * <p>
 * 需求见《线上入职培训系统—培训模块清单》：课程标注了「入职第 1 天 / 第 2 天…
 * 第 7 天起按班组分流」，即新员工入职后按天自动收到对应课程。
 * <p>
 * 几个刻意的设计：
 * <ul>
 * <li><b>只处理生效起点之后入职的人</b>。现有员工最早 2022 年入职，
 *     按历史补派会一次性压下几十门课且全部立即逾期，看板第二天就是一片红。
 *     存量员工走后台「课程指派」手工批量派。</li>
 * <li><b>用 &gt;= 而不是 = 判断天数</b>。服务停机两天再起来，中间该推的课
 *     若按「正好等于第 N 天」就永远补不回来了。配合 user_course_assign 的
 *     (user_id, course_id) 唯一键，重复插入会被静默跳过，天然幂等。</li>
 * <li><b>逐条插入并吞掉唯一键冲突</b>，而不是先查后插。先查后插在并发下仍会撞，
 *     而这里的冲突恰恰是正常情况（每天跑都会遇到已派过的课）。</li>
 * </ul>
 *
 * @author 二开
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AutoAssignJob {

    /** 必修 */
    private static final int ASSIGN_REQUIRED = 1;
    /** 未开始 */
    private static final int NOT_STARTED = 0;

    /** 推送范围：1 全员 2 指定班组 */
    private static final int SCOPE_ALL = 1;
    private static final int SCOPE_TEAM = 2;

    /** 兜底的完成期限，课程没配 deadline_days 时用 */
    private static final int DEFAULT_DEADLINE_DAYS = 7;

    @NotNull
    private final CourseDao courseDao;
    @NotNull
    private final UserCourseAssignMapper assignMapper;
    @NotNull
    private final IFeignUsers feignUsers;
    @NotNull
    private final IFeignSysConfig feignSysConfig;

    /**
     * 每天凌晨 3 点跑一次。
     * 放在凌晨是为了让员工上班时already能看到当天该学的课，
     * 也避开白天的业务高峰——这个任务要把花名册整份拉过来。
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void assign() {
        try {
            int n = doAssign();
            if (n > 0) {
                log.info("自动排课完成，新增指派 {} 条", n);
            }
        } catch (Exception e) {
            // 定时任务里异常必须自己兜住，否则单次失败会影响后续调度
            log.error("自动排课失败", e);
        }
    }

    /**
     * 执行一次排课，返回新增的指派条数。
     * 除定时触发外，后台也提供了手工触发入口——配完推送规则后可以立刻验证，
     * 不必等到第二天凌晨。
     */
    public int doAssign() {
        if (!"1".equals(config("autoAssignEnable"))) {
            return 0;
        }
        LocalDate startDate = parseDate(config("autoAssignStartDate"));
        if (startDate == null) {
            log.warn("自动排课生效起点未配置或格式不对，本次跳过");
            return 0;
        }

        // 只取配了推送天数、且已上架启用的课程
        CourseExample example = new CourseExample();
        example.createCriteria().andStatusIdEqualTo(StatusIdEnum.YES.getCode());
        List<Course> courses = courseDao.listByExample(example).stream()
                .filter(c -> c.getPushDay() != null && c.getPushDay() >= 0)
                .collect(Collectors.toList());
        if (CollUtil.isEmpty(courses)) {
            return 0;
        }

        List<UserRosterVO> roster = feignUsers.roster();
        if (CollUtil.isEmpty(roster)) {
            return 0;
        }

        LocalDate today = LocalDate.now();
        int created = 0;

        for (UserRosterVO u : roster) {
            if (u.getHireDate() == null) {
                // 没填入职日期就无从算「第 N 天」。这类人由管理员手工指派，
                // 不在这里猜一个日期，否则会把课派到错误的时间点
                continue;
            }
            LocalDate hire = u.getHireDate();
            if (hire.isBefore(startDate)) {
                continue;
            }
            long days = ChronoUnit.DAYS.between(hire, today);
            if (days < 0) {
                // 入职日期填到了未来，还没到岗
                continue;
            }

            for (Course c : courses) {
                if (days < c.getPushDay()) {
                    continue;
                }
                if (!matchScope(c, u)) {
                    continue;
                }
                if (insert(u.getId(), c, hire)) {
                    created++;
                }
            }
        }
        return created;
    }

    /**
     * 课程的推送范围是否覆盖这名员工。
     * 指定班组但员工没有班组时不推——宁可漏派让管理员补，
     * 也好过把强电组的高压操作课推给职能组的人。
     */
    private static boolean matchScope(Course c, UserRosterVO u) {
        int scope = c.getPushScope() == null ? SCOPE_ALL : c.getPushScope();
        if (scope == SCOPE_ALL) {
            return true;
        }
        if (scope != SCOPE_TEAM || u.getTeamId() == null || !StringUtils.hasText(c.getPushTeamIds())) {
            return false;
        }
        Set<String> ids = new HashSet<>(Arrays.asList(c.getPushTeamIds().split(",")));
        return ids.contains(String.valueOf(u.getTeamId()));
    }

    /**
     * 插入一条指派。已存在则返回 false。
     * <p>
     * 截止日期 = 推送日 + 课程配置的天数，推送日按「入职日 + push_day」算而不是「今天」：
     * 服务停机几天后补跑时，员工的期限不该因为系统的原因被推后。
     */
    private boolean insert(Long userId, Course c, LocalDate hire) {
        int deadlineDays = c.getDeadlineDays() == null || c.getDeadlineDays() <= 0
                ? DEFAULT_DEADLINE_DAYS : c.getDeadlineDays();
        LocalDate pushDate = hire.plusDays(c.getPushDay());
        UserCourseAssign record = new UserCourseAssign()
                .setId(IdWorker.getId())
                .setStatusId(StatusIdEnum.YES.getCode())
                .setUserId(userId)
                .setCourseId(c.getId())
                .setAssignType(ASSIGN_REQUIRED)
                .setDeadline(java.sql.Date.valueOf(pushDate.plusDays(deadlineDays)))
                .setFinishStatus(NOT_STARTED);
        try {
            return assignMapper.insert(record) > 0;
        } catch (org.springframework.dao.DuplicateKeyException e) {
            // 已经派过了。每天跑都会大量遇到，属正常情况，不记日志
            return false;
        }
    }

    private String config(String key) {
        try {
            return feignSysConfig.getByConfigKey(key);
        } catch (Exception e) {
            log.warn("读取配置 {} 失败", key, e);
            return null;
        }
    }

    private static LocalDate parseDate(String s) {
        if (!StringUtils.hasText(s)) {
            return null;
        }
        try {
            return LocalDate.parse(s.trim());
        } catch (Exception e) {
            return null;
        }
    }

}
