package com.roncoo.education.course.service.auth.resp;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 开始考试返回（不含答案）（二开）
 */
@Data
@Accessors(chain = true)
public class AuthExamStartResp implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long recordId;
    private String paperName;
    private Integer durationMinutes;
    private Integer totalScore;
    private Integer passScore;
    private Integer attemptNo;
    private Date startTime;
    private List<QuestionItem> questions;

    @Data
    @Accessors(chain = true)
    public static class QuestionItem implements Serializable {
        private static final long serialVersionUID = 1L;
        private Long questionId;
        /** 1单选 2多选 3判断 */
        private Integer questionType;
        private String questionTitle;
        private String optionsJson;
        private Integer score;
        private Integer seq;
    }
}
