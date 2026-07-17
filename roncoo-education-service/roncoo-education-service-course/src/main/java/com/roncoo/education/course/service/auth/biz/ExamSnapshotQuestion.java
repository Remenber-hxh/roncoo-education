package com.roncoo.education.course.service.auth.biz;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 考试题目快照（存 exam_record.questions_json，含答案，仅服务端使用，不下发）（二开）
 */
@Data
@Accessors(chain = true)
public class ExamSnapshotQuestion implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long questionId;
    private Integer questionType;
    private String questionTitle;
    private String optionsJson;
    private String correctAnswer;
    private String analysis;
    private Integer score;
    private Integer seq;
}
