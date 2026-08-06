package com.roncoo.education.common.video.impl.polyv.vod.resp;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 课时信息获取sign值
 *
 * @author forest
 */
@Data
@Accessors(chain = true)
public class PolyvPlayResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private String vid;

    private String ts;

    private String sign;

    private String token;

    private String code;
}
