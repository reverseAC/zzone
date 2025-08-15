package com.zjh.zzone.iot.media.gb28181.transmit;

import com.ylg.iot.media.bo.SipTransactionInfo;
import com.ylg.iot.media.gb28181.SipLayer;
import com.ylg.iot.media.config.SipConfig;
import com.ylg.iot.media.callback.SipSubscribe;
import com.ylg.iot.media.gb28181.event.sip.SipEvent;
import com.ylg.iot.media.utils.SipUtils;
import gov.nist.javax.sip.SipProviderImpl;
import gov.nist.javax.sip.address.SipUri;
import gov.nist.javax.sip.message.SIPRequest;
import gov.nist.javax.sip.message.SIPResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import javax.sip.SipException;
import javax.sip.header.*;
import javax.sip.message.Message;
import javax.sip.message.Request;
import javax.sip.message.Response;
import java.text.ParseException;

/**
 * SIP消息发送
 *
 * @author zjh
 * @since 2025-03-24 15:37
 */
@Slf4j
@Component
public class SIPSender {

    @Autowired
    private SipLayer sipLayer;

    @Autowired
    private SipSubscribe sipSubscribe;

    @Autowired
    private SipConfig sipConfig;

    public void transmitRequest(String ip, Message message) throws SipException, ParseException {
        transmitRequest(ip, message, null, null, null);
    }

    public void transmitRequest(String ip, Message message, SipSubscribe.Event errorEvent) throws SipException, ParseException {
        transmitRequest(ip, message, errorEvent, null, null);
    }

    public void transmitRequest(String ip, Message message, SipSubscribe.Event errorEvent, SipSubscribe.Event okEvent) throws SipException {
        transmitRequest(ip, message, errorEvent, okEvent, null);
    }

    /**
     * 发送请求
     *
     * @param ip 平台ip
     * @param message 消息
     * @param errorEvent 错误事件
     * @param okEvent 成功事件
     * @param timeout 超时时间
     * @throws SipException 异常
     */
    public void transmitRequest(String ip, Message message, SipSubscribe.Event errorEvent, SipSubscribe.Event okEvent,
                                Long timeout) throws SipException {

        ViaHeader viaHeader = (ViaHeader) message.getHeader(ViaHeader.NAME);
        String transport = "UDP";
        if (viaHeader == null) {
            log.warn("[消息头缺失]： ViaHeader， 使用默认的UDP方式处理数据");
        } else {
            transport = viaHeader.getTransport();
        }
        if (message.getHeader(UserAgentHeader.NAME) == null) {
            try {
                message.addHeader(SipUtils.createUserAgentHeader());
            } catch (ParseException e) {
                log.error("添加UserAgentHeader失败", e);
            }
        }

        CallIdHeader callIdHeader = (CallIdHeader) message.getHeader(CallIdHeader.NAME);
        CSeqHeader cSeqHeader = (CSeqHeader) message.getHeader(CSeqHeader.NAME);
        String key = callIdHeader.getCallId() + cSeqHeader.getSeqNumber();
        if (okEvent != null || errorEvent != null) {

            FromHeader fromHeader = (FromHeader) message.getHeader(FromHeader.NAME);
            SipEvent sipEvent = SipEvent.getInstance(key, eventResult -> {
                sipSubscribe.removeSubscribe(key);
                if(okEvent != null) {
                    okEvent.response(eventResult);
                }
            }, (eventResult -> {
                sipSubscribe.removeSubscribe(key);
                if (errorEvent != null) {
                    errorEvent.response(eventResult);
                }
            }), timeout == null ? sipConfig.getTimeout() : timeout);
            SipTransactionInfo sipTransactionInfo = new SipTransactionInfo();
            sipTransactionInfo.setFromTag(fromHeader.getTag());
            sipTransactionInfo.setCallId(callIdHeader.getCallId());

            if (message instanceof SIPResponse) {
                SIPResponse response = (SIPResponse) message;
                sipTransactionInfo.setToTag(response.getToHeader().getTag());
                sipTransactionInfo.setViaBranch(response.getTopmostViaHeader().getBranch());
            }else if (message instanceof SIPRequest) {
                SIPRequest request = (SIPRequest) message;
                sipTransactionInfo.setViaBranch(request.getTopmostViaHeader().getBranch());
                SipUri sipUri = (SipUri)request.getRequestLine().getUri();
                sipTransactionInfo.setUser(sipUri.getUser());
            }

            ExpiresHeader expiresHeader = (ExpiresHeader) message.getHeader(ExpiresHeader.NAME);
            if (expiresHeader != null) {
                sipTransactionInfo.setExpires(expiresHeader.getExpires());
            }
            sipEvent.setSipTransactionInfo(sipTransactionInfo);
            sipSubscribe.addSubscribe(key, sipEvent);
        }

        if ("TCP".equals(transport)) {
            SipProviderImpl tcpSipProvider = sipLayer.getTcpSipProvider(ip);
            if (tcpSipProvider == null) {
                log.error("[发送信息失败] 未找到tcp://{}的监听信息", ip);
                return;
            }
            if (message instanceof Request) {
                tcpSipProvider.sendRequest((Request) message);  // 发送请求
            } else if (message instanceof Response) {
                tcpSipProvider.sendResponse((Response) message);
            }

        } else if ("UDP".equals(transport)) {
            SipProviderImpl sipProvider = sipLayer.getUdpSipProvider(ip);
            if (sipProvider == null) {
                log.error("[发送信息失败] 未找到udp://{}的监听信息", ip);
                return;
            }
            if (message instanceof Request) {
                sipProvider.sendRequest((Request) message); // 发送请求
            } else if (message instanceof Response) {
                sipProvider.sendResponse((Response) message);
            }
        }
    }

    public CallIdHeader getNewCallIdHeader(String ip, String transport) {
        if (ObjectUtils.isEmpty(transport)) {
            return sipLayer.getUdpSipProvider().getNewCallId();
        }
        SipProviderImpl sipProvider;
        if (ObjectUtils.isEmpty(ip)) {
            sipProvider = transport.equalsIgnoreCase("TCP") ? sipLayer.getTcpSipProvider()
                    : sipLayer.getUdpSipProvider();
        } else {
            sipProvider = transport.equalsIgnoreCase("TCP") ? sipLayer.getTcpSipProvider(ip)
                    : sipLayer.getUdpSipProvider(ip);
        }

        if (sipProvider == null) {
            sipProvider = sipLayer.getUdpSipProvider();
        }

        if (sipProvider != null) {
            return sipProvider.getNewCallId();
        } else {
            log.warn("[新建CallIdHeader失败]， ip={}, transport={}", ip, transport);
            return null;
        }
    }

}
