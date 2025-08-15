package com.zjh.zzone.iot.media.gb28181.transmit.event.request.impl.message.response.cmd;

import com.alibaba.fastjson2.JSONObject;
import com.ylg.iot.media.bo.DeviceStatus;
import com.ylg.iot.media.bo.Platform;
import com.ylg.iot.entity.MediaDevice;
import com.ylg.iot.media.gb28181.transmit.event.request.SIPRequestParentProcessor;
import com.ylg.iot.media.gb28181.transmit.event.request.impl.message.MessageHandler;
import com.ylg.iot.media.gb28181.transmit.event.request.impl.message.response.ResponseMessageHandler;
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

@Slf4j
@Component
public class DeviceStatusResponseMessageHandler extends SIPRequestParentProcessor implements InitializingBean, MessageHandler {

    private final String cmdType = "DeviceStatus";

    @Autowired
    private ResponseMessageHandler responseMessageHandler;

    @Autowired
    private MediaDeviceService deviceService;

    @Override
    public void afterPropertiesSet() throws Exception {
        responseMessageHandler.addHandler(cmdType, this);
    }

    @Override
    public void handForDevice(RequestEvent evt, MediaDevice device, Element element) {
        log.info("接收到DeviceStatus应答消息");
        // 检查设备是否存在， 不存在则不回复
        if (device == null) {
            return;
        }
        // 回复200 OK
        try {
             responseAck((SIPRequest) evt.getRequest(), Response.OK);
        } catch (SipException | InvalidArgumentException | ParseException e) {
            log.error("[命令发送失败] 国标级联 设备状态应答回复200OK: {}", e.getMessage());
        }
        Element onlineElement = element.element("Online");
        Element recordElement = element.element("Record");
        JSONObject json = new JSONObject();
        XmlUtil.node2Json(element, json);
        if (log.isDebugEnabled()) {
            log.debug(json.toJSONString());
        }
        DeviceStatus deviceStatus = new DeviceStatus();
        deviceStatus.setOnline(onlineElement.getText());
        deviceStatus.setRecord(recordElement.getText());
        responseMessageHandler.handMessageEvent(element, deviceStatus);
    }

    @Override
    public void handForPlatform(RequestEvent evt, Platform parentPlatform, Element rootElement) {

    }
}
