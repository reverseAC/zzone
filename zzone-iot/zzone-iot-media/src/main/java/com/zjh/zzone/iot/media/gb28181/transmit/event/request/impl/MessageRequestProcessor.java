package com.zjh.zzone.iot.media.gb28181.transmit.event.request.impl;

import com.ylg.iot.media.bo.DeviceNotFoundEvent;
import com.ylg.iot.media.bo.RemoteAddressInfo;
import com.ylg.iot.media.bo.SsrcTransaction;
import com.ylg.iot.media.callback.SipSubscribe;
import com.ylg.iot.media.config.UserSetting;
import com.ylg.iot.media.gb28181.event.sip.SipEvent;
import com.ylg.iot.media.gb28181.transmit.event.request.SIPRequestParentProcessor;
import com.ylg.iot.media.gb28181.transmit.event.request.SIPRequestProcessor;
import com.ylg.iot.media.gb28181.session.SipInviteSessionManager;
import com.ylg.iot.media.gb28181.transmit.SIPProcessorHandler;
import com.ylg.iot.media.gb28181.transmit.event.request.impl.message.MessageHandler;
import com.ylg.iot.media.utils.SipUtils;
import com.ylg.iot.media.vo.MediaDeviceVO;
import com.ylg.redis.util.RedisUtils;
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
import javax.sip.header.CSeqHeader;
import javax.sip.header.CallIdHeader;
import javax.sip.message.Response;
import java.text.ParseException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.ylg.iot.constant.MediaCacheConstants.DEVICE_PREFIX;

/**
 * SIP命令类型： MESSAGE请求
 *
 * @author zjh
 * @since 2025-06-20 13:47
 */
@Slf4j
@Component
public class MessageRequestProcessor extends SIPRequestParentProcessor implements InitializingBean, SIPRequestProcessor {

    public final String method = "MESSAGE";

    @Autowired
    private SIPProcessorHandler sipProcessorObserver;

    @Autowired
    private SipSubscribe sipSubscribe;

    @Autowired
    private SipInviteSessionManager sessionManager;

    @Autowired
    private UserSetting userSetting;

    private static final Map<String, MessageHandler> messageHandlerMap = new ConcurrentHashMap<>();

    @Override
    public void afterPropertiesSet() throws Exception {
        // 添加消息处理的订阅
        sipProcessorObserver.addRequestProcessor(method, this);
    }

    public void addHandler(String name, MessageHandler handler) {
        messageHandlerMap.put(name, handler);
    }

    @Override
    public void process(RequestEvent evt) {
        SIPRequest sipRequest = (SIPRequest)evt.getRequest();
        String gbId = SipUtils.getUserIdFromFromHeader(evt.getRequest());
        CallIdHeader callIdHeader = sipRequest.getCallIdHeader();
        CSeqHeader cSeqHeader = sipRequest.getCSeqHeader();
        // 先从会话内查找
        SsrcTransaction ssrcTransaction = sessionManager.getSsrcTransactionByCallId(callIdHeader.getCallId());
        // 兼容海康 媒体通知 消息from字段不是设备ID的问题
        if (ssrcTransaction != null) {
            gbId = ssrcTransaction.getDeviceId();
        }
        // 查询设备是否存在
        log.info("从缓存获取设备信息，key：{}", DEVICE_PREFIX + gbId);
        MediaDeviceVO device = RedisUtils.getObject(DEVICE_PREFIX + gbId, MediaDeviceVO.class);
        try {
            if (device != null) {
                RemoteAddressInfo remoteAddressInfo = SipUtils.getRemoteAddressFromRequest(sipRequest,
                        userSetting.getUseSipSourceIpAsRemoteAddress());

                String hostAddress = remoteAddressInfo.getIp(); sipRequest.getRemoteAddress().getHostAddress()
                int remotePort = remoteAddressInfo.getPort();
                if (!device.getHostAddress().equals(hostAddress + ":" + remotePort)) {
                    log.warn("设备{}的ip地址不匹配，当前ip: {}, 设备ip: {}", gbId, hostAddress + ":" + remotePort, device.getHostAddress());
                    device = null;
                }
            }
            if (device == null) {
                log.warn("[设备未找到 ]deviceId: {}, callId: {}", gbId, callIdHeader.getCallId());
                // 不存在则回复404
                responseAck(sipRequest, Response.NOT_FOUND, "device "+ gbId +" not found");
                SipEvent sipEvent = sipSubscribe.getSubscribe(callIdHeader.getCallId() + cSeqHeader.getSeqNumber());
                if (sipEvent != null && sipEvent.getErrorEvent() != null){
                    DeviceNotFoundEvent deviceNotFoundEvent = new DeviceNotFoundEvent(evt.getDialog());
                    deviceNotFoundEvent.setCallId(callIdHeader.getCallId());
                    SipSubscribe.EventResult eventResult = new SipSubscribe.EventResult(deviceNotFoundEvent);
                    sipEvent.getErrorEvent().response(eventResult);
                }
            } else {
                Element rootElement;
                try {
                    rootElement = getRootElement(evt);
                    if (rootElement == null) {
                        log.error("处理MESSAGE请求  未获取到消息体{}", evt.getRequest());
                        responseAck(sipRequest, Response.BAD_REQUEST, "content is null");
                        return;
                    }
                    String name = rootElement.getName();
                    MessageHandler messageHandler = messageHandlerMap.get(name);
                    if (messageHandler != null) {
                        if (device != null) {
                            messageHandler.handForDevice(evt, device, rootElement);
                        } else { // 由于上面已经判断都为null则直接返回，所以这里device和parentPlatform必有一个不为null
                            messageHandler.handForPlatform(evt, null, rootElement);
                        }
                    } else {
                        // 不支持的message
                        // 不存在则回复415
                        responseAck(sipRequest, Response.UNSUPPORTED_MEDIA_TYPE, "Unsupported message type, must Control/Notify/Query/Response");
                    }
                } catch (DocumentException e) {
                    log.warn("解析XML消息内容异常", e);
                    // 不存在则回复404
                    responseAck(sipRequest, Response.BAD_REQUEST, e.getMessage());
                }
            }
        } catch (SipException e) {
            log.warn("SIP 回复错误", e);
        } catch (InvalidArgumentException e) {
            log.warn("参数无效", e);
        } catch (ParseException e) {
            log.warn("SIP回复时解析异常", e);
        }
    }


}
