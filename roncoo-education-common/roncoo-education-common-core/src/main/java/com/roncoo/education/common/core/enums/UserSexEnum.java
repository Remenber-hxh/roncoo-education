package com.roncoo.education.common.core.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author wujing
 */
@Getter
@AllArgsConstructor
public enum UserSexEnum {

    MALE(1, "男", "green"), FEMALE(2, "女", "red"), SECRET(3, "保密", "orange");

    private Integer code;

    private String desc;

    private String color;
}
