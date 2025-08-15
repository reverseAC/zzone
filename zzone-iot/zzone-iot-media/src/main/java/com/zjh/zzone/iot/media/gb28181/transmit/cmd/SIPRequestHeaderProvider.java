package com.zjh.zzone.iot.media.gb28181.transmit.cmd;

import com.ylg.iot.media.gb28181.SipLayer;
import com.ylg.iot.media.config.SipConfig;
import com.ylg.iot.media.bo.SipTransactionInfo;
import com.ylg.iot.entity.MediaDevice;
import com.ylg.iot.media.utils.SipUtils;
import com.ylg.redis.util.RedisUtils;
import gov.nist.javax.sip.message.SIPRequest;
import gov.nist.javax.sip.message.SIPResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sip.InvalidArgumentException;
import javax.sip.PeerUnavailableException;
import javax.sip.SipException;
import javax.sip.SipFactory;
import javax.sip.address.Address;
import javax.sip.address.AddressFactory;
import javax.sip.address.SipURI;
import javax.sip.header.*;
import javax.sip.message.MessageFactory;
import javax.sip.message.Request;
import java.text.ParseException;
import java.util.ArrayList;

import static com.ylg.iot.constant.MediaCacheConstants.SIP_CMD_SEQ_PREFIX;

/**
 * 摄像头命令request构建器
 *
 * @author zjh
 * @since 2025-03-27 17:07
 */
@Component
public class SIPRequestHeaderProvider {

	@Autowired
	private SipConfig sipConfig;
	
	@Autowired
	private SipLayer sipLayer;

	/**
	 * 创建云台控制请求
	 *
	 * @param device 设备信息
	 * @param content 请求体
	 * @param viaTag Via头
	 * @param fromTag From头
	 * @param toTag To头
	 * @param callIdHeader Call-ID头
	 * @return javax.sip.message.Request
	 * @throws ParseException,InvalidArgumentException,PeerUnavailableException 异常
	 */
	public Request createMessageRequest(MediaDevice device, String content, String viaTag, String fromTag, String toTag, CallIdHeader callIdHeader) throws ParseException, InvalidArgumentException, PeerUnavailableException {
		SipFactory sipFactory = SipFactory.getInstance();
		HeaderFactory headerFactory = sipFactory.createHeaderFactory();

		Request request = createCommonRequest(device, device.getGbId(), Request.MESSAGE, viaTag, fromTag, toTag, callIdHeader);

		request.addHeader(SipUtils.createUserAgentHeader());

		ContentTypeHeader contentTypeHeader = headerFactory.createContentTypeHeader("Application", "MANSCDP+xml");
		request.setContent(content, contentTypeHeader);
		return request;
	}

	/**
	 * 创建点播请求
	 *
	 * @param device 设备信息
	 * @param channelId 通道ID
	 * @param content 请求体
	 * @param viaTag Via头
	 * @param fromTag From头
	 * @param toTag To头
	 * @param ssrc ssrc
	 * @param callIdHeader Call-ID头
	 * @return javax.sip.message.Request
	 * @throws ParseException,InvalidArgumentException,PeerUnavailableException 异常
	 */
	public Request createInviteRequest(MediaDevice device, String channelId, String content, String viaTag, String fromTag, String toTag, String ssrc, CallIdHeader callIdHeader) throws ParseException, InvalidArgumentException, PeerUnavailableException {
		SipFactory sipFactory = SipFactory.getInstance();
		AddressFactory addressFactory = sipFactory.createAddressFactory();
		HeaderFactory headerFactory = sipFactory.createHeaderFactory();

		Request request = createCommonRequest(device, channelId, Request.INVITE, viaTag, fromTag, null, callIdHeader);

		request.addHeader(SipUtils.createUserAgentHeader());

		Address concatAddress = addressFactory.createAddress(addressFactory.createSipURI(sipConfig.getId(), sipLayer.getLocalIp(device.getLocalIp())+":"+sipConfig.getPort()));
		request.addHeader(headerFactory.createContactHeader(concatAddress));

		// Subject
		SubjectHeader subjectHeader = headerFactory.createSubjectHeader(String.format("%s:%s,%s:%s", channelId, ssrc, sipConfig.getId(), 0));
		request.addHeader(subjectHeader);

		ContentTypeHeader contentTypeHeader = headerFactory.createContentTypeHeader("APPLICATION", "SDP");
		request.setContent(content, contentTypeHeader);
		return request;
	}

	/**
	 * 创建回放请求
	 *
	 * @param device 设备信息
	 * @param channelId 通道ID
	 * @param content 请求体
	 * @param viaTag Via头
	 * @param fromTag From头
	 * @param toTag To头
	 * @param callIdHeader Call-ID头
	 * @return javax.sip.message.Request
	 * @throws ParseException,InvalidArgumentException,PeerUnavailableException 异常
	 */
	public Request createPlaybackInviteRequest(MediaDevice device, String channelId, String content, String viaTag, String fromTag, String toTag, CallIdHeader callIdHeader, String ssrc) throws ParseException, InvalidArgumentException, PeerUnavailableException {
		SipFactory sipFactory = SipFactory.getInstance();
		AddressFactory addressFactory = sipFactory.createAddressFactory();
		HeaderFactory headerFactory = sipFactory.createHeaderFactory();

		Request request = createCommonRequest(device, channelId, Request.INVITE, viaTag, fromTag, null, callIdHeader);

		Address concatAddress = addressFactory.createAddress(addressFactory.createSipURI(sipConfig.getId(), sipLayer.getLocalIp(device.getLocalIp())+":"+sipConfig.getPort()));
		request.addHeader(headerFactory.createContactHeader(concatAddress));

		request.addHeader(SipUtils.createUserAgentHeader());

		// Subject
		SubjectHeader subjectHeader = headerFactory.createSubjectHeader(String.format("%s:%s,%s:%s", channelId, ssrc, sipConfig.getId(), 0));
		request.addHeader(subjectHeader);

		ContentTypeHeader contentTypeHeader = headerFactory.createContentTypeHeader("APPLICATION", "SDP");
		request.setContent(content, contentTypeHeader);
		return request;
	}

	/**
	 * 创建停止点播请求
	 *
	 * @param device 设备信息
	 * @param channelId 通道ID
	 * @param transactionInfo 事务
	 * @return javax.sip.message.Request
	 * @throws ParseException,InvalidArgumentException,PeerUnavailableException 异常
	 */
	public Request createByeRequest(MediaDevice device, String channelId, SipTransactionInfo transactionInfo) throws ParseException, InvalidArgumentException, PeerUnavailableException {
		SipFactory sipFactory = SipFactory.getInstance();
		AddressFactory addressFactory = sipFactory.createAddressFactory();
		HeaderFactory headerFactory = sipFactory.createHeaderFactory();

		CallIdHeader callIdHeader = headerFactory.createCallIdHeader(transactionInfo.getCallId());

		Request request = createCommonRequest(device, channelId, Request.BYE, transactionInfo.getViaBranch(),
				transactionInfo.getFromTag(), transactionInfo.getToTag(), callIdHeader);

		request.addHeader(SipUtils.createUserAgentHeader());

		Address concatAddress = addressFactory.createAddress(addressFactory.createSipURI(sipConfig.getId(), sipLayer.getLocalIp(device.getLocalIp())+":"+sipConfig.getPort()));
		request.addHeader(headerFactory.createContactHeader(concatAddress));

//		request.addHeader(SipUtils.createUserAgentHeader());
		return request;
	}


	public Request createByteRequest(MediaDevice device, String channelId, SipTransactionInfo transactionInfo) throws ParseException, InvalidArgumentException, PeerUnavailableException {
		Request request = null;
		//请求行
		SipURI requestLine = SipFactory.getInstance().createAddressFactory().createSipURI(channelId, device.getHostAddress());
//		SipURI requestLine = SipFactory.getInstance().createAddressFactory().createSipURI(device.getDeviceId(), device.getHostAddress());
		// via
		ArrayList<ViaHeader> viaHeaders = new ArrayList<ViaHeader>();
//		ViaHeader viaHeader = SipFactory.getInstance().createHeaderFactory().createViaHeader(sipLayer.getLocalIp(device.getLocalIp()), sipConfig.getPort(), device.getTransport(), transactionInfo.getViaBranch());
		ViaHeader viaHeader = SipFactory.getInstance().createHeaderFactory().createViaHeader(sipLayer.getLocalIp(device.getLocalIp()), sipConfig.getPort(), device.getTransport(), SipUtils.getNewViaTag());
//		viaHeader.setRPort();
		viaHeaders.add(viaHeader);
		//from
//		SipURI fromSipURI = SipFactory.getInstance().createAddressFactory().createSipURI(sipConfig.getId(),sipConfig.getDomain());
		SipURI fromSipURI = SipFactory.getInstance().createAddressFactory().createSipURI(sipConfig.getId(), sipLayer.getLocalIp(device.getLocalIp()) + ":" + sipConfig.getPort());
		Address fromAddress = SipFactory.getInstance().createAddressFactory().createAddress(fromSipURI);
		FromHeader fromHeader = SipFactory.getInstance().createHeaderFactory().createFromHeader(fromAddress, transactionInfo.getFromTag());
		//to
		SipURI toSipURI = SipFactory.getInstance().createAddressFactory().createSipURI(channelId,device.getHostAddress());
//		SipURI toSipURI = SipFactory.getInstance().createAddressFactory().createSipURI(device.getDeviceId(),device.getHostAddress());
		Address toAddress = SipFactory.getInstance().createAddressFactory().createAddress(toSipURI);
		ToHeader toHeader = SipFactory.getInstance().createHeaderFactory().createToHeader(toAddress,	transactionInfo.getToTag());

		//Forwards
		MaxForwardsHeader maxForwards = SipFactory.getInstance().createHeaderFactory().createMaxForwardsHeader(70);

		//ceq
		CSeqHeader cSeqHeader = SipFactory.getInstance().createHeaderFactory().createCSeqHeader(RedisUtils.getNextSequence(SIP_CMD_SEQ_PREFIX), Request.BYE);
		CallIdHeader callIdHeader = SipFactory.getInstance().createHeaderFactory().createCallIdHeader(transactionInfo.getCallId());
		request = SipFactory.getInstance().createMessageFactory().createRequest(requestLine, Request.BYE, callIdHeader, cSeqHeader,fromHeader, toHeader, viaHeaders, maxForwards);

		request.addHeader(SipUtils.createUserAgentHeader());

		Address concatAddress = SipFactory.getInstance().createAddressFactory().createAddress(SipFactory.getInstance().createAddressFactory().createSipURI(sipConfig.getId(), sipLayer.getLocalIp(device.getLocalIp())+":"+sipConfig.getPort()));
		request.addHeader(SipFactory.getInstance().createHeaderFactory().createContactHeader(concatAddress));

		request.addHeader(SipUtils.createUserAgentHeader());

		return request;
	}

	/**
	 * 创建停止对讲请求
	 *
	 * @param device 设备信息
	 * @param channelId 通道ID
	 * @param transactionInfo 事务
	 * @return javax.sip.message.Request
	 * @throws ParseException,InvalidArgumentException,PeerUnavailableException 异常
	 */
	public Request createByteRequestForDeviceInvite(MediaDevice device, String channelId, SipTransactionInfo transactionInfo) throws ParseException, InvalidArgumentException, PeerUnavailableException {
		SipFactory sipFactory = SipFactory.getInstance();
		AddressFactory addressFactory = sipFactory.createAddressFactory();
		HeaderFactory headerFactory = sipFactory.createHeaderFactory();

		CallIdHeader callIdHeader = headerFactory.createCallIdHeader(transactionInfo.getCallId());

		Request request = createCommonRequest(device, channelId, Request.BYE, SipUtils.getNewViaTag(),
				transactionInfo.getToTag(), transactionInfo.getFromTag(), callIdHeader);

		request.addHeader(SipUtils.createUserAgentHeader());

		Address concatAddress = addressFactory.createAddress(addressFactory.createSipURI(sipConfig.getId(), sipLayer.getLocalIp(device.getLocalIp())+":"+sipConfig.getPort()));
		request.addHeader(headerFactory.createContactHeader(concatAddress));

		request.addHeader(SipUtils.createUserAgentHeader());
		return request;
	}

	/**
	 * 创建订阅请求
	 *
	 * @param device 设备信息
	 * @param content 请求体
	 * @param requestOld
	 * @param expires
	 * @param event 订阅事件
	 * @param callIdHeader Call-ID头
	 * @return javax.sip.message.Request
	 * @throws ParseException,InvalidArgumentException,PeerUnavailableException 异常
	 */
	public Request createSubscribeRequest(MediaDevice device, String content, SIPRequest requestOld, Integer expires, String event, CallIdHeader callIdHeader) throws ParseException, InvalidArgumentException, PeerUnavailableException {
		SipFactory sipFactory = SipFactory.getInstance();
		AddressFactory addressFactory = sipFactory.createAddressFactory();
		HeaderFactory headerFactory = sipFactory.createHeaderFactory();

		Request request = createCommonRequest(device, device.getGbId(), Request.SUBSCRIBE, SipUtils.getNewViaTag(),
				requestOld == null ? SipUtils.getNewFromTag() :requestOld.getFromTag(),
				requestOld == null ? null :requestOld.getToTag(),
				callIdHeader);

		Address concatAddress = addressFactory.createAddress(addressFactory.createSipURI(sipConfig.getId(), sipLayer.getLocalIp(device.getLocalIp())+":"+sipConfig.getPort()));
		request.addHeader(headerFactory.createContactHeader(concatAddress));

		// Expires
		ExpiresHeader expireHeader = headerFactory.createExpiresHeader(expires);
		request.addHeader(expireHeader);

		// Event
		EventHeader eventHeader = headerFactory.createEventHeader(event);

		int random = (int) Math.floor(Math.random() * 10000);
		eventHeader.setEventId(random + "");
		request.addHeader(eventHeader);

		ContentTypeHeader contentTypeHeader = headerFactory.createContentTypeHeader("Application", "MANSCDP+xml");
		request.setContent(content, contentTypeHeader);

		request.addHeader(SipUtils.createUserAgentHeader());

		return request;
	}

	/**
	 * 创建回放控制请求
	 *
	 * @param device 设备信息
	 * @param channelId 通道ID
	 * @param content 请求体
	 * @param transactionInfo 事务
	 * @return javax.sip.message.Request
	 * @throws ParseException,InvalidArgumentException,PeerUnavailableException 异常
	 */
	public SIPRequest createInfoRequest(MediaDevice device, String channelId, String content, SipTransactionInfo transactionInfo)
			throws SipException, ParseException, InvalidArgumentException {
		if (device == null || transactionInfo == null) {
			return null;
		}
		SipFactory sipFactory = SipFactory.getInstance();
		AddressFactory addressFactory = sipFactory.createAddressFactory();
		HeaderFactory headerFactory = sipFactory.createHeaderFactory();

		CallIdHeader callIdHeader = headerFactory.createCallIdHeader(transactionInfo.getCallId());

		SIPRequest request = (SIPRequest)createCommonRequest(device, channelId, Request.INFO, SipUtils.getNewViaTag(),
				transactionInfo.getFromTag(), transactionInfo.getToTag(), callIdHeader);

		request.addHeader(SipUtils.createUserAgentHeader());

		Address concatAddress = addressFactory.createAddress(addressFactory.createSipURI(sipConfig.getId(), sipLayer.getLocalIp(device.getLocalIp())+":"+sipConfig.getPort()));
		request.addHeader(headerFactory.createContactHeader(concatAddress));

		request.addHeader(SipUtils.createUserAgentHeader());

		if (content != null) {
			ContentTypeHeader contentTypeHeader = headerFactory.createContentTypeHeader("Application", "MANSRTSP");
			request.setContent(content, contentTypeHeader);
		}
		return request;
	}

	/**
	 * 创建INVITE响应请求
	 *
	 * @param localIp 平台IP
	 * @param sipURI sip uri
	 * @param sipResponse sip响应
	 * @return javax.sip.message.Request
	 * @throws ParseException,InvalidArgumentException,PeerUnavailableException 异常
	 */
	public Request createAckRequest(String localIp, SipURI sipURI, SIPResponse sipResponse) throws ParseException, InvalidArgumentException, PeerUnavailableException {

		SipFactory sipFactory = SipFactory.getInstance();
		AddressFactory addressFactory = sipFactory.createAddressFactory();
		HeaderFactory headerFactory = sipFactory.createHeaderFactory();
		MessageFactory messageFactory = sipFactory.createMessageFactory();

		// via
		ArrayList<ViaHeader> viaHeaders = new ArrayList<ViaHeader>();
		ViaHeader viaHeader = headerFactory.createViaHeader(localIp, sipConfig.getPort(), sipResponse.getTopmostViaHeader().getTransport(), SipUtils.getNewViaTag());
		viaHeaders.add(viaHeader);
		//Forwards
		MaxForwardsHeader maxForwards = headerFactory.createMaxForwardsHeader(70);
		//ceq
		CSeqHeader cSeqHeader = headerFactory.createCSeqHeader(sipResponse.getCSeqHeader().getSeqNumber(), Request.ACK);


		Request request = messageFactory.createRequest(sipURI, Request.ACK, sipResponse.getCallIdHeader(), cSeqHeader, sipResponse.getFromHeader(), sipResponse.getToHeader(), viaHeaders, maxForwards);
		request.addHeader(SipUtils.createUserAgentHeader());
		Address concatAddress = addressFactory.createAddress(addressFactory.createSipURI(sipConfig.getId(), localIp + ":"+sipConfig.getPort()));
		request.addHeader(headerFactory.createContactHeader(concatAddress));
		request.addHeader(SipUtils.createUserAgentHeader());
		return request;
	}

	private Request createCommonRequest(
			MediaDevice device,
			String user,
			String method,
			String viaTag,
			String fromTag,
			String toTag,
			CallIdHeader callIdHeader
	) throws PeerUnavailableException, ParseException, InvalidArgumentException {

		SipFactory sipFactory = SipFactory.getInstance();
		AddressFactory addressFactory = sipFactory.createAddressFactory();
		HeaderFactory headerFactory = sipFactory.createHeaderFactory();
		MessageFactory messageFactory = sipFactory.createMessageFactory();

		// spi uri
		SipURI requestLine = addressFactory.createSipURI(user, device.getHostAddress());
		// via
		ArrayList<ViaHeader> viaHeaders = new ArrayList<>();
		ViaHeader viaHeader = headerFactory.createViaHeader(sipLayer.getLocalIp(device.getLocalIp()), sipConfig.getPort(), device.getTransport(), viaTag);
		viaHeader.setRPort();
		viaHeaders.add(viaHeader);
		// from
		SipURI fromSipURI = addressFactory.createSipURI(sipConfig.getId(), sipConfig.getDomain());
		Address fromAddress = addressFactory.createAddress(fromSipURI);
		FromHeader fromHeader = headerFactory.createFromHeader(fromAddress, fromTag); //必须要有标记，否则无法创建会话，无法回应ack
		// to
		SipURI toSipURI = addressFactory.createSipURI(user, device.getHostAddress());
		Address toAddress = addressFactory.createAddress(toSipURI);
		ToHeader toHeader = headerFactory.createToHeader(toAddress, toTag);
		//Forwards
		MaxForwardsHeader maxForwards = headerFactory.createMaxForwardsHeader(70);
		//ceq
		CSeqHeader cSeqHeader = headerFactory.createCSeqHeader(RedisUtils.getNextSequence(SIP_CMD_SEQ_PREFIX), method);

		return messageFactory.createRequest(requestLine, method, callIdHeader, cSeqHeader,fromHeader, toHeader, viaHeaders, maxForwards);
	}
}
