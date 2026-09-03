package com.roncoo.education.system.feign.biz;

import com.roncoo.education.common.base.BaseBiz;
import com.roncoo.education.common.sms.Sms;
import com.roncoo.education.system.feign.interfaces.vo.*;
import com.roncoo.education.system.service.biz.SysConfigCommonBiz;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import jakarta.validation.constraints.NotNull;

/**
 * 系统配置
 *
 * @author wujing
 */
@Component
@RequiredArgsConstructor
public class FeignSysConfigBiz extends BaseBiz {

    @NotNull
    private final SysConfigCommonBiz sysConfigCommonBiz;

    @NotNull
    private final com.roncoo.education.system.dao.SysConfigDao sysConfigDao;

    public SysConfig getSys() {
        return sysConfigCommonBiz.getSysConfig(SysConfig.class);
    }

    public Sms getSms() {
        return sysConfigCommonBiz.getSysConfig(Sms.class);
    }

    public VideoConfig getVideo() {
        return sysConfigCommonBiz.getSysConfig(VideoConfig.class);
    }


    public DocConfig getDoc() {
        return sysConfigCommonBiz.getSysConfig(DocConfig.class);
    }

    public LoginConfig getLogin() {
        return sysConfigCommonBiz.getSysConfig(LoginConfig.class);
    }

    /**
     * 按 key 取单个配置值，取不到返回 null。
     * 供二开新增的、不属于任何一包配置的键使用（如自动排课开关）。
     */
    public String getByConfigKey(String configKey) {
        com.roncoo.education.system.dao.impl.mapper.entity.SysConfig c = sysConfigDao.getByConfigKey(configKey);
        return c == null ? null : c.getConfigValue();
    }
}
