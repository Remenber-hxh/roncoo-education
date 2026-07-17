package com.roncoo.education.course.service.admin.req;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;

/**
 * 批量指派课程（二开）
 */
@Data
@Accessors(chain = true)
public class AdminExamAssignBatchReq implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long courseId;
    private List<Long> userIds;
    /** 1必修 2选修 */
    private Integer assignType = 1;
    /** yyyy-MM-dd */
    private String deadline;
}
