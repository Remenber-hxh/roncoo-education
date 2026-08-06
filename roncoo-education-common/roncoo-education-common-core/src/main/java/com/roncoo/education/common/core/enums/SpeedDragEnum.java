package com.roncoo.education.common.core.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author fengyw
 */
@Getter
@AllArgsConstructor
public enum SpeedDragEnum {

    CLOSE(0, "关闭"), OPEN(1, "开启");

    private Integer code;

    private String desc;

    /**
     * 根据编码获取枚举信息
     *
     * @param code 编码
     * @return 枚举信息
     */
    public static SpeedDragEnum byCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (SpeedDragEnum val : SpeedDragEnum.values()) {
            if (val.getCode().equals(code)) {
                return val;
            }
        }
        return null;
    }

}
