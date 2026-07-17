package com.roncoo.education.course.service.admin.req;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 指派分页查询（二开）
 */
@Data
@Accessors(chain = true)
public class AdminExamAssignPageReq implements Serializable {

    private static final long serialVersionUID = 1L;

    private int pageCurrent = 1;
    private int pageSize = 20;
    private Long courseId;
    private Long userId;
    private Integer finishStatus;
}
