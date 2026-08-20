package com.roncoo.education.user.service.admin.resp;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * <p>
 * ADMIN-用户信息
 * </p>
 *
 * @author wujing
 */
@Data
@Accessors(chain = true)
@Schema(description = "ADMIN-用户信息分页")
public class AdminUsersPageResp implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键")
    private Long id;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime gmtCreate;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "修改时间")
    private LocalDateTime gmtModified;

    @Schema(description = "状态(1:正常，0:禁用)")
    private Integer statusId;

    @Schema(description = "手机号码")
    private String mobile;

    // 二开：原本把 mobileSalt / mobilePsw 一起返回给后台前端。
    // 前端一处都没用到，却让密码哈希和盐随列表接口出到浏览器，
    // 这两个字段直接去掉。

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "用户性别(1男，2女，3保密)")
    private Integer userSex;

    @Schema(description = "用户年龄")
    private LocalDate userAge;

    @Schema(description = "用户头像")
    private String userHead;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "微信唯一ID")
    private String unionId;

    @Schema(description = "微信OpenId")
    private String openId;

    @Schema(description = "城市")
    private String city;

    @Schema(description = "省份")
    private String province;

    @Schema(description = "国家")
    private String country;

    @Schema(description = "注册来源")
    private Integer registerSource;

    // ---- 员工档案（二开）----

    @Schema(description = "工号")
    private String empNo;

    @Schema(description = "班组ID")
    private Long teamId;

    @Schema(description = "班组名称")
    private String teamName;

    @Schema(description = "项目组ID")
    private Long projectGroupId;

    @Schema(description = "项目组名称")
    private String projectGroupName;

    @Schema(description = "岗位职务")
    private String position;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "入职日期")
    private LocalDate hireDate;
}
