package com.roncoo.education.user.service.auth.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * AUTH-员工自助修改密码（二开新增）
 * <p>
 * 两个密码都走 RSA 加密传输，与登录、注册保持一致，
 * 不让明文密码出现在请求体和访问日志里。
 *
 * @author 二开
 */
@Data
@Accessors(chain = true)
@Schema(description = "AUTH-修改密码")
public class AuthUsersPswReq implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "请输入原密码")
    @Schema(description = "原密码，RSA 加密")
    private String oldPwdEncrypt;

    @NotBlank(message = "请输入新密码")
    @Schema(description = "新密码，RSA 加密")
    private String newPwdEncrypt;
}
