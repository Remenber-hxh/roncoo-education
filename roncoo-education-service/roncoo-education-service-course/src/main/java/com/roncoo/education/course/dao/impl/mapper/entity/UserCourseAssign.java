package com.roncoo.education.course.dao.impl.mapper.entity;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

/**
 * 课程指派（二开）
 */
@Data
@Accessors(chain = true)
public class UserCourseAssign implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Date gmtCreate;
    private Date gmtModified;
    private Integer statusId;
    private Long userId;
    private Long courseId;
    /** 1必修 2选修 */
    private Integer assignType;
    private Date deadline;
    /** 0未开始 1学习中 2已学完 3已通过考试 */
    private Integer finishStatus;
    private Date finishTime;
    /** 联表查询字段：课程名称 */
    private String courseName;
}
