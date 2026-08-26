package com.roncoo.education.course.dao.impl.mapper;

import com.roncoo.education.course.dao.impl.mapper.entity.ExamQuestion;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 题库 Mapper（二开，注解式）
 */
@Mapper
public interface ExamQuestionMapper {

    @Insert("insert into exam_question (id, status_id, sort, category_id, course_id, chapter_id, question_type, question_title, options_json, correct_answer, analysis, difficulty) "
            + "values (#{id}, #{statusId}, #{sort}, #{categoryId}, #{courseId}, #{chapterId}, #{questionType}, #{questionTitle}, #{optionsJson}, #{correctAnswer}, #{analysis}, #{difficulty})")
    int insert(ExamQuestion record);

    /**
     * 注意：chapter_id 是无条件更新的，不像其它字段那样包在 &lt;if&gt; 里。
     * <p>
     * 包了就永远清不掉——出题人把题从「第三章」改回「不限章节」时传的是 null，
     * &lt;if test='chapterId != null'&gt; 会直接跳过这一列，界面上看着改了、
     * 存进去还是旧章节。
     * 代价是调用方必须传完整记录，目前只有 AdminExamBiz.questionUpdate 调用它，
     * 而它是从表单的完整 DTO 构造的，满足这个前提。
     */
    @Update("<script>update exam_question <set>"
            + "<if test='statusId != null'>status_id=#{statusId},</if>"
            + "<if test='sort != null'>sort=#{sort},</if>"
            + "<if test='categoryId != null'>category_id=#{categoryId},</if>"
            + "<if test='courseId != null'>course_id=#{courseId},</if>"
            + "chapter_id=#{chapterId},"
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
            + "<if test='courseId != null'>and course_id=#{courseId}</if>"
            + "<if test='chapterId != null'>and chapter_id=#{chapterId}</if>"
            + "<if test='questionType != null'>and question_type=#{questionType}</if>"
            + "<if test='keyword != null and keyword != \"\"'>and question_title like concat('%', #{keyword}, '%')</if>"
            + "</where> order by id desc limit #{offset}, #{limit}</script>")
    List<ExamQuestion> page(@Param("categoryId") Long categoryId, @Param("courseId") Long courseId,
                            @Param("chapterId") Long chapterId, @Param("questionType") Integer questionType,
                            @Param("keyword") String keyword, @Param("offset") int offset, @Param("limit") int limit);

    @Select("<script>select count(*) from exam_question <where>"
            + "<if test='categoryId != null'>and category_id=#{categoryId}</if>"
            + "<if test='courseId != null'>and course_id=#{courseId}</if>"
            + "<if test='chapterId != null'>and chapter_id=#{chapterId}</if>"
            + "<if test='questionType != null'>and question_type=#{questionType}</if>"
            + "<if test='keyword != null and keyword != \"\"'>and question_title like concat('%', #{keyword}, '%')</if>"
            + "</where></script>")
    int pageCount(@Param("categoryId") Long categoryId, @Param("courseId") Long courseId,
                  @Param("chapterId") Long chapterId, @Param("questionType") Integer questionType,
                  @Param("keyword") String keyword);

    /**
     * 按组卷规则随机抽题。
     * chapterId 传了就只从该章抽，用于「第三章测验」这类章节小测。
     */
    @Select("<script>select * from exam_question where status_id=1 "
            + "<if test='categoryId != null'>and category_id=#{categoryId}</if>"
            + "<if test='courseId != null'>and course_id=#{courseId}</if>"
            + "<if test='chapterId != null'>and chapter_id=#{chapterId}</if>"
            + "<if test='questionType != null'>and question_type=#{questionType}</if>"
            + " order by rand() limit #{count}</script>")
    List<ExamQuestion> randomPick(@Param("categoryId") Long categoryId, @Param("courseId") Long courseId,
                                  @Param("chapterId") Long chapterId, @Param("questionType") Integer questionType,
                                  @Param("count") int count);

    /**
     * 按章节统计题量，供后台显示「这一章有几道题」，
     * 也用于组卷前校验题量够不够
     */
    @Select("select chapter_id as chapterId, count(*) as cnt from exam_question "
            + "where status_id=1 and course_id=#{courseId} group by chapter_id")
    List<java.util.Map<String, Object>> countByChapter(@Param("courseId") Long courseId);
}
