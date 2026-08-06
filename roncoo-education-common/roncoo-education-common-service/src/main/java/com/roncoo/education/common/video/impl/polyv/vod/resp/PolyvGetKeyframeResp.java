package com.roncoo.education.common.video.impl.polyv.vod.resp;

import java.io.Serializable;

/**
 * @author fengyw
 */
public class PolyvGetKeyframeResp implements Serializable {

    private static final long serialVersionUID = 1L;

    private String duration;

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }
}
