package com.roncoo.education.user.service.admin.req;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "ADMIN-项目组分页")
public class AdminProjectGroupPageReq implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "项目组名称")
    private String groupName;

    @Schema(description = "状态(1:正常，0:停用)")
    private Integer statusId;

    @Schema(description = "当前页")
    private int pageCurrent = 1;

    @Schema(description = "每页条数")
    private int pageSize = 20;
}
