package com.roncoo.education.user.dao.impl.mapper;

import com.roncoo.education.user.dao.impl.mapper.entity.Users;
import com.roncoo.education.user.dao.impl.mapper.entity.UsersExample;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UsersMapper {
    int countByExample(UsersExample example);

    int deleteByExample(UsersExample example);

    int deleteByPrimaryKey(Long id);

    int insert(Users record);

    int insertSelective(Users record);

    List<Users> selectByExample(UsersExample example);

    Users selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("record") Users record, @Param("example") UsersExample example);

    int updateByExample(@Param("record") Users record, @Param("example") UsersExample example);

    int updateByPrimaryKeySelective(Users record);

    /**
     * 二开：只更新员工档案的五个字段，且无条件写入（不做 null 判断）。
     * <p>
     * 不能复用 updateByPrimaryKeySelective：
     * 1. 它按 null 跳过字段，管理员把班组/入职日期改回空值时改不掉，
     * 界面提示成功、库里没变；
     * 2. 五个字段全为空时 &lt;set&gt; 渲染为空，拼出
     * "update users where id=?"，直接 SQL 语法错误。
     */
    int updateProfileById(Users record);

    int updateByPrimaryKey(Users record);
}