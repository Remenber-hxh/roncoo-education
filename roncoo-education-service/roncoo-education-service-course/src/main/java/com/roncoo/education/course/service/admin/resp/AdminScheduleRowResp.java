package com.roncoo.education.course.service.admin.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * ADMIN-排课配置的一行（二开新增）
 * <p>
 * 一门课在「排课配置」表里的全部可编辑项。
 * 这张表与需求文件《线上入职培训系统—培训模块清单》的「培训模块总览」
 * 是一一对应的：需求上写「入职第3天 / 强电组重点 / 测验+闯关」，
 * 在这里就是 pushDay=3、指定班组=强电组、needSequential=1。
 *
 * @author 二开
 */
@Data
@Accessors(chain = true)
@Schema(description = "ADMIN-排课配置行")
public class AdminScheduleRowResp implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long courseId;

    private String courseName;

    @Schema(description = "所属模块（课程分类）名称")
    private String categoryName;

    @Schema(description = "是否上架。未上架的课即使配了推送也不会派出去，表格里要能看出来")
    private Integer isPutaway;

    @Schema(description = "入职后第几天推送；为空表示不自动推送")
    private Integer pushDay;

    @Schema(description = "推送范围 1全员 2指定班组")
    private Integer pushScope;

    @Schema(description = "指定班组的ID，逗号分隔")
    private String pushTeamIds;

    @Schema(description = "推送后多少天内需完成")
    private Integer deadlineDays;

    @Schema(description = "是否按顺序解锁课时（闯关）0否 1是")
    private Integer needSequential;

    @Schema(description = "已发布课时数。为 0 的课程派出去员工也学不了，表格里标出来")
    private Integer periodCount;
}
