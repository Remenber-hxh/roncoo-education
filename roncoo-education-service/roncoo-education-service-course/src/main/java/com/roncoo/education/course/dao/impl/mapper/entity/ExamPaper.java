package com.roncoo.education.course.dao.impl.mapper.entity;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

/**
 * 试卷定义（二开）
 */
@Data
@Accessors(chain = true)
public class ExamPaper implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Date gmtCreate;
    private Date gmtModified;
    private Integer statusId;
    private Integer sort;
    /** 关联课程 */
    private Long courseId;
    private String paperName;
    private Integer totalScore;
    private Integer passScore;
    private Integer durationMinutes;
    /** 允许补考次数 */
    private Integer retakeLimit;
    /** 是否要求学完课程 */
    private Integer needFinishCourse;
}
