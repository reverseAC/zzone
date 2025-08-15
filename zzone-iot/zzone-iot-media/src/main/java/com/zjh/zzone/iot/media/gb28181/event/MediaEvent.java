package com.zjh.zzone.iot.media.gb28181.event;

import com.ylg.iot.entity.MediaServer;
import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;

/**
 * 流到来事件
 */
@Getter
@Setter
public class MediaEvent extends ApplicationEvent {

    public MediaEvent(Object source) {
        super(source);
    }

    private String app;

    private String stream;

    private MediaServer mediaServer;

    private String schema;

}
