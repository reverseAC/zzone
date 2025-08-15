package com.zjh.zzone.iot.media.gb28181.transmit.event.request.impl.message.response.cmd;

import com.ylg.iot.media.bo.Platform;
import com.ylg.iot.entity.MediaDevice;
import com.ylg.iot.entity.MediaDeviceChannel;
import com.ylg.iot.enums.media.AudioBroadcastCatchStatus;
import com.ylg.iot.media.bo.AudioBroadcastCache;
import com.ylg.iot.media.gb28181.transmit.event.request.SIPRequestParentProcessor;
import com.ylg.iot.media.gb28181.transmit.event.request.impl.message.MessageHandler;
import com.ylg.iot.media.gb28181.transmit.event.request.impl.message.response.ResponseMessageHandler;
import com.ylg.iot.media.service.MediaDeviceChannelService;
import com.ylg.iot.media.service.PlayService;
import com.ylg.iot.media.session.AudioBroadcastManager;
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
public class BroadcastResponseMessageHandler extends SIPRequestParentProcessor implements InitializingBean, MessageHandler {

    private final String cmdType = "Broadcast";

    @Autowired
    private ResponseMessageHandler responseMessageHandler;

    @Autowired
    private MediaDeviceChannelService deviceChannelService;

    @Autowired
    private AudioBroadcastManager audioBroadcastManager;

    @Autowired
    private PlayService playService;

    @Override
    public void afterPropertiesSet() throws Exception {
        responseMessageHandler.addHandler(cmdType, this);
    }

    @Override
    public void handForDevice(RequestEvent evt, MediaDevice device, Element rootElement) {

        SIPRequest request = (SIPRequest) evt.getRequest();
        try {
            String channelId = XmlUtil.getText(rootElement, "DeviceID");
            MediaDeviceChannel channel = null;
            if (!channelId.equals(device.getGbId())) {
                channel = deviceChannelService.getChannelByGbId(device.getId(), channelId);
            } else {
                channel = deviceChannelService.getBroadcastChannel(device.getId());
            }
            if (channel == null) {
                log.info("[语音广播]回复： 未找到通道{}/{}", device.getGbId(), channelId );
                // 回复410
                responseAck((SIPRequest) evt.getRequest(), Response.NOT_FOUND);
                return;
            }
            if (!audioBroadcastManager.exit(channel.getId())) {
                // 回复410
                responseAck((SIPRequest) evt.getRequest(), Response.BUSY_HERE);
                return;
            }
            String result = XmlUtil.getText(rootElement, "Result");
            Element infoElement = rootElement.element("Info");
            String reason = null;
            if (infoElement != null) {
                reason = XmlUtil.getText(infoElement, "Reason");
            }
            log.info("[语音广播]回复：{}, {}/{}", reason == null? result : result + ": " + reason, device.getGbId(), channelId );

            // 回复200 OK
            responseAck(request, Response.OK);
            if (result.equalsIgnoreCase("OK")) {
                AudioBroadcastCache audioBroadcastCatch = audioBroadcastManager.get(channel.getId());
                audioBroadcastCatch.setStatus(AudioBroadcastCatchStatus.WaiteInvite);
                audioBroadcastManager.update(audioBroadcastCatch);
            } else {
                playService.stopAudioBroadcast(device, channel);
            }
        } catch (ParseException | SipException | InvalidArgumentException e) {
            log.error("[命令发送失败] 国标级联 语音喊话: {}", e.getMessage());
        }
    }

    @Override
    public void handForPlatform(RequestEvent evt, Platform parentPlatform, Element element) {

    }


}
