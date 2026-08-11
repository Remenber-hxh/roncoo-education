package com.roncoo.education.course.service.auth.biz;

import com.roncoo.education.common.base.BaseBiz;
import com.roncoo.education.common.base.ThreadContext;
import com.roncoo.education.common.cache.CacheRedis;
import com.roncoo.education.common.core.base.Result;
import com.roncoo.education.common.tools.IdWorker;
import com.roncoo.education.course.dao.impl.mapper.UserStudyDailyMapper;
import com.roncoo.education.course.dao.impl.mapper.entity.UserStudyDaily;
import com.roncoo.education.course.service.auth.req.AuthStudyHeartbeatReq;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * 学习时长心跳（二开新增）
 * <p>
 * 背景：user_study 表只记录播放位置和完成百分比，无法回答「员工学了多久」，
 * 而十月要交付的「学习时长统计」必须有累计时长，且历史数据无法补算。
 * 因此新增本接口，由前端在真实播放时定期上报，服务端按天累加。
 * <p>
 * 防刷设计（接口在公网可达，必须假设会被伪造）：
 * 1. 单次上报秒数上限 {@link #MAX_SECONDS_PER_BEAT}，超出按上限截断；
 * 2. 同一用户+课时两次心跳的最小间隔 {@link #MIN_INTERVAL_SECONDS}，更频繁的直接丢弃；
 * 3. 学习日期取服务端当前日期，不接受前端传值。
 * 上述三条合起来，使得刷时长的速率不可能超过真实播放速率。
 */
@Component
@RequiredArgsConstructor
public class AuthStudyHeartbeatBiz extends BaseBiz {

    /**
     * 单次心跳最多计入的秒数。前端 30 秒一次，留余量应对网络延迟与页面卡顿。
     */
    private static final int MAX_SECONDS_PER_BEAT = 120;

    /**
     * 同一用户+课时的心跳最小间隔（秒）。小于该间隔的重复上报直接丢弃。
     */
    private static final int MIN_INTERVAL_SECONDS = 20;

    private static final String BEAT_KEY = "study:beat:";

    @NotNull
    private final UserStudyDailyMapper mapper;

    @NotNull
    private final CacheRedis cacheRedis;

    public Result<String> heartbeat(AuthStudyHeartbeatReq req) {
        Long userId = ThreadContext.userId();
        if (userId == null) {
            return Result.error("未登录");
        }
        if (req.getSeconds() == null || req.getSeconds() <= 0) {
            return Result.success("忽略");
        }

        // 节流：同一用户+课时在最小间隔内只接受一次
        String key = BEAT_KEY + userId + ":" + req.getPeriodId();
        if (cacheRedis.hasKey(key)) {
            return Result.success("忽略");
        }
        cacheRedis.set(key, "1", MIN_INTERVAL_SECONDS, TimeUnit.SECONDS);

        int seconds = Math.min(req.getSeconds(), MAX_SECONDS_PER_BEAT);

        UserStudyDaily record = new UserStudyDaily()
                .setId(IdWorker.getId())
                .setUserId(userId)
                .setCourseId(req.getCourseId())
                .setStudyDate(new Date())
                .setDurationSec(seconds);
        mapper.upsertAdd(record);
        return Result.success("OK");
    }
}
