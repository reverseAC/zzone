package com.zjh.zzone.iot.media.gb28181.transmit.event.response;

import javax.sip.ResponseEvent;

/**
 * SIP响应处理器 接口
 * <p>
 * 处理平台发送请求后，收到的相应请求的响应，非级联平台通常会发送INVITE、BYE、CANCEL、OPTIONS
 *
 * @author zjh
 * @since 2025-03-24 19:56
 */
public interface SIPResponseProcessor {

    void process(ResponseEvent evt);
}
