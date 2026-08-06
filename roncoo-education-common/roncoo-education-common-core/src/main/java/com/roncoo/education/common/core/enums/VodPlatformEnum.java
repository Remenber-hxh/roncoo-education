package com.roncoo.education.common.core.enums;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 点播平台枚举
 *
 * @author fengyw
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum VodPlatformEnum {

    /**
     * 保利威
     */
    POLYV(2, "保利威", "polyv%"),

    /**
     * 百家云
     */
    BAIJY(3, "百家云(待实现)", "baijy%"),

    /**
     * 获得场景
     */
    BOKECC(4, "获得场景(待实现)", "bokecc%"),

    /**
     * 私有云（自建点播服务，需另行部署）
     */
    PRIVATEY(1, "私有云", "priy%"),

    /**
     * 本地存储（二开新增）
     * <p>
     * 视频不走第三方点播云，直接存在服务器本地磁盘，
     * 播放时由后端下发文件地址，前端用原生 video 播放。
     * 配套：LocalUploadImpl 落盘、LocalFileController 读取。
     */
    LOCAL(5, "本地存储", "local%");

    /**
     * 编码
     */
    private final Integer code;

    /**
     * 描述
     */
    private final String desc;

    /**
     * 标记
     */
    private final String tag;

    /**
     * 根据编码获取点播平台枚举
     *
     * @param code 编码
     * @return 点播平台枚举
     */
    public static VodPlatformEnum byCode(Integer code) {
        if (code == null) {
            return null;
        }

        for (VodPlatformEnum value : VodPlatformEnum.values()) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        return null;
    }
}
