package com.zjh.zzone.iot.media.service.impl;

import com.ylg.iot.media.callback.HookSubscribe;
import com.ylg.iot.media.scheduling.DynamicTask;
import com.ylg.iot.media.config.UserSetting;
import com.ylg.iot.media.contant.InviteResultCode;
import com.ylg.iot.media.callback.ErrorCallback;
import com.ylg.iot.media.bo.OpenRTPServerResult;
import com.ylg.iot.media.bo.RTPServerParam;
import com.ylg.iot.media.bo.SSRCInfo;
import com.ylg.iot.entity.MediaServer;
import com.ylg.iot.media.callback.Hook;
import com.ylg.iot.enums.media.HookType;
import com.ylg.iot.media.event.media.MediaArrivalEvent;
import com.ylg.iot.media.event.media.MediaDepartureEvent;
import com.ylg.iot.media.service.MediaServerService;
import com.ylg.iot.media.service.ReceiveRtpServerService;
import com.ylg.iot.media.gb28181.session.SSRCFactory;
import com.ylg.iot.media.gb28181.session.SipInviteSessionManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
public class ReceiveRtpServerServiceImpl implements ReceiveRtpServerService {

    @Autowired
    private MediaServerService mediaServerService;

    @Autowired
    private DynamicTask dynamicTask;

    @Autowired
    private SSRCFactory ssrcFactory;

    @Autowired
    private UserSetting userSetting;

    @Autowired
    private HookSubscribe subscribe;

    @Autowired
    private SipInviteSessionManager sessionManager;

    /**
     * 流到来的处理
     */
    @Async("taskExecutor")
    @EventListener
    public void onApplicationEvent(MediaArrivalEvent event) {

    }

    /**
     * 流离开的处理
     */
    @Async("taskExecutor")
    @EventListener
    public void onApplicationEvent(MediaDepartureEvent event) {

    }

    @Override
    public SSRCInfo openRTPServer(RTPServerParam rtpServerParam, ErrorCallback<OpenRTPServerResult> callback) {
        if (callback == null) {
            log.warn("[开启RTP收流] 失败，回调为NULL");
            return null;
        }
        if (rtpServerParam.getMediaServerItem() == null) {
            log.warn("[开启RTP收流] 失败，媒体节点为NULL");
            return null;
        }

        // 获取mediaServer可用的ssrc
        final String ssrc;
        if (rtpServerParam.getPresetSsrc() != null) {
            ssrc = rtpServerParam.getPresetSsrc();
        } else {
            if (rtpServerParam.isPlayback()) {
                ssrc = ssrcFactory.getPlayBackSsrc(rtpServerParam.getMediaServerItem().getServerId());
            } else {
                ssrc = ssrcFactory.getPlaySsrc(rtpServerParam.getMediaServerItem().getServerId());
            }
        }
        final String streamId;
        if (rtpServerParam.getStreamId() == null) {
            streamId = String.format("%08x", Long.parseLong(ssrc)).toUpperCase();
        } else {
            streamId = rtpServerParam.getStreamId();
        }
        if (rtpServerParam.isSsrcCheck() && rtpServerParam.getTcpMode() > 0) {
            // 目前zlm不支持 tcp模式更新ssrc，暂时关闭ssrc校验
            log.warn("[openRTPServer] 平台对接时下级可能自定义ssrc，但是tcp模式zlm收流目前无法更新ssrc，可能收流超时，此时请使用udp收流或者关闭ssrc校验");
        }
        int rtpServerPort;
        if (rtpServerParam.getMediaServerItem().isRtpEnable()) { // 多端口模式
            rtpServerPort = mediaServerService.createRTPServer(rtpServerParam.getMediaServerItem(), streamId,
                    rtpServerParam.isSsrcCheck() ? Long.parseLong(ssrc) : 0, rtpServerParam.getPort(), rtpServerParam.isOnlyAuto(),
                    rtpServerParam.isDisableAudio(), rtpServerParam.isReUsePort(), rtpServerParam.getTcpMode());
        } else {
            rtpServerPort = rtpServerParam.getMediaServerItem().getRtpProxyPort();
        }
        if (rtpServerPort == 0) {
            callback.run(InviteResultCode.ERROR_FOR_RESOURCE_EXHAUSTION.getCode(), "开启RTPServer失败", null);
            // 释放ssrc
            if (rtpServerParam.getPresetSsrc() == null) {
                ssrcFactory.releaseSsrc(rtpServerParam.getMediaServerItem().getServerId(), ssrc);
            }
            return null;
        }

        // 设置流超时的定时任务
        String timeOutTaskKey = UUID.randomUUID().toString();

        SSRCInfo ssrcInfo = new SSRCInfo(rtpServerPort, ssrc, streamId, timeOutTaskKey);
        OpenRTPServerResult openRTPServerResult = new OpenRTPServerResult();
        openRTPServerResult.setSsrcInfo(ssrcInfo);

        Hook rtpHook = Hook.getInstance(HookType.on_media_arrival, ssrcInfo.getApp(), streamId);
        dynamicTask.startDelay(timeOutTaskKey, () -> {
            // 收流超时
            // 释放ssrc
            if (rtpServerParam.getPresetSsrc() == null) {
                ssrcFactory.releaseSsrc(rtpServerParam.getMediaServerItem().getServerId(), ssrc);
            }
            // 关闭收流端口
            mediaServerService.closeRTPServer(rtpServerParam.getMediaServerItem(), streamId);
            subscribe.removeSubscribe(rtpHook);
            callback.run(InviteResultCode.ERROR_FOR_STREAM_TIMEOUT.getCode(), InviteResultCode.ERROR_FOR_STREAM_TIMEOUT.getMsg(), openRTPServerResult);
        }, userSetting.getPlayTimeout());
        // 开启流到来的监听
        subscribe.addSubscribe(rtpHook, (hookData) -> {
            dynamicTask.stop(timeOutTaskKey);
            // hook响应
            openRTPServerResult.setHookData(hookData);
            callback.run(InviteResultCode.SUCCESS.getCode(), InviteResultCode.SUCCESS.getMsg(), openRTPServerResult);
            subscribe.removeSubscribe(rtpHook);
        });

        return ssrcInfo;
    }

    @Override
    public void closeRTPServer(MediaServer mediaServer, SSRCInfo ssrcInfo) {
        if (mediaServer == null) {
            return;
        }
        if (ssrcInfo.getTimeOutTaskKey() != null) {
            dynamicTask.stop(ssrcInfo.getTimeOutTaskKey());
        }
        if (ssrcInfo.getSsrc() != null) {
            // 释放ssrc
            ssrcFactory.releaseSsrc(mediaServer.getServerId(), ssrcInfo.getSsrc());
        }
        mediaServerService.closeRTPServer(mediaServer, ssrcInfo.getStream());
    }
}
