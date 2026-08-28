package com.roncoo.education.course.service.admin.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 题库导入结果。
 * <p>
 * 与员工导入一样逐行处理、逐行反馈，不做「一行出错整批回滚」：
 * 出题人一次会贴几十上百道题，为某一行答案填错卡住其余全部不合理。
 * 失败的行照着清单在 Excel 里改完再导一次即可——
 * 带 ID 的行是更新，不会重复建题。
 *
 * @author 二开
 */
@Data
@Accessors(chain = true)
@Schema(description = "ADMIN-题库导入结果")
public class AdminExamQuestionImportResp implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "读到的数据行数(不含表头)")
    private Integer totalCount = 0;

    @Schema(description = "新增题目数")
    private Integer createdCount = 0;

    @Schema(description = "更新题目数")
    private Integer updatedCount = 0;

    @Schema(description = "失败行数")
    private Integer failedCount = 0;

    @Schema(description = "失败明细")
    private List<RowError> errors = new ArrayList<>();

    @Data
    @Accessors(chain = true)
    @Schema(description = "失败行")
    public static class RowError implements Serializable {

        private static final long serialVersionUID = 1L;

        @Schema(description = "Excel 行号(含表头，与打开文件看到的行号一致)")
        private Integer rowNum;

        @Schema(description = "题干(截断显示)")
        private String questionTitle;

        @Schema(description = "失败原因")
        private String reason;
    }
}
