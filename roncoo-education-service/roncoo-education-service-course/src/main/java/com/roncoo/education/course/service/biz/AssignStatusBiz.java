package com.roncoo.education.course.service.biz;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.roncoo.education.common.core.enums.StatusIdEnum;
import com.roncoo.education.course.dao.CourseChapterPeriodDao;
import com.roncoo.education.course.dao.UserStudyDao;
import com.roncoo.education.course.dao.impl.mapper.UserCourseAssignMapper;
import com.roncoo.education.course.dao.impl.mapper.entity.UserCourseAssign;
import com.roncoo.education.course.dao.impl.mapper.entity.UserStudy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * 维护课程指派的完成状态（二开新增）
 * <p>
 * 背景：user_course_assign.finish_status 是个冗余状态，
 * 原本只有「考试通过」一条路会写它（{@code AuthExamBiz} 判卷时置 3），
 * 员工把课学完并不会更新，于是后台「课程指派」列表里出现过
 * 进度 100% 却显示「未开始」的记录。
 * <p>
 * 这里把状态的推导集中到一处，由各个上报进度的地方调用，
 * 避免每个写进度的入口各写一遍判定逻辑、日后再各自漏改。
 * <p>
 * 状态不回退：已经置为「已通过考试」的不会因为课时被禁用等原因掉回去。
 *
 * @author 二开
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AssignStatusBiz {

    /** 0未开始 1学习中 2已学完 3已通过考试 */
    private static final int NOT_STARTED = 0;
    private static final int LEARNING = 1;
    private static final int FINISHED = 2;
    private static final int PASSED = 3;

    private static final BigDecimal COMPLETE = BigDecimal.valueOf(100);

    private final UserCourseAssignMapper assignMapper;
    private final CourseChapterPeriodDao periodDao;
    private final UserStudyDao userStudyDao;

    /**
     * 按当前学习进度重算某人某课的完成状态。
     * <p>
     * 没有对应指派记录时什么也不做——员工自主学习一门没派给他的课是允许的，
     * 不该凭空造出一条指派。
     * <p>
     * 整个方法不抛异常：它挂在上报进度的主流程后面，
     * 状态没更新只是后台列表显示旧值，不该因此让员工的进度上报失败。
     */
    public void refresh(Long userId, Long courseId) {
        if (userId == null || courseId == null) {
            return;
        }
        try {
            UserCourseAssign assign = assignMapper.getByUserAndCourse(userId, courseId);
            if (ObjectUtil.isEmpty(assign)) {
                return;
            }
            Integer current = assign.getFinishStatus();
            if (current != null && current == PASSED) {
                // 已通过考试是终态，不因为后续课程调整而回退
                return;
            }

            int need = CollUtil.size(periodDao.listByCourseIdAndStatusId(courseId, StatusIdEnum.YES.getCode()));
            List<UserStudy> studyList = userStudyDao.listByUserIdAndCourseId(userId, courseId);

            int done = 0;
            for (UserStudy s : studyList) {
                if (s.getProgress() != null && s.getProgress().compareTo(COMPLETE) >= 0) {
                    done++;
                }
            }

            // need == 0 时不能算学完：一门还没发布任何课时的课，
            // 「所有课时都完成了」在逻辑上成立但毫无意义，会让完成率虚高
            int status;
            if (need > 0 && done >= need) {
                status = FINISHED;
            } else if (CollUtil.isNotEmpty(studyList)) {
                status = LEARNING;
            } else {
                status = NOT_STARTED;
            }

            if (current == null || current != status) {
                assignMapper.updateFinish(userId, courseId, status);
            }
        } catch (Exception e) {
            log.warn("刷新课程指派完成状态失败, userId={}, courseId={}", userId, courseId, e);
        }
    }
}
