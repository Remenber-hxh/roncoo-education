package com.roncoo.education.user.service.admin.req;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "ADMIN-班组分页")
public class AdminTeamPageReq implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "班组名称")
    private String teamName;

    @Schema(description = "状态(1:正常，0:停用)")
    private Integer statusId;

    @Schema(description = "当前页")
    private int pageCurrent = 1;

    @Schema(description = "每页条数")
    private int pageSize = 20;
}
