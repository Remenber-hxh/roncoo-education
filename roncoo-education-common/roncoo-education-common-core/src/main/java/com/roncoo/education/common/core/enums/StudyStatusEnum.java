package com.roncoo.education.common.core.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author wujing
 */
@Getter
@AllArgsConstructor
public enum StudyStatusEnum {

    STUDY(1, "学习中", ""), PAUSE(2, "暂停", "red");

    private Integer code;

    private String desc;

    private String color;

}
