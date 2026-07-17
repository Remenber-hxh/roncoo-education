package com.roncoo.education.course.service.auth.resp;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;

/**
 * 交卷结果（二开）
 */
@Data
@Accessors(chain = true)
public class AuthExamSubmitResp implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer score;
    private Integer totalScore;
    private Integer passScore;
    private Boolean isPass;
    /** 未通过时是否还能补考 */
    private Boolean canRetake;
    private Integer attemptNo;
    private List<WrongItem> wrongList;

    @Data
    @Accessors(chain = true)
    public static class WrongItem implements Serializable {
        private static final long serialVersionUID = 1L;
        private Long questionId;
        private String questionTitle;
        private String yourAnswer;
        private String correctAnswer;
        private String analysis;
    }
}
