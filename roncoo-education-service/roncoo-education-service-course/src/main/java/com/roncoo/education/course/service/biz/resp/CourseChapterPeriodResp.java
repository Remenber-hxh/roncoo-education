package com.roncoo.education.course.service.biz.resp;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * <p>
 * API-课时信息
 * </p>
 *
 * @author wujing
 * @date 2022-08-25
 */
@Data
@Accessors(chain = true)
@Schema(description = "API-课时信息")
public class CourseChapterPeriodResp implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键")
    private Long id;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime gmtCreate;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "修改时间")
    private LocalDateTime gmtModified;

    @Schema(description = "课程ID")
    private Long courseId;

    @Schema(description = "章节ID")
    private Long chapterId;

    @Schema(description = "课时类型")
    private Integer periodType;

    @Schema(description = "课时名称")
    private String periodName;

    @Schema(description = "课时描述")
    private String periodDesc;


    @Schema(description = "资源ID")
    private Long resourceId;

    @Schema(description = "直播ID")
    private Long liveId;

    private ResourceResp resourceResp;

    private LiveResp liveResp;

    @Schema(description = "课时进度")
    private BigDecimal periodProgress = BigDecimal.ZERO;

    /**
     * 是否已解锁（对应需求里的「闯关」）。
     * 课程未开启顺序解锁时恒为 true；开启后，前一课时学完才解锁下一个。
     */
    private Boolean unlocked = Boolean.TRUE;

    /**
     * 未解锁的原因，直接展示给员工。
     * 只写「请先完成上一课时」这类可执行的说明，不回传内部状态码。
     */
    private String lockedReason;
}
