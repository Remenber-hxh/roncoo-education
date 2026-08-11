package com.roncoo.education.course.dao.impl.mapper.entity;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

/**
 * 学习时长日汇总（二开新增）
 * <p>
 * 为什么需要这张表：user_study.current_duration 存的是**播放位置**而非累计观看时长，
 * 同一视频看两遍值不变、直接拖到结尾值就是满的，无法回答「这个员工学了多久」。
 * 十月的「学习时长统计（按人/班组/模块，日周月汇总）」必须依赖本表。
 * <p>
 * 按天粒度存储，周/月由 SQL 聚合；带 course_id 以支持「按模块统计」（模块=课程分类）。
 */
@Data
@Accessors(chain = true)
public class UserStudyDaily implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Date gmtCreate;

    private Date gmtModified;

    private Long userId;

    private Long courseId;

    /**
     * 学习日期（服务端日期，不取前端传值）
     */
    private Date studyDate;

    /**
     * 当日在该课程的有效学习秒数
     */
    private Integer durationSec;
}
