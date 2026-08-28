package com.roncoo.education.user.feign.interfaces;

import com.roncoo.education.user.feign.interfaces.vo.UserRosterVO;
import com.roncoo.education.user.feign.interfaces.vo.UsersVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;

/**
 * 用户信息 接口
 *
 * @author wujing
 * @date 2022-08-27
 */
@FeignClient(value = "service-user", path = "/user/users")
public interface IFeignUsers {
    /**
     * 根据ID获取信息
     *
     * @param id 主键ID
     * @return 用户信息
     */
    @GetMapping(value = "/getById/{id}")
    UsersVO getById(@PathVariable(value = "id") Long id);

    /**
     * 根据ID获取信息
     *
     * @param id 主键ID
     * @return 用户信息
     */
    @GetMapping(value = "/getByMobile/{mobile}")
    UsersVO getByMobile(@PathVariable(value = "mobile") String mobile);

    /**
     * 根据ID获取信息，模糊查询
     *
     * @param id 主键ID
     * @return 用户信息
     */
    @GetMapping(value = "/listByMobile/{mobile}")
    List<UsersVO> listByMobile(@PathVariable(value = "mobile") String mobile);

    /**
     * 根据ID集合获取集合
     *
     * @param userIdList
     * @return
     */
    @PostMapping(value = "/listByIds")
    Map<Long, UsersVO> listByIds(@RequestBody List<Long> userIdList);

    /**
     * 全部启用员工的花名册（含班组、项目组归属）。
     * <p>
     * 供 course 服务做学习统计时按班组/项目组归组用——学习记录在 course 服务，
     * 员工归属在 user 服务，只能整份取过去再本地归组。
     * 公司规模是几十人量级，一次全量取回没有分页的必要。
     *
     * @return 花名册
     */
    @GetMapping(value = "/roster")
    List<UserRosterVO> roster();
}
