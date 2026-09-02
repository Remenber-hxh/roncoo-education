package com.roncoo.education.user.service.auth.biz;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.roncoo.education.common.base.ThreadContext;
import com.roncoo.education.common.tools.RsaUtil;
import com.roncoo.education.common.core.base.Result;
import com.roncoo.education.common.core.enums.StatusIdEnum;
import com.roncoo.education.common.tools.BeanUtil;
import com.roncoo.education.common.base.BaseBiz;
import com.roncoo.education.common.base.BaseWxBiz;
import com.roncoo.education.system.feign.interfaces.IFeignSysConfig;
import com.roncoo.education.system.feign.interfaces.vo.LoginConfig;
import com.roncoo.education.user.dao.UsersDao;
import com.roncoo.education.user.dao.impl.mapper.entity.Users;
import com.roncoo.education.user.service.auth.req.AuthBindingReq;
import com.roncoo.education.user.service.auth.req.AuthUsersHeadReq;
import com.roncoo.education.user.service.auth.req.AuthUsersPswReq;
import com.roncoo.education.user.service.auth.req.AuthUsersReq;
import com.roncoo.education.user.service.auth.resp.AuthUsersResp;
import lombok.RequiredArgsConstructor;
import me.chanjar.weixin.common.bean.WxOAuth2UserInfo;
import me.chanjar.weixin.common.error.WxErrorException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import jakarta.validation.constraints.NotNull;

/**
 * AUTH-用户信息
 *
 * @author wujing
 */
@Component
@RequiredArgsConstructor
public class AuthUsersBiz extends BaseBiz {

    /**
     * 新密码最短位数。
     * 初始密码是手机号后六位，定 6 位是为了不把「刚拿到账号想立刻改掉」的人挡在外面；
     * 要求更长的话得同时提高初始密码的强度，那是另一件事。
     */
    private static final int MIN_PSW_LEN = 6;

    @NotNull
    private final UsersDao dao;
    @NotNull
    private final IFeignSysConfig feignSysConfig;
    @NotNull
    private final BaseWxBiz baseWxBiz;

    public Result<AuthUsersResp> view() {
        Users users = dao.getById(ThreadContext.userId());
        if (users != null && users.getStatusId().equals(StatusIdEnum.YES.getCode())) {
            return Result.success(BeanUtil.copyProperties(users, AuthUsersResp.class));
        }
        return Result.error("用户不存在或被禁用");
    }

    /**
     * 只更新头像。
     * <p>
     * 不走 update(AuthUsersReq)：那个入参要求昵称、性别、生日都非空，
     * 员工只是想换张头像，却会因为没填过生日被打回。
     */
    public Result<String> updateHead(AuthUsersHeadReq req) {
        if (!StringUtils.hasText(req.getUserHead())) {
            return Result.error("头像地址不能为空");
        }
        Users record = new Users();
        record.setId(ThreadContext.userId());
        record.setUserHead(req.getUserHead().trim());
        if (dao.updateById(record) > 0) {
            return Result.success("操作成功");
        }
        return Result.error("操作失败");
    }

    /**
     * 员工自助修改密码（二开新增）。
     * <p>
     * 平台没有配短信平台，原有的「忘记密码」要发验证码，实际是断的：
     * 请求会真的去调阿里云、因为密钥为空而超时，员工看到的却是
     * 「操作频繁，请稍后再试」这种完全误导的提示。
     * <p>
     * 这里提供不依赖短信的那一半：已登录的员工凭原密码改新密码。
     * 真正忘记密码的只能由管理员在后台重置——内部系统里这是合理的，
     * 没有短信或邮箱做二次验证时，任何「自助找回」都等于谁都能改别人的密码。
     */
    public Result<String> updatePsw(AuthUsersPswReq req) {
        Long userId = ThreadContext.userId();
        if (userId == null) {
            return Result.error("未登录");
        }
        Users user = dao.getById(userId);
        if (ObjectUtil.isEmpty(user)) {
            return Result.error("账号不存在");
        }

        String oldPsw = decrypt(req.getOldPwdEncrypt());
        String newPsw = decrypt(req.getNewPwdEncrypt());
        if (!StringUtils.hasText(oldPsw) || !StringUtils.hasText(newPsw)) {
            return Result.error("密码不能为空");
        }

        // 与登录同一套算法：sha1(盐 + 明文)，小写
        if (!DigestUtil.sha1Hex(user.getMobileSalt() + oldPsw).equals(user.getMobilePsw())) {
            return Result.error("原密码不正确");
        }
        if (newPsw.equals(oldPsw)) {
            return Result.error("新密码不能与原密码相同");
        }
        if (newPsw.length() < MIN_PSW_LEN) {
            return Result.error("新密码至少 " + MIN_PSW_LEN + " 位");
        }

        // 换新盐，避免旧盐加上被泄露过的哈希还能对上
        Users record = new Users();
        record.setId(userId);
        record.setMobileSalt(IdUtil.simpleUUID());
        record.setMobilePsw(DigestUtil.sha1Hex(record.getMobileSalt() + newPsw));
        if (dao.updateById(record) > 0) {
            return Result.success("修改成功");
        }
        return Result.error("修改失败");
    }

    /**
     * RSA 解密。私钥同登录用的那把，取自「参数配置」。
     */
    private String decrypt(String encrypted) {
        if (!StringUtils.hasText(encrypted)) {
            return null;
        }
        String privateKey = feignSysConfig.getLogin().getRsaLoginPrivateKey();
        if (!StringUtils.hasText(privateKey)) {
            return null;
        }
        return RsaUtil.decrypt(encrypted, privateKey);
    }

    public Result<String> update(AuthUsersReq req) {
        Users users = BeanUtil.copyProperties(req, Users.class);
        users.setId(ThreadContext.userId());
        int result = dao.updateById(users);
        if (result > 0) {
            return Result.success("操作成功");
        }
        return Result.error("操作失败");
    }

    public Result<String> binding(AuthBindingReq req) throws WxErrorException {
        Users users = dao.getById(ThreadContext.userId());
        if (StringUtils.hasText(users.getUnionId()) || StringUtils.hasText(users.getOpenId())) {
            return Result.error("您已绑定微信，请勿重复绑定");
        }

        // 获取微信用户信息
        LoginConfig loginConfig = feignSysConfig.getLogin();
        WxOAuth2UserInfo userInfo = baseWxBiz.getAuthInfo(loginConfig.getWxPcLoginAppId(), loginConfig.getWxPcLoginAppSecret(), req.getCode());
        Users usersRecord = dao.getByUnionIdOrOpenId(userInfo.getUnionId(), userInfo.getOpenid());
        if (ObjectUtil.isNotNull(usersRecord)) {
            return Result.error("该微信已绑定其他账号，请更换微信重新绑定");
        }

        // 更新用户信息
        Users newUser = new Users();
        newUser.setId(users.getId());
        newUser.setUnionId(userInfo.getUnionId());
        newUser.setOpenId(userInfo.getOpenid());
        dao.updateById(newUser);
        return Result.success("操作成功");
    }

    /**
     * 解绑微信
     *
     * @return
     */
    public Result<String> unbind() {
        Users users = dao.getById(ThreadContext.userId());
        Users newUsers = new Users();
        newUsers.setId(users.getId());
        newUsers.setUnionId("");
        newUsers.setOpenId("");
        dao.updateById(newUsers);
        return Result.success("操作成功");
    }
}
