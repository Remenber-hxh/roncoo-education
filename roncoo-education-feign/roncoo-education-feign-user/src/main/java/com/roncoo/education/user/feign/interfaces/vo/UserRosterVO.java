package com.roncoo.education.user.feign.interfaces.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 员工花名册（二开新增）
 * <p>
 * 给学习统计用。统计要按班组、项目组分组，而员工、班组、项目组都在 user 服务，
 * 学习记录、课程、考试在 course 服务，是同一个库但不同服务，
 * course 服务不能直接查 users 表，只能通过这个接口把花名册整份取过去再本地归组。
 * <p>
 * 不复用 {@link UsersVO}：那个是给评论区、课程详情展示用的，
 * 带着头像、性别、生日；统计只需要归属信息，且要避免把手机号这类
 * 无关的个人信息传到另一个服务去。
 *
 * @author 二开
 */
@Data
@Accessors(chain = true)
public class UserRosterVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    /** 工号 */
    private String empNo;

    /** 姓名 */
    private String nickname;

    private Long teamId;

    /** 班组名称，未归属时为空 */
    private String teamName;

    private Long projectGroupId;

    /** 项目组名称，未归属时为空 */
    private String groupName;
}
