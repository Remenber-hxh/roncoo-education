package com.roncoo.education.course.service.admin.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;

/**
 * ADMIN-批量催办入参（二开）
 *
 * @author 二开
 */
@Data
@Accessors(chain = true)
@Schema(description = "ADMIN-批量催办")
public class AdminRemindReq implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "true=催办当前全部逾期人员，由服务端重新计算，忽略 items")
    private Boolean all;

    @Schema(description = "指定催办的人员与课程。all 为 true 时不用传")
    private List<Item> items;

    @Schema(description = "附言，会拼在消息正文后面。可不填")
    private String remark;

    @Data
    @Accessors(chain = true)
    @Schema(description = "催办对象")
    public static class Item implements Serializable {

        private static final long serialVersionUID = 1L;

        private Long userId;

        private Long courseId;
    }
}
