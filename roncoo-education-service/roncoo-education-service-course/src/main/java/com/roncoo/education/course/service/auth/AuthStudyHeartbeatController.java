package com.roncoo.education.course.service.auth;

import com.roncoo.education.common.core.base.Result;
import com.roncoo.education.course.service.auth.biz.AuthStudyHeartbeatBiz;
import com.roncoo.education.course.service.auth.req.AuthStudyHeartbeatReq;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AUTH-学习时长心跳（二开新增）
 */
@Tag(name = "auth-学习时长")
@RestController
@RequiredArgsConstructor
@RequestMapping("/course/auth/study")
public class AuthStudyHeartbeatController {

    @NotNull
    private final AuthStudyHeartbeatBiz biz;

    @Operation(summary = "学习心跳", description = "播放中每30秒上报一次，服务端按天累加有效学习时长")
    @PostMapping("/heartbeat")
    public Result<String> heartbeat(@RequestBody @Valid AuthStudyHeartbeatReq req) {
        return biz.heartbeat(req);
    }
}
