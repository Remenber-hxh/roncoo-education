package com.roncoo.education.user.dao.impl;

import com.roncoo.education.common.base.AbstractBaseJdbc;
import com.roncoo.education.common.base.page.Page;
import com.roncoo.education.common.base.page.PageUtil;
import com.roncoo.education.common.tools.IdWorker;
import com.roncoo.education.user.dao.TeamDao;
import com.roncoo.education.user.dao.impl.mapper.TeamMapper;
import com.roncoo.education.user.dao.impl.mapper.entity.Team;
import com.roncoo.education.user.dao.impl.mapper.entity.TeamExample;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import jakarta.validation.constraints.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class TeamDaoImpl extends AbstractBaseJdbc implements TeamDao {
    @NotNull
    private final TeamMapper teamMapper;

    @Override
    public int save(Team record) {
        if (record.getId() == null) {
            record.setId(IdWorker.getId());
        }
        return this.teamMapper.insertSelective(record);
    }

    @Override
    public int deleteById(Long id) {
        return this.teamMapper.deleteByPrimaryKey(id);
    }

    @Override
    public int updateById(Team record) {
        record.setGmtCreate(null);
        record.setGmtModified(null);
        return this.teamMapper.updateByPrimaryKeySelective(record);
    }

    @Override
    public Team getById(Long id) {
        return this.teamMapper.selectByPrimaryKey(id);
    }

    @Override
    public Page<Team> page(int pageCurrent, int pageSize, TeamExample example) {
        int count = this.teamMapper.countByExample(example);
        pageSize = PageUtil.checkPageSize(pageSize);
        pageCurrent = PageUtil.checkPageCurrent(count, pageSize, pageCurrent);
        int totalPage = PageUtil.countTotalPage(count, pageSize);
        example.setLimitStart(PageUtil.countOffset(pageCurrent, pageSize));
        example.setPageSize(pageSize);
        return new Page<Team>(count, totalPage, pageCurrent, pageSize, this.teamMapper.selectByExample(example));
    }

    @Override
    public List<Team> listByStatusId(Integer statusId) {
        TeamExample example = new TeamExample();
        TeamExample.Criteria criteria = example.createCriteria();
        criteria.andStatusIdEqualTo(statusId);
        example.setOrderByClause("sort asc, id asc");
        return this.teamMapper.selectByExample(example);
    }

    @Override
    public Team getByTeamName(String teamName) {
        TeamExample example = new TeamExample();
        TeamExample.Criteria criteria = example.createCriteria();
        criteria.andTeamNameEqualTo(teamName);
        List<Team> list = this.teamMapper.selectByExample(example);
        if (list.isEmpty()) {
            return null;
        }
        return list.get(0);
    }

    @Override
    public List<Team> listByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        TeamExample example = new TeamExample();
        TeamExample.Criteria criteria = example.createCriteria();
        criteria.andIdIn(ids);
        return this.teamMapper.selectByExample(example);
    }

    @Override
    public int countUsersByTeamId(Long teamId) {
        Integer count = this.teamMapper.countUsersByTeamId(teamId);
        return count == null ? 0 : count;
    }

    @Override
    public Map<Long, Integer> countUsersByTeamIds(List<Long> teamIds) {
        if (teamIds == null || teamIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, Integer> result = new HashMap<>();
        for (Map<String, Object> row : this.teamMapper.countUsersByTeamIds(teamIds)) {
            Object teamId = row.get("teamId");
            Object cnt = row.get("cnt");
            if (teamId != null && cnt != null) {
                result.put(((Number) teamId).longValue(), ((Number) cnt).intValue());
            }
        }
        return result;
    }
}
