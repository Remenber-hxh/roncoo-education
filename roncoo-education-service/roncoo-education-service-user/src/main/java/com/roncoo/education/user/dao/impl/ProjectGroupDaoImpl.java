package com.roncoo.education.user.dao.impl;

import com.roncoo.education.common.base.AbstractBaseJdbc;
import com.roncoo.education.common.base.page.Page;
import com.roncoo.education.common.base.page.PageUtil;
import com.roncoo.education.common.tools.IdWorker;
import com.roncoo.education.user.dao.ProjectGroupDao;
import com.roncoo.education.user.dao.impl.mapper.ProjectGroupMapper;
import com.roncoo.education.user.dao.impl.mapper.entity.ProjectGroup;
import com.roncoo.education.user.dao.impl.mapper.entity.ProjectGroupExample;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import jakarta.validation.constraints.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class ProjectGroupDaoImpl extends AbstractBaseJdbc implements ProjectGroupDao {
    @NotNull
    private final ProjectGroupMapper projectGroupMapper;

    @Override
    public int save(ProjectGroup record) {
        if (record.getId() == null) {
            record.setId(IdWorker.getId());
        }
        return this.projectGroupMapper.insertSelective(record);
    }

    @Override
    public int deleteById(Long id) {
        return this.projectGroupMapper.deleteByPrimaryKey(id);
    }

    @Override
    public int updateById(ProjectGroup record) {
        record.setGmtCreate(null);
        record.setGmtModified(null);
        return this.projectGroupMapper.updateByPrimaryKeySelective(record);
    }

    @Override
    public ProjectGroup getById(Long id) {
        return this.projectGroupMapper.selectByPrimaryKey(id);
    }

    @Override
    public Page<ProjectGroup> page(int pageCurrent, int pageSize, ProjectGroupExample example) {
        int count = this.projectGroupMapper.countByExample(example);
        pageSize = PageUtil.checkPageSize(pageSize);
        pageCurrent = PageUtil.checkPageCurrent(count, pageSize, pageCurrent);
        int totalPage = PageUtil.countTotalPage(count, pageSize);
        example.setLimitStart(PageUtil.countOffset(pageCurrent, pageSize));
        example.setPageSize(pageSize);
        return new Page<ProjectGroup>(count, totalPage, pageCurrent, pageSize, this.projectGroupMapper.selectByExample(example));
    }

    @Override
    public List<ProjectGroup> listByStatusId(Integer statusId) {
        ProjectGroupExample example = new ProjectGroupExample();
        example.createCriteria().andStatusIdEqualTo(statusId);
        example.setOrderByClause("sort asc, id asc");
        return this.projectGroupMapper.selectByExample(example);
    }

    @Override
    public ProjectGroup getByGroupName(String groupName) {
        ProjectGroupExample example = new ProjectGroupExample();
        example.createCriteria().andGroupNameEqualTo(groupName);
        List<ProjectGroup> list = this.projectGroupMapper.selectByExample(example);
        if (list.isEmpty()) {
            return null;
        }
        return list.get(0);
    }

    @Override
    public List<ProjectGroup> listByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        ProjectGroupExample example = new ProjectGroupExample();
        example.createCriteria().andIdIn(ids);
        return this.projectGroupMapper.selectByExample(example);
    }

    @Override
    public int countUsersByGroupId(Long groupId) {
        Integer count = this.projectGroupMapper.countUsersByGroupId(groupId);
        return count == null ? 0 : count;
    }

    @Override
    public Map<Long, Integer> countUsersByGroupIds(List<Long> groupIds) {
        if (groupIds == null || groupIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, Integer> result = new HashMap<>();
        for (Map<String, Object> row : this.projectGroupMapper.countUsersByGroupIds(groupIds)) {
            Object gid = row.get("groupId");
            Object cnt = row.get("cnt");
            if (gid != null && cnt != null) {
                result.put(((Number) gid).longValue(), ((Number) cnt).intValue());
            }
        }
        return result;
    }
}
