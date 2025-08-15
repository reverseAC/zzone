package com.zjh.zzone.iot.media.gb28181.transmit.event.request.impl;

import com.ylg.iot.media.bo.SendRtpInfo;
import com.ylg.iot.media.scheduling.DynamicTask;
import com.ylg.iot.entity.MediaServer;
import com.ylg.iot.entity.MediaDevice;
import com.ylg.iot.media.gb28181.transmit.SIPProcessorHandler;
import com.ylg.iot.media.gb28181.transmit.event.request.SIPRequestParentProcessor;
import com.ylg.iot.media.gb28181.transmit.event.request.SIPRequestProcessor;
import com.ylg.iot.media.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sip.RequestEvent;
import javax.sip.address.SipURI;
import javax.sip.header.CallIdHeader;
import javax.sip.header.FromHeader;
import javax.sip.header.HeaderAddress;
import javax.sip.header.ToHeader;

/**
 * SIP命令类型： ACK请求
 * @author lin
 */
@Slf4j
@Component
public class AckRequestProcessor extends SIPRequestParentProcessor implements InitializingBean, SIPRequestProcessor {

	private final String method = "ACK";

	@Autowired
	private MediaDeviceService deviceService;

	@Autowired
	private MediaDeviceChannelService deviceChannelService;

	@Autowired
	private MediaServerService mediaServerService;

	@Autowired
	private DynamicTask dynamicTask;

	@Autowired
	private PlayService playService;

	@Autowired
	private SendRtpServerService sendRtpServerService;

	@Autowired
	private SIPProcessorHandler sipProcessorObserver;

	@Override
	public void afterPropertiesSet() throws Exception {
		// 添加消息处理的订阅
		sipProcessorObserver.addRequestProcessor(method, this);
	}

	/**   
	 * 处理  ACK请求
	 */
	@Override
	public void process(RequestEvent evt) {
		CallIdHeader callIdHeader = (CallIdHeader)evt.getRequest().getHeader(CallIdHeader.NAME);
		dynamicTask.stop(callIdHeader.getCallId());
		String fromUserId = ((SipURI) ((HeaderAddress) evt.getRequest().getHeader(FromHeader.NAME)).getAddress().getURI()).getUser();
		String toUserId = ((SipURI) ((HeaderAddress) evt.getRequest().getHeader(ToHeader.NAME)).getAddress().getURI()).getUser();
		log.info("[收到ACK]： 来自->{}", fromUserId);
		SendRtpInfo sendRtpItem =  sendRtpServerService.queryByCallId(callIdHeader.getCallId());
		if (sendRtpItem == null) {
			log.warn("[收到ACK]：未找到来自{}，callId: {}", fromUserId, callIdHeader.getCallId());
			return;
		}
		// tcp主动时，此时是级联下级平台，在回复200ok时，本地已经请求zlm开启监听，跳过下面步骤
		if (sendRtpItem.isTcpActive()) {
			log.info("收到ACK，rtp/{} TCP主动方式等收到上级连接后开始发流", sendRtpItem.getStream());
			return;
		}
		MediaServer mediaServer = mediaServerService.getServerByServerId(sendRtpItem.getMediaServerId());
		log.info("收到ACK，rtp/{}开始向上级推流, 目标={}:{}，SSRC={}, 协议:{}",
				sendRtpItem.getStream(),
				sendRtpItem.getIp(),
				sendRtpItem.getPort(),
				sendRtpItem.getSsrc(),
				sendRtpItem.isTcp()?(sendRtpItem.isTcpActive()?"TCP主动":"TCP被动"):"UDP"
		);

		MediaDevice device = deviceService.getByGbId(fromUserId);
		if (device == null) {
			log.warn("[收到ACK]：来自{}，目标为({})的推流信息为找到流体服务[{}]信息",fromUserId, toUserId, sendRtpItem.getMediaServerId());
			return;
		}
		// 设置为收到ACK后发送语音的设备已经在发送200OK开始发流了
		if (!device.getBroadcastPushAfterAck()) {
			return;
		}
		if (mediaServer == null) {
			log.warn("[收到ACK]：来自{}，目标为({})的推流信息为找到流体服务[{}]信息",fromUserId, toUserId, sendRtpItem.getMediaServerId());
			return;
		}
		try {
			if (sendRtpItem.isTcpActive()) {
				mediaServerService.startSendRtpPassive(mediaServer, sendRtpItem, null);
			} else {
				mediaServerService.startSendRtp(mediaServer, sendRtpItem);
			}
		} catch (Exception e) {
			log.error("RTP推流失败: {}", e.getMessage());
			playService.startSendRtpStreamFailHand(sendRtpItem, null, callIdHeader);
		}
	}

}
