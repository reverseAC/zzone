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
 * SIP命令类型： CANCEL请求
 */
@Slf4j
@Component
public class CancelRequestProcessor extends SIPRequestParentProcessor implements InitializingBean, SIPRequestProcessor {

	private final String method = "CANCEL";

	@Autowired
	private SIPProcessorHandler sipProcessorObserver;

	@Override
	public void afterPropertiesSet() throws Exception {
		// 添加消息处理的订阅
		sipProcessorObserver.addRequestProcessor(method, this);
	}

	/**   
	 * 处理CANCEL请求
	 *  
	 * @param event 事件
	 */
	@Override
	public void process(RequestEvent event) {
		log.info("收到来自设备的CANCEL请求: {}", event.getRequest());

	}

}
