package com.roncoo.education.user.dao;

import com.roncoo.education.common.base.page.Page;
import com.roncoo.education.user.dao.impl.mapper.entity.Team;
import com.roncoo.education.user.dao.impl.mapper.entity.TeamExample;

import java.util.List;
import java.util.Map;

public interface TeamDao {
    int save(Team record);

    int deleteById(Long id);

    int updateById(Team record);

    Team getById(Long id);

    Page<Team> page(int pageCurrent, int pageSize, TeamExample example);

    /**
     * 按状态取班组列表，供下拉选择使用
     */
    List<Team> listByStatusId(Integer statusId);

    /**
     * 按名称精确查找，用于新增/修改时的重名校验
     */
    Team getByTeamName(String teamName);

    /**
     * 按ID集合批量取，供列表页回填班组名称
     */
    List<Team> listByIds(List<Long> ids);

    /**
     * 统计某班组下的在册员工数，删除前用它拦截
     */
    int countUsersByTeamId(Long teamId);

    /**
     * 批量统计各班组人数，key 为班组ID
     */
    Map<Long, Integer> countUsersByTeamIds(List<Long> teamIds);
}
