package com.roncoo.education.user.service.admin.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 导入结果。
 * <p>
 * 不做「一行出错整批回滚」：HR 那份表里本来就有个别脏数据
 * （王黎佳手机号 12 位、傅强没填入职时间），
 * 为一两行卡住其余 40 人不合理。改为逐行处理、逐行反馈，
 * 管理员照着失败清单改完再导一次即可（已存在的按工号更新，不会重复建号）。
 *
 * @author 二开
 */
@Data
@Accessors(chain = true)
@Schema(description = "ADMIN-员工导入结果")
public class AdminUsersImportResp implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "读到的数据行数(不含表头)")
    private Integer totalCount = 0;

    @Schema(description = "新建账号数")
    private Integer createdCount = 0;

    @Schema(description = "更新档案数")
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

        @Schema(description = "姓名")
        private String nickname;

        @Schema(description = "工号")
        private String empNo;

        @Schema(description = "失败原因")
        private String reason;
    }
}
