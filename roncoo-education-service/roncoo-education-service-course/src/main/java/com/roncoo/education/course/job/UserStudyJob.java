package com.roncoo.education.course.job;

import cn.hutool.core.collection.CollUtil;
import com.roncoo.education.common.cache.CacheRedis;
import com.roncoo.education.common.core.base.Constants;
import com.roncoo.education.common.core.enums.ResourceTypeEnum;
import com.roncoo.education.course.dao.UserStudyDao;
import com.roncoo.education.course.dao.impl.mapper.entity.UserStudy;
import com.roncoo.education.course.service.auth.req.AuthUserStudyReq;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * @author fengyw
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserStudyJob {

    @NotNull
    private final CacheRedis cacheRedis;

    @NotNull
    private final UserStudyDao userStudyDao;


    /**
     * 处理中途退出的学习进度
     * <p>
     * 二开改动：原为 @XxlJob("userStudyJobHandler")，依赖 xxl-job 调度中心触发。
     * 本项目内网单机部署、未部署 xxl-job（配置为占位值，日志持续报 registry fail），
     * 该任务从未被触发 —— 后果是只有「暂停」和「看完」两个事件会直接写库，
     * 员工看到一半关闭页面，进度留在 Redis，1 天后过期即丢失。
     * 为一个任务引入调度中心不划算，改用 Spring 的 @Scheduled。
     * 若将来确实部署了 xxl-job，改回 @XxlJob 即可（两者不可同时启用，会重复执行）。
     */
    @Scheduled(fixedDelay = 10_000)
    public void progress() {
        try {
            doProgress();
        } catch (Exception e) {
            // 定时任务里异常必须自己兜住，否则单次失败会污染后续调度
            log.error("处理学习进度失败", e);
        }
    }

    private void doProgress() {
        // 处理学习进度
        Set<String> keys = cacheRedis.getStringRedisTemplate().keys(Constants.RedisPre.PROGRESS + "*");
        if (CollUtil.isNotEmpty(keys)) {
            for (String key : keys) {
                if (cacheRedis.getStringRedisTemplate().getExpire(key, TimeUnit.MINUTES) < 1430) {
                    // 默认过期时间为1天，1440分钟（若超过10分钟不学习，则处理）
                    AuthUserStudyReq req = cacheRedis.get(key, AuthUserStudyReq.class);
                    UserStudy userStudy = userStudyDao.getById(req.getStudyId());
                    if (ResourceTypeEnum.VIDEO.getCode().equals(req.getResourceType()) || ResourceTypeEnum.AUDIO.getCode().equals(req.getResourceType())) {
                        userStudy.setProgress(percent(req.getCurrentDuration(), req.getTotalDuration()));
                        userStudy.setCurrentDuration(req.getCurrentDuration().intValue());
                    } else if (ResourceTypeEnum.DOC.getCode().equals(req.getResourceType())) {
                        userStudy.setProgress(percent(BigDecimal.valueOf(req.getCurrentPage()), req.getTotalPage()));
                        userStudy.setCurrentPage(req.getCurrentPage());
                    }
                    userStudyDao.updateById(userStudy);
                    // 清楚缓存
                    cacheRedis.delete(Constants.RedisPre.USER_STUDY + req.getStudyId());
                    cacheRedis.delete(key);
                }
            }
        }
    }

    /**
     * 计算观看百分比，保留两位、限制在 0~100。
     * <p>
     * 原来两处都写成 {@code 分子.divide(分母, ...).multiply(100)}，各有一个问题：
     * <ul>
     * <li>音视频那支用 {@code divide(除数, RoundingMode.CEILING)}——
     *     该重载用<b>被除数的 scale</b>，而 currentDuration 是整数秒（scale=0），
     *     于是 5/13 先被向上取整成 1，乘 100 就成了 100%：暂停一次就算学完。</li>
     * <li>文档那支 {@code divide(除数)} 连舍入模式都没有，
     *     除不尽时（如 1/3）直接抛 ArithmeticException，整个定时任务中断。</li>
     * </ul>
     * 与 ApiUserStudyBiz.percent 是同一套算法，两处都要改，
     * 否则「实时上报」和「定时补算」会算出不同的进度。
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
        return pct.min(BigDecimal.valueOf(100));
    }

}
