package com.roncoo.education.user.service.admin.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * <p>
 * ADMIN-项目组字典
 * </p>
 *
 * @author 二开
 */
@Data
@Accessors(chain = true)
@Schema(description = "ADMIN-项目组添加")
public class AdminProjectGroupSaveReq implements Serializable {

    private static final long serialVersionUID = 1L;

    // 项目组名称是统计口径的关键，库里有唯一索引，这里做长度与非空的前置校验
    @NotBlank(message = "项目组名称不能为空")
    @Size(max = 64, message = "项目组名称不能超过64个字符")
    @Schema(description = "项目组名称")
    private String groupName;

    @Size(max = 255, message = "备注不能超过255个字符")
    @Schema(description = "备注(职责范围)")
    private String remark;

    @Schema(description = "状态(1:正常，0:停用)")
    private Integer statusId = 1;

    @Schema(description = "排序")
    private Integer sort = 0;
}
