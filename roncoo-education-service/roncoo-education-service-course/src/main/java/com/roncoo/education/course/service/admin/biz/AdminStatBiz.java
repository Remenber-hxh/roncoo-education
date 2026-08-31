package com.roncoo.education.course.service.admin.biz;

import com.roncoo.education.common.core.base.Result;
import com.roncoo.education.course.dao.CourseDao;
import com.roncoo.education.course.dao.impl.mapper.StatMapper;
import com.roncoo.education.course.dao.impl.mapper.entity.Course;
import com.roncoo.education.course.dao.impl.mapper.entity.CourseExample;
import com.roncoo.education.course.dao.impl.mapper.entity.UserCourseAssign;
import com.roncoo.education.course.service.admin.resp.AdminStatOverviewResp;
import com.roncoo.education.user.feign.interfaces.IFeignUsers;
import com.roncoo.education.user.feign.interfaces.vo.UserRosterVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.sql.Date;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * ADMIN-学习统计看板（二开新增）
 * <p>
 * 完成度全部按 user_study 的实际进度现算，不读 user_course_assign.finish_status。
 * 那个冗余字段历史上只有「考试通过」一条路会写，学完课程并不更新，
 * 库里已经出现过「进度 100% 但状态还是未开始」的记录，照它统计会直接报错数。
 * <p>
 * 归组信息（班组、项目组）在 user 服务，学习记录在本服务，
 * 通过 Feign 把花名册整份取过来在内存里归组。公司几十人的规模，
 * 这样比在两个服务之间来回问要简单得多。
 *
 * @author 二开
 */
@Component
@RequiredArgsConstructor
public class AdminStatBiz {

    /** 未归属班组/项目组的员工归到这一档，不能直接丢掉——恰恰是这些人最容易漏学 */
    private static final String UNASSIGNED = "未分配";

    /** 逾期名单最多返回多少条。看板是用来发现问题的，超过这个数说明该批量催办了 */
    private static final int MAX_OVERDUE_ROWS = 100;

    /** 统计区间允许的天数范围 */
    private static final int MIN_DAYS = 7;
    private static final int MAX_DAYS = 365;
    private static final int DEFAULT_DAYS = 30;

    /** 必修 */
    private static final int ASSIGN_REQUIRED = 1;

    private final StatMapper statMapper;
    private final CourseDao courseDao;
    private final IFeignUsers feignUsers;

    public Result<AdminStatOverviewResp> overview(Integer days) {
        int span = days == null ? DEFAULT_DAYS : Math.max(MIN_DAYS, Math.min(MAX_DAYS, days));
        LocalDate today = LocalDate.now();
        LocalDate begin = today.minusDays(span - 1L);

        AdminStatOverviewResp resp = new AdminStatOverviewResp().setDays(span);

        // ---- 基础数据 ----
        List<UserRosterVO> roster = feignUsers.roster();
        if (roster == null) {
            roster = new ArrayList<>();
        }
        Map<Long, UserRosterVO> userMap = new HashMap<>();
        for (UserRosterVO u : roster) {
            userMap.put(u.getId(), u);
        }
        resp.setEmployeeCount(roster.size());

        Map<Long, String> courseNames = new HashMap<>();
        for (Course c : courseDao.listByExample(new CourseExample())) {
            courseNames.put(c.getId(), c.getCourseName());
        }

        Map<Long, Integer> periodCount = new HashMap<>();
        for (Map<String, Object> row : statMapper.periodCountByCourse()) {
            periodCount.put(toLong(row.get("courseId")), toInt(row.get("cnt")));
        }

        // (userId, courseId) -> 已完成课时数
        Map<String, Integer> doneMap = new HashMap<>();
        // (userId, courseId) -> 有学习记录的课时数
        Map<String, Integer> touchedMap = new HashMap<>();
        for (Map<String, Object> row : statMapper.studyByUserCourse()) {
            String k = key(toLong(row.get("userId")), toLong(row.get("courseId")));
            doneMap.put(k, toInt(row.get("doneCount")));
            touchedMap.put(k, toInt(row.get("touchedCount")));
        }

        Set<String> passedSet = new HashSet<>();
        for (Map<String, Object> row : statMapper.passedByUserCourse()) {
            passedSet.add(key(toLong(row.get("userId")), toLong(row.get("courseId"))));
        }

        // ---- 趋势 ----
        // 只统计在册员工。库里还有 roncoo 自带的演示账号，不过滤就会出现
        // 「42 名员工里 44 人活跃」这种自相矛盾的数
        List<Long> userIds = new ArrayList<>(userMap.keySet());
        Map<String, long[]> byDate = new HashMap<>();
        if (!userIds.isEmpty()) {
            // 空集合会让 in () 拼出非法 SQL，一个员工都没有时直接跳过查询
            for (Map<String, Object> row : statMapper.sumByDate(Date.valueOf(begin), Date.valueOf(today), userIds)) {
                Object d = row.get("studyDate");
                String ds = d instanceof java.util.Date
                        ? new java.sql.Date(((java.util.Date) d).getTime()).toLocalDate().toString()
                        : String.valueOf(d);
                byDate.put(ds, new long[]{toLong(row.get("totalSec")), toInt(row.get("userCount"))});
            }
        }
        long totalSeconds = 0;
        List<AdminStatOverviewResp.TrendPoint> trend = new ArrayList<>();
        for (LocalDate d = begin; !d.isAfter(today); d = d.plusDays(1)) {
            String ds = d.toString();
            long[] v = byDate.get(ds);
            long sec = v == null ? 0 : v[0];
            totalSeconds += sec;
            trend.add(new AdminStatOverviewResp.TrendPoint()
                    .setDate(ds)
                    .setSeconds(sec)
                    .setUserCount(v == null ? 0 : (int) v[1]));
        }
        resp.setTrend(trend);
        resp.setStudySeconds(totalSeconds);
        Integer active = userIds.isEmpty() ? 0
                : statMapper.countActiveUsers(Date.valueOf(begin), Date.valueOf(today), userIds);
        resp.setActiveUserCount(active == null ? 0 : active);

        // ---- 逐条指派做判定 ----
        List<AdminStatOverviewResp.OverdueRow> overdueList = new ArrayList<>();
        Map<String, AdminStatOverviewResp.GroupStat> teamStats = new LinkedHashMap<>();
        Map<String, AdminStatOverviewResp.GroupStat> groupStats = new LinkedHashMap<>();
        Map<Long, AdminStatOverviewResp.CourseStat> courseStats = new LinkedHashMap<>();
        Set<Long> assignedUsers = new HashSet<>();

        int requiredTotal = 0;
        int requiredDone = 0;
        int overdueCount = 0;

        for (UserCourseAssign a : statMapper.listAllAssign()) {
            UserRosterVO user = userMap.get(a.getUserId());
            if (user == null) {
                // 指派还在、员工已被停用或删除。这类记录不该计入完成率，
                // 否则分母里永远躺着一批不可能完成的任务，指标再也回不到 100%
                continue;
            }
            boolean required = ASSIGN_REQUIRED == (a.getAssignType() == null ? ASSIGN_REQUIRED : a.getAssignType());

            String k = key(a.getUserId(), a.getCourseId());
            int need = periodCount.getOrDefault(a.getCourseId(), 0);
            int done = doneMap.getOrDefault(k, 0);
            int touched = touchedMap.getOrDefault(k, 0);
            boolean passed = passedSet.contains(k);
            boolean finished = isFinished(need, done, passed);
            boolean started = touched > 0 || passed;
            int progress = passed ? 100 : (need > 0 ? Math.min(100, done * 100 / need) : 0);

            // 课程维度：必修选修都算，看板要反映这门课整体铺开得怎么样
            AdminStatOverviewResp.CourseStat cs = courseStats.computeIfAbsent(a.getCourseId(),
                    id -> new AdminStatOverviewResp.CourseStat().setCourseId(id)
                            .setCourseName(courseNames.getOrDefault(id, "已删除的课程")));
            cs.setTotal(cs.getTotal() + 1);
            if (finished) {
                cs.setFinished(cs.getFinished() + 1);
            } else if (started) {
                cs.setLearning(cs.getLearning() + 1);
            } else {
                cs.setNotStarted(cs.getNotStarted() + 1);
            }

            if (!required) {
                // 选修不进完成率与逾期：选修没学完不是问题，混进来会把指标压低，
                // 看板上就分不清「该学的没学」和「可学的没学」
                continue;
            }
            assignedUsers.add(a.getUserId());
            requiredTotal++;
            if (finished) {
                requiredDone++;
            }

            boolean overdue = isOverdue(finished, a.getDeadline(), today);
            if (overdue) {
                overdueCount++;
                if (overdueList.size() < MAX_OVERDUE_ROWS) {
                    LocalDate dl = toLocalDate(a.getDeadline());
                    overdueList.add(new AdminStatOverviewResp.OverdueRow()
                            .setUserId(user.getId())
                            .setEmpNo(user.getEmpNo())
                            .setNickname(user.getNickname())
                            .setTeamName(nvl(user.getTeamName()))
                            .setGroupName(nvl(user.getGroupName()))
                            .setCourseId(a.getCourseId())
                            .setCourseName(courseNames.getOrDefault(a.getCourseId(), "已删除的课程"))
                            .setDeadline(dl.toString())
                            .setOverdueDays((int) ChronoUnit.DAYS.between(dl, today))
                            .setProgress(progress));
                }
            }

            accumulate(teamStats, name(user.getTeamName()), finished, overdue);
            accumulate(groupStats, name(user.getGroupName()), finished, overdue);
        }

        resp.setAssignedUserCount(assignedUsers.size());
        resp.setRequiredTotal(requiredTotal);
        resp.setRequiredDone(requiredDone);
        resp.setOverdueCount(overdueCount);

        // 各组人数按花名册统计，而不是按有任务的人数：
        // 一个班组一个任务都没派，恰恰是最需要在看板上看见的情况
        Map<String, Integer> teamHeadcount = new HashMap<>();
        Map<String, Integer> groupHeadcount = new HashMap<>();
        for (UserRosterVO u : roster) {
            teamHeadcount.merge(name(u.getTeamName()), 1, Integer::sum);
            groupHeadcount.merge(name(u.getGroupName()), 1, Integer::sum);
        }
        for (Map.Entry<String, Integer> e : teamHeadcount.entrySet()) {
            teamStats.computeIfAbsent(e.getKey(), n -> new AdminStatOverviewResp.GroupStat().setName(n))
                    .setUserCount(e.getValue());
        }
        for (Map.Entry<String, Integer> e : groupHeadcount.entrySet()) {
            groupStats.computeIfAbsent(e.getKey(), n -> new AdminStatOverviewResp.GroupStat().setName(n))
                    .setUserCount(e.getValue());
        }

        resp.setTeamStats(finishGroups(teamStats));
        resp.setProjectGroupStats(finishGroups(groupStats));

        List<AdminStatOverviewResp.CourseStat> courseList = new ArrayList<>(courseStats.values());
        for (AdminStatOverviewResp.CourseStat cs : courseList) {
            cs.setRate(rate(cs.getFinished(), cs.getTotal()));
        }
        // 完成率低的排前面，看板第一眼就是最该关注的课
        courseList.sort(Comparator.comparingInt(AdminStatOverviewResp.CourseStat::getRate)
                .thenComparing(Comparator.comparingInt(AdminStatOverviewResp.CourseStat::getTotal).reversed()));
        resp.setCourseStats(courseList);

        // 逾期最久的排前面
        overdueList.sort(Comparator.comparingInt(AdminStatOverviewResp.OverdueRow::getOverdueDays).reversed());
        resp.setOverdueList(overdueList);

        return Result.success(resp);
    }

    /**
     * 一门课是否算完成。
     * <p>
     * 考试通过即算完成，与 user_course_assign.finish_status 的语义一致
     * （3=已通过考试是终态）。漏掉这一条的后果是：考了满分的员工因为
     * 没把每个课时都点开，被算成未完成、进了逾期名单、还会收到催办，
     * 让他去学一门已经考过的课。
     * <p>
     * 没通过考试时看课时：一个已发布课时都没有的课不能算完成，
     * 否则空课程会让完成率虚高到 100%，而员工其实什么都没学到。
     * <p>
     * 催办（{@link AdminRemindBiz}）要用同一套判定，所以放在这里共用：
     * 两处各写一遍，迟早会出现「看板说逾期、催办说不逾期」。
     */
    static boolean isFinished(int need, int done, boolean passedExam) {
        return passedExam || (need > 0 && done >= need);
    }

    /**
     * 是否逾期未完成。只有设了截止日期且已过期、且没学完才算。
     */
    static boolean isOverdue(boolean finished, java.util.Date deadline, LocalDate today) {
        return !finished && deadline != null && toLocalDate(deadline).isBefore(today);
    }

    private static void accumulate(Map<String, AdminStatOverviewResp.GroupStat> map, String name,
                                   boolean finished, boolean overdue) {
        AdminStatOverviewResp.GroupStat g = map.computeIfAbsent(name,
                n -> new AdminStatOverviewResp.GroupStat().setName(n));
        g.setTotal(g.getTotal() + 1);
        if (finished) {
            g.setDone(g.getDone() + 1);
        }
        if (overdue) {
            g.setOverdue(g.getOverdue() + 1);
        }
    }

    private static List<AdminStatOverviewResp.GroupStat> finishGroups(Map<String, AdminStatOverviewResp.GroupStat> map) {
        List<AdminStatOverviewResp.GroupStat> list = new ArrayList<>(map.values());
        for (AdminStatOverviewResp.GroupStat g : list) {
            g.setRate(rate(g.getDone(), g.getTotal()));
        }
        // 完成率低的排前面；没有任务的组排最后，它们的 0% 不是「没学」而是「没派」
        list.sort(Comparator.comparingInt((AdminStatOverviewResp.GroupStat g) -> g.getTotal() == 0 ? 1 : 0)
                .thenComparingInt(AdminStatOverviewResp.GroupStat::getRate)
                .thenComparing(AdminStatOverviewResp.GroupStat::getName));
        return list;
    }

    private static int rate(int done, int total) {
        return total == 0 ? 0 : (int) Math.round(done * 100.0 / total);
    }

    private static String name(String raw) {
        return StringUtils.hasText(raw) ? raw : UNASSIGNED;
    }

    private static String nvl(String s) {
        return s == null ? "" : s;
    }

    private static String key(Long userId, Long courseId) {
        return userId + "\0" + courseId;
    }

    private static LocalDate toLocalDate(java.util.Date d) {
        return new java.sql.Date(d.getTime()).toLocalDate();
    }

    private static long toLong(Object o) {
        return o == null ? 0L : ((Number) o).longValue();
    }

    private static int toInt(Object o) {
        return o == null ? 0 : ((Number) o).intValue();
    }
}
