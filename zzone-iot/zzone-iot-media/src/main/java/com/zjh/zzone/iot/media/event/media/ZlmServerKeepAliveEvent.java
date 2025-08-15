package com.zjh.zzone.iot.media.event.media;

import com.ylg.iot.entity.MediaServer;
import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;

/**
 * zlm 心跳事件
 */
@Getter
@Setter
public class ZlmServerKeepAliveEvent extends ApplicationEvent {

    private MediaServer mediaServerItem;

    public ZlmServerKeepAliveEvent(Object source) {
        super(source);
    }

}
