package com.roncoo.education.course.service.admin.req;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;

/**
 * 试卷新增/修改（二开）。rules 不为空时全量替换组卷规则
 */
@Data
@Accessors(chain = true)
public class AdminExamPaperEditReq implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 修改时必填 */
    private Long id;
    private Long courseId;
    private String paperName;
    private Integer totalScore;
    private Integer passScore;
    private Integer durationMinutes;
    private Integer retakeLimit;
    private Integer needFinishCourse;
    private Integer sort;
    private Integer statusId;

    private List<RuleItem> rules;

    @Data
    @Accessors(chain = true)
    public static class RuleItem implements Serializable {
        private static final long serialVersionUID = 1L;
        private Long categoryId;
        private Integer questionType;
        private Integer questionCount;
        private Integer scorePerQuestion;
    }
}
