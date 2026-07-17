package com.roncoo.education.course.dao.impl.mapper.entity;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

/**
 * 考试题库（二开）
 */
@Data
@Accessors(chain = true)
public class ExamQuestion implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Date gmtCreate;
    private Date gmtModified;
    private Integer statusId;
    private Integer sort;
    /** 所属课程分类/模块 */
    private Long categoryId;
    /** 关联课程(可空=模块通用题) */
    private Long courseId;
    /** 1单选 2多选 3判断 */
    private Integer questionType;
    /** 题干 */
    private String questionTitle;
    /** 选项JSON [{"key":"A","value":"..."}] */
    private String optionsJson;
    /** 正确答案 如 A 或 A,C */
    private String correctAnswer;
    /** 错题解析 */
    private String analysis;
    /** 1易 2中 3难 */
    private Integer difficulty;
}
