package com.roncoo.education.course.dao.impl.mapper.entity;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

/**
 * 站内消息（二开新增）
 *
 * @author 二开
 */
@Data
@Accessors(chain = true)
public class UserNotice implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Date gmtCreate;

    private Date gmtModified;

    private Integer statusId;

    /** 接收人 */
    private Long userId;

    /** 1学习催办 2系统通知 */
    private Integer noticeType;

    private String title;

    private String content;

    /** 关联课程，催办时带上，员工可直接点进去学 */
    private Long courseId;

    private Integer isRead;

    private Date readTime;

    /** 联表带出，不落库 */
    private String courseName;
}
