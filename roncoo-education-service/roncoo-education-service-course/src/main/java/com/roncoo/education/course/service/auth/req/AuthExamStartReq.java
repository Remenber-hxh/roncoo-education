package com.roncoo.education.course.service.auth.req;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 开始考试（二开）
 */
@Data
@Accessors(chain = true)
public class AuthExamStartReq implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long paperId;
}
