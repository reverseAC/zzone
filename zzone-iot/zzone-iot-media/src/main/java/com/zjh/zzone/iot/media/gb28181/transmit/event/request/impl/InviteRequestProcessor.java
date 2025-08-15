package com.zjh.zzone.iot.media.gb28181.transmit.event.request.impl;

import com.ylg.iot.constant.MediaConstants;
import com.ylg.iot.entity.MediaDevice;
import com.ylg.iot.entity.MediaDeviceChannel;
import com.ylg.iot.entity.MediaServer;
import com.ylg.iot.enums.media.AudioBroadcastCatchStatus;
import com.ylg.iot.enums.media.InviteSessionType;
import com.ylg.iot.enums.media.InviteStreamType;
import com.ylg.iot.media.bo.*;
import com.ylg.iot.media.config.SipConfig;
import com.ylg.iot.media.gb28181.exception.InviteDecodeException;
import com.ylg.iot.media.gb28181.session.SipInviteSessionManager;
import com.ylg.iot.media.gb28181.transmit.SIPProcessorHandler;
import com.ylg.iot.media.gb28181.transmit.event.request.SIPRequestParentProcessor;
import com.ylg.iot.media.gb28181.transmit.event.request.SIPRequestProcessor;
import com.ylg.iot.media.scheduling.DynamicTask;
import com.ylg.iot.media.service.*;
import com.ylg.iot.media.session.AudioBroadcastManager;
import com.ylg.iot.media.utils.SipUtils;
import gov.nist.javax.sdp.TimeDescriptionImpl;
import gov.nist.javax.sdp.fields.TimeField;
import gov.nist.javax.sdp.fields.URIField;
import gov.nist.javax.sip.message.SIPRequest;
import gov.nist.javax.sip.message.SIPResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sdp.*;
import javax.sip.InvalidArgumentException;
import javax.sip.RequestEvent;
import javax.sip.SipException;
import javax.sip.header.CallIdHeader;
import javax.sip.message.Response;
import java.text.ParseException;
import java.util.List;
import java.util.Vector;

/**
 * SIP命令类型： INVITE请求
 *
 * @author zjh
 * @since 2025-06-20 13:47
 */
@Slf4j
@Component
public class InviteRequestProcessor extends SIPRequestParentProcessor implements InitializingBean, SIPRequestProcessor {

    private final String method = "INVITE";

    @Autowired
    private SIPProcessorHandler sipProcessorHandler;

    @Autowired
    private MediaDeviceService deviceService;

    @Autowired
    private MediaDeviceChannelService channelService;

    @Autowired
    private DynamicTask dynamicTask;

    @Autowired
    private PlayService playService;

    @Autowired
    private SendRtpServerService sendRtpServerService;

    @Autowired
    private MediaServerService mediaServerService;

    @Autowired
    private AudioBroadcastManager audioBroadcastManager;

    @Autowired
    private SipInviteSessionManager sessionManager;

    @Autowired
    private SipConfig config;

    @Override
    public void afterPropertiesSet() throws Exception {
        sipProcessorHandler.addRequestProcessor(method, this);
    }

    /**
     * 处理INVITE消息
     *
     * @param event SIP INVITE请求事件
     */
    @Override
    public void process(RequestEvent event) {
        log.info("收到来自设备的INVITE请求: {}", event.getRequest());

        SIPRequest request = (SIPRequest)event.getRequest();

        try {
            InviteMessageInfo inviteInfo = decode(event);
            // 只考虑设备发的送的INVITE请求，目前没有级联需求
            inviteFromDeviceHandle(request, inviteInfo);
        } catch (SdpException e) {
            // 参数不全， 发400，请求错误
            try {
                responseAck(request, Response.BAD_REQUEST);
            } catch (SipException | InvalidArgumentException | ParseException sendException) {
                log.error("[命令发送失败] invite BAD_REQUEST: {}", sendException.getMessage());
            }
        }
    }

    private InviteMessageInfo decode(RequestEvent evt) throws SdpException {


        InviteMessageInfo inviteInfo = new InviteMessageInfo();
        SIPRequest request = (SIPRequest)evt.getRequest();
        String[] channelIdArrayFromSub = SipUtils.getChannelIdFromRequest(request);

        // 解析sdp消息, 使用jainsip 自带的sdp解析方式
        String contentString = new String(request.getRawContent());
        Gb28181Sdp gb28181Sdp = SipUtils.parseSDP(contentString);
        SessionDescription sdp = gb28181Sdp.getBaseSdb();
        String sessionName = sdp.getSessionName().getValue();
        String channelIdFromSdp = null;
        if(StringUtils.equalsIgnoreCase("Playback", sessionName)){
            URIField uriField = (URIField)sdp.getURI();
            channelIdFromSdp = uriField.getURI().split(":")[0];
        }
        final String channelId = StringUtils.isNotBlank(channelIdFromSdp) ? channelIdFromSdp :
                (channelIdArrayFromSub != null ? channelIdArrayFromSub[0]: null);
        String requesterId = SipUtils.getUserIdFromFromHeader(request);
        CallIdHeader callIdHeader = (CallIdHeader) request.getHeader(CallIdHeader.NAME);
        if (requesterId == null || channelId == null) {
            log.warn("[解析INVITE消息] 无法从请求中获取到来源id，返回400错误");
            throw new InviteDecodeException(Response.BAD_REQUEST, "request decode fail");
        }
        log.info("[INVITE] 来源ID: {}, callId: {}, 来自：{}：{}",
                requesterId, callIdHeader.getCallId(), request.getRemoteAddress(), request.getRemotePort());
        inviteInfo.setRequesterId(requesterId);
        inviteInfo.setTargetChannelId(channelId);
        if (channelIdArrayFromSub != null && channelIdArrayFromSub.length == 2) {
            inviteInfo.setSourceChannelId(channelIdArrayFromSub[1]);
        }
        inviteInfo.setSessionName(sessionName);
        inviteInfo.setSsrc(gb28181Sdp.getSsrc());
        inviteInfo.setCallId(callIdHeader.getCallId());

        // 如果是录像回放，则会存在录像的开始时间与结束时间
        Long startTime = null;
        Long stopTime = null;
        if (sdp.getTimeDescriptions(false) != null && !sdp.getTimeDescriptions(false).isEmpty()) {
            TimeDescriptionImpl timeDescription = (TimeDescriptionImpl) (sdp.getTimeDescriptions(false).get(0));
            TimeField startTimeFiled = (TimeField) timeDescription.getTime();
            startTime = startTimeFiled.getStartTime();
            stopTime = startTimeFiled.getStopTime();
        }
        //  获取支持的格式
        Vector mediaDescriptions = sdp.getMediaDescriptions(true);
        // 查看是否支持PS 负载96
        //String ip = null;
        int port = -1;
        boolean mediaTransmissionTCP = false;
        Boolean tcpActive = null;
        for (Object description : mediaDescriptions) {
            MediaDescription mediaDescription = (MediaDescription) description;
            Media media = mediaDescription.getMedia();

            Vector mediaFormats = media.getMediaFormats(false);
            if (mediaFormats.contains("96") || mediaFormats.contains("8")) {
                port = media.getMediaPort();
                //String mediaType = media.getMediaType();
                String protocol = media.getProtocol();

                // 区分TCP发流还是udp， 当前默认udp
                if ("TCP/RTP/AVP".equalsIgnoreCase(protocol)) {
                    String setup = mediaDescription.getAttribute("setup");
                    if (setup != null) {
                        mediaTransmissionTCP = true;
                        if ("active".equalsIgnoreCase(setup)) {
                            tcpActive = true;
                        } else if ("passive".equalsIgnoreCase(setup)) {
                            tcpActive = false;
                        }
                    }
                }
                break;
            }
        }
        if (port == -1) {
            log.info("[解析INVITE消息]  不支持的媒体格式，返回415");
            throw new RuntimeException("unsupported media type");
        }
        inviteInfo.setTcp(mediaTransmissionTCP);
        inviteInfo.setTcpActive(tcpActive != null? tcpActive: false);
        inviteInfo.setStartTime(startTime);
        inviteInfo.setStopTime(stopTime);

        Vector sdpMediaDescriptions = sdp.getMediaDescriptions(true);
        MediaDescription mediaDescription = null;
        String downloadSpeed = "1";
        if (!sdpMediaDescriptions.isEmpty()) {
            mediaDescription = (MediaDescription) sdpMediaDescriptions.get(0);
        }
        if (mediaDescription != null) {
            downloadSpeed = mediaDescription.getAttribute("downloadspeed");
        }
        inviteInfo.setIp(sdp.getConnection().getAddress());
        inviteInfo.setPort(port);
        inviteInfo.setDownloadSpeed(downloadSpeed);

        return inviteInfo;

    }

    public void inviteFromDeviceHandle(SIPRequest request, InviteMessageInfo inviteInfo) {

        if (inviteInfo.getSourceChannelId() == null) {
            log.warn("来自设备的Invite请求，无法从请求信息中确定请求来自的通道，已忽略，requesterId： {}", inviteInfo.getRequesterId());
            try {
                responseAck(request, Response.FORBIDDEN);
            } catch (SipException | InvalidArgumentException | ParseException e) {
                log.error("[命令发送失败] 来自设备的Invite请求，无法从请求信息中确定所属设备 FORBIDDEN: {}", e.getMessage());
            }
            return;
        }
        // 判断requesterId是设备还是通道
        MediaDevice device = deviceService.getDeviceByChannelGbId(inviteInfo.getRequesterId());
        if (device == null) {
            device = deviceService.getDeviceByChannelGbId(inviteInfo.getSourceChannelId());
        }

        if (device == null) {
            log.warn("来自设备的Invite请求，无法从请求信息中确定所属设备，已忽略，requesterId： {}/{}", inviteInfo.getRequesterId(),
                    inviteInfo.getSourceChannelId());
            try {
                responseAck(request, Response.FORBIDDEN);
            } catch (SipException | InvalidArgumentException | ParseException e) {
                log.error("[命令发送失败] 来自设备的Invite请求，无法从请求信息中确定所属设备 FORBIDDEN: {}", e.getMessage());
            }
            return;
        }
        MediaDeviceChannel deviceChannel = channelService.getChannelByGbId(device.getId(), inviteInfo.getSourceChannelId());
        if (deviceChannel == null) {
            List<AudioBroadcastCache> audioBroadcastCatchList = audioBroadcastManager.getByDeviceId(device.getGbId());
            if (audioBroadcastCatchList.isEmpty()) {
                log.warn("来自设备的Invite请求，无法从请求信息中确定所属通道，已忽略，requesterId： {}/{}", inviteInfo.getRequesterId(), inviteInfo.getSourceChannelId());
                try {
                    responseAck(request, Response.FORBIDDEN);
                } catch (SipException | InvalidArgumentException | ParseException e) {
                    log.error("[命令发送失败] 来自设备的Invite请求，无法从请求信息中确定所属设备 FORBIDDEN: {}", e.getMessage());
                }
                return;
            }else {
                deviceChannel = channelService.getById(audioBroadcastCatchList.get(0).getChannelId());
            }
        }
        AudioBroadcastCache broadcastCatch = audioBroadcastManager.get(deviceChannel.getId());
        if (broadcastCatch == null) {
            log.warn("来自设备的Invite请求非语音广播，已忽略，requesterId： {}/{}", inviteInfo.getRequesterId(), inviteInfo.getSourceChannelId());
            try {
                responseAck(request, Response.FORBIDDEN);
            } catch (SipException | InvalidArgumentException | ParseException e) {
                log.error("[命令发送失败] 来自设备的Invite请求非语音广播 FORBIDDEN: {}", e.getMessage());
            }
            return;
        }
        log.info("收到设备" + inviteInfo.getRequesterId() + "的语音广播Invite请求");
        String key = MediaConstants.BROADCAST_WAITE_INVITE + device.getGbId();
        if (!SipUtils.isFrontEnd(device.getGbId())) {
            key += broadcastCatch.getChannelId();
        }
        dynamicTask.stop(key);
        try {
            responseAck(request, Response.TRYING);
        } catch (SipException | InvalidArgumentException | ParseException e) {
            log.error("[命令发送失败] invite BAD_REQUEST: {}", e.getMessage());
            playService.stopAudioBroadcast(device, deviceChannel);
            return;
        }
        String contentString = new String(request.getRawContent());

        try {
            Gb28181Sdp gb28181Sdp = SipUtils.parseSDP(contentString);
            SessionDescription sdp = gb28181Sdp.getBaseSdb();
            //  获取支持的格式
            Vector mediaDescriptions = sdp.getMediaDescriptions(true);

            // 查看是否支持PS 负载96
            int port = -1;
            boolean mediaTransmissionTCP = false;
            Boolean tcpActive = null;
            for (int i = 0; i < mediaDescriptions.size(); i++) {
                MediaDescription mediaDescription = (MediaDescription) mediaDescriptions.get(i);
                Media media = mediaDescription.getMedia();

                Vector mediaFormats = media.getMediaFormats(false);
//                    if (mediaFormats.contains("8")) {
                port = media.getMediaPort();
                String protocol = media.getProtocol();
                // 区分TCP发流还是udp， 当前默认udp
                if ("TCP/RTP/AVP".equals(protocol)) {
                    String setup = mediaDescription.getAttribute("setup");
                    if (setup != null) {
                        mediaTransmissionTCP = true;
                        if ("active".equals(setup)) {
                            tcpActive = true;
                        } else if ("passive".equals(setup)) {
                            tcpActive = false;
                        }
                    }
                }
                break;
//                    }
            }
            if (port == -1) {
                log.info("不支持的媒体格式，返回415");
                // 回复不支持的格式
                try {
                    responseAck(request, Response.UNSUPPORTED_MEDIA_TYPE); // 不支持的格式，发415
                } catch (SipException | InvalidArgumentException | ParseException e) {
                    log.error("[命令发送失败] invite 不支持的媒体格式: {}", e.getMessage());
                    playService.stopAudioBroadcast(device, deviceChannel);
                    return;
                }
                return;
            }
            String addressStr = sdp.getOrigin().getAddress();
            log.info("设备{}请求语音流，地址：{}:{}，ssrc：{}, {}", inviteInfo.getRequesterId(), addressStr, port, gb28181Sdp.getSsrc(),
                    mediaTransmissionTCP ? (tcpActive ? "TCP主动" : "TCP被动") : "UDP");

            MediaServer mediaServerItem = broadcastCatch.getMediaServerItem();
            if (mediaServerItem == null) {
                log.warn("未找到语音喊话使用的zlm");
                try {
                    responseAck(request, Response.BUSY_HERE);
                } catch (SipException | InvalidArgumentException | ParseException e) {
                    log.error("[命令发送失败] invite 未找到可用的zlm: {}", e.getMessage());
                    playService.stopAudioBroadcast(device, deviceChannel);
                }
                return;
            }
            log.info("设备{}请求语音流， 收流地址：{}:{}，ssrc：{}, {}, 对讲方式：{}", inviteInfo.getRequesterId(), addressStr, port, gb28181Sdp.getSsrc(),
                    mediaTransmissionTCP ? (tcpActive ? "TCP主动" : "TCP被动") : "UDP", sdp.getSessionName().getValue());
            CallIdHeader callIdHeader = (CallIdHeader) request.getHeader(CallIdHeader.NAME);

            SendRtpInfo sendRtpItem = sendRtpServerService.createSendRtpInfo(mediaServerItem, addressStr, port, gb28181Sdp.getSsrc(), inviteInfo.getRequesterId(),
                    device.getGbId(), deviceChannel.getId(),
                    mediaTransmissionTCP, false);

            if (sendRtpItem == null) {
                log.warn("服务器端口资源不足");
                try {
                    responseAck(request, Response.BUSY_HERE);
                } catch (SipException | InvalidArgumentException | ParseException e) {
                    log.error("[命令发送失败] invite 服务器端口资源不足: {}", e.getMessage());
                    playService.stopAudioBroadcast(device, deviceChannel);
                    return;
                }
                return;
            }

            sendRtpItem.setPlayType(InviteStreamType.BROADCAST);
            sendRtpItem.setCallId(callIdHeader.getCallId());
            sendRtpItem.setStatus(1);
            sendRtpItem.setApp(broadcastCatch.getApp());
            sendRtpItem.setStream(broadcastCatch.getStream());
            sendRtpItem.setPt(8);
            sendRtpItem.setUsePs(false);
            sendRtpItem.setRtcp(false);
            sendRtpItem.setOnlyAudio(true);
            sendRtpItem.setTcp(mediaTransmissionTCP);
            if (tcpActive != null) {
                sendRtpItem.setTcpActive(tcpActive);
            }

            sendRtpServerService.update(sendRtpItem);

            Boolean streamReady = mediaServerService.isStreamReady(mediaServerItem, broadcastCatch.getApp(), broadcastCatch.getStream());
            if (streamReady) {
                sendOk(device, deviceChannel, sendRtpItem, sdp, request, mediaServerItem, mediaTransmissionTCP, gb28181Sdp.getSsrc());
            } else {
                log.warn("[语音通话]， 未发现待推送的流,app={},stream={}", broadcastCatch.getApp(), broadcastCatch.getStream());
                try {
                    responseAck(request, Response.GONE);
                } catch (SipException | InvalidArgumentException | ParseException e) {
                    log.error("[命令发送失败] 语音通话 回复410失败， {}", e.getMessage());
                    return;
                }
                playService.stopAudioBroadcast(device, deviceChannel);
            }
        } catch (SdpException e) {
            log.error("[SDP解析异常]", e);
            playService.stopAudioBroadcast(device, deviceChannel);
        }
    }


    SIPResponse sendOk(MediaDevice device, MediaDeviceChannel channel, SendRtpInfo sendRtpItem, SessionDescription sdp, SIPRequest request, MediaServer mediaServerItem, boolean mediaTransmissionTCP, String ssrc) {
        SIPResponse sipResponse = null;
        try {
            sendRtpItem.setStatus(2);
            sendRtpServerService.update(sendRtpItem);
            StringBuffer content = new StringBuffer(200);
            content.append("v=0\r\n");
            content.append("o=" + config.getId() + " " + sdp.getOrigin().getSessionId() + " " + sdp.getOrigin().getSessionVersion() + " IN IP4 " + mediaServerItem.getSdpIp() + "\r\n");
            content.append("s=Play\r\n");
            content.append("c=IN IP4 " + mediaServerItem.getSdpIp() + "\r\n");
            content.append("t=0 0\r\n");

            if (mediaTransmissionTCP) {
                content.append("m=audio " + sendRtpItem.getLocalPort() + " TCP/RTP/AVP 8\r\n");
            } else {
                content.append("m=audio " + sendRtpItem.getLocalPort() + " RTP/AVP 8\r\n");
            }

            content.append("a=rtpmap:8 PCMA/8000/1\r\n");

            content.append("a=sendonly\r\n");
            if (sendRtpItem.isTcp()) {
                content.append("a=connection:new\r\n");
                if (!sendRtpItem.isTcpActive()) {
                    content.append("a=setup:active\r\n");
                } else {
                    content.append("a=setup:passive\r\n");
                }
            }
            content.append("y=" + ssrc + "\r\n");
            content.append("f=v/////a/1/8/1\r\n");

            Platform parentPlatform = new Platform();
            parentPlatform.setServerIp(device.getIp());
            parentPlatform.setServerPort(device.getPort());
            parentPlatform.setServerGBId(device.getGbId());

            sipResponse = responseSdpAck(request, content.toString(), parentPlatform);

            AudioBroadcastCache audioBroadcastCatch = audioBroadcastManager.get(sendRtpItem.getChannelId());

            audioBroadcastCatch.setStatus(AudioBroadcastCatchStatus.Ok);
            audioBroadcastCatch.setSipTransactionInfoByRequest(sipResponse);
            audioBroadcastManager.update(audioBroadcastCatch);
            SsrcTransaction ssrcTransaction = SsrcTransaction.buildForDevice(device.getGbId(), sendRtpItem.getChannelId(),
                    request.getCallIdHeader().getCallId(), sendRtpItem.getApp(), sendRtpItem.getStream(), sendRtpItem.getSsrc(), sendRtpItem.getMediaServerId(), sipResponse, InviteSessionType.BROADCAST);
            sessionManager.put(ssrcTransaction);
            // 开启发流，大华在收到200OK后就会开始建立连接
            if (sendRtpItem.isTcpActive() || !device.getBroadcastPushAfterAck()) {
                if (sendRtpItem.isTcpActive()) {
                    log.info("[语音喊话] 监听端口等待设备连接后推流");
                }else {
                    log.info("[语音喊话] 回复200OK后发现 BroadcastPushAfterAck为False，现在开始推流");
                }

                playService.startPushStream(sendRtpItem, channel, sipResponse, parentPlatform, request.getCallIdHeader());
            }

        } catch (SipException | InvalidArgumentException | ParseException | SdpParseException e) {
            log.error("[命令发送失败] 语音喊话 回复200OK（SDP）: {}", e.getMessage());
        }
        return sipResponse;
    }
}
