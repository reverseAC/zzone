package com.zjh.zzone.iot.media.gb28181.transmit.cmd;

import com.ylg.iot.enums.media.HookType;
import com.ylg.iot.media.bo.*;
import com.ylg.iot.media.callback.ErrorCallback;
import com.ylg.iot.media.contant.StreamModeType;
import com.ylg.iot.media.callback.MessageSubscribe;
import com.ylg.iot.media.gb28181.event.sip.MessageEvent;
import com.ylg.iot.media.gb28181.SipLayer;
import com.ylg.iot.media.gb28181.common.SsrcTransactionNotFoundException;
import com.ylg.iot.media.config.SipConfig;
import com.ylg.iot.media.config.UserSetting;
import com.ylg.iot.enums.media.InviteSessionType;
import com.ylg.iot.entity.MediaServer;
import com.ylg.iot.entity.MediaDeviceChannel;
import com.ylg.iot.entity.MediaDevice;
import com.ylg.iot.media.callback.SipSubscribe;
import com.ylg.iot.media.callback.Hook;
import com.ylg.iot.media.callback.HookSubscribe;
import com.ylg.iot.media.service.MediaServerService;
import com.ylg.iot.media.gb28181.session.SipInviteSessionManager;
import com.ylg.iot.media.gb28181.transmit.SIPSender;
import com.ylg.iot.media.utils.DateUtil;
import com.ylg.iot.media.utils.SipUtils;
import gov.nist.javax.sip.message.SIPRequest;
import gov.nist.javax.sip.message.SIPResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import javax.sip.InvalidArgumentException;
import javax.sip.ResponseEvent;
import javax.sip.SipException;
import javax.sip.header.CallIdHeader;
import javax.sip.message.Request;
import java.text.ParseException;

/**
 * 平台下发请求（播放请求、设备控制、查询信息）
 *
 * @author zjh
 * @since 2025-03-26 11:35
 */
@Component
@DependsOn("sipLayer")
@Slf4j
public class SIPCommanderImpl implements SIPCommander {

    @Autowired
    private SipConfig sipConfig;

    @Autowired
    private SipLayer sipLayer;

    @Autowired
    private SIPSender sipSender;

    @Autowired
    private SIPRequestHeaderProvider headerProvider;

    @Autowired
    private MediaServerService mediaServerService;

    @Autowired
    private SipInviteSessionManager sessionManager;

    @Autowired
    private MessageSubscribe messageSubscribe;

    @Autowired
    private HookSubscribe subscribe;

    @Autowired
    private UserSetting userSetting;

    /**
     * 云台控制，支持方向与缩放控制
     *
     * @param device  控制设备
     * @param channelId  预览通道
     * @param leftRight  镜头左移右移 0:停止 1:左移 2:右移
     * @param upDown     镜头上移下移 0:停止 1:上移 2:下移
     * @param inOut      镜头放大缩小 0:停止 1:缩小 2:放大
     * @param moveSpeed  镜头移动速度
     * @param zoomSpeed  镜头缩放速度
     */
    @Override
    public void ptzCmd(MediaDevice device, String channelId, int leftRight, int upDown, int inOut, int moveSpeed,
                       int zoomSpeed) throws InvalidArgumentException, SipException, ParseException {

        String cmdStr = SipUtils.cmdString(leftRight, upDown, inOut, moveSpeed, zoomSpeed);
        StringBuilder ptzXml = new StringBuilder(200);
        String charset = device.getCharset();
        ptzXml.append("<?xml version=\"1.0\" encoding=\"").append(charset).append("\"?>\r\n");
        ptzXml.append("<Control>\r\n");
        ptzXml.append("<CmdType>DeviceControl</CmdType>\r\n");
        ptzXml.append("<SN>").append((int) ((Math.random() * 9 + 1) * 100000)).append("</SN>\r\n");
        ptzXml.append("<DeviceID>").append(channelId).append("</DeviceID>\r\n");
        ptzXml.append("<PTZCmd>").append(cmdStr).append("</PTZCmd>\r\n");
        ptzXml.append("<Info>\r\n");
        ptzXml.append("<ControlPriority>5</ControlPriority>\r\n");
        ptzXml.append("</Info>\r\n");
        ptzXml.append("</Control>\r\n");

        Request request = headerProvider.createMessageRequest(device, ptzXml.toString(), SipUtils.getNewViaTag(),
                SipUtils.getNewFromTag(), null,
                sipSender.getNewCallIdHeader(sipLayer.getLocalIp(device.getLocalIp()),device.getTransport()));

        sipSender.transmitRequest(sipLayer.getLocalIp(device.getLocalIp()),request);
    }

    /**
     * 前端控制，包括PTZ指令、FI指令、预置位指令、巡航指令、扫描指令和辅助开关指令
     *
     * @param device  		控制设备
     * @param channelId		预览通道
     * @param cmdCode		指令码
     * @param parameter1	数据1
     * @param parameter2	数据2
     * @param combineCode2	组合码2
     */
    @Override
    public void frontEndCmd(MediaDevice device, String channelId, int cmdCode, int parameter1, int parameter2, int combineCode2) throws SipException, InvalidArgumentException, ParseException {

        String cmdStr = frontEndCmdString(cmdCode, parameter1, parameter2, combineCode2);
        StringBuffer ptzXml = new StringBuffer(200);
        String charset = device.getCharset();
        ptzXml.append("<?xml version=\"1.0\" encoding=\"" + charset + "\"?>\r\n");
        ptzXml.append("<Control>\r\n");
        ptzXml.append("<CmdType>DeviceControl</CmdType>\r\n");
        ptzXml.append("<SN>" + (int) ((Math.random() * 9 + 1) * 100000) + "</SN>\r\n");
        ptzXml.append("<DeviceID>" + channelId + "</DeviceID>\r\n");
        ptzXml.append("<PTZCmd>" + cmdStr + "</PTZCmd>\r\n");
        ptzXml.append("<Info>\r\n");
        ptzXml.append("<ControlPriority>5</ControlPriority>\r\n");
        ptzXml.append("</Info>\r\n");
        ptzXml.append("</Control>\r\n");

        SIPRequest request = (SIPRequest) headerProvider.createMessageRequest(device, ptzXml.toString(), SipUtils.getNewViaTag(), SipUtils.getNewFromTag(), null,sipSender.getNewCallIdHeader(sipLayer.getLocalIp(device.getLocalIp()),device.getTransport()));
        sipSender.transmitRequest(sipLayer.getLocalIp(device.getLocalIp()),request);

    }

    /**
     * 云台指令码计算
     *
     * @param cmdCode      指令码
     * @param parameter1   数据1
     * @param parameter2   数据2
     * @param combineCode2 组合码2
     */
    private static String frontEndCmdString(int cmdCode, int parameter1, int parameter2, int combineCode2) {
        StringBuilder builder = new StringBuilder("A50F01");
        String strTmp;
        strTmp = String.format("%02X", cmdCode);
        builder.append(strTmp, 0, 2);
        strTmp = String.format("%02X", parameter1);
        builder.append(strTmp, 0, 2);
        strTmp = String.format("%02X", parameter2);
        builder.append(strTmp, 0, 2);
        strTmp = String.format("%02X", combineCode2 << 4);
        builder.append(strTmp, 0, 2);
        //计算校验码
        int checkCode = (0XA5 + 0X0F + 0X01 + cmdCode + parameter1 + parameter2 + (combineCode2 << 4)) % 0X100;
        strTmp = String.format("%02X", checkCode);
        builder.append(strTmp, 0, 2);
        return builder.toString();
    }

    /**
     * 请求预览视频流
     *
     * @param mediaServerItem 媒体服务实例 不直接读取配置，兼容转发点播
     * @param device 视频设备
     * @param channel 预览通道
     * @param errorEvent sip错误订阅
     */
    @Override
    public void playStreamCmd(MediaServer mediaServerItem, SSRCInfo ssrcInfo, MediaDevice device, MediaDeviceChannel channel,
                              SipSubscribe.Event okEvent, SipSubscribe.Event errorEvent, Long timeout)
            throws InvalidArgumentException, SipException, ParseException {

        if (device == null) {
            return;
        }
        // 设备指定收流ip ? 设备收流ip : 媒体服务器收流ip
        String sdpIp;
        if (!ObjectUtils.isEmpty(device.getSdpIp())) {
            sdpIp = device.getSdpIp();
        } else {
            sdpIp = mediaServerItem.getSdpIp();
        }
        StringBuilder content = new StringBuilder(200);
        content.append("v=0\r\n");
        content.append("o=").append(device.getGbId()).append(" 0 0 IN IP4 ").append(sdpIp).append("\r\n");
        content.append("s=Play\r\n");
        content.append("c=IN IP4 ").append(sdpIp).append("\r\n");
        content.append("t=0 0\r\n");

        if (userSetting.getSeniorSdp()) {
            if (StreamModeType.TCP_PASSIVE.name().equalsIgnoreCase(device.getStreamMode())) {
                content.append("m=video ").append(ssrcInfo.getPort()).append(" TCP/RTP/AVP 96 126 125 99 34 98 97\r\n");
            } else if (StreamModeType.TCP_ACTIVE.name().equalsIgnoreCase(device.getStreamMode())) {
                content.append("m=video ").append(ssrcInfo.getPort()).append(" TCP/RTP/AVP 96 126 125 99 34 98 97\r\n");
            } else if (StreamModeType.UDP.name().equalsIgnoreCase(device.getStreamMode())) {
                content.append("m=video ").append(ssrcInfo.getPort()).append(" RTP/AVP 96 126 125 99 34 98 97\r\n");
            }
            content.append("a=recvonly\r\n");
            content.append("a=rtpmap:96 PS/90000\r\n");
            content.append("a=fmtp:126 profile-level-id=42e01e\r\n");
            content.append("a=rtpmap:126 H264/90000\r\n");
            content.append("a=rtpmap:125 H264S/90000\r\n");
            content.append("a=fmtp:125 profile-level-id=42e01e\r\n");
            content.append("a=rtpmap:99 H265/90000\r\n");
            content.append("a=rtpmap:98 H264/90000\r\n");
            content.append("a=rtpmap:97 MPEG4/90000\r\n");
            if (StreamModeType.TCP_PASSIVE.name().equalsIgnoreCase(device.getStreamMode())) { // tcp被动模式
                content.append("a=setup:passive\r\n");
                content.append("a=connection:new\r\n");
            } else if (StreamModeType.TCP_ACTIVE.name().equalsIgnoreCase(device.getStreamMode())) { // tcp主动模式
                content.append("a=setup:active\r\n");
                content.append("a=connection:new\r\n");
            }
        } else {
            if (StreamModeType.TCP_PASSIVE.name().equalsIgnoreCase(device.getStreamMode())) {
                content.append("m=video ").append(ssrcInfo.getPort()).append(" TCP/RTP/AVP 96 97 98 99\r\n");
            } else if (StreamModeType.TCP_ACTIVE.name().equalsIgnoreCase(device.getStreamMode())) {
                content.append("m=video ").append(ssrcInfo.getPort()).append(" TCP/RTP/AVP 96 97 98 99\r\n");
            } else if (StreamModeType.UDP.name().equalsIgnoreCase(device.getStreamMode())) {
                content.append("m=video ").append(ssrcInfo.getPort()).append(" RTP/AVP 96 97 98 99\r\n");
            }
            content.append("a=recvonly\r\n");
            content.append("a=rtpmap:96 PS/90000\r\n");
            content.append("a=rtpmap:98 H264/90000\r\n");
            content.append("a=rtpmap:97 MPEG4/90000\r\n");
            content.append("a=rtpmap:99 H265/90000\r\n");
            if (StreamModeType.TCP_PASSIVE.name().equalsIgnoreCase(device.getStreamMode())) { // tcp被动模式
                content.append("a=setup:passive\r\n");
                content.append("a=connection:new\r\n");
            } else if (StreamModeType.TCP_ACTIVE.name().equalsIgnoreCase(device.getStreamMode())) { // tcp主动模式
                content.append("a=setup:active\r\n");
                content.append("a=connection:new\r\n");
            }
        }

        if (!ObjectUtils.isEmpty(channel.getStreamIdentification())) {
            content.append("a=").append(channel.getStreamIdentification()).append("\r\n");
        }

        content.append("y=").append(ssrcInfo.getSsrc()).append("\r\n");//ssrc
        // f字段:f= v/编码格式/分辨率/帧率/码率类型/码率大小a/编码格式/码率大小/采样率
//			content.append("f=v/2/5/25/1/4000a/1/8/1" + "\r\n"); // 未发现支持此特性的设备

        Request request = headerProvider.createInviteRequest(device, channel.getGbId(), content.toString(),
                SipUtils.getNewViaTag(), SipUtils.getNewFromTag(), null, ssrcInfo.getSsrc(),
                sipSender.getNewCallIdHeader(sipLayer.getLocalIp(device.getLocalIp()), device.getTransport()));

        sipSender.transmitRequest(sipLayer.getLocalIp(device.getLocalIp()), request,
                e -> {
                    sessionManager.removeByStream(ssrcInfo.getApp(), ssrcInfo.getStream());
                    mediaServerService.releaseSsrc(mediaServerItem.getServerId(), ssrcInfo.getSsrc());
                    errorEvent.response(e);
                },
                e -> {
                    ResponseEvent responseEvent = (ResponseEvent) e.event;
                    SIPResponse response = (SIPResponse) responseEvent.getResponse();
                    String callId = response.getCallIdHeader().getCallId();
                    SsrcTransaction ssrcTransaction = SsrcTransaction.buildForDevice(device.getGbId(), channel.getId(),
                            callId,ssrcInfo.getApp(), ssrcInfo.getStream(), ssrcInfo.getSsrc(), mediaServerItem.getServerId(), response,
                            InviteSessionType.PLAY);
                    sessionManager.put(ssrcTransaction);
                    okEvent.response(e);
                },
                timeout);

    }

    @Override
    public void streamByeCmd(MediaDevice device, String channelId, String app, String stream, String callId, SipSubscribe.Event okEvent) throws InvalidArgumentException, SipException, ParseException, SsrcTransactionNotFoundException {
        if (device == null) {
            log.warn("[发送BYE] device为null");
            return;
        }
        SsrcTransaction ssrcTransaction = null;
        if (callId != null) {
            ssrcTransaction = sessionManager.getSsrcTransactionByCallId(callId);
        } else if (stream != null) {
            ssrcTransaction = sessionManager.getSsrcTransactionByStream(app, stream);
        }

        if (ssrcTransaction == null) {
            log.info("[发送BYE] 未找到事务信息,设备： device: {}, channel: {}", device.getGbId(), channelId);
            throw new SsrcTransactionNotFoundException(device.getGbId(), channelId, callId, stream);
        }

        log.info("[发送BYE] 设备： device: {}, channel: {}, callId: {}", device.getGbId(), channelId, ssrcTransaction.getCallId());
        sessionManager.removeByCallId(ssrcTransaction.getCallId());
        Request byteRequest = headerProvider.createByteRequest(device, channelId, ssrcTransaction.getSipTransactionInfo());
        sipSender.transmitRequest(sipLayer.getLocalIp(device.getLocalIp()), byteRequest, null, okEvent);
    }

    @Override
    public void streamByeCmd(MediaDevice device, String channelId, SipTransactionInfo sipTransactionInfo, SipSubscribe.Event okEvent) throws InvalidArgumentException, SipException, ParseException, SsrcTransactionNotFoundException {
        Request byteRequest = headerProvider.createByteRequest(device, channelId, sipTransactionInfo);
        sipSender.transmitRequest(sipLayer.getLocalIp(device.getLocalIp()), byteRequest, null, okEvent);
    }

    /**
     * 请求回放视频流
     *
     * @param device    视频设备
     * @param channel 预览通道
     * @param startTime 开始时间,格式要求：yyyy-MM-dd HH:mm:ss
     * @param endTime   结束时间,格式要求：yyyy-MM-dd HH:mm:ss
     */
    @Override
    public void playbackStreamCmd(MediaServer mediaServerItem, SSRCInfo ssrcInfo, MediaDevice device, MediaDeviceChannel channel,
                                  String startTime, String endTime,
                                  SipSubscribe.Event okEvent, SipSubscribe.Event errorEvent, Long timeout) throws InvalidArgumentException, SipException, ParseException {


        log.info("{} 分配的ZLM为: {} [{}:{}]", ssrcInfo.getStream(), mediaServerItem.getId(), mediaServerItem.getSdpIp(), ssrcInfo.getPort());
        String sdpIp;
        if (!ObjectUtils.isEmpty(device.getSdpIp())) {
            sdpIp = device.getSdpIp();
        } else {
            sdpIp = mediaServerItem.getSdpIp();
        }
        StringBuffer content = new StringBuffer(200);
        content.append("v=0\r\n");
        content.append("o=" + device.getGbId() + " 0 0 IN IP4 " + sdpIp + "\r\n");
        content.append("s=Playback\r\n");
        content.append("u=" + channel.getGbId() + ":0\r\n");
        content.append("c=IN IP4 " + sdpIp + "\r\n");
        content.append("t=" + DateUtil.yyyy_MM_dd_HH_mm_ssToTimestamp(startTime) + " "
                + DateUtil.yyyy_MM_dd_HH_mm_ssToTimestamp(endTime) + "\r\n");

        String streamMode = device.getStreamMode();

        if (userSetting.getSeniorSdp()) {
            if (StreamModeType.TCP_PASSIVE.name().equalsIgnoreCase(streamMode)) {
                content.append("m=video " + ssrcInfo.getPort() + " TCP/RTP/AVP 96 126 125 99 34 98 97\r\n");
            } else if (StreamModeType.TCP_ACTIVE.name().equalsIgnoreCase(streamMode)) {
                content.append("m=video " + ssrcInfo.getPort() + " TCP/RTP/AVP 96 126 125 99 34 98 97\r\n");
            } else if (StreamModeType.UDP.name().equalsIgnoreCase(streamMode)) {
                content.append("m=video " + ssrcInfo.getPort() + " RTP/AVP 96 126 125 99 34 98 97\r\n");
            }
            content.append("a=recvonly\r\n");
            content.append("a=rtpmap:96 PS/90000\r\n");
            content.append("a=fmtp:126 profile-level-id=42e01e\r\n");
            content.append("a=rtpmap:126 H264/90000\r\n");
            content.append("a=rtpmap:125 H264S/90000\r\n");
            content.append("a=fmtp:125 profile-level-id=42e01e\r\n");
            content.append("a=rtpmap:99 H265/90000\r\n");
            content.append("a=rtpmap:98 H264/90000\r\n");
            content.append("a=rtpmap:97 MPEG4/90000\r\n");
            if (StreamModeType.TCP_PASSIVE.name().equalsIgnoreCase(streamMode)) { // tcp被动模式
                content.append("a=setup:passive\r\n");
                content.append("a=connection:new\r\n");
            } else if (StreamModeType.TCP_ACTIVE.name().equalsIgnoreCase(streamMode)) { // tcp主动模式
                content.append("a=setup:active\r\n");
                content.append("a=connection:new\r\n");
            }
        } else {
            if (StreamModeType.TCP_PASSIVE.name().equalsIgnoreCase(streamMode)) {
                content.append("m=video " + ssrcInfo.getPort() + " TCP/RTP/AVP 96 97 98 99\r\n");
            } else if (StreamModeType.TCP_ACTIVE.name().equalsIgnoreCase(streamMode)) {
                content.append("m=video " + ssrcInfo.getPort() + " TCP/RTP/AVP 96 97 98 99\r\n");
            } else if (StreamModeType.UDP.name().equalsIgnoreCase(streamMode)) {
                content.append("m=video " + ssrcInfo.getPort() + " RTP/AVP 96 97 98 99\r\n");
            }
            content.append("a=recvonly\r\n");
            content.append("a=rtpmap:96 PS/90000\r\n");
            content.append("a=rtpmap:97 MPEG4/90000\r\n");
            content.append("a=rtpmap:98 H264/90000\r\n");
            content.append("a=rtpmap:99 H265/90000\r\n");
            if (StreamModeType.TCP_PASSIVE.name().equalsIgnoreCase(streamMode)) {
                // tcp被动模式
                content.append("a=setup:passive\r\n");
                content.append("a=connection:new\r\n");
            } else if (StreamModeType.TCP_ACTIVE.name().equalsIgnoreCase(streamMode)) {
                // tcp主动模式
                content.append("a=setup:active\r\n");
                content.append("a=connection:new\r\n");
            }
        }

        //ssrc
        content.append("y=" + ssrcInfo.getSsrc() + "\r\n");

        Request request = headerProvider.createPlaybackInviteRequest(device, channel.getGbId(), content.toString(), SipUtils.getNewViaTag(), SipUtils.getNewFromTag(), null,sipSender.getNewCallIdHeader(sipLayer.getLocalIp(device.getLocalIp()),device.getTransport()), ssrcInfo.getSsrc());

        sipSender.transmitRequest(sipLayer.getLocalIp(device.getLocalIp()), request, errorEvent, event -> {
            ResponseEvent responseEvent = (ResponseEvent) event.event;
            SIPResponse response = (SIPResponse) responseEvent.getResponse();
            SsrcTransaction ssrcTransaction = SsrcTransaction.buildForDevice(device.getGbId(),
                    channel.getId(), sipSender.getNewCallIdHeader(sipLayer.getLocalIp(device.getLocalIp()),
                            device.getTransport()).getCallId(), ssrcInfo.getApp(), ssrcInfo.getStream(), ssrcInfo.getSsrc(),
                    mediaServerItem.getServerId(), response, InviteSessionType.PLAYBACK);
            sessionManager.put(ssrcTransaction);
            okEvent.response(event);
        }, timeout);
    }

    /**
     * 请求历史媒体下载
     */
    @Override
    public void downloadStreamCmd(MediaServer mediaServerItem, SSRCInfo ssrcInfo, MediaDevice device, MediaDeviceChannel channel,
                                  String startTime, String endTime, int downloadSpeed,
                                  SipSubscribe.Event errorEvent, SipSubscribe.Event okEvent, Long timeout) throws InvalidArgumentException, SipException, ParseException {

        log.info("[发送-请求历史媒体下载-命令] 流ID： {}，节点为: {} [{}:{}]", ssrcInfo.getStream(), mediaServerItem.getId(), mediaServerItem.getSdpIp(), ssrcInfo.getPort());
        String sdpIp;
        if (!ObjectUtils.isEmpty(device.getSdpIp())) {
            sdpIp = device.getSdpIp();
        } else {
            sdpIp = mediaServerItem.getSdpIp();
        }
        StringBuffer content = new StringBuffer(200);
        content.append("v=0\r\n");
        content.append("o=" + device.getGbId() + " 0 0 IN IP4 " + sdpIp + "\r\n");
        content.append("s=Download\r\n");
        content.append("u=" + channel.getGbId() + ":0\r\n");
        content.append("c=IN IP4 " + sdpIp + "\r\n");
        content.append("t=" + DateUtil.yyyy_MM_dd_HH_mm_ssToTimestamp(startTime) + " "
                + DateUtil.yyyy_MM_dd_HH_mm_ssToTimestamp(endTime) + "\r\n");

        String streamMode = device.getStreamMode().toUpperCase();

        if (userSetting.getSeniorSdp()) {
            if (StreamModeType.TCP_PASSIVE.name().equals(streamMode)) {
                content.append("m=video " + ssrcInfo.getPort() + " TCP/RTP/AVP 96 126 125 99 34 98 97\r\n");
            } else if (StreamModeType.TCP_ACTIVE.name().equals(streamMode)) {
                content.append("m=video " + ssrcInfo.getPort() + " TCP/RTP/AVP 96 126 125 99 34 98 97\r\n");
            } else if (StreamModeType.UDP.name().equals(streamMode)) {
                content.append("m=video " + ssrcInfo.getPort() + " RTP/AVP 96 126 125 99 34 98 97\r\n");
            }
            content.append("a=recvonly\r\n");
            content.append("a=rtpmap:96 PS/90000\r\n");
            content.append("a=fmtp:126 profile-level-id=42e01e\r\n");
            content.append("a=rtpmap:126 H264/90000\r\n");
            content.append("a=rtpmap:125 H264S/90000\r\n");
            content.append("a=fmtp:125 profile-level-id=42e01e\r\n");
            content.append("a=rtpmap:99 MP4V-ES/90000\r\n");
            content.append("a=fmtp:99 profile-level-id=3\r\n");
            content.append("a=rtpmap:98 H264/90000\r\n");
            content.append("a=rtpmap:97 MPEG4/90000\r\n");
            if (StreamModeType.TCP_PASSIVE.name().equals(streamMode)) { // tcp被动模式
                content.append("a=setup:passive\r\n");
                content.append("a=connection:new\r\n");
            } else if (StreamModeType.TCP_ACTIVE.name().equals(streamMode)) { // tcp主动模式
                content.append("a=setup:active\r\n");
                content.append("a=connection:new\r\n");
            }
        } else {
            if (StreamModeType.TCP_PASSIVE.name().equals(streamMode)) {
                content.append("m=video " + ssrcInfo.getPort() + " TCP/RTP/AVP 96 97 98 99\r\n");
            } else if (StreamModeType.TCP_ACTIVE.name().equals(streamMode)) {
                content.append("m=video " + ssrcInfo.getPort() + " TCP/RTP/AVP 96 97 98 99\r\n");
            } else if (StreamModeType.UDP.name().equals(streamMode)) {
                content.append("m=video " + ssrcInfo.getPort() + " RTP/AVP 96 97 98 99\r\n");
            }
            content.append("a=recvonly\r\n");
            content.append("a=rtpmap:96 PS/90000\r\n");
            content.append("a=rtpmap:97 MPEG4/90000\r\n");
            content.append("a=rtpmap:98 H264/90000\r\n");
            content.append("a=rtpmap:99 H265/90000\r\n");
            if (StreamModeType.TCP_PASSIVE.name().equals(streamMode)) { // tcp被动模式
                content.append("a=setup:passive\r\n");
                content.append("a=connection:new\r\n");
            } else if (StreamModeType.TCP_ACTIVE.name().equals(streamMode)) { // tcp主动模式
                content.append("a=setup:active\r\n");
                content.append("a=connection:new\r\n");
            }
        }
        content.append("a=downloadspeed:" + downloadSpeed + "\r\n");

        content.append("y=" + ssrcInfo.getSsrc() + "\r\n");//ssrc
        log.debug("此时请求下载信令的ssrc===>{}",ssrcInfo.getSsrc());
        // 添加订阅
        CallIdHeader newCallIdHeader = sipSender.getNewCallIdHeader(sipLayer.getLocalIp(device.getLocalIp()), device.getTransport());
        Request request = headerProvider.createPlaybackInviteRequest(device, channel.getGbId(), content.toString(), SipUtils.getNewViaTag(), SipUtils.getNewFromTag(), null,newCallIdHeader, ssrcInfo.getSsrc());

        sipSender.transmitRequest(sipLayer.getLocalIp(device.getLocalIp()), request, errorEvent, event -> {
            ResponseEvent responseEvent = (ResponseEvent) event.event;
            SIPResponse response = (SIPResponse) responseEvent.getResponse();
            String contentString =new String(response.getRawContent());
            String ssrc = SipUtils.getSsrcFromSdp(contentString);
            SsrcTransaction ssrcTransaction = SsrcTransaction.buildForDevice(device.getGbId(), channel.getId(),
                    response.getCallIdHeader().getCallId(), ssrcInfo.getApp(), ssrcInfo.getStream(), ssrc,
                    mediaServerItem.getServerId(), response, InviteSessionType.DOWNLOAD);
            sessionManager.put(ssrcTransaction);
            okEvent.response(event);
        }, timeout);
    }

    @Override
    public void talkStreamCmd(MediaServer mediaServerItem, SendRtpInfo sendRtpItem, MediaDevice device, MediaDeviceChannel channel,
                              String callId, HookSubscribe.Event event, HookSubscribe.Event eventForPush, SipSubscribe.Event okEvent,
                              SipSubscribe.Event errorEvent, Long timeout) throws InvalidArgumentException, SipException, ParseException {

        String stream = sendRtpItem.getStream();

        if (device == null) {
            return;
        }
        if (!mediaServerItem.isRtpEnable()) {
            // 单端口暂不支持语音喊话
            log.info("[语音喊话] 单端口暂不支持此操作");
            return;
        }

        log.info("[语音喊话] {} 分配的ZLM为: {} [{}:{}]", stream, mediaServerItem.getId(), mediaServerItem.getIp(), sendRtpItem.getPort());
        Hook hook = Hook.getInstance(HookType.on_media_arrival, "rtp", stream);
        subscribe.addSubscribe(hook, (hookData) -> {
            if (event != null) {
                event.response(hookData);
                subscribe.removeSubscribe(hook);
            }
        });

        CallIdHeader callIdHeader = sipSender.getNewCallIdHeader(sipLayer.getLocalIp(device.getLocalIp()), device.getTransport());
        callIdHeader.setCallId(callId);
        Hook publishHook = Hook.getInstance(HookType.on_publish, "rtp", stream);
        subscribe.addSubscribe(publishHook, (hookData) -> {
            if (eventForPush != null) {
                eventForPush.response(hookData);
            }
        });
        //
        StringBuffer content = new StringBuffer(200);
        content.append("v=0\r\n");
        content.append("o=" + device.getGbId() + " 0 0 IN IP4 " + mediaServerItem.getSdpIp() + "\r\n");
        content.append("s=Talk\r\n");
        content.append("c=IN IP4 " + mediaServerItem.getSdpIp() + "\r\n");
        content.append("t=0 0\r\n");

        content.append("m=audio " + sendRtpItem.getPort() + " TCP/RTP/AVP 8\r\n");
        content.append("a=setup:passive\r\n");
        content.append("a=connection:new\r\n");
        content.append("a=sendrecv\r\n");
        content.append("a=rtpmap:8 PCMA/8000\r\n");

        content.append("y=" + sendRtpItem.getSsrc() + "\r\n");//ssrc
        // f字段:f= v/编码格式/分辨率/帧率/码率类型/码率大小a/编码格式/码率大小/采样率
        content.append("f=v/////a/1/8/1" + "\r\n");

        Request request = headerProvider.createInviteRequest(device, channel.getGbId(), content.toString(),
                SipUtils.getNewViaTag(), SipUtils.getNewFromTag(), null, sendRtpItem.getSsrc(), callIdHeader);
        sipSender.transmitRequest(sipLayer.getLocalIp(device.getLocalIp()), request, (e -> {
            sessionManager.removeByStream(sendRtpItem.getApp(), sendRtpItem.getStream());
            mediaServerService.releaseSsrc(mediaServerItem.getServerId(), sendRtpItem.getSsrc());
            errorEvent.response(e);
        }), e -> {
            // 这里为例避免一个通道的点播只有一个callID这个参数使用一个固定值
            ResponseEvent responseEvent = (ResponseEvent) e.event;
            SIPResponse response = (SIPResponse) responseEvent.getResponse();
            SsrcTransaction ssrcTransaction = SsrcTransaction.buildForDevice(device.getGbId(), channel.getId(), "talk",sendRtpItem.getApp(), stream, sendRtpItem.getSsrc(), mediaServerItem.getServerId(), response, InviteSessionType.TALK);
            sessionManager.put(ssrcTransaction);
            okEvent.response(e);
        }, timeout);
    }


    @Override
    public void audioBroadcastCmd(MediaDevice device, String channelId, SipSubscribe.Event okEvent, SipSubscribe.Event errorEvent) throws InvalidArgumentException, SipException, ParseException {
        StringBuffer broadcastXml = new StringBuffer(200);
        String charset = device.getCharset();
        broadcastXml.append("<?xml version=\"1.0\" encoding=\"" + charset + "\"?>\r\n");
        broadcastXml.append("<Notify>\r\n");
        broadcastXml.append("<CmdType>Broadcast</CmdType>\r\n");
        broadcastXml.append("<SN>" + (int)((Math.random()*9+1)*100000) + "</SN>\r\n");
        broadcastXml.append("<SourceID>" + sipConfig.getId() + "</SourceID>\r\n");
        broadcastXml.append("<TargetID>" + channelId + "</TargetID>\r\n");
        broadcastXml.append("</Notify>\r\n");

        Request request = headerProvider.createMessageRequest(device, broadcastXml.toString(), SipUtils.getNewViaTag(), SipUtils.getNewFromTag(), null,sipSender.getNewCallIdHeader(sipLayer.getLocalIp(device.getLocalIp()),device.getTransport()));
        sipSender.transmitRequest(sipLayer.getLocalIp(device.getLocalIp()), request, errorEvent, okEvent);

    }

    /**
     * 回放暂停
     */
    @Override
    public void playPauseCmd(MediaDevice device, MediaDeviceChannel channel, StreamDetail streamInfo) throws InvalidArgumentException, ParseException, SipException {
        StringBuffer content = new StringBuffer(200);
        content.append("PAUSE RTSP/1.0\r\n");
        content.append("CSeq: " + getInfoCseq() + "\r\n");
        content.append("PauseTime: now\r\n");

        playbackControlCmd(device, channel, streamInfo, content.toString(), null, null);
    }



    /**
     * 回放恢复
     */
    @Override
    public void playResumeCmd(MediaDevice device, MediaDeviceChannel channel, StreamDetail streamInfo) throws InvalidArgumentException, ParseException, SipException {
        StringBuffer content = new StringBuffer(200);
        content.append("PLAY RTSP/1.0\r\n");
        content.append("CSeq: " + getInfoCseq() + "\r\n");
        content.append("Range: npt=now-\r\n");

        playbackControlCmd(device, channel, streamInfo, content.toString(), null, null);
    }

    /**
     * 回放拖动播放
     */
    @Override
    public void playSeekCmd(MediaDevice device, MediaDeviceChannel channel, StreamDetail streamInfo, long seekTime) throws InvalidArgumentException, ParseException, SipException {
        StringBuffer content = new StringBuffer(200);
        content.append("PLAY RTSP/1.0\r\n");
        content.append("CSeq: " + getInfoCseq() + "\r\n");
        content.append("Range: npt=" + Math.abs(seekTime) + "-\r\n");

        playbackControlCmd(device, channel, streamInfo, content.toString(), null, null);
    }

    /**
     * 回放倍速播放
     */
    @Override
    public void playSpeedCmd(MediaDevice device, MediaDeviceChannel channel, StreamDetail streamInfo, Double speed) throws InvalidArgumentException, ParseException, SipException {
        StringBuffer content = new StringBuffer(200);
        content.append("PLAY RTSP/1.0\r\n");
        content.append("CSeq: " + getInfoCseq() + "\r\n");
        content.append("Scale: " + String.format("%.6f", speed) + "\r\n");

        playbackControlCmd(device, channel, streamInfo, content.toString(), null, null);
    }

    private int getInfoCseq() {
        return (int) ((Math.random() * 9 + 1) * Math.pow(10, 8));
    }

    @Override
    public void playbackControlCmd(MediaDevice device, MediaDeviceChannel channel, StreamDetail streamInfo,
                                   String content, SipSubscribe.Event errorEvent, SipSubscribe.Event okEvent) throws SipException, InvalidArgumentException, ParseException {

        playbackControlCmd(device, channel, streamInfo.getStream(), content, errorEvent, okEvent);
    }

    @Override
    public void playbackControlCmd(MediaDevice device, MediaDeviceChannel channel, String stream, String content, SipSubscribe.Event errorEvent, SipSubscribe.Event okEvent) throws SipException, InvalidArgumentException, ParseException {

        SsrcTransaction ssrcTransaction = sessionManager.getSsrcTransactionByStream("rtp", stream);
        if (ssrcTransaction == null) {
            log.info("[回放控制]未找到视频流信息，设备：{}, 流ID: {}", device.getGbId(), stream);
            return;
        }

        SIPRequest request = headerProvider.createInfoRequest(device, channel.getGbId(), content, ssrcTransaction.getSipTransactionInfo());
        if (request == null) {
            log.info("[回放控制]构建Request信息失败，设备：{}, 流ID: {}", device.getGbId(), stream);
            return;
        }

        sipSender.transmitRequest(sipLayer.getLocalIp(device.getLocalIp()), request, errorEvent, okEvent);
    }

    @Override
    public void streamByeCmdForDeviceInvite(MediaDevice device, String channelId, SipTransactionInfo sipTransactionInfo, SipSubscribe.Event okEvent) throws InvalidArgumentException, SipException, ParseException, SsrcTransactionNotFoundException {
        Request byteRequest = headerProvider.createByteRequestForDeviceInvite(device, channelId, sipTransactionInfo);
        sipSender.transmitRequest(sipLayer.getLocalIp(device.getLocalIp()), byteRequest, null, okEvent);
    }

    /**
     * 音视频录像控制
     *
     * @param device       视频设备
     * @param channelId    预览通道
     * @param recordCmdStr 录像命令：Record / StopRecord
     */
    @Override
    public void recordCmd(MediaDevice device, String channelId, String recordCmdStr, ErrorCallback<String> callback) throws InvalidArgumentException, SipException, ParseException {
        final String cmdType = "DeviceControl";
        final int sn = (int) ((Math.random() * 9 + 1) * 100000);

        StringBuffer cmdXml = new StringBuffer(200);
        String charset = device.getCharset();
        cmdXml.append("<?xml version=\"1.0\" encoding=\"" + charset + "\"?>\r\n");
        cmdXml.append("<Control>\r\n");
        cmdXml.append("<CmdType>" + cmdType + "</CmdType>\r\n");
        cmdXml.append("<SN>" + sn + "</SN>\r\n");
        if (ObjectUtils.isEmpty(channelId)) {
            cmdXml.append("<DeviceID>" + device.getGbId() + "</DeviceID>\r\n");
        } else {
            cmdXml.append("<DeviceID>" + channelId + "</DeviceID>\r\n");
        }
        cmdXml.append("<RecordCmd>" + recordCmdStr + "</RecordCmd>\r\n");
        cmdXml.append("</Control>\r\n");

        MessageEvent<String> messageEvent = MessageEvent.getInstance(cmdType, sn + "", channelId, 1000L, callback);
        messageSubscribe.addSubscribe(messageEvent);

        Request request = headerProvider.createMessageRequest(device, cmdXml.toString(), null, SipUtils.getNewFromTag(),
                null,sipSender.getNewCallIdHeader(sipLayer.getLocalIp(device.getLocalIp()),device.getTransport()));
        sipSender.transmitRequest(sipLayer.getLocalIp(device.getLocalIp()), request, eventResult -> {
            messageSubscribe.removeSubscribe(messageEvent.getKey());
            callback.run(500, "失败，" + eventResult.msg, null);
        },null);
    }

    /**
     * 查询设备状态
     *
     * @param device 视频设备
     */
    @Override
    public void deviceStatusQuery(MediaDevice device, MediaDeviceChannel channel, ErrorCallback<DeviceStatus> callback) throws InvalidArgumentException, SipException, ParseException {

        String cmdType = "DeviceStatus";
        int sn = (int) ((Math.random() * 9 + 1) * 100000);

        String charset = device.getCharset();
        StringBuffer catalogXml = new StringBuffer(200);
        catalogXml.append("<?xml version=\"1.0\" encoding=\"" + charset + "\"?>\r\n");
        catalogXml.append("<Query>\r\n");
        catalogXml.append("<CmdType>" + cmdType + "</CmdType>\r\n");
        catalogXml.append("<SN>" + sn + "</SN>\r\n");
        if (channel != null && channel.getGbId() != null) {
            catalogXml.append("<DeviceID>" + channel.getGbId() + "</DeviceID>\r\n");
        } else {
            catalogXml.append("<DeviceID>" + device.getGbId() + "</DeviceID>\r\n");
        }
        catalogXml.append("</Query>\r\n");

        MessageEvent<DeviceStatus> messageEvent = MessageEvent.getInstance(cmdType, sn + "", device.getGbId(), 1000L, callback);
        messageSubscribe.addSubscribe(messageEvent);

        Request request = headerProvider.createMessageRequest(device, catalogXml.toString(), null, SipUtils.getNewFromTag(),
                null, sipSender.getNewCallIdHeader(sipLayer.getLocalIp(device.getLocalIp()), device.getTransport()));

        sipSender.transmitRequest(sipLayer.getLocalIp(device.getLocalIp()), request, eventResult -> {
            messageSubscribe.removeSubscribe(messageEvent.getKey());
            callback.run(500, "失败，" + eventResult.msg, null);
        });
    }

    /**
     * 设备信息查询
     * @param device 视频设备信息
     * @throws InvalidArgumentException,SipException,ParseException 异常
     */
    @Override
    public void deviceInfoQuery(MediaDevice device) throws InvalidArgumentException, SipException, ParseException {

        StringBuffer catalogXml = new StringBuffer(200);
        String charset = device.getCharset();
        catalogXml.append("<?xml version=\"1.0\" encoding=\"").append(charset).append("\"?>\r\n");
        catalogXml.append("<Query>\r\n");
        catalogXml.append("<CmdType>DeviceInfo</CmdType>\r\n");
        catalogXml.append("<SN>").append((int) ((Math.random() * 9 + 1) * 100000)).append("</SN>\r\n");
        catalogXml.append("<DeviceID>").append(device.getGbId()).append("</DeviceID>\r\n");
        catalogXml.append("</Query>\r\n");

        // 创建MESSAGE消息
        Request request = headerProvider.createMessageRequest(device, catalogXml.toString(), SipUtils.getNewViaTag(), SipUtils.getNewFromTag(), null,sipSender.getNewCallIdHeader(sipLayer.getLocalIp(device.getLocalIp()),device.getTransport()));

        sipSender.transmitRequest(sipLayer.getLocalIp(device.getLocalIp()), request);
    }

    /**
     * 设备通道查询
     * @param device 视频设备
     * @param sn sn
     * @param errorEvent 异常回调
     * @throws InvalidArgumentException,SipException,ParseException 异常
     */
    @Override
    public void catalogQuery(MediaDevice device, int sn, SipSubscribe.Event errorEvent) throws SipException, InvalidArgumentException, ParseException {
        StringBuffer catalogXml = new StringBuffer(200);
        String charset = device.getCharset();
        catalogXml.append("<?xml version=\"1.0\" encoding=\"").append(charset).append("\"?>\r\n");
        catalogXml.append("<Query>\r\n");
        catalogXml.append("  <CmdType>Catalog</CmdType>\r\n");
        catalogXml.append("  <SN>").append(sn).append("</SN>\r\n");
        catalogXml.append("  <DeviceID>").append(device.getGbId()).append("</DeviceID>\r\n");
        catalogXml.append("</Query>\r\n");

        Request request = headerProvider.createMessageRequest(device, catalogXml.toString(), SipUtils.getNewViaTag(), SipUtils.getNewFromTag(), null,sipSender.getNewCallIdHeader(sipLayer.getLocalIp(device.getLocalIp()),device.getTransport()));

        sipSender.transmitRequest(sipLayer.getLocalIp(device.getLocalIp()), request, errorEvent);
    }

    @Override
    public void recordInfoQuery(MediaDevice device, String channelId, String startTime, String endTime, int sn,
                                Integer secrecy, String type, SipSubscribe.Event okEvent, SipSubscribe.Event errorEvent)
            throws InvalidArgumentException, SipException, ParseException {

        if (secrecy == null) {
            secrecy = 0;
        }
        if (type == null) {
            type = "all";
        }

        StringBuffer recordInfoXml = new StringBuffer(200);
        String charset = device.getCharset();
        recordInfoXml.append("<?xml version=\"1.0\" encoding=\"" + charset + "\"?>\r\n");
        recordInfoXml.append("<Query>\r\n");
        recordInfoXml.append("<CmdType>RecordInfo</CmdType>\r\n");
        recordInfoXml.append("<SN>" + sn + "</SN>\r\n");
        recordInfoXml.append("<DeviceID>" + channelId + "</DeviceID>\r\n");
        if (startTime != null) {
            recordInfoXml.append("<StartTime>" + DateUtil.yyyy_MM_dd_HH_mm_ssToISO8601(startTime) + "</StartTime>\r\n");
        }
        if (endTime != null) {
            recordInfoXml.append("<EndTime>" + DateUtil.yyyy_MM_dd_HH_mm_ssToISO8601(endTime) + "</EndTime>\r\n");
        }
        if (secrecy != null) {
            recordInfoXml.append("<Secrecy> " + secrecy + " </Secrecy>\r\n");
        }
        if (type != null) {
            // 大华NVR要求必须增加一个值为all的文本元素节点Type
            recordInfoXml.append("<Type>" + type + "</Type>\r\n");
        }
        recordInfoXml.append("</Query>\r\n");

        Request request = headerProvider.createMessageRequest(device, recordInfoXml.toString(), SipUtils.getNewViaTag(),
                SipUtils.getNewFromTag(), null,
                sipSender.getNewCallIdHeader(sipLayer.getLocalIp(device.getLocalIp()), device.getTransport()));

        sipSender.transmitRequest(sipLayer.getLocalIp(device.getLocalIp()), request, errorEvent, okEvent);
    }

    /**
     * 设备信息配置
     * @param device 视频设备信息
     * @throws InvalidArgumentException,SipException,ParseException 异常
     */
    @Override
    public void deviceConfigQuery(MediaDevice device, String channelId, String configType, SipSubscribe.Event errorEvent) throws InvalidArgumentException, SipException, ParseException {

        StringBuffer cmdXml = new StringBuffer(200);
        String charset = device.getCharset();
        cmdXml.append("<?xml version=\"1.0\" encoding=\"").append(charset).append("\"?>\r\n");
        cmdXml.append("<Query>\r\n");
        cmdXml.append("<CmdType>ConfigDownload</CmdType>\r\n");
        cmdXml.append("<SN>").append((int) ((Math.random() * 9 + 1) * 100000)).append("</SN>\r\n");
        if (ObjectUtils.isEmpty(channelId)) {
            cmdXml.append("<DeviceID>").append(device.getGbId()).append("</DeviceID>\r\n");
        } else {
            cmdXml.append("<DeviceID>").append(channelId).append("</DeviceID>\r\n");
        }
        cmdXml.append("<ConfigType>").append(configType).append("</ConfigType>\r\n");
        cmdXml.append("</Query>\r\n");

        Request request = headerProvider.createMessageRequest(device, cmdXml.toString(), null, SipUtils.getNewFromTag(), null,sipSender.getNewCallIdHeader(sipLayer.getLocalIp(device.getLocalIp()),device.getTransport()));

        sipSender.transmitRequest(sipLayer.getLocalIp(device.getLocalIp()), request, errorEvent);
    }

}
