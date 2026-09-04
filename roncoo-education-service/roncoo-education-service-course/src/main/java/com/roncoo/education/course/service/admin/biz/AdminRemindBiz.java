package com.roncoo.education.course.service.admin.biz;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.roncoo.education.common.core.base.Result;
import com.roncoo.education.common.tools.IdWorker;
import com.roncoo.education.common.tools.WeComBotUtil;
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
import com.roncoo.education.user.feign.interfaces.vo.UsersVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
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
@Slf4j
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
    private final com.roncoo.education.system.feign.interfaces.IFeignSysConfig feignSysConfig;

    /** 读单个配置项，取不到返回 null */
    private String config(String key) {
        try {
            return feignSysConfig.getByConfigKey(key);
        } catch (Exception e) {
            log.warn("读取配置 {} 失败", key, e);
            return null;
        }
    }

    private static String nvl(String s) {
        return s == null ? "" : s;
    }

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

        // 考试通过即算完成，必须和看板用同一套判定，
        // 否则会出现「看板不显示逾期，催办却把人催了」
        Set<String> passedSet = new HashSet<>();
        for (Map<String, Object> row : statMapper.passedByUserCourse()) {
            passedSet.add(key(toLong(row.get("userId")), toLong(row.get("courseId"))));
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
                    periodCount.getOrDefault(a.getCourseId(), 0), doneMap.getOrDefault(k, 0), passedSet.contains(k));

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
        // 超过单次上限而没发出去的条数。必须单独记并告诉管理员——
        // 只报「已发出 500 条」的话，他会以为所有人都催到了，剩下的人再也不会被想起
        int truncated = 0;
        for (UserCourseAssign a : overdue) {
            if (recent.contains(key(a.getUserId(), a.getCourseId()))) {
                resp.setSkipped(resp.getSkipped() + 1);
                continue;
            }
            if (batch.size() >= MAX_PER_CALL) {
                truncated++;
                continue;
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
            // 站内消息要员工主动登录才看得见。企微群机器人会直接弹到手机上，
            // 是目前唯一能主动触达的通道（短信平台没配、也没有可对外访问的地址）
            pushToWeCom(batch, userMap, courseNames);
        }
        resp.setSent(batch.size());

        StringBuilder msg = new StringBuilder("已发出 ").append(resp.getSent()).append(" 条催办");
        if (resp.getSkipped() > 0) {
            msg.append("；").append(resp.getSkipped()).append(" 条因 ").append(DEDUP_HOURS).append(" 小时内已催过而跳过");
        }
        if (resp.getInvalid() > 0) {
            msg.append("；").append(resp.getInvalid()).append(" 条已完成或不再逾期");
        }
        if (truncated > 0) {
            msg.append("；还有 ").append(truncated).append(" 条超出单次上限（")
                    .append(MAX_PER_CALL).append(" 条）未发送，请再点一次继续");
        }
        resp.setMessage(msg.toString());
        return Result.success(resp);
    }

    /**
     * 把这批催办推到企业微信群，并 @ 到本人。
     * <p>
     * 手机号只用于 @（放进 mentioned_mobile_list，企微据此解析成「@姓名」，
     * 不会把号码显示出来），正文里只出现姓名、班组和课程名——
     * 群里所有人都看得见这条消息，没必要把手机号也摊开。
     * 也正因如此，手机号没有放进花名册 VO：只在机器人真的开启时，
     * 才为被催的那几个人单独取一次。
     * <p>
     * 整个方法不抛异常：群通知是附加的触达层，站内消息此时已经写成功了，
     * 不能因为发群消息失败就让整个催办报错。
     */
    private void pushToWeCom(List<UserNotice> batch, Map<Long, UserRosterVO> userMap,
                             Map<Long, String> courseNames) {
        try {
            if (!"1".equals(config("wecomBotEnable"))) {
                return;
            }
            String webhook = config("wecomBotWebhook");
            if (!StringUtils.hasText(webhook)) {
                return;
            }

            // 同一个人被催多门课时合并成一行，避免刷屏
            Map<Long, List<String>> byUser = new LinkedHashMap<>();
            for (UserNotice n : batch) {
                byUser.computeIfAbsent(n.getUserId(), k -> new ArrayList<>())
                        .add(courseNames.getOrDefault(n.getCourseId(), "培训课程"));
            }

            Map<Long, UsersVO> detail = feignUsers.listByIds(new ArrayList<>(byUser.keySet()));
            List<String> mobiles = new ArrayList<>();
            StringBuilder sb = new StringBuilder("【培训催办】以下同事有必修课已逾期，请尽快完成：\n");
            for (Map.Entry<Long, List<String>> e : byUser.entrySet()) {
                UserRosterVO u = userMap.get(e.getKey());
                sb.append("· ").append(u == null ? "" : nvl(u.getNickname()));
                if (u != null && StringUtils.hasText(u.getTeamName())) {
                    sb.append("（").append(u.getTeamName()).append("）");
                }
                sb.append("：").append(String.join("、", e.getValue())).append('\n');

                UsersVO vo = detail == null ? null : detail.get(e.getKey());
                if (vo != null && StringUtils.hasText(vo.getMobile())) {
                    mobiles.add(vo.getMobile());
                }
            }
            sb.append("请登录培训平台完成学习。");

            WeComBotUtil.sendText(webhook, sb.toString(), mobiles);
        } catch (Exception e) {
            log.warn("推送企业微信群通知失败，站内消息已发出，不影响催办结果", e);
        }
    }

    private static String key(Long userId, Long courseId) {
        return userId + "\u0000" + courseId;
    }

    private static long toLong(Object o) {
        return o == null ? 0L : ((Number) o).longValue();
    }

    private static int toInt(Object o) {
        return o == null ? 0 : ((Number) o).intValue();
    }
}
