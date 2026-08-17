package com.roncoo.education.common.core.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author wujing
 */
@Getter
@AllArgsConstructor
public enum PeriodTypeEnum {

    RESOURCE(10, "资源"),
    LIVE(20, "直播"),

    /**
     * 图文（二开新增）
     * <p>
     * 正文用富文本直接存在 course_chapter_period.content 上，不依赖 resource 表。
     * 入职引导类内容（企业文化、员工手册、薪酬制度）用这个类型，
     * 不用先拍视频就能上线，改内容也不用重新传文件。
     */
    ARTICLE(30, "图文");

    private Integer code;

    private String desc;

}
