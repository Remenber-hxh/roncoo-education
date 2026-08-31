package com.roncoo.education.course.service.admin.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * ADMIN-批量催办结果（二开）
 * <p>
 * 三个数分开返回而不是只回一句「操作成功」：管理员点了催办 30 人，
 * 结果只发出 5 条，得让他知道另外 25 条是「刚催过」还是「已经不逾期了」，
 * 否则会以为功能坏了，反复点。
 *
 * @author 二开
 */
@Data
@Accessors(chain = true)
@Schema(description = "ADMIN-批量催办结果")
public class AdminRemindResp implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "实际发出的催办条数")
    private Integer sent = 0;

    @Schema(description = "因近期已催过而跳过的条数")
    private Integer skipped = 0;

    @Schema(description = "已不再逾期或员工已停用，无需催办的条数")
    private Integer invalid = 0;

    @Schema(description = "给管理员看的结果说明")
    private String message;
}
