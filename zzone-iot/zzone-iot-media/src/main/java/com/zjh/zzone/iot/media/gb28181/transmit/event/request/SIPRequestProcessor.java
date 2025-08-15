package com.zjh.zzone.iot.media.gb28181.transmit.event.request;

import javax.sip.RequestEvent;

/**
 * SIP请求处理器 接口
 *
 * @author zjh
 * @since 2025-03-24 15:33
 */
public interface SIPRequestProcessor {

    /**
     * SIP请求处理方法
     *
     * @param event SIP请求事件
     */
    void process(RequestEvent event);
}
