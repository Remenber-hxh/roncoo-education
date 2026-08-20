package com.roncoo.education.user.service.admin.biz;

import com.roncoo.education.common.base.BaseBiz;
import com.roncoo.education.common.base.page.Page;
import com.roncoo.education.common.base.page.PageUtil;
import com.roncoo.education.common.core.base.Result;
import com.roncoo.education.common.tools.BeanUtil;
import com.roncoo.education.user.dao.TeamDao;
import com.roncoo.education.user.dao.impl.mapper.entity.Team;
import com.roncoo.education.user.dao.impl.mapper.entity.TeamExample;
import com.roncoo.education.user.dao.impl.mapper.entity.TeamExample.Criteria;
import com.roncoo.education.user.service.admin.req.AdminTeamEditReq;
import com.roncoo.education.user.service.admin.req.AdminTeamPageReq;
import com.roncoo.education.user.service.admin.req.AdminTeamSaveReq;
import com.roncoo.education.user.service.admin.resp.AdminTeamListResp;
import com.roncoo.education.user.service.admin.resp.AdminTeamPageResp;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;

/**
 * ADMIN-班组字典
 *
 * @author 二开
 */
@Component
@RequiredArgsConstructor
public class AdminTeamBiz extends BaseBiz {

    /**
     * 正常状态
     */
    private static final int STATUS_NORMAL = 1;

    @NotNull
    private final TeamDao dao;

    /**
     * 班组分页
     */
    public Result<Page<AdminTeamPageResp>> page(AdminTeamPageReq req) {
        TeamExample example = new TeamExample();
        Criteria c = example.createCriteria();
        if (StringUtils.hasText(req.getTeamName())) {
            c.andTeamNameLike(PageUtil.like(req.getTeamName()));
        }
        if (req.getStatusId() != null) {
            c.andStatusIdEqualTo(req.getStatusId());
        }
        example.setOrderByClause("sort asc, id asc");
        Page<Team> page = dao.page(req.getPageCurrent(), req.getPageSize(), example);
        Page<AdminTeamPageResp> respPage = PageUtil.transform(page, AdminTeamPageResp.class);

        // 一次查完本页所有班组的人数，避免逐行查询
        List<Long> ids = respPage.getList().stream().map(AdminTeamPageResp::getId).toList();
        Map<Long, Integer> countMap = dao.countUsersByTeamIds(ids);
        for (AdminTeamPageResp item : respPage.getList()) {
            item.setUserCount(countMap.getOrDefault(item.getId(), 0));
        }
        return Result.success(respPage);
    }

    /**
     * 班组下拉列表，只返回正常状态的
     */
    public Result<List<AdminTeamListResp>> list() {
        List<Team> list = dao.listByStatusId(STATUS_NORMAL);
        return Result.success(BeanUtil.copyProperties(list, AdminTeamListResp.class));
    }

    /**
     * 班组添加
     */
    public Result<String> save(AdminTeamSaveReq req) {
        String teamName = req.getTeamName().trim();
        // 库里 team_name 有唯一索引，撞了会抛 SQL 异常。这里先查一次，
        // 好让管理员看到「班组名称已存在」而不是一个 500。
        if (dao.getByTeamName(teamName) != null) {
            return Result.error("班组名称已存在");
        }
        Team record = BeanUtil.copyProperties(req, Team.class);
        record.setTeamName(teamName);
        if (dao.save(record) > 0) {
            return Result.success("操作成功");
        }
        return Result.error("操作失败");
    }

    /**
     * 班组查看
     */
    public Result<AdminTeamPageResp> view(Long id) {
        Team team = dao.getById(id);
        if (team == null) {
            return Result.error("班组不存在");
        }
        AdminTeamPageResp resp = BeanUtil.copyProperties(team, AdminTeamPageResp.class);
        resp.setUserCount(dao.countUsersByTeamId(id));
        return Result.success(resp);
    }

    /**
     * 班组修改
     */
    public Result<String> edit(AdminTeamEditReq req) {
        Team old = dao.getById(req.getId());
        if (old == null) {
            return Result.error("班组不存在");
        }
        String teamName = req.getTeamName().trim();
        Team sameName = dao.getByTeamName(teamName);
        // 改名时要排除自己，否则原名保存不回去
        if (sameName != null && !sameName.getId().equals(req.getId())) {
            return Result.error("班组名称已存在");
        }
        Team record = BeanUtil.copyProperties(req, Team.class);
        record.setTeamName(teamName);
        if (dao.updateById(record) > 0) {
            return Result.success("操作成功");
        }
        return Result.error("操作失败");
    }

    /**
     * 班组删除
     */
    public Result<String> delete(Long id) {
        Team team = dao.getById(id);
        if (team == null) {
            return Result.error("班组不存在");
        }
        // 班组是统计口径的分组依据。删掉还有人挂着的班组，
        // 这些员工的 team_id 会变成悬空值，按班组统计时既不属于任何班组、
        // 也不会出现在「未分组」里，数据直接对不上。
        int userCount = dao.countUsersByTeamId(id);
        if (userCount > 0) {
            return Result.error("该班组下还有 " + userCount + " 名员工，请先调整这些员工的班组再删除");
        }
        if (dao.deleteById(id) > 0) {
            return Result.success("操作成功");
        }
        return Result.error("操作失败");
    }
}
