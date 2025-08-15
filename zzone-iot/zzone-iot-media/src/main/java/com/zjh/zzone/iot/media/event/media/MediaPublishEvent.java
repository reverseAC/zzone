package com.zjh.zzone.iot.media.event.media;

import com.ylg.iot.entity.MediaServer;
import com.ylg.iot.media.gb28181.event.MediaEvent;
import com.ylg.iot.media.zlm.hook.params.OnPublishHookParam;

/**
 * 推流鉴权事件
 */
public class MediaPublishEvent extends MediaEvent {
    public MediaPublishEvent(Object source) {
        super(source);
    }

    public static MediaPublishEvent getInstance(Object source, OnPublishHookParam hookParam, MediaServer mediaServer){
        MediaPublishEvent mediaPublishEvent = new MediaPublishEvent(source);
        mediaPublishEvent.setApp(hookParam.getApp());
        mediaPublishEvent.setStream(hookParam.getStream());
        mediaPublishEvent.setMediaServer(mediaServer);
        mediaPublishEvent.setSchema(hookParam.getSchema());
        mediaPublishEvent.setParams(hookParam.getParams());
        return mediaPublishEvent;
    }

    private String params;

    public String getParams() {
        return params;
    }

    public void setParams(String params) {
        this.params = params;
    }
}
