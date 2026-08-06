package com.roncoo.education.common.core.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author LHR
 */
@Getter
@AllArgsConstructor
public enum PutawayEnum {

    UP(1, "上架", ""), DOWN(0, "下架", "red");

    private Integer code;

    private String desc;

    private String color;
}
