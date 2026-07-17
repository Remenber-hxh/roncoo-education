package com.roncoo.education.course.dao.impl.mapper;

import com.roncoo.education.course.dao.impl.mapper.entity.ExamRecord;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 考试记录 Mapper（二开，注解式）
 */
@Mapper
public interface ExamRecordMapper {

    @Insert("insert into exam_record (id, status_id, user_id, paper_id, course_id, attempt_no, start_time, exam_status, questions_json) "
            + "values (#{id}, #{statusId}, #{userId}, #{paperId}, #{courseId}, #{attemptNo}, #{startTime}, #{examStatus}, #{questionsJson})")
    int insert(ExamRecord record);

    @Update("update exam_record set submit_time=#{submitTime}, score=#{score}, is_pass=#{isPass}, exam_status=#{examStatus}, answers_json=#{answersJson} where id=#{id}")
    int updateSubmit(ExamRecord record);

    @Select("select * from exam_record where id=#{id}")
    ExamRecord getById(@Param("id") Long id);

    /** 已交卷次数 */
    @Select("select count(*) from exam_record where user_id=#{userId} and paper_id=#{paperId} and exam_status in (2,3)")
    int countSubmitted(@Param("userId") Long userId, @Param("paperId") Long paperId);

    /** 是否已通过 */
    @Select("select count(*) from exam_record where user_id=#{userId} and paper_id=#{paperId} and is_pass=1")
    int countPassed(@Param("userId") Long userId, @Param("paperId") Long paperId);

    /** 进行中的考试 */
    @Select("select * from exam_record where user_id=#{userId} and paper_id=#{paperId} and exam_status=1 order by id desc limit 1")
    ExamRecord getOngoing(@Param("userId") Long userId, @Param("paperId") Long paperId);

    /** 我的成绩列表(不含大字段, 联表带试卷名) */
    @Select("select r.id, r.gmt_create, r.gmt_modified, r.status_id, r.user_id, r.paper_id, r.course_id, r.attempt_no, r.start_time, r.submit_time, r.score, r.is_pass, r.exam_status, p.paper_name "
            + "from exam_record r left join exam_paper p on r.paper_id = p.id where r.user_id=#{userId} order by r.id desc limit 100")
    List<ExamRecord> listByUserId(@Param("userId") Long userId);

    @Select("<script>select id, gmt_create, gmt_modified, status_id, user_id, paper_id, course_id, attempt_no, start_time, submit_time, score, is_pass, exam_status from exam_record <where>"
            + "<if test='userId != null'>and user_id=#{userId}</if>"
            + "<if test='paperId != null'>and paper_id=#{paperId}</if>"
            + "<if test='isPass != null'>and is_pass=#{isPass}</if>"
            + "</where> order by id desc limit #{offset}, #{limit}</script>")
    List<ExamRecord> page(@Param("userId") Long userId, @Param("paperId") Long paperId, @Param("isPass") Integer isPass,
                          @Param("offset") int offset, @Param("limit") int limit);

    @Select("<script>select count(*) from exam_record <where>"
            + "<if test='userId != null'>and user_id=#{userId}</if>"
            + "<if test='paperId != null'>and paper_id=#{paperId}</if>"
            + "<if test='isPass != null'>and is_pass=#{isPass}</if>"
            + "</where></script>")
    int pageCount(@Param("userId") Long userId, @Param("paperId") Long paperId, @Param("isPass") Integer isPass);
}
