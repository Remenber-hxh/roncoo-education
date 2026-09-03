package com.roncoo.education.course.dao.impl.mapper;

import com.roncoo.education.course.dao.impl.mapper.entity.UserCourseAssign;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 学习统计 Mapper（二开，注解式）
 * <p>
 * 看板要的都是聚合数，不能把明细捞到内存里再算——
 * 员工数×课程数×课时数很快就上万行，看板还要频繁刷新。
 * 这里每个方法都对应看板上的一块，返回的行数与员工数或课程数同量级。
 *
 * @author 二开
 */
@Mapper
public interface StatMapper {

    /**
     * 各课程的已发布课时数，用来判断一门课「学完」需要完成几个课时。
     * <p>
     * 只算 status_id=1：禁用的课时员工看不到，把它算进分母会导致
     * 所有人的完成率永远差一截，而且没人知道差在哪。
     */
    @Select("select course_id as courseId, count(*) as cnt from course_chapter_period "
            + "where status_id = 1 group by course_id")
    List<Map<String, Object>> periodCountByCourse();

    /**
     * 每人每课已完成的课时数与已接触的课时数。
     * <p>
     * progress 是 decimal，用 &gt;= 100 而不是 = 100：视频进度按比例算出来
     * 可能是 100.00 之外的值（早期数据里出现过 100.01）。
     */
    @Select("select user_id as userId, course_id as courseId, "
            + "sum(case when progress >= 100 then 1 else 0 end) as doneCount, "
            + "count(*) as touchedCount "
            + "from user_study group by user_id, course_id")
    List<Map<String, Object>> studyByUserCourse();

    /**
     * 通过考试的(人,课)对。
     * <p>
     * 只认已交卷的记录（exam_status 2 已交卷 / 3 超时交卷）；
     * 进行中的记录 is_pass 还是空，算进来会让通过数虚高。
     */
    @Select("select user_id as userId, course_id as courseId from exam_record "
            + "where exam_status in (2, 3) and is_pass = 1 and course_id is not null "
            + "group by user_id, course_id")
    List<Map<String, Object>> passedByUserCourse();

    /**
     * 按日汇总学习时长与学习人数，供趋势图使用。
     * <p>
     * 没有学习记录的日期这里不会出现，前端要按日期轴补零，
     * 否则折线会把中间空掉的几天直接连过去，看上去像是天天都在学。
     */
    @Select("<script>select study_date as studyDate, sum(duration_sec) as totalSec, "
            + "count(distinct user_id) as userCount "
            + "from user_study_daily where study_date &gt;= #{beginDate} and study_date &lt;= #{endDate} "
            + "and user_id in <foreach item='id' collection='userIds' open='(' separator=',' close=')'>#{id}</foreach> "
            + "group by study_date order by study_date</script>")
    List<Map<String, Object>> sumByDate(@Param("beginDate") Date beginDate, @Param("endDate") Date endDate,
                                        @Param("userIds") List<Long> userIds);

    /**
     * 区间内有过学习记录的人数。
     * <p>
     * 必须 count(distinct user_id)，不能把 {@link #sumByDate} 每天的人数相加——
     * 同一个人学了三天会被算成三个人，活跃人数轻易超过员工总数。
     * <p>
     * 同样只统计在册员工：库里还留着 roncoo 自带的演示账号和已停用的人，
     * 不过滤会出现「42 名员工里有 44 人活跃」这种自相矛盾的数。
     */
    @Select("<script>select count(distinct user_id) from user_study_daily "
            + "where study_date &gt;= #{beginDate} and study_date &lt;= #{endDate} "
            + "and user_id in <foreach item='id' collection='userIds' open='(' separator=',' close=')'>#{id}</foreach></script>")
    Integer countActiveUsers(@Param("beginDate") Date beginDate, @Param("endDate") Date endDate,
                             @Param("userIds") List<Long> userIds);

    /**
     * 全部有效的课程指派。看板要按班组、按课程反复归组，
     * 分页取没有意义，一次全量取回在内存里分。
     */
    @Select("select * from user_course_assign where status_id = 1")
    List<UserCourseAssign> listAllAssign();

    /**
     * 更新一门课的排课配置。
     * <p>
     * 五列全部无条件写入，不用 {@code updateByPrimaryKeySelective}——
     * 那个对 null 字段会跳过，于是「把推送天数清空、改回手工指派」这个操作
     * 永远存不进去，界面上看着改了、实际没变，且没有任何报错。
     */
    @Update("update course set push_day = #{pushDay}, push_scope = #{pushScope}, "
            + "push_team_ids = #{pushTeamIds}, deadline_days = #{deadlineDays}, "
            + "need_sequential = #{needSequential} where id = #{courseId}")
    int updateSchedule(@Param("courseId") Long courseId,
                       @Param("pushDay") Integer pushDay,
                       @Param("pushScope") Integer pushScope,
                       @Param("pushTeamIds") String pushTeamIds,
                       @Param("deadlineDays") Integer deadlineDays,
                       @Param("needSequential") Integer needSequential);
}
