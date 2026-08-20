package com.roncoo.education.user.dao.impl.mapper;

import com.roncoo.education.user.dao.impl.mapper.entity.ProjectGroup;
import com.roncoo.education.user.dao.impl.mapper.entity.ProjectGroupExample;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface ProjectGroupMapper {
    /**
     * 二开：统计某项目组下的在册员工数，删除前用它拦截
     */
    Integer countUsersByGroupId(@Param("groupId") Long groupId);

    /**
     * 二开：批量统计各项目组人数，供列表页一次查完
     *
     * @return 每项形如 {groupId=1, cnt=8}
     */
    List<Map<String, Object>> countUsersByGroupIds(@Param("groupIds") List<Long> groupIds);

    int countByExample(ProjectGroupExample example);

    int deleteByExample(ProjectGroupExample example);

    int deleteByPrimaryKey(Long id);

    int insert(ProjectGroup record);

    int insertSelective(ProjectGroup record);

    List<ProjectGroup> selectByExample(ProjectGroupExample example);

    ProjectGroup selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("record") ProjectGroup record, @Param("example") ProjectGroupExample example);

    int updateByExample(@Param("record") ProjectGroup record, @Param("example") ProjectGroupExample example);

    int updateByPrimaryKeySelective(ProjectGroup record);

    int updateByPrimaryKey(ProjectGroup record);
}
