package com.roncoo.education.course.service.admin.biz;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.roncoo.education.common.core.base.Result;
import com.roncoo.education.common.tools.IdWorker;
import com.roncoo.education.course.dao.CourseDao;
import com.roncoo.education.course.dao.impl.mapper.StatMapper;
import com.roncoo.education.course.dao.impl.mapper.UserNoticeMapper;
import com.roncoo.education.course.dao.impl.mapper.entity.Course;
import com.roncoo.education.course.dao.impl.mapper.entity.CourseExample;
import com.roncoo.education.course.dao.impl.mapper.entity.UserCourseAssign;
import com.roncoo.education.course.dao.impl.mapper.entity.UserNotice;
import com.roncoo.education.course.service.admin.req.AdminRemindReq;
import com.roncoo.education.course.service.admin.resp.AdminRemindResp;
import com.roncoo.education.user.feign.interfaces.IFeignUsers;
import com.roncoo.education.user.feign.interfaces.vo.UserRosterVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * ADMIN-逾期催办（二开新增）
 * <p>
 * 触达方式是站内消息。短信平台没配、企业微信要十月才接，
 * 现阶段只有站内消息能真正送到员工手上；十月接企微时，
 * 这里生成的记录可以直接拿去推送，不用重做。
 * <p>
 * 逾期与否一律以服务端重算为准，不信任前端传来的判断——
 * 看板数据可能已经放了几分钟，员工这期间把课学完了，
 * 照着旧数据催办会催到已经学完的人头上。
 *
 * @author 二开
 */
@Component
@RequiredArgsConstructor
public class AdminRemindBiz {

    /** 1学习催办 */
    private static final int NOTICE_REMIND = 1;

    /** 必修 */
    private static final int ASSIGN_REQUIRED = 1;

    /**
     * 同一员工同一课程的催办间隔（小时）。
     * <p>
     * 管理员可能连点两下，或者主管和培训管理员各催一次，
     * 员工一次收到三条一样的消息就再也不看消息了。
     * 隔天再催是允许的，所以用时间窗口而不是唯一索引。
     */
    private static final int DEDUP_HOURS = 24;

    /** 单次催办上限，防止误点「催办全部」时产生天量消息 */
    private static final int MAX_PER_CALL = 500;

    private final StatMapper statMapper;
    private final UserNoticeMapper noticeMapper;
    private final CourseDao courseDao;
    private final IFeignUsers feignUsers;

    public Result<AdminRemindResp> remind(AdminRemindReq req) {
        boolean all = Boolean.TRUE.equals(req.getAll());
        if (!all && CollUtil.isEmpty(req.getItems())) {
            return Result.error("请先选择要催办的人员");
        }

        LocalDate today = LocalDate.now();

        // ---- 花名册 ----
        List<UserRosterVO> roster = feignUsers.roster();
        Map<Long, UserRosterVO> userMap = new HashMap<>();
        if (roster != null) {
            for (UserRosterVO u : roster) {
                userMap.put(u.getId(), u);
            }
        }

        Map<Long, String> courseNames = new HashMap<>();
        for (Course c : courseDao.listByExample(new CourseExample())) {
            courseNames.put(c.getId(), c.getCourseName());
        }

        Map<Long, Integer> periodCount = new HashMap<>();
        for (Map<String, Object> row : statMapper.periodCountByCourse()) {
            periodCount.put(toLong(row.get("courseId")), toInt(row.get("cnt")));
        }

        Map<String, Integer> doneMap = new HashMap<>();
        for (Map<String, Object> row : statMapper.studyByUserCourse()) {
            doneMap.put(key(toLong(row.get("userId")), toLong(row.get("courseId"))), toInt(row.get("doneCount")));
        }

        // 前端指定催办时，只处理这批；催办全部时不设过滤
        Set<String> wanted = null;
        if (!all) {
            wanted = new HashSet<>();
            for (AdminRemindReq.Item it : req.getItems()) {
                if (it.getUserId() != null && it.getCourseId() != null) {
                    wanted.add(key(it.getUserId(), it.getCourseId()));
                }
            }
            if (wanted.isEmpty()) {
                return Result.error("请先选择要催办的人员");
            }
        }

        // ---- 重算逾期 ----
        AdminRemindResp resp = new AdminRemindResp();
        List<UserCourseAssign> overdue = new ArrayList<>();
        Set<Long> overdueUserIds = new HashSet<>();

        for (UserCourseAssign a : statMapper.listAllAssign()) {
            String k = key(a.getUserId(), a.getCourseId());
            if (wanted != null && !wanted.contains(k)) {
                continue;
            }
            UserRosterVO user = userMap.get(a.getUserId());
            boolean required = ASSIGN_REQUIRED == (a.getAssignType() == null ? ASSIGN_REQUIRED : a.getAssignType());
            boolean finished = AdminStatBiz.isFinished(
                    periodCount.getOrDefault(a.getCourseId(), 0), doneMap.getOrDefault(k, 0));

            if (user == null || !required || !AdminStatBiz.isOverdue(finished, a.getDeadline(), today)) {
                // 员工已停用、是选修、或已经不逾期了：都不该发催办
                if (wanted != null) {
                    resp.setInvalid(resp.getInvalid() + 1);
                }
                continue;
            }
            overdue.add(a);
            overdueUserIds.add(a.getUserId());
        }

        // 指定催办时，选中的行里可能有服务端根本找不到的指派（已被删除）
        if (wanted != null) {
            int matched = overdue.size() + resp.getInvalid();
            resp.setInvalid(resp.getInvalid() + Math.max(0, wanted.size() - matched));
        }

        if (overdue.isEmpty()) {
            resp.setMessage("没有需要催办的记录，所选人员均已完成或不再逾期");
            return Result.success(resp);
        }

        // ---- 去掉近期已催过的 ----
        Date since = new Date(System.currentTimeMillis() - DEDUP_HOURS * 3600L * 1000L);
        Set<String> recent = new HashSet<>();
        for (UserNotice n : noticeMapper.listRecent(new ArrayList<>(overdueUserIds), NOTICE_REMIND, since)) {
            recent.add(key(n.getUserId(), n.getCourseId()));
        }

        String remark = StrUtil.trimToEmpty(req.getRemark());
        List<UserNotice> batch = new ArrayList<>();
        for (UserCourseAssign a : overdue) {
            if (recent.contains(key(a.getUserId(), a.getCourseId()))) {
                resp.setSkipped(resp.getSkipped() + 1);
                continue;
            }
            if (batch.size() >= MAX_PER_CALL) {
                break;
            }
            LocalDate dl = new java.sql.Date(a.getDeadline().getTime()).toLocalDate();
            long days = ChronoUnit.DAYS.between(dl, today);
            String courseName = courseNames.getOrDefault(a.getCourseId(), "培训课程");

            StringBuilder content = new StringBuilder()
                    .append("你的必修课程《").append(courseName).append("》已于 ")
                    .append(dl).append(" 到期，已逾期 ").append(days).append(" 天，请尽快完成学习。");
            if (!remark.isEmpty()) {
                content.append('\n').append(remark);
            }

            batch.add(new UserNotice()
                    .setId(IdWorker.getId())
                    .setUserId(a.getUserId())
                    .setNoticeType(NOTICE_REMIND)
                    .setTitle("课程逾期提醒")
                    // 正文长度受 content 字段 512 限制，课程名和附言都可能很长，这里兜底截断
                    .setContent(StrUtil.maxLength(content.toString(), 500))
                    .setCourseId(a.getCourseId()));
        }

        if (!batch.isEmpty()) {
            noticeMapper.batchInsert(batch);
        }
        resp.setSent(batch.size());

        StringBuilder msg = new StringBuilder("已发出 ").append(resp.getSent()).append(" 条催办");
        if (resp.getSkipped() > 0) {
            msg.append("；").append(resp.getSkipped()).append(" 条因 ").append(DEDUP_HOURS).append(" 小时内已催过而跳过");
        }
        if (resp.getInvalid() > 0) {
            msg.append("；").append(resp.getInvalid()).append(" 条已完成或不再逾期");
        }
        resp.setMessage(msg.toString());
        return Result.success(resp);
    }

    private static String key(Long userId, Long courseId) {
        return userId + " " + courseId;
    }

    private static long toLong(Object o) {
        return o == null ? 0L : ((Number) o).longValue();
    }

    private static int toInt(Object o) {
        return o == null ? 0 : ((Number) o).intValue();
    }
}
