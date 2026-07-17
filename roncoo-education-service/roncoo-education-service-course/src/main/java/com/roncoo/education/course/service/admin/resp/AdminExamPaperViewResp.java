package com.roncoo.education.course.service.admin.resp;

import com.roncoo.education.course.dao.impl.mapper.entity.ExamPaper;
import com.roncoo.education.course.dao.impl.mapper.entity.ExamPaperRule;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;

/**
 * 试卷详情（含组卷规则）（二开）
 */
@Data
@Accessors(chain = true)
public class AdminExamPaperViewResp implements Serializable {

    private static final long serialVersionUID = 1L;

    private ExamPaper paper;
    private List<ExamPaperRule> rules;
}
