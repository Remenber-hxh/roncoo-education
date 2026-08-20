package com.roncoo.education.user.service.admin.req;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * <p>
 * ADMIN-员工档案编辑
 * </p>
 * 只管档案四件套，不碰手机号和密码——那两样属于账号，改动影响登录。
 *
 * @author 二开
 */
@Data
@Accessors(chain = true)
@Schema(description = "ADMIN-员工档案编辑")
public class AdminUsersProfileEditReq implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "用户ID不能为空")
    @Schema(description = "用户ID")
    private Long id;

    // 公司现有工号是 1~98 的纯数字流水号，不连续（离职留空），
    // 故只校验「纯数字」和「唯一」，不校验连续性和位数
    @Pattern(regexp = "^\\d*$", message = "工号只能是数字")
    @Size(max = 64, message = "工号过长")
    @Schema(description = "工号")
    private String empNo;

    // 班组和项目组是两个正交维度：班组=干什么活，项目组=在哪个点上班。
    // 例：仓管属职能组，人却在无锡国际会议中心。
    @Schema(description = "班组ID")
    private Long teamId;

    @Schema(description = "项目组ID")
    private Long projectGroupId;

    @Size(max = 64, message = "岗位职务不能超过64个字符")
    @Schema(description = "岗位职务")
    private String position;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "入职日期")
    private LocalDate hireDate;
}
