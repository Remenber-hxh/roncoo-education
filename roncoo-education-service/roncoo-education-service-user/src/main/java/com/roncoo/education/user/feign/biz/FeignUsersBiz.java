package com.roncoo.education.user.feign.biz;


import cn.hutool.core.collection.CollUtil;
import com.roncoo.education.common.core.enums.StatusIdEnum;
import com.roncoo.education.common.tools.BeanUtil;
import com.roncoo.education.common.base.BaseBiz;
import com.roncoo.education.user.dao.ProjectGroupDao;
import com.roncoo.education.user.dao.TeamDao;
import com.roncoo.education.user.dao.UsersDao;
import com.roncoo.education.user.dao.impl.mapper.entity.ProjectGroup;
import com.roncoo.education.user.dao.impl.mapper.entity.Team;
import com.roncoo.education.user.dao.impl.mapper.entity.Users;
import com.roncoo.education.user.dao.impl.mapper.entity.UsersExample;
import com.roncoo.education.user.feign.interfaces.vo.UserRosterVO;
import com.roncoo.education.user.feign.interfaces.vo.UsersVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 讲师信息
 *
 * @author wujing
 */
@Component
@RequiredArgsConstructor
public class FeignUsersBiz extends BaseBiz {

    @NotNull
    private final UsersDao dao;

    @NotNull
    private final TeamDao teamDao;

    @NotNull
    private final ProjectGroupDao projectGroupDao;

    public UsersVO getById(Long id) {
        Users record = dao.getById(id);
        return BeanUtil.copyProperties(record, UsersVO.class);
    }

    public UsersVO getByMobile(String mobile) {
        Users record = dao.getByMobile(mobile);
        return BeanUtil.copyProperties(record, UsersVO.class);
    }

    public List<UsersVO> listByMobile(String mobile) {
        List<Users> list = dao.listByMobile(mobile);
        return BeanUtil.copyProperties(list, UsersVO.class);
    }

    public Map<Long, UsersVO> listByIds(List<Long> userIdList) {
        List<Users> usersList = dao.listByIds(userIdList);
        if (CollUtil.isNotEmpty(usersList)) {
            return usersList.stream().collect(Collectors.toMap(item -> item.getId(), item -> BeanUtil.copyProperties(item, UsersVO.class)));
        }
        return new HashMap<>();
    }

    /**
     * 花名册：启用状态的员工 + 班组/项目组名称。
     * <p>
     * 班组、项目组名称在这里就查好带上，不让调用方拿着 ID 再回头问一次——
     * 调用方是另一个服务，多一次往返没有意义。
     * 逐个员工查字典会有 N+1，字典只有十来条，一次性查出来在内存里对。
     */
    public List<UserRosterVO> roster() {
        UsersExample example = new UsersExample();
        example.createCriteria().andStatusIdEqualTo(StatusIdEnum.YES.getCode());
        example.setOrderByClause("emp_no + 0 asc, id asc");
        List<Users> list = dao.listByExample(example);
        if (CollUtil.isEmpty(list)) {
            return new ArrayList<>();
        }

        Map<Long, String> teamNames = teamDao.listByStatusId(StatusIdEnum.YES.getCode()).stream()
                .collect(Collectors.toMap(Team::getId, Team::getTeamName));
        Map<Long, String> groupNames = projectGroupDao.listByStatusId(StatusIdEnum.YES.getCode()).stream()
                .collect(Collectors.toMap(ProjectGroup::getId, ProjectGroup::getGroupName));

        List<UserRosterVO> result = new ArrayList<>(list.size());
        for (Users u : list) {
            result.add(new UserRosterVO()
                    .setId(u.getId())
                    .setEmpNo(u.getEmpNo())
                    .setNickname(u.getNickname())
                    .setTeamId(u.getTeamId())
                    .setTeamName(u.getTeamId() == null ? null : teamNames.get(u.getTeamId()))
                    .setProjectGroupId(u.getProjectGroupId())
                    .setGroupName(u.getProjectGroupId() == null ? null : groupNames.get(u.getProjectGroupId())));
        }
        return result;
    }
}
