package com.zjh.zzone.iot.media.event.media;

import com.ylg.iot.entity.MediaServer;
import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;

/**
 * zlm server_start事件
 */
@Getter
@Setter
public class ZlmServerStartEvent extends ApplicationEvent {

    private MediaServer mediaServerItem;

    public ZlmServerStartEvent(Object source) {
        super(source);
    }

}
