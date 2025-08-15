package com.zjh.zzone.iot.media.event.media;

import com.ylg.iot.media.gb28181.event.MediaEvent;
import com.ylg.iot.media.zlm.hook.params.OnStreamNotFoundHookParam;
import com.ylg.iot.entity.MediaServer;

/**
 * 流未找到
 */
public class MediaNotFoundEvent extends MediaEvent {
    public MediaNotFoundEvent(Object source) {
        super(source);
    }

    public static MediaNotFoundEvent getInstance(Object source, OnStreamNotFoundHookParam hookParam, MediaServer mediaServer){
        MediaNotFoundEvent mediaDepartureEven = new MediaNotFoundEvent(source);
        mediaDepartureEven.setApp(hookParam.getApp());
        mediaDepartureEven.setStream(hookParam.getStream());
        mediaDepartureEven.setSchema(hookParam.getSchema());
        mediaDepartureEven.setMediaServer(mediaServer);
        return mediaDepartureEven;
    }
}
