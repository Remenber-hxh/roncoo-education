package com.roncoo.education.course.service.admin.biz;

import cn.hutool.core.collection.CollUtil;
import com.roncoo.education.common.core.base.Result;
import com.roncoo.education.course.dao.CategoryDao;
import com.roncoo.education.course.dao.CourseDao;
import com.roncoo.education.course.dao.impl.mapper.StatMapper;
import com.roncoo.education.course.dao.impl.mapper.entity.Category;
import com.roncoo.education.course.dao.impl.mapper.entity.Course;
import com.roncoo.education.course.dao.impl.mapper.entity.CourseExample;
import com.roncoo.education.course.service.admin.req.AdminScheduleSaveReq;
import com.roncoo.education.course.service.admin.resp.AdminScheduleRowResp;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ADMIN-排课配置（二开新增）
 * <p>
 * 把「哪门课、入职第几天、推给谁、几天内完成、要不要闯关」集中到一张表里维护。
 * <p>
 * 课程编辑页里也有这几项，两者不冲突：那边是建课时顺手配一下，
 * 这边是对着需求表统一看、批量调。需求文件《线上入职培训系统—培训模块清单》
 * 的「培训模块总览」有 42 行，散在 42 个编辑页里既改不动、
 * 也看不出哪些课漏配了、哪个班组一门课都没分到。
 *
 * @author 二开
 */
@Component
@RequiredArgsConstructor
public class AdminScheduleBiz {

    /** 全员 */
    private static final int SCOPE_ALL = 1;
    /** 指定班组 */
    private static final int SCOPE_TEAM = 2;

    private static final int DEFAULT_DEADLINE_DAYS = 7;
    private static final int MAX_PUSH_DAY = 365;

    @NotNull
    private final CourseDao courseDao;
    @NotNull
    private final CategoryDao categoryDao;
    @NotNull
    private final StatMapper statMapper;

    /**
     * 全部课程及其排课配置。
     * <p>
     * 不分页：这张表的价值就在于「一屏看完所有课的分发规则」，
     * 分页之后就看不出哪些课漏配了。课程是几十门的量级，一次取回没有压力。
     */
    public Result<List<AdminScheduleRowResp>> list() {
        List<Course> courses = courseDao.listByExample(new CourseExample());
        if (CollUtil.isEmpty(courses)) {
            return Result.success(new ArrayList<>());
        }

        // 分类名
        List<Long> categoryIds = courses.stream().map(Course::getCategoryId)
                .filter(java.util.Objects::nonNull).distinct().toList();
        Map<Long, String> categoryNames = new HashMap<>();
        if (CollUtil.isNotEmpty(categoryIds)) {
            for (Category c : categoryDao.listByIds(categoryIds)) {
                categoryNames.put(c.getId(), c.getCategoryName());
            }
        }

        // 已发布课时数。课时为 0 的课派出去员工也学不了，表里要标出来
        Map<Long, Integer> periodCount = new HashMap<>();
        for (Map<String, Object> row : statMapper.periodCountByCourse()) {
            Object cid = row.get("courseId");
            Object cnt = row.get("cnt");
            if (cid != null) {
                periodCount.put(((Number) cid).longValue(), cnt == null ? 0 : ((Number) cnt).intValue());
            }
        }

        List<AdminScheduleRowResp> list = new ArrayList<>(courses.size());
        for (Course c : courses) {
            list.add(new AdminScheduleRowResp()
                    .setCourseId(c.getId())
                    .setCourseName(c.getCourseName())
                    .setCategoryName(c.getCategoryId() == null ? "" : categoryNames.getOrDefault(c.getCategoryId(), ""))
                    .setIsPutaway(c.getIsPutaway())
                    .setPushDay(c.getPushDay())
                    .setPushScope(c.getPushScope() == null ? SCOPE_ALL : c.getPushScope())
                    .setPushTeamIds(c.getPushTeamIds())
                    .setDeadlineDays(c.getDeadlineDays() == null ? DEFAULT_DEADLINE_DAYS : c.getDeadlineDays())
                    .setNeedSequential(c.getNeedSequential() == null ? 0 : c.getNeedSequential())
                    .setPeriodCount(periodCount.getOrDefault(c.getId(), 0)));
        }

        // 按「推送天数 → 分类 → 课程名」排，与需求表的阅读顺序一致；
        // 没配推送天数的排在最后，正好是待办清单
        list.sort(Comparator
                .comparing((AdminScheduleRowResp r) -> r.getPushDay() == null ? Integer.MAX_VALUE : r.getPushDay())
                .thenComparing(r -> r.getCategoryName() == null ? "" : r.getCategoryName())
                .thenComparing(r -> r.getCourseName() == null ? "" : r.getCourseName()));
        return Result.success(list);
    }

    /**
     * 批量保存。整表提交，逐行更新。
     * <p>
     * 加事务：管理员对着需求表一次调几十行，中途失败留下改了一半的状态，
     * 比整批失败更难收拾——他不知道哪几行进去了。
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<String> save(AdminScheduleSaveReq req) {
        if (req == null || CollUtil.isEmpty(req.getItems())) {
            return Result.error("没有需要保存的内容");
        }

        int n = 0;
        for (AdminScheduleSaveReq.Item it : req.getItems()) {
            if (it.getCourseId() == null) {
                continue;
            }
            Integer pushDay = it.getPushDay();
            if (pushDay != null && (pushDay < 0 || pushDay > MAX_PUSH_DAY)) {
                return Result.error("推送天数要在 0 ~ " + MAX_PUSH_DAY + " 之间");
            }

            int scope = it.getPushScope() == null ? SCOPE_ALL : it.getPushScope();
            // 范围改回「全员」时清掉班组，否则旧的班组还留在库里，
            // 下次再切回「指定班组」会莫名其妙地带出上次的选择
            String teamIds = scope == SCOPE_TEAM ? StringUtils.trimAllWhitespace(
                    it.getPushTeamIds() == null ? "" : it.getPushTeamIds()) : null;

            Integer deadlineDays = it.getDeadlineDays() == null || it.getDeadlineDays() <= 0
                    ? DEFAULT_DEADLINE_DAYS : it.getDeadlineDays();

            statMapper.updateSchedule(it.getCourseId(), pushDay, scope,
                    StringUtils.hasText(teamIds) ? teamIds : null,
                    deadlineDays,
                    it.getNeedSequential() == null ? 0 : it.getNeedSequential());
            n++;
        }
        return Result.success("已保存 " + n + " 门课的排课配置");
    }
}
