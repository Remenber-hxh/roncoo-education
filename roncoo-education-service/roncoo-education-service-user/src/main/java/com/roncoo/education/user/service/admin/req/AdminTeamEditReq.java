package com.roncoo.education.user.service.admin.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * <p>
 * ADMIN-班组字典
 * </p>
 *
 * @author 二开
 */
@Data
@Accessors(chain = true)
@Schema(description = "ADMIN-班组修改")
public class AdminTeamEditReq implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "主键不能为空")
    @Schema(description = "主键")
    private Long id;

    @NotBlank(message = "班组名称不能为空")
    @Size(max = 64, message = "班组名称不能超过64个字符")
    @Schema(description = "班组名称")
    private String teamName;

    @Size(max = 255, message = "备注不能超过255个字符")
    @Schema(description = "备注(职责范围)")
    private String remark;

    @Schema(description = "状态(1:正常，0:停用)")
    private Integer statusId;

    @Schema(description = "排序")
    private Integer sort;
}
