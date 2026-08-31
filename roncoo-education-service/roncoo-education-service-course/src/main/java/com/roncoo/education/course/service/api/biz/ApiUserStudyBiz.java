package com.roncoo.education.course.service.api.biz;

import cn.hutool.core.util.ObjectUtil;
import com.roncoo.education.common.core.base.Constants;
import com.roncoo.education.common.core.base.Result;
import com.roncoo.education.common.core.enums.ResourceTypeEnum;
import com.roncoo.education.common.core.enums.StudyStatusEnum;
import com.roncoo.education.common.base.BaseBiz;
import com.roncoo.education.common.tools.JsonUtil;
import com.roncoo.education.course.dao.ResourceDao;
import com.roncoo.education.course.dao.UserStudyDao;
import com.roncoo.education.course.dao.impl.mapper.entity.Resource;
import com.roncoo.education.course.dao.impl.mapper.entity.UserStudy;
import com.roncoo.education.course.service.auth.req.AuthUserStudyReq;
import com.roncoo.education.course.service.biz.AssignStatusBiz;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.TimeUnit;

/**
 * API-资源学习记录
 *
 * @author wujing
 */
@Component
@RequiredArgsConstructor
public class ApiUserStudyBiz extends BaseBiz {

    @NotNull
    private final UserStudyDao dao;
    @NotNull
    private final ResourceDao resourceDao;
    @NotNull
    private final AssignStatusBiz assignStatusBiz;

    public Result<String> study(AuthUserStudyReq req) {
        // 资源信息
        Resource resource = getByResource(req);
        if (ObjectUtil.isEmpty(resource)) {
            log.error("{}", JsonUtil.toJsonString(req));
            return Result.error("resourceId不正确");
        }
        req.setResourceType(resource.getResourceType());
        if (ResourceTypeEnum.AUDIO.getCode().equals(resource.getResourceType()) || ResourceTypeEnum.VIDEO.getCode().equals(resource.getResourceType())) {
            // 音视频处理
            req.setTotalDuration(resource.getVideoLength());
            if (new BigDecimal(resource.getVideoLength()).subtract(req.getCurrentDuration()).intValue() < 1) {
                // 学习完成
                return completeStudy(req);
            }
            if (req.getStudyStatus().equals(StudyStatusEnum.PAUSE.getCode())) {
                // 暂停学习
                return pauseStudy(req);
            }

            // 没观看完成，进度存入redis，如没看完，定时任务处理
        } else if (ResourceTypeEnum.DOC.getCode().equals(resource.getResourceType())) {
            // 文档处理
            req.setTotalPage(resource.getDocPage());
            if (req.getCurrentPage().compareTo(1) >= 0) {
                // 学习完成
                return completeStudy(req);
            }
            // 没学习完成，进度存入redis，如没学习完，定时任务处理
        } else if (ResourceTypeEnum.PIC.getCode().equals(resource.getResourceType())) {
            // 学习完成(查看图片即学习完成)
            return completeStudy(req);
        }
        cacheRedis.set(Constants.RedisPre.PROGRESS + req.getStudyId(), req, 1, TimeUnit.DAYS);
        return Result.success(StudyStatusEnum.STUDY.getDesc());
    }

    /**
     * 计算观看百分比，结果保留两位、限制在 0~100。
     * <p>
     * 原来写的是 {@code currentDuration.divide(totalDuration, RoundingMode.CEILING).multiply(100)}。
     * BigDecimal 的 divide(除数, 舍入模式) 用的是<b>被除数的 scale</b>，
     * 而 currentDuration 是整数秒（scale=0），于是 5/13 先被向上取整成 1，
     * 再乘 100 就成了 100%——只要员工暂停过一次，这个课时就被记成学完了。
     * 学习统计看板的完成率、逾期名单全都建立在这个进度上，必须先算对。
     * <p>
     * 先乘 100 再除，并显式指定 scale，避免同样的坑。
     */
    private static BigDecimal percent(BigDecimal current, Integer total) {
        if (current == null || total == null || total <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal pct = current.multiply(BigDecimal.valueOf(100))
                .divide(new BigDecimal(total), 2, RoundingMode.HALF_UP);
        if (pct.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }
        // 前端上报的播放位置可能略大于视频长度（拖到结尾、时长有小数被截断）
        return pct.min(BigDecimal.valueOf(100));
    }

    /**
     * 暂停学习
     */
    private Result<String> pauseStudy(AuthUserStudyReq req) {
        UserStudy userStudy = getUserStudy(req);
        if (ObjectUtil.isEmpty(userStudy)) {
            return Result.error("studyId不正确");
        }
        // 存播放位置，用于「继续观看」。原来这里存的是视频总长度，
        // 一暂停就等于把断点挪到了片尾，下次进来直接从结尾开始
        userStudy.setCurrentDuration(req.getCurrentDuration() == null ? 0 : req.getCurrentDuration().intValue());
        userStudy.setCurrentPage(req.getTotalPage());
        userStudy.setProgress(percent(req.getCurrentDuration(), req.getTotalDuration()));
        // 更新观看记录
        dao.updateById(userStudy);
        assignStatusBiz.refresh(userStudy.getUserId(), userStudy.getCourseId());

        // 更新缓存，当重新开始学习的记录该进度
        cacheRedis.set(Constants.RedisPre.PROGRESS + req.getStudyId(), req, 1, TimeUnit.DAYS);
        return Result.success(StudyStatusEnum.PAUSE.getDesc());
    }

    /**
     * 完成学习
     *
     * @param req
     * @return
     */
    private Result<String> completeStudy(AuthUserStudyReq req) {
        UserStudy userStudy = getUserStudy(req);
        if (ObjectUtil.isEmpty(userStudy)) {
            return Result.error("studyId不正确");
        }
        userStudy.setCurrentDuration(req.getTotalDuration());
        userStudy.setCurrentPage(req.getTotalPage());
        userStudy.setProgress(BigDecimal.valueOf(100));
        // 更新观看记录
        dao.updateById(userStudy);
        assignStatusBiz.refresh(userStudy.getUserId(), userStudy.getCourseId());
        // 清空缓存
        cacheRedis.delete(Constants.RedisPre.USER_STUDY + req.getStudyId());
        cacheRedis.delete(Constants.RedisPre.PROGRESS + req.getStudyId());
        return Result.success("OK");
    }

    private Resource getByResource(AuthUserStudyReq req) {
        Resource resource = cacheRedis.get(Constants.RedisPre.RESOURCE + req.getResourceId(), Resource.class);
        if (ObjectUtil.isEmpty(resource)) {
            resource = resourceDao.getById(req.getResourceId());
            cacheRedis.set(Constants.RedisPre.RESOURCE + req.getResourceId(), resource, 1, TimeUnit.HOURS);
        }
        return resource;
    }

    private UserStudy getUserStudy(AuthUserStudyReq req) {
        UserStudy userStudy = cacheRedis.get(Constants.RedisPre.USER_STUDY + req.getStudyId(), UserStudy.class);
        if (ObjectUtil.isEmpty(userStudy)) {
            userStudy = dao.getById(req.getStudyId());
            cacheRedis.set(Constants.RedisPre.USER_STUDY + req.getStudyId(), userStudy, 1, TimeUnit.HOURS);
        }
        return userStudy;
    }
}
