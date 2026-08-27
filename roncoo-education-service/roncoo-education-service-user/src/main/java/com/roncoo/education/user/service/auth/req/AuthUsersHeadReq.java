package com.roncoo.education.user.service.auth.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * AUTH-更新头像（二开）
 * <p>
 * 单独开一个入参，不复用 AuthUsersReq：
 * 后者的昵称、性别、生日都是必填，员工只想换张头像却因为没填过生日被拒，
 * 而且他在头像这个操作里根本没机会补这些字段。
 *
 * @author 二开
 */
@Data
@Accessors(chain = true)
@Schema(description = "AUTH-更新头像")
public class AuthUsersHeadReq implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "头像地址不能为空")
    @Schema(description = "头像地址")
    private String userHead;
}
