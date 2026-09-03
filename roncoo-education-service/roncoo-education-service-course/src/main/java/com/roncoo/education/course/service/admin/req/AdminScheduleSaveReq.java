package com.roncoo.education.course.service.admin.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;

/**
 * ADMIN-排课配置批量保存（二开新增）
 * <p>
 * 整表提交而不是逐行提交：管理员对着需求表一次调好几十行，
 * 逐行发请求既慢、中途失败还会留下改了一半的状态。
 *
 * @author 二开
 */
@Data
@Accessors(chain = true)
@Schema(description = "ADMIN-排课配置批量保存")
public class AdminScheduleSaveReq implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "要保存的行。只传改动过的行即可")
    private List<Item> items;

    @Data
    @Accessors(chain = true)
    @Schema(description = "排课配置行")
    public static class Item implements Serializable {

        private static final long serialVersionUID = 1L;

        private Long courseId;

        /** 为空表示不自动推送，只能手工指派 */
        private Integer pushDay;

        private Integer pushScope;

        private String pushTeamIds;

        private Integer deadlineDays;

        private Integer needSequential;
    }
}
