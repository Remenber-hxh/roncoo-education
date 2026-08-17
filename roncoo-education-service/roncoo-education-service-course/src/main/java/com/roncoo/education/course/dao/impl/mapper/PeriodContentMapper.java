package com.roncoo.education.course.dao.impl.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 图文课时正文的按需查询（二开新增，注解式）
 * <p>
 * content 是 LONGTEXT，故意没放进 CourseChapterPeriodMapper.xml 的 Base_Column_List：
 * 章节列表会一次性取出课程下所有课时，若带上正文，门户课程详情页会把每篇图文的
 * 完整 HTML 都传一遍。因此列表查询不含正文，真正要看的时候用这里单独取。
 */
@Mapper
public interface PeriodContentMapper {

    @Select("select content from course_chapter_period where id = #{periodId}")
    String selectContentById(@Param("periodId") Long periodId);
}
