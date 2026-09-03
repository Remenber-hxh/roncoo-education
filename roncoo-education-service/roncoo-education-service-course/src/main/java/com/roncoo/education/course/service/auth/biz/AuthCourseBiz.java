package com.roncoo.education.course.service.auth.biz;

import cn.hutool.core.util.ObjectUtil;
import com.roncoo.education.common.base.ThreadContext;
import com.roncoo.education.common.core.base.Result;
import com.roncoo.education.common.core.enums.*;
import com.roncoo.education.common.base.BaseBiz;
import com.roncoo.education.common.tools.FileSignUtil;
import com.roncoo.education.common.tools.JsonUtil;
import com.roncoo.education.common.upload.UploadFace;
import com.roncoo.education.common.video.LiveUtil;
import com.roncoo.education.common.video.VodUtil;
import com.roncoo.education.common.video.req.LiveWatchReq;
import com.roncoo.education.common.video.req.VodPlayConfigReq;
import com.roncoo.education.course.dao.*;
import com.roncoo.education.course.dao.impl.mapper.PeriodContentMapper;
import com.roncoo.education.course.dao.impl.mapper.UserAgreementSignMapper;
import com.roncoo.education.course.dao.impl.mapper.entity.*;
import com.roncoo.education.course.service.auth.req.AuthCourseSignReq;
import com.roncoo.education.course.service.auth.resp.AuthCourseSignResp;
import com.roncoo.education.system.feign.interfaces.IFeignSysConfig;
import com.roncoo.education.system.feign.interfaces.vo.DocConfig;
import com.roncoo.education.system.feign.interfaces.vo.VideoConfig;
import com.roncoo.education.user.feign.interfaces.IFeignUsers;
import com.roncoo.education.user.feign.interfaces.vo.UsersVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * AUTH-课程信息
 *
 * @author wujing
 */
@Component
@RequiredArgsConstructor
public class AuthCourseBiz extends BaseBiz {

    @NotNull
    private final CourseChapterPeriodDao periodDao;
    @NotNull
    private final CourseChapterDao chapterDao;
    @NotNull
    private final CourseDao courseDao;
    @NotNull
    private final LiveDao liveDao;
    @NotNull
    private final ResourceDao resourceDao;
    @NotNull
    private final UserCourseDao userCourseDao;
    @NotNull
    private final UserStudyDao userStudyDao;
    @NotNull
    private final Map<String, UploadFace> uploadFaceMap;

    /**
     * 图文课时用：正文按需查询、签署状态查询（二开新增）
     */
    @NotNull
    private final PeriodContentMapper periodContentMapper;

    @NotNull
    private final UserAgreementSignMapper agreementSignMapper;

    @NotNull
    private final com.roncoo.education.course.service.biz.SequentialBiz sequentialBiz;

    /**
     * 本地存储私有文件的签名密钥与有效期，见 application-prod.properties。
     * 开发环境有默认值，生产必须覆盖，否则签名可被伪造。
     */
    @Value("${roncoo.file.sign-secret:roncooLocalFileDevSecret}")
    private String fileSignSecret;

    @Value("${roncoo.file.sign-expire-seconds:7200}")
    private int fileSignExpireSeconds;

    @NotNull
    private final IFeignSysConfig feignSysConfig;
    @NotNull
    private final IFeignUsers feignUsers;

    public Result<AuthCourseSignResp> sign(AuthCourseSignReq req) {
        if (ObjectUtil.isNotEmpty(req.getCourseId()) && ObjectUtil.isEmpty(req.getPeriodId())) {
            // 若课程ID存在，则获取该课程的最新学习课时
            UserStudy userStudy = userStudyDao.getByCourseIdForLast(ThreadContext.userId(), req.getCourseId());
            if (ObjectUtil.isNotNull(userStudy)) {
                req.setPeriodId(userStudy.getPeriodId());
            } else {
                return Result.error("请选择要学习的课时");
            }
        }
        CourseChapterPeriod period = periodDao.getById(req.getPeriodId());
        if (ObjectUtil.isEmpty(period) || period.getStatusId().equals(StatusIdEnum.NO.getCode())) {
            return Result.error("该课时不存在或不可用");
        }
        // 顺序解锁（闯关）。前端会把未解锁的课时置灰，但改一下地址栏的 periodId
        // 就能直接跳到最后一课时，所以这里必须再判一次
        if (!sequentialBiz.isUnlocked(ThreadContext.userId(), period.getId())) {
            return Result.error("请先完成上一课时");
        }
        // 图文课时的正文存在课时自身上，不依赖 resource，故资源校验前先分支
        boolean isArticle = PeriodTypeEnum.ARTICLE.getCode().equals(period.getPeriodType());
        if (!isArticle && ObjectUtil.isEmpty(period.getResourceId())) {
            return Result.error("该课时没设置资源");
        }

        // 学习权限校验
        if (!check(period)) {
            return Result.error("暂无该课程的学习权限");
        }

        AuthCourseSignResp resp = new AuthCourseSignResp();
        resp.setPeriodId(req.getPeriodId());
        resp.setPeriodType(period.getPeriodType());

        if (isArticle) {
            return articleSign(period, resp);
        }

        // 直播类型
        if (period.getPeriodType().equals(PeriodTypeEnum.LIVE.getCode())) {
            Live live = liveDao.getById(period.getLiveId());
            if (ObjectUtil.isEmpty(live)) {
                return Result.error("该直播不存在");
            }
            if (live.getLiveStatus().equals(LiveStatusEnum.WAITING.getCode())) {
                return Result.error("直播还没开始，请稍后再试");
            }
            if (live.getLiveStatus().equals(LiveStatusEnum.PLAYBACK.getCode())) {
                // 待回放状态
                return Result.error("直播已经结束，回放请稍候");
            }
            if (live.getLiveStatus().equals(LiveStatusEnum.LIVING.getCode())) {
                // 获取直播观看地址
                viewConfig(live.getChannelId(), resp);
                return Result.success(resp);
            }
            if (live.getLiveStatus().equals(LiveStatusEnum.COMPLETION.getCode())) {
                // 获取回放地址观看回放
                period.setResourceId(live.getResourceId());
            }
        }

        // 资源类型
        Resource resource = resourceDao.getById(period.getResourceId());
        if (ObjectUtil.isEmpty(resource)) {
            return Result.error("该资源不存在");
        }
        if ((resource.getResourceType().equals(ResourceTypeEnum.VIDEO.getCode()) || resource.getResourceType().equals(ResourceTypeEnum.AUDIO.getCode())) && !resource.getVideoStatus().equals(VideoStatusEnum.SUCCES.getCode())) {
            return Result.error("资源处理中，暂不能学习");
        }

        // 资源学习记录
        UserStudy userStudy = userStudyDao.getByPeriodIdAndUserId(req.getPeriodId(), ThreadContext.userId());
        if (ObjectUtil.isEmpty(userStudy)) {
            userStudy = new UserStudy();
            userStudy.setCourseId(period.getCourseId());
            userStudy.setChapterId(period.getChapterId());
            userStudy.setPeriodId(period.getId());
            userStudy.setUserId(ThreadContext.userId());
            userStudy.setResourceType(resource.getResourceType());
            userStudy.setCurrentDuration(0);
            userStudy.setCurrentPage(0);
            userStudy.setProgress(BigDecimal.ZERO);
            userStudyDao.save(userStudy);
        }

        resp.setResourceId(resource.getId());
        resp.setResourceType(resource.getResourceType());
        resp.setVid(resource.getVideoVid());
        resp.setVodPlatform(resource.getVodPlatform());
        resp.setStoragePlatform(resource.getStoragePlatform());
        resp.setStudyId(userStudy.getId());
        resp.setCurrentDuration(userStudy.getCurrentDuration());
        resp.setCurrentPage(userStudy.getCurrentPage());

        if (ResourceTypeEnum.VIDEO.getCode().equals(resource.getResourceType()) || ResourceTypeEnum.AUDIO.getCode().equals(resource.getResourceType())) {
            // 音视频
            if (VodPlatformEnum.LOCAL.getCode().equals(resource.getVodPlatform())) {
                // 本地存储：没有第三方播放器，直接把文件地址下发给前端用原生 video 播放
                localPlayConfig(resource, resp);
            } else {
                playConfig(req, resp);
            }
        } else if (ResourceTypeEnum.DOC.getCode().equals(resource.getResourceType())) {
            // 文档
            docConfig(resource, resp);
        } else if (ResourceTypeEnum.PIC.getCode().equals(resource.getResourceType())) {
            // 图片
            picConfig(resource, resp);
        }
        return Result.success(resp);
    }

    private void viewConfig(String channelNo, AuthCourseSignResp resp) {
        LiveWatchReq liveWatchReq = new LiveWatchReq();
        liveWatchReq.setChannelId(channelNo);
        UsersVO usersVO = feignUsers.getById(ThreadContext.userId());
        liveWatchReq.setUserId(usersVO.getId());
        liveWatchReq.setUserName(usersVO.getNickname());
        liveWatchReq.setUserAvatar(usersVO.getUserHead());

        // 视频云配置
        VideoConfig videoConfig = feignSysConfig.getVideo();
        videoConfig.setVodPlatform(resp.getVodPlatform());
        resp.setLiveViewConfig(LiveUtil.getLiveWatchUrl(videoConfig, liveWatchReq));
    }

    /**
     * 图文课时的学习配置（二开新增）
     * <p>
     * 与音视频不同，图文没有"资源"，正文直接存在课时上。这里负责：
     * 建立/复用学习记录、按需取出正文、下发阅读达标条件与签署状态。
     */
    private Result<AuthCourseSignResp> articleSign(CourseChapterPeriod period, AuthCourseSignResp resp) {
        Long userId = ThreadContext.userId();

        UserStudy userStudy = userStudyDao.getByPeriodIdAndUserId(period.getId(), userId);
        if (ObjectUtil.isEmpty(userStudy)) {
            userStudy = new UserStudy();
            userStudy.setCourseId(period.getCourseId());
            userStudy.setChapterId(period.getChapterId());
            userStudy.setPeriodId(period.getId());
            userStudy.setUserId(userId);
            // 图文归到文档类型，学习记录与统计口径和文档一致
            userStudy.setResourceType(ResourceTypeEnum.DOC.getCode());
            userStudy.setCurrentDuration(0);
            userStudy.setCurrentPage(0);
            userStudy.setProgress(BigDecimal.ZERO);
            userStudyDao.save(userStudy);
        }

        resp.setStudyId(userStudy.getId());
        resp.setResourceType(ResourceTypeEnum.DOC.getCode());
        resp.setCurrentDuration(userStudy.getCurrentDuration());
        resp.setCurrentPage(userStudy.getCurrentPage());
        resp.setProgress(userStudy.getProgress());

        // 正文按需取，列表查询不含该字段
        resp.setContent(periodContentMapper.selectContentById(period.getId()));
        resp.setNeedSign(period.getNeedSign());
        resp.setReadSeconds(period.getReadSeconds());
        resp.setSigned(agreementSignMapper.exists(userId, period.getId()) > 0);
        return Result.success(resp);
    }

    /**
     * 本地存储视频的播放配置（二开新增）
     * <p>
     * 结构保持和第三方点播的 vodPlayConfig 一致（都是 JSON 字符串），
     * 前端按 vodPlatform 分支解析，本地这一支只需要 playUrl。
     * <p>
     * 视频存在 private 目录下，直链不可访问，这里下发带过期时间的签名地址。
     * 有效期需大于最长视频时长，否则播到一半地址失效。
     */
    private void localPlayConfig(Resource resource, AuthCourseSignResp resp) {
        Map<String, String> config = new HashMap<>(2);
        config.put("playUrl", FileSignUtil.signUrl(resource.getResourceUrl(), fileSignExpireSeconds, fileSignSecret));
        resp.setVodPlayConfig(JsonUtil.toJsonString(config));
    }

    private void playConfig(AuthCourseSignReq req, AuthCourseSignResp resp) {
        VodPlayConfigReq playConfigReq = new VodPlayConfigReq();
        playConfigReq.setVid(resp.getVid());
        playConfigReq.setViewerId(ThreadContext.userId().toString());
        playConfigReq.setViewerIp(req.getClientIp());
        VodPlayConfigReq.VodAuthCode authCode = new VodPlayConfigReq.VodAuthCode();
        authCode.setUserId(ThreadContext.userId());
        authCode.setPeriodId(req.getPeriodId());
        playConfigReq.setVodAuthCode(authCode);

        // 视频云配置
        VideoConfig videoConfig = feignSysConfig.getVideo();
        videoConfig.setVodPlatform(resp.getVodPlatform());
        resp.setVodPlayConfig(VodUtil.getPlayConfig(videoConfig, playConfigReq));
    }

    private void docConfig(Resource resource, AuthCourseSignResp resp) {
        DocConfig docConfig = feignSysConfig.getDoc();
        UploadFace uploadFace = uploadFaceMap.get(StoragePlatformEnum.byCode(resource.getStoragePlatform()).getMode());
        resp.setDocStudyConfig(uploadFace.getPreviewConfig(resource.getResourceUrl(), 300, docConfig));
    }

    private void picConfig(Resource resource, AuthCourseSignResp resp) {
        DocConfig docConfig = feignSysConfig.getDoc();
        UploadFace uploadFace = uploadFaceMap.get(StoragePlatformEnum.byCode(resource.getStoragePlatform()).getMode());
        resp.setPicStudyConfig(uploadFace.getPreviewConfig(resource.getResourceUrl(), 300, docConfig));
    }

    /**
     * 学习权限校验
     * <p>
     * 二开：已移除商品/售卖模块，内部培训平台的课程不再有免费/付费之分，
     * 登录员工均可学习，首次访问时自动建立报名记录（user_course）。
     * 该记录仍被学习进度、统计等功能使用，因此保留。
     *
     * @return 恒为 true
     */
    private Boolean check(CourseChapterPeriod period) {
        UserCourse userCourse = userCourseDao.getByCourseIdAndUserId(period.getCourseId(), ThreadContext.userId());
        if (ObjectUtil.isEmpty(userCourse)) {
            userCourse = new UserCourse();
            userCourse.setUserId(ThreadContext.userId());
            userCourse.setCourseId(period.getCourseId());
            userCourseDao.save(userCourse);
        }
        return true;
    }
}
