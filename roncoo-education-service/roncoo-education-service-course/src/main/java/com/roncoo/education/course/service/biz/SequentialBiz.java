package com.roncoo.education.course.service.biz;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.roncoo.education.common.core.enums.StatusIdEnum;
import com.roncoo.education.course.dao.CourseChapterDao;
import com.roncoo.education.course.dao.CourseChapterPeriodDao;
import com.roncoo.education.course.dao.CourseDao;
import com.roncoo.education.course.dao.UserStudyDao;
import com.roncoo.education.course.dao.impl.mapper.entity.Course;
import com.roncoo.education.course.dao.impl.mapper.entity.CourseChapter;
import com.roncoo.education.course.dao.impl.mapper.entity.CourseChapterPeriod;
import com.roncoo.education.course.dao.impl.mapper.entity.UserStudy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 顺序解锁校验（二开新增）
 * <p>
 * 需求里的「闯关」：必须学完上一课时才能进下一课时。
 * <p>
 * 前端会把未解锁的课时置灰，但那只挡得住误点——
 * 改一下地址栏的 periodId 就能直接跳到最后一课时把进度刷满。
 * 所以进入课时和上报进度这两个入口都必须在服务端再判一次。
 *
 * @author 二开
 */
@Component
@RequiredArgsConstructor
public class SequentialBiz {

    /** 已学完的判定阈值，与统计口径一致 */
    private static final BigDecimal COMPLETE = BigDecimal.valueOf(100);

    @NotNull
    private final CourseDao courseDao;
    @NotNull
    private final CourseChapterDao chapterDao;
    @NotNull
    private final CourseChapterPeriodDao periodDao;
    @NotNull
    private final UserStudyDao userStudyDao;

    /**
     * 该课时对这名员工是否已解锁。
     * <p>
     * 课程没开启闯关、或找不到课程/课时时一律放行——
     * 校验的职责是挡住越级，不是替其它校验兜底；
     * 在这里因为查不到数据就拒绝，会把正常的边界情况变成学不了。
     */
    public boolean isUnlocked(Long userId, Long periodId) {
        if (userId == null || periodId == null) {
            return true;
        }
        CourseChapterPeriod period = periodDao.getById(periodId);
        if (ObjectUtil.isEmpty(period)) {
            return true;
        }
        Course course = courseDao.getById(period.getCourseId());
        if (ObjectUtil.isEmpty(course) || !Integer.valueOf(1).equals(course.getNeedSequential())) {
            return true;
        }

        List<CourseChapterPeriod> ordered = orderedPeriods(period.getCourseId());
        int idx = -1;
        for (int i = 0; i < ordered.size(); i++) {
            if (ordered.get(i).getId().equals(periodId)) {
                idx = i;
                break;
            }
        }
        // 第一个课时恒解锁；课时不在已发布列表里（被禁用了）也不拦
        if (idx <= 0) {
            return true;
        }

        Map<Long, BigDecimal> progress = progressMap(userId, period.getCourseId());
        BigDecimal prev = progress.get(ordered.get(idx - 1).getId());
        return prev != null && prev.compareTo(COMPLETE) >= 0;
    }

    /**
     * 课程下全部已发布课时，按「章节顺序 → 课时顺序」摊平。
     * <p>
     * 跨章节必须连续：第一章最后一课时没学完，第二章第一课时也该锁着，
     * 否则跳到下一章就绕过去了。
     */
    private List<CourseChapterPeriod> orderedPeriods(Long courseId) {
        List<CourseChapter> chapters = chapterDao.listByCourseIdAndStatusId(courseId, StatusIdEnum.YES.getCode());
        Map<Long, Integer> chapterOrder = new HashMap<>();
        if (CollUtil.isNotEmpty(chapters)) {
            for (int i = 0; i < chapters.size(); i++) {
                chapterOrder.put(chapters.get(i).getId(), i);
            }
        }
        List<CourseChapterPeriod> periods = periodDao.listByCourseIdAndStatusId(courseId, StatusIdEnum.YES.getCode());
        if (CollUtil.isEmpty(periods)) {
            return List.of();
        }
        periods.sort(Comparator
                // 章节列表本身已按 sort 排好，这里沿用它的下标作为章节次序
                .comparingInt((CourseChapterPeriod p) -> chapterOrder.getOrDefault(p.getChapterId(), Integer.MAX_VALUE))
                .thenComparing(p -> p.getSort() == null ? 0 : p.getSort())
                .thenComparing(CourseChapterPeriod::getId));
        return periods;
    }

    private Map<Long, BigDecimal> progressMap(Long userId, Long courseId) {
        Map<Long, BigDecimal> map = new HashMap<>();
        List<UserStudy> list = userStudyDao.listByUserIdAndCourseId(userId, courseId);
        if (CollUtil.isNotEmpty(list)) {
            for (UserStudy s : list) {
                if (s.getPeriodId() != null && s.getProgress() != null) {
                    map.put(s.getPeriodId(), s.getProgress());
                }
            }
        }
        return map;
    }
}
