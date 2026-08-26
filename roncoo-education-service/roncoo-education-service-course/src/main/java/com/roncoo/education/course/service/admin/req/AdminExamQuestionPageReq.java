package com.roncoo.education.course.service.admin.req;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 题库分页查询（二开）
 */
@Data
@Accessors(chain = true)
public class AdminExamQuestionPageReq implements Serializable {

    private static final long serialVersionUID = 1L;

    private int pageCurrent = 1;
    private int pageSize = 20;
    private Long categoryId;
    /** 按课程筛选题库 */
    private Long courseId;
    /** 按章节筛选题库，为空表示不限 */
    private Long chapterId;
    private Integer questionType;
    private String keyword;
}
