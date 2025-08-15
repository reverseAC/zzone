package com.zjh.zzone.iot.media.gb28181.transmit.event.request.impl.message;

import com.ylg.iot.media.bo.Platform;
import com.ylg.iot.entity.MediaDevice;
import com.ylg.iot.media.contant.ResultCode;
import com.ylg.iot.media.callback.MessageSubscribe;
import com.ylg.iot.media.gb28181.event.sip.MessageEvent;
import com.ylg.iot.media.gb28181.transmit.event.request.SIPRequestParentProcessor;
import com.ylg.iot.media.utils.XmlUtil;
import gov.nist.javax.sip.message.SIPRequest;
import lombok.extern.slf4j.Slf4j;
import org.dom4j.Element;
import org.springframework.beans.factory.annotation.Autowired;

import javax.sip.InvalidArgumentException;
import javax.sip.RequestEvent;
import javax.sip.SipException;
import javax.sip.message.Response;
import java.text.ParseException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@Slf4j
public abstract class MessageParentHandler extends SIPRequestParentProcessor implements MessageHandler{

    public Map<String, MessageHandler> messageHandlerMap = new ConcurrentHashMap<>();

    @Autowired
    private MessageSubscribe messageSubscribe;

    public void addHandler(String cmdType, MessageHandler messageHandler) {
        messageHandlerMap.put(cmdType, messageHandler);
    }

    @Override
    public void handForDevice(RequestEvent evt, MediaDevice device, Element element) {
        String cmd = XmlUtil.getText(element, "CmdType");
        if (cmd == null) {
            try {
                responseAck((SIPRequest) evt.getRequest(), Response.OK);
            } catch (SipException | InvalidArgumentException | ParseException e) {
                log.error("[命令发送失败] 回复200 OK: {}", e.getMessage());
            }
            return;
        }
        MessageHandler messageHandler = messageHandlerMap.get(cmd);

        if (messageHandler != null) {
            messageHandler.handForDevice(evt, device, element);
        }
    }

    @Override
    public void handForPlatform(RequestEvent evt, Platform parentPlatform, Element element) {

    }

    public void handMessageEvent(Element element, Object data) {

        String cmd = XmlUtil.getText(element, "CmdType");
        String sn = XmlUtil.getText(element, "SN");
        MessageEvent<Object> subscribe = (MessageEvent<Object>)messageSubscribe.getSubscribe(cmd + sn);
        if (subscribe != null && subscribe.getCallback() != null) {
            String result = XmlUtil.getText(element, "Result");
            if (result == null || "OK".equalsIgnoreCase(result) || data != null) {
                subscribe.getCallback().run(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMsg(), data);
            } else {
                subscribe.getCallback().run(ResultCode.ERROR500.getCode(), ResultCode.ERROR500.getMsg(), result);
            }
            messageSubscribe.removeSubscribe(cmd + sn);
        }
    }
}
