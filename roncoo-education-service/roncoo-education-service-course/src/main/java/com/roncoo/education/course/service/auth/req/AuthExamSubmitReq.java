package com.roncoo.education.course.service.auth.req;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;

/**
 * 交卷（二开）
 */
@Data
@Accessors(chain = true)
public class AuthExamSubmitReq implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long recordId;
    private List<AnswerItem> answers;

    @Data
    @Accessors(chain = true)
    public static class AnswerItem implements Serializable {
        private static final long serialVersionUID = 1L;
        private Long questionId;
        /** A 或 A,C */
        private String answer;
    }
}
