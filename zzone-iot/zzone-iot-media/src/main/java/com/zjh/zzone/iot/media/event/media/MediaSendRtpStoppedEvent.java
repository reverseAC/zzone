package com.zjh.zzone.iot.media.event.media;

import com.ylg.iot.media.zlm.hook.params.OnStreamNotFoundHookParam;
import com.ylg.iot.entity.MediaServer;
import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;

/**
 * 发送流停止事件
 */
@Getter
@Setter
public class MediaSendRtpStoppedEvent extends ApplicationEvent {
    public MediaSendRtpStoppedEvent(Object source) {
        super(source);
    }

    private String app;

    private String stream;

    private MediaServer mediaServer;

    public static MediaSendRtpStoppedEvent getInstance(Object source, OnStreamNotFoundHookParam hookParam, MediaServer mediaServer){
        MediaSendRtpStoppedEvent mediaDepartureEven = new MediaSendRtpStoppedEvent(source);
        mediaDepartureEven.setApp(hookParam.getApp());
        mediaDepartureEven.setStream(hookParam.getStream());
        mediaDepartureEven.setMediaServer(mediaServer);
        return mediaDepartureEven;
    }

}
