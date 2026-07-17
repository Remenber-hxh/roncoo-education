package com.roncoo.education.course.dao.impl.mapper;

import com.roncoo.education.course.dao.impl.mapper.entity.ExamPaper;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 试卷 Mapper（二开，注解式）
 */
@Mapper
public interface ExamPaperMapper {

    @Insert("insert into exam_paper (id, status_id, sort, course_id, paper_name, total_score, pass_score, duration_minutes, retake_limit, need_finish_course) "
            + "values (#{id}, #{statusId}, #{sort}, #{courseId}, #{paperName}, #{totalScore}, #{passScore}, #{durationMinutes}, #{retakeLimit}, #{needFinishCourse})")
    int insert(ExamPaper record);

    @Update("<script>update exam_paper <set>"
            + "<if test='statusId != null'>status_id=#{statusId},</if>"
            + "<if test='sort != null'>sort=#{sort},</if>"
            + "<if test='courseId != null'>course_id=#{courseId},</if>"
            + "<if test='paperName != null'>paper_name=#{paperName},</if>"
            + "<if test='totalScore != null'>total_score=#{totalScore},</if>"
            + "<if test='passScore != null'>pass_score=#{passScore},</if>"
            + "<if test='durationMinutes != null'>duration_minutes=#{durationMinutes},</if>"
            + "<if test='retakeLimit != null'>retake_limit=#{retakeLimit},</if>"
            + "<if test='needFinishCourse != null'>need_finish_course=#{needFinishCourse},</if>"
            + "</set> where id=#{id}</script>")
    int updateById(ExamPaper record);

    @Delete("delete from exam_paper where id=#{id}")
    int deleteById(@Param("id") Long id);

    @Select("select * from exam_paper where id=#{id}")
    ExamPaper getById(@Param("id") Long id);

    @Select("<script>select * from exam_paper <where>"
            + "<if test='courseId != null'>and course_id=#{courseId}</if>"
            + "<if test='keyword != null and keyword != \"\"'>and paper_name like concat('%', #{keyword}, '%')</if>"
            + "</where> order by id desc limit #{offset}, #{limit}</script>")
    List<ExamPaper> page(@Param("courseId") Long courseId, @Param("keyword") String keyword,
                         @Param("offset") int offset, @Param("limit") int limit);

    @Select("<script>select count(*) from exam_paper <where>"
            + "<if test='courseId != null'>and course_id=#{courseId}</if>"
            + "<if test='keyword != null and keyword != \"\"'>and paper_name like concat('%', #{keyword}, '%')</if>"
            + "</where></script>")
    int pageCount(@Param("courseId") Long courseId, @Param("keyword") String keyword);

    @Select("select * from exam_paper where course_id=#{courseId} and status_id=1 order by id desc")
    List<ExamPaper> listByCourseId(@Param("courseId") Long courseId);
}
