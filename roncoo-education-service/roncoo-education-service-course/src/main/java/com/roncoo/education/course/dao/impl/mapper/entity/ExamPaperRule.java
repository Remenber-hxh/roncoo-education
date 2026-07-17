package com.roncoo.education.course.dao.impl.mapper.entity;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

/**
 * 组卷规则（二开）
 */
@Data
@Accessors(chain = true)
public class ExamPaperRule implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Date gmtCreate;
    private Date gmtModified;
    private Integer statusId;
    private Integer sort;
    private Long paperId;
    /** 从哪个模块抽(空=全库) */
    private Long categoryId;
    /** 抽哪种题型(空=不限) */
    private Integer questionType;
    private Integer questionCount;
    private Integer scorePerQuestion;
}
