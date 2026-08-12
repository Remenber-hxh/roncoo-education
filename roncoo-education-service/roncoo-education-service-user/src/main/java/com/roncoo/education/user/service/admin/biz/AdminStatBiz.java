package com.roncoo.education.user.service.admin.biz;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import com.roncoo.education.common.core.base.Result;
import com.roncoo.education.common.base.BaseBiz;
import com.roncoo.education.course.feign.interfaces.IFeignCourse;
import com.roncoo.education.user.dao.UsersDao;
import com.roncoo.education.user.dao.UsersLogDao;
import com.roncoo.education.user.dao.impl.mapper.entity.UsersExample;
import com.roncoo.education.user.service.admin.resp.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 注册登录统计
 *
 * @author wujing
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminStatBiz extends BaseBiz {

    @NotNull
    private final UsersLogDao usersLogDao;

    @NotNull
    private final UsersDao usersDao;

    @NotNull
    private final IFeignCourse feignCourse;

    public Result<AdminStatLoginResp> statLogin(Integer dates) {
        AdminStatLoginResp resp = new AdminStatLoginResp();
        List<AdminStatLogin> respList = usersLogDao.statByDate(dates);
        if (CollUtil.isNotEmpty(respList)) {
            resp.setDateList(respList.stream().map(AdminStatLogin::getDates).distinct().collect(Collectors.toList()));
            Map<String, Long> loginMap = respList.stream().filter(s -> s.getLoginStatus().equals(1)).collect(Collectors.toMap(s -> s.getDates(), s -> s.getLogins()));
            Map<String, Long> registerMap = respList.stream().filter(s -> s.getLoginStatus().equals(2)).collect(Collectors.toMap(s -> s.getDates(), s -> s.getLogins()));
            List<Long> loginList = new ArrayList<>();
            List<Long> registerList = new ArrayList<>();
            for (String data : resp.getDateList()) {
                loginList.add(loginMap.get(data) == null ? 0 : loginMap.get(data));
                registerList.add(registerMap.get(data) == null ? 0 : registerMap.get(data));
            }
            resp.setLoginList(loginList);
            resp.setRegisterList(registerList);
        }
        return Result.success(resp);
    }

    public Result<AdminStatDataResp> statData() {
        // 二开：已移除商品/订单模块，概况页只统计员工数与课程数
        AdminStatDataResp resp = new AdminStatDataResp();
        resp.setUserSum(usersDao.count(new UsersExample()));
        resp.setCourseSum(feignCourse.count());
        return Result.success(resp);
    }

}
