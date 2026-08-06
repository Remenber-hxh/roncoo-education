package com.roncoo.education.common.core.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author YZJ
 */
@Getter
@AllArgsConstructor
public enum FileTypeEnum {

    VIDEO(1, "视频"), FILE(2, "文件");

    private Integer code;

    private String desc;

}
