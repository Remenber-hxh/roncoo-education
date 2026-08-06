package com.roncoo.education.common.core.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author wujing
 */
@Getter
@AllArgsConstructor
public enum LoginTypeEnum {

    PASSWORD(1, "密码登录"),
    WX_WEB_AUTH(21, "微信网页授权");

    private Integer code;

    private String desc;

}
