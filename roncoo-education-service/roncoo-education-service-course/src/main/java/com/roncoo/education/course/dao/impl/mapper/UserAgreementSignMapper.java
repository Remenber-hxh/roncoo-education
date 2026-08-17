package com.roncoo.education.course.dao.impl.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 阅读签署记录（二开新增，注解式）
 * <p>
 * 《培训模块清单》要求「员工手册与规章制度」含签署确认。
 * 员工阅读达标后点击确认，这里留痕（含时间与来源IP），作为培训合规的凭证。
 */
@Mapper
public interface UserAgreementSignMapper {

    /**
     * 是否已签署。唯一键 uk_sign_user_period 保证一人一课时只有一条。
     */
    @Select("select count(*) from user_agreement_sign where user_id = #{userId} and period_id = #{periodId}")
    int exists(@Param("userId") Long userId, @Param("periodId") Long periodId);

    /**
     * 记录签署。重复提交时忽略，保持首次签署时间不变。
     */
    @Insert("insert ignore into user_agreement_sign (id, user_id, period_id, sign_ip) "
            + "values (#{id}, #{userId}, #{periodId}, #{signIp})")
    int save(@Param("id") Long id,
             @Param("userId") Long userId,
             @Param("periodId") Long periodId,
             @Param("signIp") String signIp);

    /**
     * 签署时间，用于门户回显
     */
    @Select("select sign_time from user_agreement_sign where user_id = #{userId} and period_id = #{periodId}")
    java.util.Date signTime(@Param("userId") Long userId, @Param("periodId") Long periodId);
}
