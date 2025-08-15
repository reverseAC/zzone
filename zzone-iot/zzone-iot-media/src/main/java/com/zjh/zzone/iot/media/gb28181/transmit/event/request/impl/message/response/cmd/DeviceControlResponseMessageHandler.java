package com.zjh.zzone.iot.media.gb28181.transmit.event.request.impl.message.response.cmd;

import com.ylg.iot.entity.MediaDevice;
import com.ylg.iot.media.bo.Platform;
import com.ylg.iot.media.contant.ResultCode;
import com.ylg.iot.media.callback.MessageSubscribe;
import com.ylg.iot.media.gb28181.event.sip.MessageEvent;
import com.ylg.iot.media.gb28181.transmit.cmd.SIPCommander;
import com.ylg.iot.media.gb28181.transmit.event.request.SIPRequestParentProcessor;
import com.ylg.iot.media.gb28181.transmit.event.request.impl.message.MessageHandler;
import com.ylg.iot.media.gb28181.transmit.event.request.impl.message.response.ResponseMessageHandler;
import com.ylg.iot.media.service.MediaDeviceChannelService;
import com.ylg.iot.media.service.MediaDeviceService;
import com.ylg.iot.media.utils.XmlUtil;
import gov.nist.javax.sip.message.SIPRequest;
import lombok.extern.slf4j.Slf4j;
import org.dom4j.Element;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sip.InvalidArgumentException;
import javax.sip.RequestEvent;
import javax.sip.SipException;
import javax.sip.message.Response;
import java.text.ParseException;

import static com.ylg.iot.media.utils.XmlUtil.getText;

@Slf4j
@Component
public class DeviceControlResponseMessageHandler extends SIPRequestParentProcessor implements InitializingBean, MessageHandler {

    private final String cmdType = "DeviceControl";

    @Autowired
    private ResponseMessageHandler responseMessageHandler;

    @Autowired
    private MediaDeviceService deviceService;

    @Autowired
    private MediaDeviceChannelService deviceChannelService;

    @Autowired
    private SIPCommander cmder;

    @Autowired
    private MessageSubscribe messageSubscribe;

    @Override
    public void afterPropertiesSet() throws Exception {
        responseMessageHandler.addHandler(cmdType, this);
    }

    @Override
    public void handForDevice(RequestEvent evt, MediaDevice device, Element rootElement) {
        try {
            // 回复200 OK
            responseAck((SIPRequest) evt.getRequest(), Response.OK);
        } catch (SipException | InvalidArgumentException | ParseException e) {
            log.error("[命令发送失败] 国标级联 国标录像: {}", e.getMessage());
        }
        try {
            String sn = getText(rootElement, "SN");
            MessageEvent<Object> subscribe = (MessageEvent<Object>)messageSubscribe.getSubscribe(cmdType + sn);
            if (subscribe != null && subscribe.getCallback() != null) {
                String result = XmlUtil.getText(rootElement, "Result");
                if (result == null || "OK".equalsIgnoreCase(result)) {
                    subscribe.getCallback().run(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMsg(), null);
                } else {
                    subscribe.getCallback().run(ResultCode.ERROR500.getCode(), ResultCode.ERROR500.getMsg(), result);
                }
                messageSubscribe.removeSubscribe(sn);
            }
        } catch (Exception e) {
            log.error("[国标录像] 发现未处理的异常, \r\n{}", evt.getRequest());
            log.error("[国标录像] 异常内容： ", e);
        }
    }

    @Override
    public void handForPlatform(RequestEvent evt, Platform platform, Element rootElement) {

    }

}
