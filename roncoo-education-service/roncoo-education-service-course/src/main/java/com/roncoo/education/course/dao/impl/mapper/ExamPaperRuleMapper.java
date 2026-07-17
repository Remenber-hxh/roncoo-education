package com.roncoo.education.course.dao.impl.mapper;

import com.roncoo.education.course.dao.impl.mapper.entity.ExamPaperRule;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 组卷规则 Mapper（二开，注解式）
 */
@Mapper
public interface ExamPaperRuleMapper {

    @Insert("insert into exam_paper_rule (id, status_id, sort, paper_id, category_id, question_type, question_count, score_per_question) "
            + "values (#{id}, #{statusId}, #{sort}, #{paperId}, #{categoryId}, #{questionType}, #{questionCount}, #{scorePerQuestion})")
    int insert(ExamPaperRule record);

    @Delete("delete from exam_paper_rule where id=#{id}")
    int deleteById(@Param("id") Long id);

    @Delete("delete from exam_paper_rule where paper_id=#{paperId}")
    int deleteByPaperId(@Param("paperId") Long paperId);

    @Select("select * from exam_paper_rule where paper_id=#{paperId} and status_id=1 order by sort, id")
    List<ExamPaperRule> listByPaperId(@Param("paperId") Long paperId);
}
