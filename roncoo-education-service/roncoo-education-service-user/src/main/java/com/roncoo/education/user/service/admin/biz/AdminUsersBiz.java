package com.roncoo.education.user.service.admin.biz;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.DesensitizedUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.roncoo.education.common.base.page.Page;
import com.roncoo.education.common.base.page.PageUtil;
import com.roncoo.education.common.core.base.Result;
import com.roncoo.education.common.tools.BeanUtil;
import com.roncoo.education.common.tools.Md5Util;
import com.roncoo.education.common.base.BaseBiz;
import com.roncoo.education.user.dao.ProjectGroupDao;
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

    @NotNull
    private final ProjectGroupDao projectGroupDao;

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
        if (req.getProjectGroupId() != null) {
            c.andProjectGroupIdEqualTo(req.getProjectGroupId());
        }
        example.setOrderByClause("id desc");
        Page<Users> page = dao.page(req.getPageCurrent(), req.getPageSize(), example);
        Page<AdminUsersPageResp> respPage = PageUtil.transform(page, AdminUsersPageResp.class);
        if (CollUtil.isNotEmpty(respPage.getList())) {
            // 班组和项目组名称各一次查完再回填，不逐行查
            List<Long> teamIds = respPage.getList().stream()
                    .map(AdminUsersPageResp::getTeamId).filter(java.util.Objects::nonNull).distinct().toList();
            Map<Long, String> teamNames = teamDao.listByIds(teamIds).stream()
                    .collect(Collectors.toMap(t -> t.getId(), t -> t.getTeamName()));
            List<Long> groupIds = respPage.getList().stream()
                    .map(AdminUsersPageResp::getProjectGroupId).filter(java.util.Objects::nonNull).distinct().toList();
            Map<Long, String> groupNames = projectGroupDao.listByIds(groupIds).stream()
                    .collect(Collectors.toMap(g -> g.getId(), g -> g.getGroupName()));
            for (AdminUsersPageResp resp : respPage.getList()) {
                // 脱敏处理
                resp.setMobile(DesensitizedUtil.mobilePhone(resp.getMobile()));
                if (resp.getTeamId() != null) {
                    resp.setTeamName(teamNames.get(resp.getTeamId()));
                }
                if (resp.getProjectGroupId() != null) {
                    resp.setProjectGroupName(groupNames.get(resp.getProjectGroupId()));
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
        if (req.getProjectGroupId() != null && projectGroupDao.getById(req.getProjectGroupId()) == null) {
            return Result.error("项目组不存在");
        }

        Users record = new Users();
        record.setId(req.getId());
        record.setEmpNo(StringUtils.hasText(empNo) ? empNo : null);
        record.setTeamId(req.getTeamId());
        record.setProjectGroupId(req.getProjectGroupId());
        record.setPosition(StringUtils.hasText(req.getPosition()) ? req.getPosition().trim() : null);
        record.setHireDate(req.getHireDate());
        // 走 updateProfileById 而不是 updateById：
        // 后者是选择性更新，管理员把班组/入职日期改回空值时会被跳过（提示成功、库里没变），
        // 且五个字段全为空时会拼出 "update users where id=?" 直接 SQL 报错。
        dao.updateProfileById(record);
        // 不按影响行数判断成败：档案没做任何改动时 MySQL 返回 0 行，
        // 那不是失败。上面已经确认过用户存在，执行不抛异常即为成功。
        return Result.success("操作成功");
    }

    /**
     * 用户信息添加
     *
     * @param req 用户信息
     * @return 添加结果
     */
    public Result<String> save(AdminUsersSaveReq req) {
        Users record = BeanUtil.copyProperties(req, Users.class);
        // 密码必须加盐哈希后再存。
        // 原来是把表单里的明文直接 copy 进去，而登录时比对的是
        // sha1(salt + 输入)，两边对不上——后台这样建出来的账号根本登不进去，
        // 而且明文密码直接躺在库里。
        if (StringUtils.hasText(req.getMobilePsw())) {
            record.setMobileSalt(IdUtil.simpleUUID());
            record.setMobilePsw(DigestUtil.sha1Hex(record.getMobileSalt() + req.getMobilePsw().trim()));
        } else if (StringUtils.hasText(req.getMobile())) {
            // 没填密码时给个默认值，与批量导入保持一致：手机号后 6 位
            String init = defaultPsw(req.getMobile());
            record.setMobileSalt(IdUtil.simpleUUID());
            record.setMobilePsw(DigestUtil.sha1Hex(record.getMobileSalt() + init));
        }
        if (dao.save(record) > 0) {
            return Result.success("操作成功");
        }
        return Result.error("操作失败");
    }

    /**
     * 重置登录密码。
     * <p>
     * 密码是加盐哈希存的，管理员看不到也找不回，员工忘记密码时
     * 之前后台完全没有办法处理。重置成手机号后 6 位，
     * 与批量导入建号时的初始密码规则一致，并把新密码返回给管理员转告。
     */
    public Result<String> resetPsw(Long id) {
        Users user = dao.getById(id);
        if (user == null) {
            return Result.error("用户不存在");
        }
        if (!StringUtils.hasText(user.getMobile()) || user.getMobile().trim().length() < 6) {
            return Result.error("该账号的手机号不足 6 位，无法按规则生成初始密码，请先补全手机号");
        }
        String init = defaultPsw(user.getMobile());
        Users record = new Users();
        record.setId(id);
        record.setMobileSalt(IdUtil.simpleUUID());
        record.setMobilePsw(DigestUtil.sha1Hex(record.getMobileSalt() + init));
        if (dao.updateById(record) > 0) {
            return Result.success(init);
        }
        return Result.error("重置失败");
    }

    /** 初始密码规则：手机号后 6 位 */
    private static String defaultPsw(String mobile) {
        String m = mobile.trim();
        return m.substring(m.length() - 6);
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
