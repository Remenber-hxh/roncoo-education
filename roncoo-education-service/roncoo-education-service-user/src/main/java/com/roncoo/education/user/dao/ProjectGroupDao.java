package com.roncoo.education.user.dao;

import com.roncoo.education.common.base.page.Page;
import com.roncoo.education.user.dao.impl.mapper.entity.ProjectGroup;
import com.roncoo.education.user.dao.impl.mapper.entity.ProjectGroupExample;

import java.util.List;
import java.util.Map;

public interface ProjectGroupDao {
    int save(ProjectGroup record);

    int deleteById(Long id);

    int updateById(ProjectGroup record);

    ProjectGroup getById(Long id);

    Page<ProjectGroup> page(int pageCurrent, int pageSize, ProjectGroupExample example);

    /**
     * 按状态取项目组列表，供下拉选择使用
     */
    List<ProjectGroup> listByStatusId(Integer statusId);

    /**
     * 按名称精确查找，用于新增/修改时的重名校验
     */
    ProjectGroup getByGroupName(String groupName);

    /**
     * 按ID集合批量取，供列表页回填项目组名称
     */
    List<ProjectGroup> listByIds(List<Long> ids);

    /**
     * 统计某项目组下的在册员工数，删除前用它拦截
     */
    int countUsersByGroupId(Long groupId);

    /**
     * 批量统计各项目组人数，key 为项目组ID
     */
    Map<Long, Integer> countUsersByGroupIds(List<Long> groupIds);
}
