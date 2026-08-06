package com.roncoo.education.common.core.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author LHR
 */
@Getter
@AllArgsConstructor
public enum FreeEnum {
    
    CHARGE(0, "收费", "red"),
    FREE(1, "免费", "");

    private Integer code;

    private String desc;

    private String color;
}
