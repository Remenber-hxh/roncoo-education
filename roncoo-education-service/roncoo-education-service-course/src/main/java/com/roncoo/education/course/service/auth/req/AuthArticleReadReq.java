package com.roncoo.education.course.service.auth.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 图文课时阅读进度上报（二开新增）
 */
@Data
@Accessors(chain = true)
@Schema(description = "图文阅读进度")
public class AuthArticleReadReq implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "课时ID不能为空")
    @Schema(description = "课时ID", required = true)
    private Long periodId;

    @NotNull(message = "阅读比例不能为空")
    @Schema(description = "已阅读比例 0-100，按滚动位置计算", required = true)
    private Integer percent;

    @Schema(description = "本次停留秒数，用于判断是否达到最短阅读时长")
    private Integer staySeconds;
}
