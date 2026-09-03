package com.roncoo.education.course.service.biz;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.DesensitizedUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.roncoo.education.common.base.page.Page;
import com.roncoo.education.common.base.page.PageUtil;
import com.roncoo.education.common.core.base.Result;
import com.roncoo.education.common.core.enums.PeriodTypeEnum;
import com.roncoo.education.common.core.enums.StatusIdEnum;
import com.roncoo.education.common.tools.BeanUtil;
import com.roncoo.education.common.base.BaseBiz;
import com.roncoo.education.course.dao.*;
import com.roncoo.education.course.dao.impl.mapper.entity.*;
import com.roncoo.education.course.service.biz.req.CourseCommentPageReq;
import com.roncoo.education.course.service.biz.req.CourseReq;
import com.roncoo.education.course.service.biz.resp.*;
import com.roncoo.education.user.feign.interfaces.IFeignLecturer;
import com.roncoo.education.user.feign.interfaces.IFeignUsers;
import com.roncoo.education.user.feign.interfaces.vo.LecturerViewVO;
import com.roncoo.education.user.feign.interfaces.vo.UsersVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * API-课程信息
 *
 * @author wujing
 */
@Component
@RequiredArgsConstructor
public class CourseBiz extends BaseBiz {

    @NotNull
    private final CourseDao dao;
    @NotNull
    private final UserCourseDao userCourseDao;
    @NotNull
    private final UserStudyDao userStudyDao;
    @NotNull
    private final CourseChapterDao chapterDao;
    @NotNull
    private final CourseChapterPeriodDao periodDao;
    @NotNull
    private final UserCourseCommentDao userCourseCommentDao;
    @NotNull
    private final UserCourseCollectDao userCourseCollectDao;
    @NotNull
    private final ResourceDao resourceDao;
    @NotNull
    private final LiveDao liveDao;
    @NotNull
    private final IFeignLecturer feignLecturer;
    @NotNull
    private final IFeignUsers feignUsers;

    /**
     * 课程查看接口
     *
     * @param req
     * @param userId
     * @return
     */
    public Result<CourseResp> view(CourseReq req, Long userId) {
        Course course = dao.getById(req.getCourseId());
        if (course == null) {
            return Result.error("找不到该课程信息");
        }
        if (!course.getStatusId().equals(StatusIdEnum.YES.getCode())) {
            return Result.error("该课程已被禁用");
        }
        CourseResp courseResp = BeanUtil.copyProperties(course, CourseResp.class);

        Map<Long, BigDecimal> userStudyProgressMap = new HashMap<>();
        if (ObjectUtil.isNotEmpty(userId)) {
            // userId存在，即为登录。
            // 二开：已移除商品/售卖模块，课程不再有免费/付费之分，登录员工均可学习。
            courseResp.setAllowStudy(1);

            // 课时进度
            List<UserStudy> userStudyList = userStudyDao.listByUserIdAndCourseId(userId, course.getId());
            if (CollUtil.isNotEmpty(userStudyList)) {
                userStudyProgressMap = userStudyList.stream().collect(Collectors.toMap(UserStudy::getPeriodId, UserStudy::getProgress));
            }

            // 课程收藏状态
            UserCourseCollect userCourseCollect = userCourseCollectDao.getByCourseIdAndUserId(req.getCourseId(), userId);
            if (ObjectUtil.isNotEmpty(userCourseCollect)) {
                courseResp.setCourseCollect(Boolean.TRUE);
            }
        }
        // 获取讲师信息
        LecturerViewVO lecturerViewVO = feignLecturer.getById(course.getLecturerId());
        if (ObjectUtil.isNotEmpty(lecturerViewVO)) {
            courseResp.setLecturerResp(BeanUtil.copyProperties(lecturerViewVO, CourseLecturerResp.class));
        }
        // 章节信息
        List<CourseChapter> chapterList = chapterDao.listByCourseIdAndStatusId(course.getId(), StatusIdEnum.YES.getCode());
        if (CollUtil.isNotEmpty(chapterList)) {
            courseResp.setChapterRespList(BeanUtil.copyProperties(chapterList, CourseChapterResp.class));
            // 课时信息
            List<CourseChapterPeriod> periodList = periodDao.listByCourseIdAndStatusId(course.getId(), StatusIdEnum.YES.getCode());

            if (CollUtil.isNotEmpty(periodList)) {
                Map<Long, List<CourseChapterPeriod>> map = periodList.stream().collect(Collectors.groupingBy(CourseChapterPeriod::getChapterId, Collectors.toList()));
                // 资源信息
                Map<Long, Resource> resourceMap = null;
                List<Long> resourceIdList = periodList.stream().filter(courseChapterPeriod -> courseChapterPeriod.getPeriodType().equals(PeriodTypeEnum.RESOURCE.getCode())).map(courseChapterPeriod -> courseChapterPeriod.getResourceId()).collect(Collectors.toList());
                if (CollUtil.isNotEmpty(resourceIdList)) {
                    List<Resource> resourceList = resourceDao.listByIds(resourceIdList);
                    if (CollUtil.isNotEmpty(resourceList)) {
                        resourceMap = resourceList.stream().collect(Collectors.toMap(Resource::getId, item -> item));
                    }
                }

                // 直播信息
                Map<Long, Live> liveMap = null;
                List<Long> liveIdList = periodList.stream().filter(courseChapterPeriod -> courseChapterPeriod.getPeriodType().equals(PeriodTypeEnum.LIVE.getCode())).map(courseChapterPeriod -> courseChapterPeriod.getLiveId()).collect(Collectors.toList());
                if (CollUtil.isNotEmpty(liveIdList)) {
                    List<Live> liveList = liveDao.listByIds(liveIdList);
                    if (CollUtil.isNotEmpty(liveList)) {
                        liveMap = liveList.stream().collect(Collectors.toMap(Live::getId, item -> item));
                    }
                }

                for (CourseChapterResp chapterResp : courseResp.getChapterRespList()) {
                    chapterResp.setPeriodRespList(BeanUtil.copyProperties(map.get(chapterResp.getId()), CourseChapterPeriodResp.class));
                    for (CourseChapterPeriodResp periodResp : chapterResp.getPeriodRespList()) {
                        if (resourceMap != null) {
                            periodResp.setResourceResp(BeanUtil.copyProperties(resourceMap.get(periodResp.getResourceId()), ResourceResp.class));
                        }
                        if (liveMap != null) {
                            periodResp.setLiveResp(BeanUtil.copyProperties(liveMap.get(periodResp.getLiveId()), LiveResp.class));
                        }
                        periodResp.setPeriodProgress(userStudyProgressMap.get(periodResp.getId()));
                    }
                }

                // 顺序解锁（需求里的「闯关」）。必须在章节和课时都填好之后算，
                // 因为它依赖跨章节的全局顺序：第一章最后一课时没学完，第二章第一课时也该锁着
                markUnlocked(courseResp, userStudyProgressMap);
            }
        }
        return Result.success(courseResp);
    }

    /** 已学完的判定阈值。与统计口径一致：progress >= 100 */
    private static final BigDecimal COMPLETE = BigDecimal.valueOf(100);

    /**
     * 标记每个课时的解锁状态（需求里的「闯关」）。
     * <p>
     * 规则：把全部章节的课时按章节顺序、课时顺序摊平成一条线，
     * 第一个恒解锁，之后每个都要求前一个已学完。
     * <b>跨章节也连续</b>——第一章最后一课时没学完，第二章第一课时同样锁着，
     * 否则员工跳到下一章就能绕过，闯关形同虚设。
     * <p>
     * 未登录时没有任何进度，只解锁第一个课时，让人能看到课程长什么样。
     */
    private static void markUnlocked(CourseResp courseResp, Map<Long, BigDecimal> progressMap) {
        if (!Integer.valueOf(1).equals(courseResp.getNeedSequential())) {
            // 未开启闯关：保持默认的全部解锁
            return;
        }
        boolean prevDone = true;
        for (CourseChapterResp chapter : courseResp.getChapterRespList()) {
            if (CollUtil.isEmpty(chapter.getPeriodRespList())) {
                continue;
            }
            for (CourseChapterPeriodResp period : chapter.getPeriodRespList()) {
                period.setUnlocked(prevDone);
                if (!prevDone) {
                    period.setLockedReason("请先完成上一课时");
                }
                BigDecimal p = progressMap.get(period.getId());
                prevDone = p != null && p.compareTo(COMPLETE) >= 0;
            }
        }
    }

    public Result<Page<CourseCommentResp>> comment(CourseCommentPageReq req) {
        UserCourseCommentExample example = new UserCourseCommentExample();
        example.createCriteria().andCourseIdEqualTo(req.getCourseId());
        example.setOrderByClause("id desc");
        Page<UserCourseComment> userCourseCommentPage = userCourseCommentDao.page(req.getPageCurrent(), req.getPageSize(), example);
        Page<CourseCommentResp> resp = PageUtil.transform(userCourseCommentPage, CourseCommentResp.class);
        if (CollUtil.isNotEmpty(userCourseCommentPage.getList())) {
            resp.setList(filter(userCourseCommentPage.getList(), 0L));
            // 用户信息
            List<Long> userIds = userCourseCommentPage.getList().stream().map(UserCourseComment::getUserId).collect(Collectors.toList());
            Map<Long, UsersVO> usersVOMap = feignUsers.listByIds(userIds);
            // 回复是挂在顶级评论下面的，只遍历 resp.getList() 会漏掉它们的用户信息，
            // 界面上回复者就成了空白。这里递归补齐每一层。
            fillUsers(resp.getList(), usersVOMap);
        }
        return Result.success(resp);
    }

    /**
     * 逐层填充评论的用户信息。
     * <p>
     * 取不到用户时不能直接用返回值调方法——评论人被删除后
     * usersVOMap.get(userId) 是 null，原来那行 usersVO.setMobile(...)
     * 会抛空指针，一条脏数据就让整个评论列表打不开。
     * 这种情况给个占位，让其余评论正常显示。
     */
    private void fillUsers(List<CourseCommentResp> list, Map<Long, UsersVO> usersVOMap) {
        if (CollUtil.isEmpty(list)) {
            return;
        }
        for (CourseCommentResp commentResp : list) {
            UsersVO usersVO = usersVOMap.get(commentResp.getUserId());
            if (usersVO == null) {
                usersVO = new UsersVO();
                usersVO.setNickname("已注销用户");
            } else {
                usersVO.setMobile(DesensitizedUtil.mobilePhone(usersVO.getMobile()));
                if (StrUtil.isBlank(usersVO.getNickname())) {
                    usersVO.setNickname(usersVO.getMobile());
                }
            }
            commentResp.setUsersVO(usersVO);
            fillUsers(commentResp.getCourseCommentRespList(), usersVOMap);
        }
    }

    /**
     * 按父评论ID挑出该层的评论，再递归挂上各自的回复。
     * <p>
     * commentId 用 Objects.equals 比较，不能写成 item.getCommentId().equals(...)：
     * 顶级评论的 comment_id 允许为空（新增接口早期没有赋值，库里已有这样的数据），
     * 一旦遇到 null 就抛 NullPointerException，整个评论列表接口返回「服务繁忙」，
     * 一条评论都看不到。
     * 这里把 null 视同 0，与顶级评论的约定一致。
     */
    private List<CourseCommentResp> filter(List<UserCourseComment> userCourseComments, Long commentId) {
        List<UserCourseComment> list = userCourseComments.stream()
                .filter(item -> Objects.equals(item.getCommentId() == null ? 0L : item.getCommentId(), commentId))
                .collect(Collectors.toList());
        if (CollUtil.isNotEmpty(list)) {
            List<CourseCommentResp> resps = BeanUtil.copyProperties(list, CourseCommentResp.class);
            for (CourseCommentResp resp : resps) {
                resp.setCourseCommentRespList(filter(userCourseComments, resp.getId()));
            }
            return resps;
        }
        return new ArrayList<>();
    }
}
