package com.roncoo.education.course.service.auth.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 学习心跳请求（二开新增）
 */
@Data
@Accessors(chain = true)
@Schema(description = "学习心跳")
public class AuthStudyHeartbeatReq implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "课程ID不能为空")
    @Schema(description = "课程ID", required = true)
    private Long courseId;

    @NotNull(message = "课时ID不能为空")
    @Schema(description = "课时ID", required = true)
    private Long periodId;

    @NotNull(message = "本次心跳的学习秒数不能为空")
    @Schema(description = "距上次心跳的有效学习秒数，前端每30秒上报一次", required = true)
    private Integer seconds;
}
