package com.roncoo.education.user.dao.impl.mapper;

import com.roncoo.education.user.dao.impl.mapper.entity.Team;
import com.roncoo.education.user.dao.impl.mapper.entity.TeamExample;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface TeamMapper {
    /**
     * 二开：统计某个班组下的在册员工数，删除班组前用它拦截
     */
    Integer countUsersByTeamId(@Param("teamId") Long teamId);

    /**
     * 二开：批量统计各班组人数，供列表页一次查完，避免每行一次查询
     *
     * @return 每项形如 {teamId=1, cnt=3}
     */
    List<Map<String, Object>> countUsersByTeamIds(@Param("teamIds") List<Long> teamIds);

    int countByExample(TeamExample example);

    int deleteByExample(TeamExample example);

    int deleteByPrimaryKey(Long id);

    int insert(Team record);

    int insertSelective(Team record);

    List<Team> selectByExample(TeamExample example);

    Team selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("record") Team record, @Param("example") TeamExample example);

    int updateByExample(@Param("record") Team record, @Param("example") TeamExample example);

    int updateByPrimaryKeySelective(Team record);

    int updateByPrimaryKey(Team record);
}
