package com.roncoo.education.user.service.admin.biz;

import com.roncoo.education.common.base.BaseBiz;
import com.roncoo.education.common.base.page.Page;
import com.roncoo.education.common.base.page.PageUtil;
import com.roncoo.education.common.core.base.Result;
import com.roncoo.education.common.tools.BeanUtil;
import com.roncoo.education.user.dao.ProjectGroupDao;
import com.roncoo.education.user.dao.impl.mapper.entity.ProjectGroup;
import com.roncoo.education.user.dao.impl.mapper.entity.ProjectGroupExample;
import com.roncoo.education.user.dao.impl.mapper.entity.ProjectGroupExample.Criteria;
import com.roncoo.education.user.service.admin.req.AdminProjectGroupEditReq;
import com.roncoo.education.user.service.admin.req.AdminProjectGroupPageReq;
import com.roncoo.education.user.service.admin.req.AdminProjectGroupSaveReq;
import com.roncoo.education.user.service.admin.resp.AdminProjectGroupListResp;
import com.roncoo.education.user.service.admin.resp.AdminProjectGroupPageResp;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;

/**
 * ADMIN-项目组字典
 * <p>
 * 项目组回答「在哪个点上班」，班组回答「干什么活」，两者正交：
 * 大修房 8 人里 7 个维修技工 + 1 个强电技工；
 * 仓管周瑞鳌属职能组，人却在无锡国际会议中心。
 *
 * @author 二开
 */
@Component
@RequiredArgsConstructor
public class AdminProjectGroupBiz extends BaseBiz {

    private static final int STATUS_NORMAL = 1;

    @NotNull
    private final ProjectGroupDao dao;

    public Result<Page<AdminProjectGroupPageResp>> page(AdminProjectGroupPageReq req) {
        ProjectGroupExample example = new ProjectGroupExample();
        Criteria c = example.createCriteria();
        if (StringUtils.hasText(req.getGroupName())) {
            c.andGroupNameLike(PageUtil.like(req.getGroupName()));
        }
        if (req.getStatusId() != null) {
            c.andStatusIdEqualTo(req.getStatusId());
        }
        example.setOrderByClause("sort asc, id asc");
        Page<ProjectGroup> page = dao.page(req.getPageCurrent(), req.getPageSize(), example);
        Page<AdminProjectGroupPageResp> respPage = PageUtil.transform(page, AdminProjectGroupPageResp.class);

        List<Long> ids = respPage.getList().stream().map(AdminProjectGroupPageResp::getId).toList();
        Map<Long, Integer> countMap = dao.countUsersByGroupIds(ids);
        for (AdminProjectGroupPageResp item : respPage.getList()) {
            item.setUserCount(countMap.getOrDefault(item.getId(), 0));
        }
        return Result.success(respPage);
    }

    public Result<List<AdminProjectGroupListResp>> list() {
        List<ProjectGroup> list = dao.listByStatusId(STATUS_NORMAL);
        return Result.success(BeanUtil.copyProperties(list, AdminProjectGroupListResp.class));
    }

    public Result<String> save(AdminProjectGroupSaveReq req) {
        String groupName = req.getGroupName().trim();
        if (dao.getByGroupName(groupName) != null) {
            return Result.error("项目组名称已存在");
        }
        ProjectGroup record = BeanUtil.copyProperties(req, ProjectGroup.class);
        record.setGroupName(groupName);
        if (dao.save(record) > 0) {
            return Result.success("操作成功");
        }
        return Result.error("操作失败");
    }

    public Result<AdminProjectGroupPageResp> view(Long id) {
        ProjectGroup group = dao.getById(id);
        if (group == null) {
            return Result.error("项目组不存在");
        }
        AdminProjectGroupPageResp resp = BeanUtil.copyProperties(group, AdminProjectGroupPageResp.class);
        resp.setUserCount(dao.countUsersByGroupId(id));
        return Result.success(resp);
    }

    public Result<String> edit(AdminProjectGroupEditReq req) {
        ProjectGroup old = dao.getById(req.getId());
        if (old == null) {
            return Result.error("项目组不存在");
        }
        String groupName = req.getGroupName().trim();
        ProjectGroup sameName = dao.getByGroupName(groupName);
        if (sameName != null && !sameName.getId().equals(req.getId())) {
            return Result.error("项目组名称已存在");
        }
        ProjectGroup record = BeanUtil.copyProperties(req, ProjectGroup.class);
        record.setGroupName(groupName);
        if (dao.updateById(record) > 0) {
            return Result.success("操作成功");
        }
        return Result.error("操作失败");
    }

    public Result<String> delete(Long id) {
        ProjectGroup group = dao.getById(id);
        if (group == null) {
            return Result.error("项目组不存在");
        }
        // 同班组的理由：删掉还有人挂着的项目组，这些员工的 project_group_id
        // 会变成悬空值，按项目组统计时既不属于任何组也不在「未分组」里
        int userCount = dao.countUsersByGroupId(id);
        if (userCount > 0) {
            return Result.error("该项目组下还有 " + userCount + " 名员工，请先调整这些员工的项目组再删除");
        }
        if (dao.deleteById(id) > 0) {
            return Result.success("操作成功");
        }
        return Result.error("操作失败");
    }
}
