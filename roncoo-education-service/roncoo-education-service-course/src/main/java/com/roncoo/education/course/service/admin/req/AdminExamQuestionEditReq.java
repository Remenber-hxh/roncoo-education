package com.roncoo.education.course.service.admin.req;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 题目新增/修改（二开）
 */
@Data
@Accessors(chain = true)
public class AdminExamQuestionEditReq implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 修改时必填 */
    private Long id;
    private Long categoryId;
    private Long courseId;
    /** 1单选 2多选 3判断 */
    private Integer questionType;
    private String questionTitle;
    /** [{"key":"A","value":"..."}] */
    private String optionsJson;
    /** A 或 A,C */
    private String correctAnswer;
    private String analysis;
    private Integer difficulty;
    private Integer sort;
    private Integer statusId;
}
