package com.roncoo.education.user.service.admin.biz;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.DesensitizedUtil;
import cn.hutool.core.util.ObjectUtil;
import com.roncoo.education.common.base.page.Page;
import com.roncoo.education.common.base.page.PageUtil;
import com.roncoo.education.common.core.base.Result;
import com.roncoo.education.common.tools.BeanUtil;
import com.roncoo.education.common.tools.Md5Util;
import com.roncoo.education.common.base.BaseBiz;
import com.roncoo.education.user.dao.TeamDao;
import com.roncoo.education.user.dao.UsersDao;
import com.roncoo.education.user.dao.impl.mapper.entity.Users;
import com.roncoo.education.user.dao.impl.mapper.entity.UsersExample;
import com.roncoo.education.user.dao.impl.mapper.entity.UsersExample.Criteria;
import com.roncoo.education.user.service.admin.req.AdminUsersEditReq;
import com.roncoo.education.user.service.admin.req.AdminUsersPageReq;
import com.roncoo.education.user.service.admin.req.AdminUsersProfileEditReq;
import com.roncoo.education.user.service.admin.req.AdminUsersSaveReq;
import com.roncoo.education.user.service.admin.resp.AdminUsersPageResp;
import com.roncoo.education.user.service.admin.resp.AdminUsersViewResp;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ADMIN-用户信息
 *
 * @author wujing
 */
@Component
@RequiredArgsConstructor
public class AdminUsersBiz extends BaseBiz {

    @NotNull
    private final UsersDao dao;

    @NotNull
    private final TeamDao teamDao;

    /**
     * 用户信息分页
     *
     * @param req 用户信息分页查询参数
     * @return 用户信息分页查询结果
     */
    public Result<Page<AdminUsersPageResp>> page(AdminUsersPageReq req) {
        UsersExample example = new UsersExample();
        Criteria c = example.createCriteria();
        if (StringUtils.hasText(req.getMobile())) {
            c.andMobileLike(PageUtil.rightLike(req.getMobile()));
        }
        if (StringUtils.hasText(req.getEmpNo())) {
            c.andEmpNoLike(PageUtil.rightLike(req.getEmpNo()));
        }
        if (req.getTeamId() != null) {
            c.andTeamIdEqualTo(req.getTeamId());
        }
        example.setOrderByClause("id desc");
        Page<Users> page = dao.page(req.getPageCurrent(), req.getPageSize(), example);
        Page<AdminUsersPageResp> respPage = PageUtil.transform(page, AdminUsersPageResp.class);
        if (CollUtil.isNotEmpty(respPage.getList())) {
            // 班组名称一次查完再回填，不逐行查
            List<Long> teamIds = respPage.getList().stream()
                    .map(AdminUsersPageResp::getTeamId).filter(java.util.Objects::nonNull).distinct().toList();
            Map<Long, String> teamNames = teamDao.listByIds(teamIds).stream()
                    .collect(Collectors.toMap(t -> t.getId(), t -> t.getTeamName()));
            for (AdminUsersPageResp resp : respPage.getList()) {
                // 脱敏处理
                resp.setMobile(DesensitizedUtil.mobilePhone(resp.getMobile()));
                if (resp.getTeamId() != null) {
                    resp.setTeamName(teamNames.get(resp.getTeamId()));
                }
            }
        }
        return Result.success(respPage);
    }

    /**
     * 员工档案编辑：只改工号、班组、岗位、入职日期
     */
    public Result<String> profileEdit(AdminUsersProfileEditReq req) {
        Users old = dao.getById(req.getId());
        if (old == null) {
            return Result.error("用户不存在");
        }
        String empNo = req.getEmpNo() == null ? null : req.getEmpNo().trim();
        if (StringUtils.hasText(empNo)) {
            // 工号是统计和导入的对齐键，必须全局唯一。
            // 库上有唯一索引，这里先查一次好给出可读的提示。
            UsersExample example = new UsersExample();
            example.createCriteria().andEmpNoEqualTo(empNo).andIdNotEqualTo(req.getId());
            if (dao.count(example) > 0) {
                return Result.error("工号 " + empNo + " 已被其他员工占用");
            }
        }
        if (req.getTeamId() != null && teamDao.getById(req.getTeamId()) == null) {
            return Result.error("班组不存在");
        }

        Users record = new Users();
        record.setId(req.getId());
        record.setEmpNo(StringUtils.hasText(empNo) ? empNo : null);
        record.setTeamId(req.getTeamId());
        record.setPosition(req.getPosition());
        record.setHireDate(req.getHireDate());
        if (dao.updateById(record) > 0) {
            return Result.success("操作成功");
        }
        return Result.error("操作失败");
    }

    /**
     * 用户信息添加
     *
     * @param req 用户信息
     * @return 添加结果
     */
    public Result<String> save(AdminUsersSaveReq req) {
        Users record = BeanUtil.copyProperties(req, Users.class);
        if (dao.save(record) > 0) {
            return Result.success("操作成功");
        }
        return Result.error("操作失败");
    }

    /**
     * 用户信息查看
     *
     * @param id 主键ID
     * @return 用户信息
     */
    public Result<AdminUsersViewResp> view(Long id) {
        return Result.success(BeanUtil.copyProperties(dao.getById(id), AdminUsersViewResp.class));
    }

    /**
     * 用户信息修改
     *
     * @param req 用户信息修改对象
     * @return 修改结果
     */
    public Result<String> edit(AdminUsersEditReq req) {
        Users record = BeanUtil.copyProperties(req, Users.class);
        if (dao.updateById(record) > 0) {
            return Result.success("操作成功");
        }
        return Result.error("操作失败");
    }

    /**
     * 用户信息删除
     *
     * @param id ID主键
     * @return 删除结果
     */
    public Result<String> delete(Long id) {
        if (dao.deleteById(id) > 0) {
            return Result.success("操作成功");
        }
        return Result.error("操作失败");
    }

}
