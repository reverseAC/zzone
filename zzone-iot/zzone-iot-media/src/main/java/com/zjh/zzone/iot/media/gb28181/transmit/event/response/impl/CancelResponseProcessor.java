package com.zjh.zzone.iot.media.gb28181.transmit.event.response.impl;

import com.ylg.iot.media.gb28181.transmit.SIPProcessorHandler;
import com.ylg.iot.media.gb28181.transmit.event.response.SIPResponseParentProcessor;
import com.ylg.iot.media.gb28181.transmit.event.response.SIPResponseProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sip.ResponseEvent;

/**
 * CANCEL请求响应
 *
 * @author zjh
 * @since 2025-06-20 15:36
 */
@Slf4j
@Component
public class CancelResponseProcessor extends SIPResponseParentProcessor implements InitializingBean, SIPResponseProcessor {

	private final String method = "CANCEL";

	@Autowired
	private SIPProcessorHandler sipProcessorHandler;

	@Override
	public void afterPropertiesSet() throws Exception {
		sipProcessorHandler.addResponseProcessor(method, this);
	}

	/**
	 * 处理CANCEL响应
	 * 
	 * @param evt 响应消息
	 */
	@Override
	public void process(ResponseEvent evt ){
        log.info("接收到CANCEL消息响应：{}", evt.getResponse());
	}

}
