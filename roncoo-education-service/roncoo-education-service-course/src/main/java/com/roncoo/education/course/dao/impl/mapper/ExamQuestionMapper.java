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

    /**
     * 整条覆盖，每一列都无条件写入，专供批量导入使用。
     * <p>
     * 不能复用上面的 updateById：它给多数列包了 &lt;if test='xx != null'&gt;，
     * 语义是「没传的字段保持原样」，这对表单是对的（表单只提交填了的项），
     * 对导入却正好相反——导入是「文件里是什么，库里就该是什么」。
     * 出题人从库里导出、把某道题的「所属课程」清空再导回来，
     * 期望这道题变成通用题；走 updateById 那一列会被直接跳过，
     * 界面上看着改了、实际没变，而且没有任何报错。
     * <p>
     * 代价是调用方必须传完整记录。目前只有 AdminExamQuestionImportBiz 调用，
     * 它对每一列都做了解析和默认值处理，满足这个前提。
     */
    @Update("update exam_question set "
            + "status_id=#{statusId}, sort=#{sort}, category_id=#{categoryId}, course_id=#{courseId}, "
            + "chapter_id=#{chapterId}, question_type=#{questionType}, question_title=#{questionTitle}, "
            + "options_json=#{optionsJson}, correct_answer=#{correctAnswer}, analysis=#{analysis}, "
            + "difficulty=#{difficulty} where id=#{id}")
    int updateAllById(ExamQuestion record);

    /**
     * 只取查重需要的三列。导入时要判断「题库里是不是已经有一模一样的题干」，
     * 用 select * 会把几千道题的题干、选项、解析全拉进内存，没必要。
     */
    @Select("select id, course_id, question_title from exam_question")
    List<ExamQuestion> listTitles();

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
     * <p>
     * 课程条件写成「属于本课程 或 未绑定课程」，不能只写 course_id=#{courseId}：
     * 题目并不要求携带 course_id，通用题（安全常识那类）本来就是留空的。
     * 只按等值过滤的话，试卷绑了课程、题目没绑课程，一道也抽不到——
     * 冒烟测试卷就是这么被打挂的（4 道题瞬间变 0 道）。
     * 绑了别的课程的题仍然排除，避免串课。
     * <p>
     * chapterId 传了就严格只从该章抽，用于「第三章测验」这类章节小测，
     * 这里不放通用题，否则章节小测会混进无关题目。
     */
    @Select("<script>select * from exam_question where status_id=1 "
            + "<if test='categoryId != null'>and category_id=#{categoryId}</if>"
            + "<if test='courseId != null'>and (course_id=#{courseId} or course_id is null)</if>"
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
