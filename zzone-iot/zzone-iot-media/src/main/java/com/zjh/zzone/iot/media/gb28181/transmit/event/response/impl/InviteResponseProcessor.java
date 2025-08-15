package com.zjh.zzone.iot.media.gb28181.transmit.event.response.impl;

import com.ylg.iot.media.bo.Gb28181Sdp;
import com.ylg.iot.media.gb28181.transmit.SIPProcessorHandler;
import com.ylg.iot.media.gb28181.transmit.SIPSender;
import com.ylg.iot.media.gb28181.transmit.cmd.SIPRequestHeaderProvider;
import com.ylg.iot.media.gb28181.transmit.event.response.SIPResponseParentProcessor;
import com.ylg.iot.media.gb28181.transmit.event.response.SIPResponseProcessor;
import com.ylg.iot.media.utils.SipUtils;
import gov.nist.javax.sip.ResponseEventExt;
import gov.nist.javax.sip.message.SIPResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sdp.SdpParseException;
import javax.sdp.SessionDescription;
import javax.sip.InvalidArgumentException;
import javax.sip.ResponseEvent;
import javax.sip.SipException;
import javax.sip.SipFactory;
import javax.sip.address.SipURI;
import javax.sip.message.Request;
import javax.sip.message.Response;
import java.text.ParseException;

/**
 * INVITE请求响应
 *
 * @author zjh
 * @since 2025-06-20 15:36
 */
@Slf4j
@Component
public class InviteResponseProcessor  extends SIPResponseParentProcessor implements InitializingBean, SIPResponseProcessor {

	private final String method = "INVITE";

	@Autowired
	private SIPSender sipSender;

	@Autowired
	private SIPRequestHeaderProvider headerProvider;

	@Autowired
	private SIPProcessorHandler sipProcessorHandler;

	@Override
	public void afterPropertiesSet() throws Exception {
		sipProcessorHandler.addResponseProcessor(method, this);
	}

	/**
	 * 处理invite响应
	 * 
	 * @param evt 响应消息
	 */
	@Override
	public void process(ResponseEvent evt ){
        log.info("接收到INVITE消息响应：{}", evt.getResponse());
		try {
			SIPResponse response = (SIPResponse)evt.getResponse();
			int statusCode = response.getStatusCode();

			if (statusCode == Response.TRYING) {

			} else if (statusCode == Response.OK) {
				// 成功响应
				// 下发ack
				ResponseEventExt event = (ResponseEventExt)evt;

				String contentString = new String(response.getRawContent());
				Gb28181Sdp gb28181Sdp = SipUtils.parseSDP(contentString);
				SessionDescription sdp = gb28181Sdp.getBaseSdb();
				SipURI requestUri = SipFactory.getInstance().createAddressFactory().createSipURI(sdp.getOrigin().getUsername(), event.getRemoteIpAddress() + ":" + event.getRemotePort());
				Request reqAck = headerProvider.createAckRequest(response.getLocalAddress().getHostAddress(), requestUri, response);

				log.info("[回复ack] {}-> {}:{} ", sdp.getOrigin().getUsername(), event.getRemoteIpAddress(), event.getRemotePort());
				sipSender.transmitRequest(response.getLocalAddress().getHostAddress(), reqAck);
			}
		} catch (InvalidArgumentException | ParseException | SipException | SdpParseException e) {
			log.info("[点播回复ACK]，异常：", e );
		}
	}

}
