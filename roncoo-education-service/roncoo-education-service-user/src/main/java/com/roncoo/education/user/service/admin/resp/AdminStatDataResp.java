package com.roncoo.education.user.service.admin.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * <p>
 * 数据统计
 * </p>
 * <p>
 * 二开：已移除商品/订单模块，原有的订单数、收入等字段一并删除。
 *
 * @author wujing
 */
@Data
@Accessors(chain = true)
@Schema(description = "数据统计")
public class AdminStatDataResp implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "员工总数")
    private Integer userSum = 0;

    @Schema(description = "课程总数")
    private Integer courseSum = 0;

}
