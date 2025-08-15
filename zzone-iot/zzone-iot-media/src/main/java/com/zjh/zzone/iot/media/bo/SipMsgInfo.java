package com.zjh.zzone.iot.media.bo;

import com.ylg.iot.entity.MediaDevice;
import lombok.Getter;
import lombok.Setter;
import org.dom4j.Element;

import javax.sip.RequestEvent;

@Getter
@Setter
public class SipMsgInfo {
    private RequestEvent evt;
    private MediaDevice device;
    private Platform platform;
    private Element rootElement;

    public SipMsgInfo(RequestEvent evt, MediaDevice device, Element rootElement) {
        this.evt = evt;
        this.device = device;
        this.rootElement = rootElement;
    }

    public SipMsgInfo(RequestEvent evt, Platform platform, Element rootElement) {
        this.evt = evt;
        this.platform = platform;
        this.rootElement = rootElement;
    }
}
