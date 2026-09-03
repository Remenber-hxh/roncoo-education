package com.roncoo.education.course.service.admin.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * <p>
 * ADMIN-课程信息
 * </p>
 *
 * @author wujing
 */
@Data
@Accessors(chain = true)
@Schema(description = "ADMIN-课程信息修改")
public class AdminCourseEditReq implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "状态(1:正常，0:禁用)")
    private Integer statusId;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "讲师ID")
    private Long lecturerId;

    @Schema(description = "分类ID")
    private Long categoryId;

    @Schema(description = "课程名称")
    private String courseName;

    @Schema(description = "课程封面")
    private String courseLogo;

    @Schema(description = "课程简介")
    private String introduce;




    @Schema(description = "是否上架(1:上架，0:下架)")
    private Integer isPutaway;


    @Schema(description = "学习人数")
    private Integer countStudy;

    @Schema(description = "倍速播放")
    private Integer speedDouble;

    @Schema(description = "拖拽播放")
    private Integer speedDrag;

    @Schema(description = "入职后第几天推送；为空表示不自动推送")
    private Integer pushDay;

    @Schema(description = "推送范围 1全员 2指定班组")
    private Integer pushScope;

    @Schema(description = "指定班组时的班组ID，逗号分隔")
    private String pushTeamIds;

    @Schema(description = "推送后多少天内需完成")
    private Integer deadlineDays;

    @Schema(description = "是否按顺序解锁课时 0否 1是（闯关）")
    private Integer needSequential;
}
