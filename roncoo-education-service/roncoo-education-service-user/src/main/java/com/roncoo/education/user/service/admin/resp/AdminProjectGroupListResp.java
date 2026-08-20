package com.roncoo.education.user.service.admin.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * <p>
 * ADMIN-项目组字典（下拉选择用的精简结构）
 * </p>
 *
 * @author 二开
 */
@Data
@Accessors(chain = true)
@Schema(description = "ADMIN-项目组下拉列表")
public class AdminProjectGroupListResp implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "项目组名称")
    private String groupName;
}
