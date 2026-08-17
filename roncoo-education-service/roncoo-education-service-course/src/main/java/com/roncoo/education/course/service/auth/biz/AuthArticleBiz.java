package com.roncoo.education.course.service.auth.biz;

import cn.hutool.core.util.ObjectUtil;
import com.roncoo.education.common.base.BaseBiz;
import com.roncoo.education.common.base.ThreadContext;
import com.roncoo.education.common.core.base.Result;
import com.roncoo.education.common.core.enums.PeriodTypeEnum;
import com.roncoo.education.common.tools.IdWorker;
import com.roncoo.education.course.dao.CourseChapterPeriodDao;
import com.roncoo.education.course.dao.UserStudyDao;
import com.roncoo.education.course.dao.impl.mapper.UserAgreementSignMapper;
import com.roncoo.education.course.dao.impl.mapper.entity.CourseChapterPeriod;
import com.roncoo.education.course.dao.impl.mapper.entity.UserStudy;
import com.roncoo.education.course.service.auth.req.AuthArticleReadReq;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 图文课时的阅读进度与签署确认（二开新增）
 * <p>
 * 视频的完成判定是"播放到结尾"，图文没有这个信号，需要另立标准。
 * 这里用**滚动到底 + 停留够时长**双条件：只满足其一都不算学完。
 * 只看滚动会被"秒拉到底"绕过；只看时长则开着页面挂机就能刷完成。
 */
@Component
@RequiredArgsConstructor
public class AuthArticleBiz extends BaseBiz {

    /**
     * 认为"读到底"的滚动比例阈值。留 5% 余量，避免因浏览器缩放、
     * 底部留白等原因永远差一两像素到不了 100%。
     */
    private static final int READ_DONE_PERCENT = 95;

    @NotNull
    private final CourseChapterPeriodDao periodDao;

    @NotNull
    private final UserStudyDao userStudyDao;

    @NotNull
    private final UserAgreementSignMapper agreementSignMapper;

    /**
     * 上报阅读进度
     */
    public Result<String> read(AuthArticleReadReq req) {
        Long userId = ThreadContext.userId();
        if (userId == null) {
            return Result.error("未登录");
        }

        CourseChapterPeriod period = periodDao.getById(req.getPeriodId());
        if (ObjectUtil.isEmpty(period) || !PeriodTypeEnum.ARTICLE.getCode().equals(period.getPeriodType())) {
            return Result.error("该课时不是图文类型");
        }

        UserStudy userStudy = userStudyDao.getByPeriodIdAndUserId(req.getPeriodId(), userId);
        if (ObjectUtil.isEmpty(userStudy)) {
            return Result.error("学习记录不存在，请重新进入课时");
        }

        int percent = Math.max(0, Math.min(100, req.getPercent()));

        // 累计停留秒数，复用 currentDuration 字段，与视频的时长口径一致
        int stay = req.getStaySeconds() == null ? 0 : Math.max(0, Math.min(300, req.getStaySeconds()));
        int totalStay = (userStudy.getCurrentDuration() == null ? 0 : userStudy.getCurrentDuration()) + stay;

        // 必须两个条件同时满足才能到 100。
        // 只按滚动比例算的话，一拉到底进度就是 100，最短阅读时长的门槛等于没有，
        // 后面的「读完才能签署」也会跟着被绕过，所以未达标时最高只给 99。
        int needSeconds = period.getReadSeconds() == null ? 0 : period.getReadSeconds();
        boolean scrolledToEnd = percent >= READ_DONE_PERCENT;
        boolean stayedEnough = totalStay >= needSeconds;
        int effective = (scrolledToEnd && stayedEnough) ? 100 : Math.min(percent, 99);

        // 进度只增不减：来回滚动不应该把已读进度冲掉
        BigDecimal newProgress = BigDecimal.valueOf(effective);
        if (userStudy.getProgress() != null && userStudy.getProgress().compareTo(newProgress) > 0) {
            newProgress = userStudy.getProgress();
        }

        userStudy.setProgress(newProgress);
        userStudy.setCurrentDuration(totalStay);
        userStudy.setCurrentPage(1);
        userStudyDao.updateById(userStudy);

        return Result.success(newProgress.intValue() >= 100 ? "COMPLETE" : "OK");
    }

    /**
     * 签署确认
     * <p>
     * 要求先读完再签，否则"签署确认"没有意义——员工没看就点确认，
     * 出了事这条记录反而对公司不利。
     */
    public Result<String> sign(Long periodId, String clientIp) {
        Long userId = ThreadContext.userId();
        if (userId == null) {
            return Result.error("未登录");
        }

        CourseChapterPeriod period = periodDao.getById(periodId);
        if (ObjectUtil.isEmpty(period) || !PeriodTypeEnum.ARTICLE.getCode().equals(period.getPeriodType())) {
            return Result.error("该课时不是图文类型");
        }
        if (!Integer.valueOf(1).equals(period.getNeedSign())) {
            return Result.error("该课时无需签署");
        }

        UserStudy userStudy = userStudyDao.getByPeriodIdAndUserId(periodId, userId);
        if (ObjectUtil.isEmpty(userStudy) || userStudy.getProgress() == null
                || userStudy.getProgress().compareTo(BigDecimal.valueOf(100)) < 0) {
            return Result.error("请先阅读完整篇内容再签署");
        }

        if (agreementSignMapper.exists(userId, periodId) > 0) {
            return Result.success("已签署");
        }
        agreementSignMapper.save(IdWorker.getId(), userId, periodId, clientIp);
        return Result.success("签署成功");
    }
}
