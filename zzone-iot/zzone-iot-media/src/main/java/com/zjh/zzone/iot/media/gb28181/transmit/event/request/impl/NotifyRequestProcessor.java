package com.zjh.zzone.iot.media.gb28181.transmit.event.request.impl;

import com.ylg.iot.media.contant.CmdType;
import com.ylg.iot.media.config.SipConfig;
import com.ylg.iot.media.gb28181.event.EventPublisher;
import com.ylg.iot.media.gb28181.transmit.SIPProcessorHandler;
import com.ylg.iot.media.gb28181.transmit.event.request.SIPRequestParentProcessor;
import com.ylg.iot.media.gb28181.transmit.event.request.SIPRequestProcessor;
import com.ylg.iot.media.utils.XmlUtil;
import com.ylg.iot.media.service.MediaDeviceChannelService;
import gov.nist.javax.sip.message.SIPRequest;
import lombok.extern.slf4j.Slf4j;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sip.InvalidArgumentException;
import javax.sip.RequestEvent;
import javax.sip.SipException;
import javax.sip.message.Response;
import java.text.ParseException;

/**
 * SIP命令类型： NOTIFY请求，NOTIFY请求包含多种子类型，在此类统一接收后进行分发
 *
 * @author zjh
 * @since 2025-06-20 13:47
 */
@Slf4j
@Component
public class NotifyRequestProcessor extends SIPRequestParentProcessor implements InitializingBean, SIPRequestProcessor {

	public final String method = "NOTIFY";

	@Autowired
	private SipConfig sipConfig;

	@Autowired
	private EventPublisher publisher;

	@Autowired
	private SIPProcessorHandler sipProcessorHandler;

	@Autowired
	private MediaDeviceChannelService deviceChannelService;

	@Autowired
	private NotifyRequestForCatalogProcessor notifyRequestForCatalogProcessor;

	@Override
	public void afterPropertiesSet() throws Exception {
		// 添加消息处理的订阅
		sipProcessorHandler.addRequestProcessor(method, this);
	}

	@Override
	public void process(RequestEvent evt) {
		try {
			// 快速响应，然后异步处理NOTIFY消息
			responseAck((SIPRequest) evt.getRequest(), Response.OK, null, null);
			Element rootElement = getRootElement(evt);
			if (rootElement == null) {
				log.error("处理NOTIFY消息时未获取到消息体,{}", evt.getRequest());
				responseAck((SIPRequest) evt.getRequest(), Response.OK, null, null);
				return;
			}
			String cmd = XmlUtil.getText(rootElement, "CmdType");

			if (CmdType.CATALOG.equals(cmd)) {
				// 设备目录变更
				notifyRequestForCatalogProcessor.process(evt);
			} else if (CmdType.ALARM.equals(cmd)) {
				// 告警事件上报
				log.info("接收到告警事件上报消息");
			} else if (CmdType.MOBILE_POSITION.equals(cmd)) {
				// 移动位置推送
				log.info("接收到移动位置推送消息");
			} else {
				log.info("接收到消息：" + cmd);
			}
		} catch (SipException | InvalidArgumentException | ParseException e) {
			log.error("未处理的异常 ", e);
		} catch (DocumentException e) {
			throw new RuntimeException(e);
		}

	}

}
