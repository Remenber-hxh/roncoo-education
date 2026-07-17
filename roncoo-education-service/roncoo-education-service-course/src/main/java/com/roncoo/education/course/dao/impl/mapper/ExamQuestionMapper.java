package com.roncoo.education.course.dao.impl.mapper;

import com.roncoo.education.course.dao.impl.mapper.entity.ExamQuestion;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 题库 Mapper（二开，注解式）
 */
@Mapper
public interface ExamQuestionMapper {

    @Insert("insert into exam_question (id, status_id, sort, category_id, course_id, question_type, question_title, options_json, correct_answer, analysis, difficulty) "
            + "values (#{id}, #{statusId}, #{sort}, #{categoryId}, #{courseId}, #{questionType}, #{questionTitle}, #{optionsJson}, #{correctAnswer}, #{analysis}, #{difficulty})")
    int insert(ExamQuestion record);

    @Update("<script>update exam_question <set>"
            + "<if test='statusId != null'>status_id=#{statusId},</if>"
            + "<if test='sort != null'>sort=#{sort},</if>"
            + "<if test='categoryId != null'>category_id=#{categoryId},</if>"
            + "<if test='courseId != null'>course_id=#{courseId},</if>"
            + "<if test='questionType != null'>question_type=#{questionType},</if>"
            + "<if test='questionTitle != null'>question_title=#{questionTitle},</if>"
            + "<if test='optionsJson != null'>options_json=#{optionsJson},</if>"
            + "<if test='correctAnswer != null'>correct_answer=#{correctAnswer},</if>"
            + "<if test='analysis != null'>analysis=#{analysis},</if>"
            + "<if test='difficulty != null'>difficulty=#{difficulty},</if>"
            + "</set> where id=#{id}</script>")
    int updateById(ExamQuestion record);

    @Delete("delete from exam_question where id=#{id}")
    int deleteById(@Param("id") Long id);

    @Select("select * from exam_question where id=#{id}")
    ExamQuestion getById(@Param("id") Long id);

    @Select("<script>select * from exam_question <where>"
            + "<if test='categoryId != null'>and category_id=#{categoryId}</if>"
            + "<if test='questionType != null'>and question_type=#{questionType}</if>"
            + "<if test='keyword != null and keyword != \"\"'>and question_title like concat('%', #{keyword}, '%')</if>"
            + "</where> order by id desc limit #{offset}, #{limit}</script>")
    List<ExamQuestion> page(@Param("categoryId") Long categoryId, @Param("questionType") Integer questionType,
                            @Param("keyword") String keyword, @Param("offset") int offset, @Param("limit") int limit);

    @Select("<script>select count(*) from exam_question <where>"
            + "<if test='categoryId != null'>and category_id=#{categoryId}</if>"
            + "<if test='questionType != null'>and question_type=#{questionType}</if>"
            + "<if test='keyword != null and keyword != \"\"'>and question_title like concat('%', #{keyword}, '%')</if>"
            + "</where></script>")
    int pageCount(@Param("categoryId") Long categoryId, @Param("questionType") Integer questionType, @Param("keyword") String keyword);

    /**
     * 按组卷规则随机抽题
     */
    @Select("<script>select * from exam_question where status_id=1 "
            + "<if test='categoryId != null'>and category_id=#{categoryId}</if>"
            + "<if test='questionType != null'>and question_type=#{questionType}</if>"
            + " order by rand() limit #{count}</script>")
    List<ExamQuestion> randomPick(@Param("categoryId") Long categoryId, @Param("questionType") Integer questionType, @Param("count") int count);
}
