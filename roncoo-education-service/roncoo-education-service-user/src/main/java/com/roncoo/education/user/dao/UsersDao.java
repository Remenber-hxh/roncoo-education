package com.roncoo.education.user.dao;

import com.roncoo.education.common.base.page.Page;
import com.roncoo.education.user.dao.impl.mapper.entity.Users;
import com.roncoo.education.user.dao.impl.mapper.entity.UsersExample;

import java.util.List;

public interface UsersDao {
    int save(Users record);

    int deleteById(Long id);

    int updateById(Users record);

    /**
     * 只更新员工档案（工号/班组/项目组/岗位/入职日期），且允许把字段改回空值。
     * 走 updateById 会因为选择性更新而清不掉字段，也会在全空时拼出非法 SQL。
     */
    int updateProfileById(Users record);

    Users getById(Long id);

    Page<Users> page(int pageCurrent, int pageSize, UsersExample example);

    Users getByMobile(String mobile);

    List<Users> listByIds(List<Long> userIdList);

    Integer count(UsersExample example);

    List<Users> listByMobile(String mobile);

    /**
     * 根据unionId或openId查询用户，优先查询unionId
     *
     * @param unionId
     * @param openId
     * @return
     */
    Users getByUnionIdOrOpenId(String unionId, String openId);
}
