package com.roncoo.education.course.dao.impl.mapper.entity;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

/**
 * 考试记录（二开）
 */
@Data
@Accessors(chain = true)
public class ExamRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Date gmtCreate;
    private Date gmtModified;
    private Integer statusId;
    private Long userId;
    private Long paperId;
    private Long courseId;
    /** 第几次: 1正考 2补考... */
    private Integer attemptNo;
    private Date startTime;
    private Date submitTime;
    private Integer score;
    private Integer isPass;
    /** 1进行中 2已交卷 3超时交卷 */
    private Integer examStatus;
    /** 题目快照(含正确答案,不下发) */
    private String questionsJson;
    /** 作答快照 */
    private String answersJson;
    /** 联表查询字段：试卷名称 */
    private String paperName;
}
