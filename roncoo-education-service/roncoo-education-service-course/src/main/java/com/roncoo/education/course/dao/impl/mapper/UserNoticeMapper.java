package com.roncoo.education.course.dao.impl.mapper;

import com.roncoo.education.course.dao.impl.mapper.entity.UserNotice;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Date;
import java.util.List;

/**
 * 站内消息 Mapper（二开，注解式）
 *
 * @author 二开
 */
@Mapper
public interface UserNoticeMapper {

    /**
     * 批量插入。催办一次动辄几十条，逐条 insert 会打出几十个来回。
     */
    @Update("<script>insert into user_notice "
            + "(id, status_id, user_id, notice_type, title, content, course_id, is_read) values "
            + "<foreach collection='list' item='it' separator=','>"
            + "(#{it.id}, 1, #{it.userId}, #{it.noticeType}, #{it.title}, #{it.content}, #{it.courseId}, 0)"
            + "</foreach></script>")
    int batchInsert(@Param("list") List<UserNotice> list);

    /**
     * 查出这批(人,课)在某时间点之后已经发过的催办，用于跳过重复。
     * <p>
     * 不用唯一索引来防重：催办本来就允许隔几天再催一次，
     * 唯一索引会把「过一周再催」也一起挡掉。
     */
    @Select("<script>select user_id as userId, course_id as courseId from user_notice "
            + "where notice_type = #{noticeType} and gmt_create >= #{since} "
            + "and course_id is not null "
            + "and user_id in <foreach collection='userIds' item='u' open='(' separator=',' close=')'>#{u}</foreach> "
            + "group by user_id, course_id</script>")
    List<UserNotice> listRecent(@Param("userIds") List<Long> userIds,
                                @Param("noticeType") Integer noticeType,
                                @Param("since") Date since);

    @Select("select count(*) from user_notice where user_id = #{userId} and is_read = 0 and status_id = 1")
    int countUnread(@Param("userId") Long userId);

    @Select("select n.*, c.course_name from user_notice n "
            + "left join course c on c.id = n.course_id "
            + "where n.user_id = #{userId} and n.status_id = 1 "
            + "order by n.id desc limit #{offset}, #{limit}")
    List<UserNotice> page(@Param("userId") Long userId, @Param("offset") int offset, @Param("limit") int limit);

    @Select("select count(*) from user_notice where user_id = #{userId} and status_id = 1")
    int pageCount(@Param("userId") Long userId);

    /**
     * 标记已读。带 user_id 条件，防止改到别人的消息。
     */
    @Update("update user_notice set is_read = 1, read_time = now() "
            + "where id = #{id} and user_id = #{userId} and is_read = 0")
    int markRead(@Param("id") Long id, @Param("userId") Long userId);

    @Update("update user_notice set is_read = 1, read_time = now() "
            + "where user_id = #{userId} and is_read = 0")
    int markAllRead(@Param("userId") Long userId);
}
