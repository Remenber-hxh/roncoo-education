package com.roncoo.education.course.dao.impl.mapper;

import com.roncoo.education.course.dao.impl.mapper.entity.UserStudyDaily;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 学习时长日汇总 Mapper（二开，注解式）
 */
@Mapper
public interface UserStudyDailyMapper {

    /**
     * 累加当日学习秒数。
     * 依赖唯一键 uk_user_course_date，同一天同一课程重复上报时累加而非插入新行。
     * <p>
     * study_date 用数据库的 CURDATE() 而不是应用传入的日期：
     * JDBC 连接串的 serverTimezone 与数据库实际时区不一致时，应用算出的日期会整体偏移，
     * 导致按日汇总落到错误的日期上。由数据库定日期可以规避这类配置问题。
     */
    @Update("insert into user_study_daily (id, user_id, course_id, study_date, duration_sec) "
            + "values (#{id}, #{userId}, #{courseId}, CURDATE(), #{durationSec}) "
            + "on duplicate key update duration_sec = duration_sec + values(duration_sec)")
    int upsertAdd(UserStudyDaily record);

    /**
     * 某人某课程某天已累计的秒数（用于校验与调试）
     */
    @Select("select duration_sec from user_study_daily where user_id=#{userId} and course_id=#{courseId} and study_date=#{studyDate}")
    Integer getDuration(@Param("userId") Long userId, @Param("courseId") Long courseId, @Param("studyDate") Date studyDate);

    /**
     * 按人汇总（时间区间内），返回 user_id / total_sec
     */
    @Select("<script>select user_id as userId, sum(duration_sec) as totalSec from user_study_daily "
            + "<where>"
            + "<if test='beginDate != null'>and study_date &gt;= #{beginDate}</if>"
            + "<if test='endDate != null'>and study_date &lt;= #{endDate}</if>"
            + "</where>"
            + " group by user_id order by totalSec desc</script>")
    List<Map<String, Object>> sumByUser(@Param("beginDate") Date beginDate, @Param("endDate") Date endDate);

    /**
     * 按课程汇总（时间区间内），供「按模块统计」二次聚合到分类
     */
    @Select("<script>select course_id as courseId, sum(duration_sec) as totalSec from user_study_daily "
            + "<where>"
            + "<if test='beginDate != null'>and study_date &gt;= #{beginDate}</if>"
            + "<if test='endDate != null'>and study_date &lt;= #{endDate}</if>"
            + "</where>"
            + " group by course_id order by totalSec desc</script>")
    List<Map<String, Object>> sumByCourse(@Param("beginDate") Date beginDate, @Param("endDate") Date endDate);
}
