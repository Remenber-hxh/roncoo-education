package com.roncoo.education.system.feign.interfaces;

import com.roncoo.education.common.sms.Sms;
import com.roncoo.education.system.feign.interfaces.vo.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 系统配置 接口
 *
 * @author wujing
 * @date 2022-08-25
 */
@FeignClient(value = "service-system", path = "/system/sys/config")
public interface IFeignSysConfig {

    /**
     * 系统配置
     *
     * @return
     */
    @GetMapping(value = "/getSys")
    SysConfig getSys();

    /**
     * 登录
     *
     * @return
     */
    @GetMapping(value = "/getLogin")
    LoginConfig getLogin();

    /**
     * 视频云配置(录播）
     *
     * @return
     */
    @GetMapping(value = "/getVideo")
    VideoConfig getVideo();

    /**
     * 文档存储配置
     *
     * @return
     */
    @GetMapping(value = "/getDoc")
    DocConfig getDoc();

    /**
     * 短信配置
     *
     * @return
     */
    @GetMapping(value = "/getSms")
    Sms getSms();

    /**
     * 按 key 取单个配置值。
     * <p>
     * 现有的 getSys/getLogin 等都是按用途打包返回，
     * 二开新增的配置（如自动排课开关）不属于任何一包，
     * 为一两个键各加一个 VO 不划算。取不到返回 null。
     *
     * @param configKey 配置键
     * @return 配置值
     */
    @GetMapping(value = "/getByKey/{configKey}")
    String getByConfigKey(@PathVariable(value = "configKey") String configKey);

}
