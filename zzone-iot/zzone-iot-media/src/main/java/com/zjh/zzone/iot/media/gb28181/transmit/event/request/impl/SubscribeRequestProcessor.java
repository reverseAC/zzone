package com.zjh.zzone.iot.media.gb28181.transmit.event.request.impl;

import com.ylg.iot.media.gb28181.transmit.SIPProcessorHandler;
import com.ylg.iot.media.gb28181.transmit.event.request.SIPRequestParentProcessor;
import com.ylg.iot.media.gb28181.transmit.event.request.SIPRequestProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sip.RequestEvent;

/**
 * SIP命令类型： SUBSCRIBE请求
 *
 * @author zjh
 * @since 2025-06-20 13:47
 */
@Slf4j
@Component
public class SubscribeRequestProcessor extends SIPRequestParentProcessor implements InitializingBean, SIPRequestProcessor {

    public final String method = "SUBSCRIBE";

    @Autowired
    private SIPProcessorHandler sipProcessorHandler;

    @Override
    public void afterPropertiesSet() throws Exception {
        // 添加消息处理的订阅
        sipProcessorHandler.addRequestProcessor(method, this);
    }

    /**
     * 收到订阅请求 处理
     *
     * @param event 订阅请求
     */
    @Override
    public void process(RequestEvent event) {

    }
}
