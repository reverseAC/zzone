package com.zjh.zzone.iot.media.gb28181.transmit.event.request.impl.message.notify.cmd;

import com.ylg.core.utils.bean.BeanUtils;
import com.ylg.iot.media.bo.Platform;
import com.ylg.iot.media.bo.RemoteAddressInfo;
import com.ylg.iot.media.bo.SipMsgInfo;
import com.ylg.iot.entity.MediaDevice;
import com.ylg.iot.enums.DeviceStatusEnum;
import com.ylg.iot.media.scheduling.DynamicTask;
import com.ylg.iot.media.config.UserSetting;
import com.ylg.iot.media.gb28181.task.deviceStatus.DeviceStatusTaskRunner;
import com.ylg.iot.media.gb28181.transmit.event.request.SIPRequestParentProcessor;
import com.ylg.iot.media.gb28181.transmit.event.request.impl.message.MessageHandler;
import com.ylg.iot.media.gb28181.transmit.event.request.impl.message.notify.NotifyMessageHandler;
import com.ylg.iot.media.service.MediaDeviceService;
import com.ylg.iot.media.utils.SipUtils;
import com.ylg.iot.media.vo.MediaDeviceVO;
import gov.nist.javax.sip.message.SIPRequest;
import lombok.extern.slf4j.Slf4j;
import org.dom4j.Element;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.sip.InvalidArgumentException;
import javax.sip.RequestEvent;
import javax.sip.SipException;
import javax.sip.message.Response;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 状态信息(心跳)报送
 */
@Slf4j
@Component
public class KeepaliveNotifyMessageHandler extends SIPRequestParentProcessor implements InitializingBean, MessageHandler {


    private final static String cmdType = "Keepalive";

    private final ConcurrentLinkedQueue<SipMsgInfo> taskQueue = new ConcurrentLinkedQueue<>();

    @Autowired
    private NotifyMessageHandler notifyMessageHandler;

    @Autowired
    private MediaDeviceService deviceService;

    @Autowired
    private DeviceStatusTaskRunner statusTaskRunner;

    @Autowired
    private UserSetting userSetting;

    @Autowired
    private DynamicTask dynamicTask;

    @Override
    public void afterPropertiesSet() throws Exception {
        notifyMessageHandler.addHandler(cmdType, this);
    }

    @Override
    public void handForDevice(RequestEvent evt, MediaDevice device, Element rootElement) {
        if (taskQueue.size() >= userSetting.getMaxNotifyCountQueue()) {
            log.error("[心跳] 待处理消息队列已满 {}，返回486 BUSY_HERE，消息不做处理", userSetting.getMaxNotifyCountQueue());
            return;
        }
        taskQueue.offer(new SipMsgInfo(evt, device, rootElement));
    }

    @Scheduled(fixedDelay = 100)
    public void executeTaskQueue() {
        log.debug("Scheduled: KeepaliveNotifyMessageHandler...");
        if (taskQueue.isEmpty()) {
            return;
        }
        List<SipMsgInfo> handlerCatchDataList = new ArrayList<>();
        int size = taskQueue.size();
        for (int i = 0; i < size; i++) {
            SipMsgInfo poll = taskQueue.poll();
            if (poll != null) {
                handlerCatchDataList.add(poll);
            }
        }
        if (handlerCatchDataList.isEmpty()) {
            return;
        }
        List<MediaDevice> deviceListForUpdate = new ArrayList<>();
        for (SipMsgInfo sipMsgInfo : handlerCatchDataList) {
            if (sipMsgInfo == null) {
                continue;
            }
            RequestEvent evt = sipMsgInfo.getEvt();
            // 回复200 OK
            try {
                responseAck((SIPRequest) evt.getRequest(), Response.OK);
            } catch (SipException | InvalidArgumentException | ParseException e) {
                log.error("[命令发送失败] 心跳回复: {}", e.getMessage());
            }
            MediaDevice device = sipMsgInfo.getDevice();
            SIPRequest request = (SIPRequest) evt.getRequest();

            RemoteAddressInfo remoteAddressInfo = SipUtils.getRemoteAddressFromRequest(request, userSetting.getUseSipSourceIpAsRemoteAddress());
            if (device.getIp() == null || !device.getIp().equalsIgnoreCase(remoteAddressInfo.getIp()) || device.getPort() != remoteAddressInfo.getPort()) {
                log.info("[收到心跳] 地址变化, {}({}), {}:{}->{}", device.getName(), device.getGbId(), remoteAddressInfo.getIp(), remoteAddressInfo.getPort(), request.getLocalAddress().getHostAddress());
                device.setPort(remoteAddressInfo.getPort());
                device.setHostAddress(remoteAddressInfo.getIp().concat(":").concat(String.valueOf(remoteAddressInfo.getPort())));
                device.setIp(remoteAddressInfo.getIp());
                device.setLocalIp(request.getLocalAddress().getHostAddress());
            }

            device.setKeepAliveTime(LocalDateTime.now());

            if (DeviceStatusEnum.ONLINE.getCode().equals(device.getOnline())) {
                deviceListForUpdate.add(device);
                long expiresTime = Math.min(device.getExpires(), device.getHeartBeatInterval() * device.getHeartBeatCount()) * 1000L;
                if (statusTaskRunner.containsKey(device.getGbId())) {
                    statusTaskRunner.updateDelay(device.getGbId(), expiresTime + System.currentTimeMillis());
                }
            } else {
                if (userSetting.getGbDeviceOnline() == 1) {
                    // 对于已经离线的设备判断他的注册是否已经过期
                    deviceService.online(BeanUtils.copyProperties(device, MediaDeviceVO::new), null);
                }
            }
        }
        if (!deviceListForUpdate.isEmpty()) {
            deviceService.updateDeviceList(deviceListForUpdate);
        }
    }

    @Override
    public void handForPlatform(RequestEvent evt, Platform parentPlatform, Element element) {

    }
}
