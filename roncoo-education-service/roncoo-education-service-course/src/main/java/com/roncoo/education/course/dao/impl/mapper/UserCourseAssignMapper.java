package com.roncoo.education.course.dao.impl.mapper;

import com.roncoo.education.course.dao.impl.mapper.entity.UserCourseAssign;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 课程指派 Mapper（二开，注解式）
 */
@Mapper
public interface UserCourseAssignMapper {

    @Insert("insert into user_course_assign (id, status_id, user_id, course_id, assign_type, deadline, finish_status) "
            + "values (#{id}, #{statusId}, #{userId}, #{courseId}, #{assignType}, #{deadline}, #{finishStatus})")
    int insert(UserCourseAssign record);

    @Delete("delete from user_course_assign where id=#{id}")
    int deleteById(@Param("id") Long id);

    @Select("select * from user_course_assign where user_id=#{userId} and course_id=#{courseId}")
    UserCourseAssign getByUserAndCourse(@Param("userId") Long userId, @Param("courseId") Long courseId);

    @Select("select a.*, c.course_name from user_course_assign a left join course c on a.course_id = c.id "
            + "where a.user_id=#{userId} and a.status_id=1 order by a.assign_type, a.id desc")
    List<UserCourseAssign> listByUserId(@Param("userId") Long userId);

    /**
     * 更新完成状态。
     * <p>
     * finish_time 只在真正完成（2已学完 / 3已通过考试）时写入，未完成时置空。
     * 原来无条件写 now()，结果状态刚变成「学习中」就带上了完成时间，
     * 将来出「完成时间」报表会把开始学的时间当成学完的时间。
     */
    @Update("<script>update user_course_assign set finish_status=#{finishStatus}, "
            + "<choose>"
            + "<when test='finishStatus != null and finishStatus &gt;= 2'>finish_time=now()</when>"
            + "<otherwise>finish_time=null</otherwise>"
            + "</choose>"
            + " where user_id=#{userId} and course_id=#{courseId}</script>")
    int updateFinish(@Param("userId") Long userId, @Param("courseId") Long courseId, @Param("finishStatus") Integer finishStatus);

    @Select("<script>select * from user_course_assign <where>"
            + "<if test='courseId != null'>and course_id=#{courseId}</if>"
            + "<if test='userId != null'>and user_id=#{userId}</if>"
            + "<if test='finishStatus != null'>and finish_status=#{finishStatus}</if>"
            + "</where> order by id desc limit #{offset}, #{limit}</script>")
    List<UserCourseAssign> page(@Param("courseId") Long courseId, @Param("userId") Long userId, @Param("finishStatus") Integer finishStatus,
                                @Param("offset") int offset, @Param("limit") int limit);

    @Select("<script>select count(*) from user_course_assign <where>"
            + "<if test='courseId != null'>and course_id=#{courseId}</if>"
            + "<if test='userId != null'>and user_id=#{userId}</if>"
            + "<if test='finishStatus != null'>and finish_status=#{finishStatus}</if>"
            + "</where></script>")
    int pageCount(@Param("courseId") Long courseId, @Param("userId") Long userId, @Param("finishStatus") Integer finishStatus);
}
