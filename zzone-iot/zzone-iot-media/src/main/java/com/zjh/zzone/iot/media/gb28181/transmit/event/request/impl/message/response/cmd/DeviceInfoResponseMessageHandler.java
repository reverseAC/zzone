package com.zjh.zzone.iot.media.gb28181.transmit.event.request.impl.message.response.cmd;

import com.ylg.iot.constant.DeviceStatusEnum;
import com.ylg.iot.media.bo.Platform;
import com.ylg.iot.entity.MediaDevice;
import com.ylg.iot.media.contant.StreamModeType;
import com.ylg.iot.media.service.MediaDeviceService;
import com.ylg.iot.media.gb28181.transmit.callback.DeferredResultHolder;
import com.ylg.iot.media.gb28181.transmit.callback.RequestMessage;
import com.ylg.iot.media.gb28181.transmit.event.request.SIPRequestParentProcessor;
import com.ylg.iot.media.gb28181.transmit.event.request.impl.message.MessageHandler;
import com.ylg.iot.media.gb28181.transmit.event.request.impl.message.response.ResponseMessageHandler;
import com.ylg.iot.media.utils.XmlUtil;
import gov.nist.javax.sip.message.SIPRequest;
import lombok.extern.slf4j.Slf4j;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import javax.sip.InvalidArgumentException;
import javax.sip.RequestEvent;
import javax.sip.SipException;
import javax.sip.message.Response;
import java.text.ParseException;

/**
 * 设备信息查询的回复
 * @author zjh
 * @since 2025-04-09 16:41
 */
@Slf4j
@Component
public class DeviceInfoResponseMessageHandler extends SIPRequestParentProcessor implements InitializingBean, MessageHandler {

    private final String cmdType = "DeviceInfo";

    @Autowired
    private ResponseMessageHandler responseMessageHandler;

    @Autowired
    private DeferredResultHolder deferredResultHolder;


    @Autowired
    private MediaDeviceService deviceService;

    @Override
    public void afterPropertiesSet() throws Exception {
        responseMessageHandler.addHandler(cmdType, this);
    }

    @Override
    public void handForDevice(RequestEvent evt, MediaDevice device, Element rootElement) {
        log.info("接收到信息查询回复消息：{}", evt.getRequest());
        // 检查设备是否存在， 不存在则不回复
        if (device == null || DeviceStatusEnum.OFFLINE.getCode().equals(device.getOnline())) {
            log.warn("[接收到DeviceInfo应答消息,但是设备已经离线]：" + (device != null ? device.getGbId() : "" ));
            return;
        }
        SIPRequest request = (SIPRequest) evt.getRequest();
        try {
            rootElement = getRootElement(evt, device.getCharset());

            if (rootElement == null) {
                log.warn("[ 接收到DeviceInfo应答消息 ] content cannot be null, {}", evt.getRequest());
                try {
                    responseAck((SIPRequest) evt.getRequest(), Response.BAD_REQUEST);
                } catch (SipException | InvalidArgumentException | ParseException e) {
                    log.error("[命令发送失败] DeviceInfo应答消息 BAD_REQUEST: {}", e.getMessage());
                }
                return;
            }
            Element deviceIdElement = rootElement.element("DeviceID");
            String channelId = deviceIdElement.getTextTrim();
            String key = DeferredResultHolder.CALLBACK_CMD_DEVICE_INFO + device.getGbId() + channelId;
            device.setName(XmlUtil.getText(rootElement, "DeviceName"));

            device.setManufacturer(XmlUtil.getText(rootElement, "Manufacturer"));
            device.setModel(XmlUtil.getText(rootElement, "Model"));
            device.setFirmware(XmlUtil.getText(rootElement, "Firmware"));
            if (ObjectUtils.isEmpty(device.getStreamMode())) {
                device.setStreamMode(StreamModeType.TCP_PASSIVE.name());
            }
            deviceService.updateDevice(device);

            RequestMessage msg = new RequestMessage();
            msg.setKey(key);
            msg.setData(device);
            deferredResultHolder.invokeAllResult(msg);
        } catch (DocumentException e) {
            throw new RuntimeException(e);
        }
        try {
            // 回复200 OK
            responseAck(request, Response.OK);
        } catch (SipException | InvalidArgumentException | ParseException e) {
            log.error("[命令发送失败] DeviceInfo应答消息 200: {}", e.getMessage());
        }

    }

    @Override
    public void handForPlatform(RequestEvent evt, Platform parentPlatform, Element rootElement) {

    }
}
