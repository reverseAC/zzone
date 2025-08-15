package com.zjh.zzone.iot.media.service.impl;

import com.ylg.core.exception.CheckedException;
import com.ylg.iot.constant.MediaCacheConstants;
import com.ylg.iot.constant.MediaConstants;
import com.ylg.iot.enums.media.*;
import com.ylg.iot.media.bo.*;
import com.ylg.iot.media.scheduling.DynamicTask;
import com.ylg.iot.media.dto.CloudRecordDTO;
import com.ylg.iot.media.event.media.MediaArrivalEvent;
import com.ylg.iot.media.gb28181.event.AudioBroadcastEvent;
import com.ylg.iot.media.callback.Hook;
import com.ylg.iot.media.callback.HookSubscribe;
import com.ylg.iot.entity.CloudRecord;
import com.ylg.iot.media.callback.ErrorCallback;
import com.ylg.iot.media.config.ServerInstanceConfig;
import com.ylg.iot.media.bo.HookRecordInfo;
import com.ylg.iot.media.service.*;
import com.ylg.iot.media.utils.CloudRecordUtils;
import com.ylg.iot.media.vo.AudioBroadcastResultVO;
import com.ylg.iot.media.gb28181.common.SsrcTransactionNotFoundException;
import com.ylg.iot.media.config.UserSetting;
import com.ylg.iot.media.contant.InviteResultCode;
import com.ylg.iot.media.contant.StreamModeType;
import com.ylg.iot.entity.MediaServer;
import com.ylg.iot.entity.MediaDeviceChannel;
import com.ylg.iot.entity.MediaDevice;
import com.ylg.iot.media.callback.SipSubscribe;
import com.ylg.iot.media.gb28181.session.SSRCFactory;
import com.ylg.iot.media.gb28181.session.SipInviteSessionManager;
import com.ylg.iot.media.gb28181.transmit.cmd.SIPCommander;
import com.ylg.iot.media.session.AudioBroadcastManager;
import com.ylg.iot.media.utils.DateUtil;
import com.ylg.iot.media.utils.SipUtils;
import com.ylg.iot.vo.DownloadFileInfo;
import com.ylg.iot.vo.MediaInfo;
import com.ylg.redis.util.RedisUtils;
import gov.nist.javax.sip.message.SIPResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import javax.annotation.Resource;
import javax.sdp.*;
import javax.sip.InvalidArgumentException;
import javax.sip.ResponseEvent;
import javax.sip.SipException;
import javax.sip.header.CallIdHeader;
import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Vector;

/**
 * 国标视频点播 服务实现类
 *
 * @author zjh
 * @since 2025-03-26 10:40
 */
@Service
@Slf4j
public class PlayServiceImpl implements PlayService {

    @Resource
    private MediaServerService mediaServerService;

    @Autowired
    private CloudRecordService cloudRecordService;

    @Autowired
    private AudioBroadcastManager audioBroadcastManager;

    @Resource
    private MediaDeviceChannelService deviceChannelService;

    @Resource
    private InviteStreamService inviteStreamService;

    @Resource
    private ReceiveRtpServerService receiveRtpServerService;

    @Autowired
    private SendRtpServerService sendRtpServerService;

    @Autowired
    private MediaDeviceService deviceService;

    @Autowired
    private SipInviteSessionManager sessionManager;

    @Autowired
    private SIPCommander cmder;

    @Resource
    private UserSetting userSetting;

    @Autowired
    private ServerInstanceConfig serverInstance;

    @Autowired
    private SSRCFactory ssrcFactory;

    @Autowired
    private HookSubscribe subscribe;

    @Autowired
    private DynamicTask dynamicTask;


    /**
     * 流到来的处理
     */
    @Async("taskExecutor")
    @org.springframework.context.event.EventListener
    public void onApplicationEvent(MediaArrivalEvent event) {   // 广播
        if ("broadcast".equals(event.getApp()) || "talk".equals(event.getApp())) {
            if (event.getStream().indexOf("_") > 0) {
                String[] streamArray = event.getStream().split("_");
                if (streamArray.length == 2) {
                    String deviceId = streamArray[0];
                    String channelId = streamArray[1];
                    MediaDevice device = deviceService.getByGbId(deviceId);
                    MediaDeviceChannel channel = deviceChannelService.getByGbId(deviceId, channelId);
                    if (device == null) {
                        log.info("[语音对讲/喊话] 未找到设备：{}", deviceId);
                        return;
                    }
                    if (channel == null) {
                        log.info("[语音对讲/喊话] 未找到通道：{}", channelId);
                        return;
                    }
                    if ("broadcast".equals(event.getApp())) {
                        if (audioBroadcastManager.exit(channel.getId())) {
                            stopAudioBroadcast(device, channel);
                        }
                        // 开启语音对讲通道
                        try {
                            audioBroadcastCmd(device, channel, event.getMediaServer(),
                                    event.getApp(), event.getStream(), 60, false, (msg) -> {
                                        log.info("[语音对讲] 通道建立成功, device: {}, channel: {}", deviceId, channelId);
                                    });
                        } catch (InvalidArgumentException | ParseException | SipException e) {
                            log.error("[命令发送失败] 语音对讲: {}", e.getMessage());
                        }
                    } else if ("talk".equals(event.getApp())) {
                        // 开启语音对讲通道
                        talkCmd(device, channel, event.getMediaServer(), event.getStream(), (msg) -> {
                            log.info("[语音对讲] 通道建立成功, device: {}, channel: {}", deviceId, channelId);
                        });
                    }
                }
            }
        }


    }

    /**
     * 视频点播
     *
     * @param device 设备媒体信息
     * @param channel 设备媒体通道
     * @param callback 异常回调
     * @return 视频流标识信息
     */
    @Override
    public SSRCInfo play(MediaDevice device, MediaDeviceChannel channel, ErrorCallback<StreamDetail> callback) {

        MediaServer mediaServerItem = getAMediaServerItem(device);
        if (mediaServerItem == null) {
            log.warn("[点播] 未找到可用的zlm gbId: {},channelNo:{}", device.getGbId(), channel.getGbId());
            throw new CheckedException("未找到可用的zlm");
        }
        if (StreamModeType.TCP_ACTIVE.name().equalsIgnoreCase(device.getStreamMode()) && !mediaServerItem.isRtpEnable()) {
            log.warn("[点播] 单端口收流时不支持TCP主动方式收流 deviceId: {},channelId:{}", device.getGbId(), channel.getGbId());
            throw new CheckedException("单端口收流时不支持TCP主动方式收流");
        }

        return play(mediaServerItem, device, channel, null, userSetting.getRecordSip(), callback);
    }

    /**
     * 视频点播
     *
     * @param mediaServerItem 流媒体服务
     * @param device 设备
     * @param channel 通道
     * @param ssrc 流唯一标识
     * @param record 点播时是否录制录像
     * @param callback 结果回调
     * @return
     */
    private SSRCInfo play(MediaServer mediaServerItem, MediaDevice device, MediaDeviceChannel channel, String ssrc, Boolean record,
                          ErrorCallback<StreamDetail> callback) {

        if (mediaServerItem == null ) {
            if (callback != null) {
                callback.run(InviteResultCode.ERROR_FOR_PARAMETER_ERROR.getCode(),
                        InviteResultCode.ERROR_FOR_PARAMETER_ERROR.getMsg(),
                        null);
            }
            return null;
        }

        // 检查当前通道是否已经存在点播会话
        InviteInfo inviteInfoInCache = inviteStreamService.getInviteInfoByDeviceAndChannel(InviteSessionType.PLAY, channel.getId());
        if (inviteInfoInCache != null ) {
            if (inviteInfoInCache.getStreamDetail() == null) {
                // 释放生成的ssrc，使用上一次申请的322
                ssrcFactory.releaseSsrc(mediaServerItem.getServerId(), ssrc);
                // 点播发起了但是尚未成功, 仅注册回调等待结果即可
                inviteStreamService.addCallback(InviteSessionType.PLAY, channel.getId(), null, callback);
                log.info("[点播开始] 已经请求中，等待结果， deviceId: {}, channelId({}): {}", device.getGbId(), channel.getGbId(), channel.getName());
                return inviteInfoInCache.getSsrcInfo();
            } else {
                StreamDetail streamDetail = inviteInfoInCache.getStreamDetail();
                String streamId = streamDetail.getStream();
                if (streamId == null) {
                    callback.run(InviteResultCode.ERROR_FOR_CATCH_DATA.getCode(), "点播失败， redis缓存streamId等于null", null);
                    inviteStreamService.runCallback(InviteSessionType.PLAY, channel.getId(), null,
                            InviteResultCode.ERROR_FOR_CATCH_DATA.getCode(),
                            "点播失败， redis缓存streamId等于null",
                            null);
                    return inviteInfoInCache.getSsrcInfo();
                }
                MediaServer mediaInfo = streamDetail.getMediaServer();
                // 检查流是否仍然可用
                Boolean ready = mediaServerService.isStreamReady(mediaInfo, "rtp", streamId);
                if (ready != null && ready) {
                    if(callback != null) {
                        callback.run(InviteResultCode.SUCCESS.getCode(), InviteResultCode.SUCCESS.getMsg(), streamDetail);
                    }
                    inviteStreamService.runCallback(InviteSessionType.PLAY, channel.getId(), null,
                            InviteResultCode.SUCCESS.getCode(),
                            InviteResultCode.SUCCESS.getMsg(),
                            streamDetail);
                    log.info("[点播已存在] 直接返回， deviceId: {}, channelId: {}", device.getGbId(), channel.getGbId());
                    return inviteInfoInCache.getSsrcInfo();
                } else {
                    // 点播发起了但是尚未成功, 仅注册回调等待结果即可
                    inviteStreamService.addCallback(InviteSessionType.PLAY, channel.getId(), null, callback);
                    deviceChannelService.stopPlay(channel.getId());
                    inviteStreamService.removeInviteInfoByDeviceAndChannel(InviteSessionType.PLAY, channel.getId());
                }
            }
        }

        // 开启RTP服务器
        String streamId = String.format("%s_%s", device.getGbId(), channel.getGbId());
        int tcpMode = StreamModeType.valueOf(device.getStreamMode()).value();
        RTPServerParam rtpServerParam = new RTPServerParam();
        rtpServerParam.setMediaServerItem(mediaServerItem);
        rtpServerParam.setStreamId(streamId);
        rtpServerParam.setPresetSsrc(ssrc);
        rtpServerParam.setSsrcCheck(device.getSsrcCheck());
        rtpServerParam.setPlayback(false);
        rtpServerParam.setPort(0);
        rtpServerParam.setTcpMode(tcpMode);
        rtpServerParam.setOnlyAuto(false);
        rtpServerParam.setDisableAudio(!channel.isHasAudio());
        SSRCInfo ssrcInfo = receiveRtpServerService.openRTPServer(rtpServerParam, (code, msg, result) -> {

            if (code == InviteResultCode.SUCCESS.getCode() && result != null && result.getHookData() != null) {
                // hook响应
                StreamDetail streamDetail = onPublishHandlerForPlay(result.getHookData().getMediaServer(), result.getHookData().getMediaInfo(), device, channel);
                if (streamDetail == null){
                    if (callback != null) {
                        callback.run(InviteResultCode.ERROR_FOR_STREAM_PARSING_EXCEPTIONS.getCode(),
                                InviteResultCode.ERROR_FOR_STREAM_PARSING_EXCEPTIONS.getMsg(), null);
                    }
                    inviteStreamService.runCallback(InviteSessionType.PLAY, channel.getId(), null,
                            InviteResultCode.ERROR_FOR_STREAM_PARSING_EXCEPTIONS.getCode(),
                            InviteResultCode.ERROR_FOR_STREAM_PARSING_EXCEPTIONS.getMsg(), null);
                    return;
                }
                if (callback != null) {
                    callback.run(InviteResultCode.SUCCESS.getCode(), InviteResultCode.SUCCESS.getMsg(), streamDetail);
                }
                inviteStreamService.runCallback(InviteSessionType.PLAY, channel.getId(), null,
                        InviteResultCode.SUCCESS.getCode(),
                        InviteResultCode.SUCCESS.getMsg(),
                        streamDetail);

                log.info("[点播成功] deviceId: {}, channelId:{}, 码流类型：{}", device.getGbId(), channel.getGbId(),
                        channel.getStreamIdentification());
                // 点播成功调用截图
                snapOnPlay(result.getHookData().getMediaServer(), device.getGbId(), channel.getGbId(), streamId);
            } else {
                if (callback != null) {
                    callback.run(code, msg, null);
                }
                inviteStreamService.runCallback(InviteSessionType.PLAY, channel.getId(), null, code, msg, null);
                inviteStreamService.removeInviteInfoByDeviceAndChannel(InviteSessionType.PLAY, channel.getId());
                SsrcTransaction ssrcTransaction = sessionManager.getSsrcTransactionByStream("rtp", streamId);
                if (ssrcTransaction != null) {
                    try {
                        cmder.streamByeCmd(device, channel.getGbId(),"rtp", streamId, null, null);
                    } catch (InvalidArgumentException | ParseException | SipException | SsrcTransactionNotFoundException e) {
                        log.error("[点播超时]， 发送BYE失败 {}", e.getMessage());
                    } finally {
                        sessionManager.removeByStream("rtp", streamId);
                    }
                }
            }
        });
        if (ssrcInfo == null || ssrcInfo.getPort() <= 0) {
            log.info("[点播端口/SSRC]获取失败，deviceId={},channelId={},ssrcInfo={}", device.getGbId(), channel.getGbId(), ssrcInfo);
            callback.run(InviteResultCode.ERROR_FOR_RESOURCE_EXHAUSTION.getCode(), "获取端口或者ssrc失败", null);
            inviteStreamService.runCallback(InviteSessionType.PLAY, channel.getId(), null,
                    InviteResultCode.ERROR_FOR_RESOURCE_EXHAUSTION.getCode(),
                    InviteResultCode.ERROR_FOR_RESOURCE_EXHAUSTION.getMsg(),
                    null);
            return null;
        }
        log.info("[点播开始] deviceId: {}, channelId({}): {},码流类型：{}, 收流端口： {}, 码流：{}, 收流模式：{}, SSRC: {}, SSRC校验：{}",
                device.getGbId(), channel.getGbId(), channel.getId(), channel.getStreamIdentification(), ssrcInfo.getPort(), ssrcInfo.getStream(),
                device.getStreamMode(), ssrcInfo.getSsrc(), device.getSsrcCheck());

        // 初始化redis中的invite消息状态
        InviteInfo inviteInfo = InviteInfo.getInviteInfo(device.getGbId(), channel.getId(), ssrcInfo.getStream(), ssrcInfo, mediaServerItem.getServerId(),
                mediaServerItem.getSdpIp(), ssrcInfo.getPort(), device.getStreamMode(), InviteSessionType.PLAY,
                InviteSessionStatus.ready, userSetting.getRecordSip());
        if (record != null) {
            inviteInfo.setRecord(record);
        } else {
            inviteInfo.setRecord(userSetting.getRecordSip());
        }

        inviteStreamService.updateInviteInfo(inviteInfo);

        try {
            // 发送SIP播放命令
            cmder.playStreamCmd(mediaServerItem, ssrcInfo, device, channel, (eventResult) -> {
                // 处理收到200ok后的TCP主动连接以及SSRC不一致的问题
                inviteOKHandler(eventResult, ssrcInfo, mediaServerItem, device, channel, callback, inviteInfo, InviteSessionType.PLAY);
            }, (event) -> {
                log.info("[点播失败]{}:{} deviceId: {}, channelId:{}",event.statusCode, event.msg, device.getGbId(), channel.getGbId());
                receiveRtpServerService.closeRTPServer(mediaServerItem, ssrcInfo);

                sessionManager.removeByStream(ssrcInfo.getApp(), ssrcInfo.getStream());
                if (callback != null) {
                    callback.run(event.statusCode, event.msg, null);
                }
                inviteStreamService.runCallback(InviteSessionType.PLAY, channel.getId(), null,
                        event.statusCode, event.msg, null);

                inviteStreamService.removeInviteInfoByDeviceAndChannel(InviteSessionType.PLAY, channel.getId());
            }, userSetting.getPlayTimeout().longValue());

        } catch (InvalidArgumentException | SipException | ParseException e) {
            log.error("[命令发送失败] 点播消息: {}", e.getMessage());
            receiveRtpServerService.closeRTPServer(mediaServerItem, ssrcInfo);
            sessionManager.removeByStream(ssrcInfo.getApp(), ssrcInfo.getStream());
            if (callback != null) {
                callback.run(InviteResultCode.ERROR_FOR_SIP_SENDING_FAILED.getCode(),
                        InviteResultCode.ERROR_FOR_SIP_SENDING_FAILED.getMsg(), null);
            }
            inviteStreamService.runCallback(InviteSessionType.PLAY, channel.getId(), null,
                    InviteResultCode.ERROR_FOR_SIP_SENDING_FAILED.getCode(),
                    InviteResultCode.ERROR_FOR_SIP_SENDING_FAILED.getMsg(), null);

            inviteStreamService.removeInviteInfoByDeviceAndChannel(InviteSessionType.PLAY, channel.getId());
        }
        return ssrcInfo;
    }


    @Override
    public void download(MediaDevice device, MediaDeviceChannel channel, String startTime, String endTime,
                         int downloadSpeed, ErrorCallback<StreamDetail> callback) {

        MediaServer newMediaServerItem = this.getAMediaServerItem(device);
        if (newMediaServerItem == null) {
            callback.run(InviteResultCode.ERROR_FOR_ASSIST_NOT_READY.getCode(),
                    InviteResultCode.ERROR_FOR_ASSIST_NOT_READY.getMsg(),
                    null);
            return;
        }

        download(newMediaServerItem, device, channel, startTime, endTime, downloadSpeed, callback);
    }


    private void download(MediaServer mediaServerItem, MediaDevice device, MediaDeviceChannel channel, String startTime,
                          String endTime, int downloadSpeed, ErrorCallback<StreamDetail> callback) {

        if (mediaServerItem == null ) {
            callback.run(InviteResultCode.ERROR_FOR_PARAMETER_ERROR.getCode(),
                    InviteResultCode.ERROR_FOR_PARAMETER_ERROR.getMsg(),
                    null);
            return;
        }

        // GB28181协议中，下载设备录像的标准方式是：通过点播回放（playback）让设备发送RTP流，平台边接收边保存
        // 1. 开启RTP服务
        int tcpMode = StreamModeType.valueOf(device.getStreamMode()).value();
        // 录像下载不使用固定流地址，固定流地址会导致如果开始时间与结束时间一致时文件错误的叠加在一起
        RTPServerParam rtpServerParam = new RTPServerParam();
        rtpServerParam.setMediaServerItem(mediaServerItem);
        rtpServerParam.setSsrcCheck(device.getSsrcCheck());
        rtpServerParam.setPlayback(true);
        rtpServerParam.setPort(0);
        rtpServerParam.setTcpMode(tcpMode);
        rtpServerParam.setOnlyAuto(false);
        rtpServerParam.setDisableAudio(!channel.isHasAudio());
        SSRCInfo ssrcInfo = receiveRtpServerService.openRTPServer(rtpServerParam, (code, msg, result) -> {
            if (code == InviteResultCode.SUCCESS.getCode() && result != null && result.getHookData() != null) {
                // hook响应
                StreamDetail streamInfo = onPublishHandlerForDownload(mediaServerItem, result.getHookData().getMediaInfo(), device, channel, startTime, endTime);
                if (streamInfo == null) {
                    log.warn("[录像下载] 获取流地址信息失败");
                    callback.run(InviteResultCode.ERROR_FOR_STREAM_PARSING_EXCEPTIONS.getCode(),
                            InviteResultCode.ERROR_FOR_STREAM_PARSING_EXCEPTIONS.getMsg(), null);
                    return;
                }
                callback.run(InviteResultCode.SUCCESS.getCode(), InviteResultCode.SUCCESS.getMsg(), streamInfo);
                log.info("[录像下载] 调用成功 deviceId: {}, channelId: {},  开始时间: {}, 结束时间： {}", device.getGbId(), channel, startTime, endTime);
            } else {
                if (callback != null) {
                    callback.run(code, msg, null);
                }
                inviteStreamService.runCallback(InviteSessionType.DOWNLOAD, channel.getId(), null, code, msg, null);
                inviteStreamService.removeInviteInfoByDeviceAndChannel(InviteSessionType.DOWNLOAD, channel.getId());
                if (result != null && result.getSsrcInfo() != null) {
                    SsrcTransaction ssrcTransaction = sessionManager.getSsrcTransactionByStream(result.getSsrcInfo().getApp(), result.getSsrcInfo().getStream());
                    if (ssrcTransaction != null) {
                        try {
                            cmder.streamByeCmd(device, channel.getGbId(), ssrcTransaction.getApp(), ssrcTransaction.getStream(), null, null);
                        } catch (InvalidArgumentException | ParseException | SipException | SsrcTransactionNotFoundException e) {
                            log.error("[录像下载] 发送BYE失败 {}", e.getMessage());
                        } finally {
                            sessionManager.removeByStream(ssrcTransaction.getApp(), ssrcTransaction.getStream());
                        }
                    }
                }
            }
        });
        if (ssrcInfo == null || ssrcInfo.getPort() <= 0) {
            log.info("[录像下载端口/SSRC]获取失败，deviceId={},channelId={},ssrcInfo={}", device.getGbId(), channel.getGbId(), ssrcInfo);
            if (callback != null) {
                callback.run(InviteResultCode.ERROR_FOR_RESOURCE_EXHAUSTION.getCode(), "获取端口或者ssrc失败", null);
            }
            inviteStreamService.runCallback(InviteSessionType.PLAY, channel.getId(), null,
                    InviteResultCode.ERROR_FOR_RESOURCE_EXHAUSTION.getCode(),
                    InviteResultCode.ERROR_FOR_RESOURCE_EXHAUSTION.getMsg(),
                    null);
            return;
        }
        log.info("[录像下载] deviceId: {}, channelId: {}, 开始时间： {}, 结束时间： {}， 下载速度：{}, 收流端口：{}, 收流模式：{}, SSRC: {}({}), SSRC校验：{}",
                device.getGbId(), channel.getGbId(), startTime, endTime, downloadSpeed, ssrcInfo.getPort(), device.getStreamMode(),
                ssrcInfo.getSsrc(), String.format("%08x", Long.parseLong(ssrcInfo.getSsrc())).toUpperCase(),
                device.getSsrcCheck());

        // 2. 点播回放
        // 初始化redis中的invite消息状态
        InviteInfo inviteInfo = InviteInfo.getInviteInfo(device.getGbId(), channel.getId(), ssrcInfo.getStream(), ssrcInfo, mediaServerItem.getServerId(),
                mediaServerItem.getSdpIp(), ssrcInfo.getPort(), device.getStreamMode(), InviteSessionType.DOWNLOAD,
                InviteSessionStatus.ready, true);
        inviteInfo.setStartTime(startTime);
        inviteInfo.setEndTime(endTime);

        inviteStreamService.updateInviteInfo(inviteInfo);
        try {
            cmder.downloadStreamCmd(mediaServerItem, ssrcInfo, device, channel, startTime, endTime, downloadSpeed,
                    eventResult -> {
                        // 对方返回错误
                        callback.run(InviteResultCode.FAIL.getCode(), String.format("录像下载失败， 错误码： %s, %s",
                                eventResult.statusCode, eventResult.msg), null);
                        receiveRtpServerService.closeRTPServer(mediaServerItem, ssrcInfo);
                        sessionManager.removeByStream(ssrcInfo.getApp(), ssrcInfo.getStream());
                        inviteStreamService.removeInviteInfo(inviteInfo);
                    }, eventResult ->{
                        // 处理收到200ok后的TCP主动连接以及SSRC不一致的问题
                        inviteOKHandler(eventResult, ssrcInfo, mediaServerItem, device, channel,
                                callback, inviteInfo, InviteSessionType.DOWNLOAD);

                        // 注册录像回调事件，录像下载结束后写入下载地址
                        HookSubscribe.Event hookEventForRecord = (hookData) -> {
                            log.info("[录像下载] 收到录像写入磁盘消息： ， {}/{}-{}",
                                    inviteInfo.getDeviceId(), inviteInfo.getChannelId(), ssrcInfo.getStream());
                            log.info("[录像下载] 收到录像写入磁盘消息内容： " + hookData);
                            HookRecordInfo recordInfo = hookData.getRecordInfo();
                            String filePath = recordInfo.getFilePath();
                            DownloadFileInfo downloadFileInfo = CloudRecordUtils.getDownloadFilePath(mediaServerItem, filePath);
                            InviteInfo inviteInfoForNew = inviteStreamService.getInviteInfo(inviteInfo.getType()
                                    , inviteInfo.getChannelId(), inviteInfo.getStream());
                            if (inviteInfoForNew != null && inviteInfoForNew.getStreamDetail() != null) {
                                inviteInfoForNew.getStreamDetail().setDownLoadFilePath(downloadFileInfo);
                                // 不可以马上移除会导致后续接口拿不到下载地址
                                inviteStreamService.updateInviteInfo(inviteInfoForNew, 60 * 15L);
                            }
                        };
                        Hook hook = Hook.getInstance(HookType.on_record_mp4, "rtp", ssrcInfo.getStream());
                        // 设置过期时间，下载失败时自动处理订阅数据
                        hook.setExpireTime(System.currentTimeMillis() + 24 * 60 * 60 * 1000);
                        subscribe.addSubscribe(hook, hookEventForRecord);
                    }, userSetting.getPlayTimeout().longValue());
        } catch (InvalidArgumentException | SipException | ParseException e) {
            log.error("[命令发送失败] 录像下载: {}", e.getMessage());
            callback.run(InviteResultCode.FAIL.getCode(),e.getMessage(), null);
            receiveRtpServerService.closeRTPServer(mediaServerItem, ssrcInfo);
            sessionManager.removeByStream(ssrcInfo.getApp(), ssrcInfo.getStream());
            inviteStreamService.removeInviteInfo(inviteInfo);
        }
    }

    @Override
    public StreamDetail getDownLoadInfo(MediaDevice device, MediaDeviceChannel channel, String stream) {

        InviteInfo inviteInfo = inviteStreamService.getInviteInfo(InviteSessionType.DOWNLOAD, channel.getId(), stream);
        if (inviteInfo == null) {
            String app = "rtp";
            StreamAuthorityInfo streamAuthorityInfo = RedisUtils.getHashKey(
                    MediaCacheConstants.MEDIA_STREAM_AUTHORITY + serverInstance.getInstanceId(),
                    app+ "_" + stream,
                    StreamAuthorityInfo.class);
            if (streamAuthorityInfo != null) {
                CloudRecordDTO cloudRecordDTO = new CloudRecordDTO();
                cloudRecordDTO.setCallId(streamAuthorityInfo.getCallId());
                cloudRecordDTO.setApp(app);
                cloudRecordDTO.setStream(stream);
                List<CloudRecord> allList = cloudRecordService.getAllList(cloudRecordDTO);
                if (allList.isEmpty()) {
                    log.warn("[获取下载进度] 未查询到录像下载的信息 {}/{}-{}", device.getGbId(), channel.getGbId(), stream);
                    return null;
                }
                String filePath = allList.get(0).getFilePath();
                if (filePath == null) {
                    log.warn("[获取下载进度] 未查询到录像下载的文件路径 {}/{}-{}", device.getGbId(), channel.getGbId(), stream);
                    return null;
                }
                String mediaServerId = allList.get(0).getMediaServerId();
                MediaServer mediaServer = mediaServerService.getServerByServerId(mediaServerId);
                if (mediaServer == null) {
                    log.warn("[获取下载进度] 未查询到录像下载的节点信息 {}/{}-{}", device.getGbId(), channel.getGbId(), stream);
                    return null;
                }
                log.warn("[获取下载进度] 发现下载已经结束，直接从数据库获取到文件 {}/{}-{}", device.getGbId(), channel.getGbId(), stream);
                DownloadFileInfo downloadFileInfo = CloudRecordUtils.getDownloadFilePath(mediaServer, filePath);
                StreamDetail streamInfo = new StreamDetail();
                streamInfo.setDownLoadFilePath(downloadFileInfo);
                streamInfo.setApp(app);
                streamInfo.setStream(stream);
                streamInfo.setServerId(mediaServerId);
                streamInfo.setProgress(1.0);
                return streamInfo;
            }
        }

        if (inviteInfo == null || inviteInfo.getStreamDetail() == null) {
            log.warn("[获取下载进度] 未查询到录像下载的信息 {}/{}-{}", device.getGbId(), channel.getGbId(), stream);
            return null;
        }

        if (inviteInfo.getStreamDetail().getProgress() == 1) {
            return inviteInfo.getStreamDetail();
        }

        // 获取当前已下载时长
        MediaServer mediaServerItem = inviteInfo.getStreamDetail().getMediaServer();
        if (mediaServerItem == null) {
            log.warn("[获取下载进度] 查询录像信息时发现节点不存在");
            return null;
        }
        String app = "rtp";
        Long duration  = mediaServerService.updateDownloadProcess(mediaServerItem, app, stream);
        if (duration == null || duration == 0) {
            inviteInfo.getStreamDetail().setProgress(0);
        } else {
            String startTime = inviteInfo.getStreamDetail().getStartTime();
            String endTime = inviteInfo.getStreamDetail().getEndTime();
            // 此时start和end单位是秒
            long start = DateUtil.yyyy_MM_dd_HH_mm_ssToTimestamp(startTime);
            long end = DateUtil.yyyy_MM_dd_HH_mm_ssToTimestamp(endTime);

            BigDecimal currentCount = new BigDecimal(duration);
            BigDecimal totalCount = new BigDecimal((end - start) * 1000);
            BigDecimal divide = currentCount.divide(totalCount, 2, RoundingMode.HALF_UP);
            double process = divide.doubleValue();
            if (process > 0.999) {
                process = 1.0;
            }
            inviteInfo.getStreamDetail().setProgress(process);
        }
        inviteStreamService.updateInviteInfo(inviteInfo);
        return inviteInfo.getStreamDetail();
    }

    @Override
    public void zlmServerOnline(MediaServer mediaServer) {
        // 获取缓存的点播信息
        List<InviteInfo> inviteInfoList = inviteStreamService.getAllInviteInfo();
        if (inviteInfoList.isEmpty()) {
            return;
        }

        // 获取zlm开启的RTP Server
        List<String> rtpServerList = mediaServerService.listRtpServer(mediaServer);
        if (rtpServerList.isEmpty()) {
            return;
        }
        for (InviteInfo inviteInfo : inviteInfoList) {
            // 如果当前流没有在RTP Server中，则移除
            if (!rtpServerList.contains(inviteInfo.getStream())){
                inviteStreamService.removeInviteInfo(inviteInfo);
            }
        }
    }

    @Override
    public void zlmServerOffline(MediaServer mediaServer) {
        // 处理正在观看的国标设备
        List<SsrcTransaction> allSsrc = sessionManager.getAll();
        if (!CollectionUtils.isEmpty(allSsrc)) {
            for (SsrcTransaction ssrcTransaction : allSsrc) {
                if (ssrcTransaction.getMediaServerId().equals(mediaServer.getServerId())) {
                    MediaDevice device = deviceService.getByGbId(ssrcTransaction.getDeviceId());
                    if (device == null) {
                        continue;
                    }
                    MediaDeviceChannel deviceChannel = deviceChannelService.getById(ssrcTransaction.getChannelId());
                    if (deviceChannel == null) {
                        continue;
                    }
                    try {
                        cmder.streamByeCmd(device, deviceChannel.getGbId(), ssrcTransaction.getApp(),
                                ssrcTransaction.getStream(), null, null);
                    } catch (InvalidArgumentException | ParseException | SipException |
                             SsrcTransactionNotFoundException e) {
                        log.error("[zlm离线]为正在使用此zlm的设备， 发送BYE失败 {}", e.getMessage());
                    }
                }
            }
        }
    }

    private StreamDetail onPublishHandlerForDownload(MediaServer mediaServerItemInuse, MediaInfo mediaInfo,
                                     MediaDevice device, MediaDeviceChannel channel, String startTime, String endTime) {

        StreamDetail streamInfo = onPublishHandler(mediaServerItemInuse, mediaInfo, device, channel);
        if (streamInfo != null) {
            streamInfo.setProgress(0);
            streamInfo.setStartTime(startTime);
            streamInfo.setEndTime(endTime);
            InviteInfo inviteInfo = inviteStreamService.getInviteInfo(InviteSessionType.DOWNLOAD, channel.getId(), streamInfo.getStream());
            if (inviteInfo != null) {
                log.info("[录像下载] 更新invite消息中的stream信息");
                inviteInfo.setStatus(InviteSessionStatus.ok);
                inviteInfo.setStreamDetail(streamInfo);
                inviteStreamService.updateInviteInfo(inviteInfo);
            }
        }
        return streamInfo;
    }

    @Override
    public StreamDetail onPublishHandlerForPlay(MediaServer mediaServerItem, MediaInfo mediaInfo, MediaDevice device, MediaDeviceChannel channel) {
        StreamDetail streamDetail = null;
        streamDetail = onPublishHandler(mediaServerItem, mediaInfo, device, channel);
        if (streamDetail != null) {
            deviceChannelService.startPlay(channel.getId(), streamDetail.getStream());
            InviteInfo inviteInfo = inviteStreamService.getInviteInfoByDeviceAndChannel(InviteSessionType.PLAY, channel.getId());
            if (inviteInfo != null) {
                inviteInfo.setStatus(InviteSessionStatus.ok);
                inviteInfo.setStreamDetail(streamDetail);
                inviteStreamService.updateInviteInfo(inviteInfo);
            }
        }
        return streamDetail;
    }

    public StreamDetail onPublishHandler(MediaServer mediaServerItem, MediaInfo mediaInfo, MediaDevice device,
                                         MediaDeviceChannel channel) {

        StreamDetail streamInfo = mediaServerService.getStreamInfoByAppAndStream(mediaServerItem, "rtp",
                mediaInfo.getStream(), mediaInfo, null);
        streamInfo.setGbId(device.getGbId());
        streamInfo.setChannelId(channel.getId());
        return streamInfo;
    }

    /**
     * 点播成功时调用截图.
     *
     * @param mediaServerItemInuse media
     * @param deviceId 设备 ID
     * @param channelId 通道 ID
     * @param stream ssrc
     */
    private void snapOnPlay(MediaServer mediaServerItemInuse, String deviceId, String channelId, String stream) {
        String streamUrl;
        if (mediaServerItemInuse.getRtspPort() != 0) {
            streamUrl = String.format("rtsp://127.0.0.1:%s/%s/%s", mediaServerItemInuse.getRtspPort(), "rtp", stream);
        } else {
            streamUrl = String.format("http://127.0.0.1:%s/%s/%s.live.mp4", mediaServerItemInuse.getHttpPort(), "rtp", stream);
        }
        String path = "snap";
        String fileName = deviceId + "_" + channelId + ".jpg";
        // 请求截图
        log.info("[请求截图]: " + fileName);
        mediaServerService.getSnap(mediaServerItemInuse, streamUrl, 15, 1, path, fileName);
    }

    @Override
    public MediaServer getAMediaServerItem(MediaDevice device) {
        if (device == null) {
            return null;
        }
        MediaServer mediaServerItem;
        if (ObjectUtils.isEmpty(device.getMediaServerId())) {
            // 未配置媒体服务，则从已注册的服务中选择一个 // TODO: 优化，目前只考虑单一流媒体服务器，后续有拓展再修改
            List<MediaServer> serverList = mediaServerService.list();
            mediaServerItem = serverList.get(0);
        } else {
            // 配置了媒体服务，直接返回
            mediaServerItem = mediaServerService.getById(device.getMediaServerId());
        }
        if (mediaServerItem == null) {
            log.warn("点播时未找到可使用的ZLM...");
        }
        return mediaServerItem;
    }

    @Override
    public void stop(InviteSessionType type, MediaDevice device, MediaDeviceChannel channel, String stream) {
        InviteInfo inviteInfo = inviteStreamService.getInviteInfo(type, channel.getId(), stream);
        if (inviteInfo == null) {
            if (type == InviteSessionType.PLAY) {
                deviceChannelService.stopPlay(channel.getId());
            }
            return;
        }
        inviteStreamService.removeInviteInfo(inviteInfo);
        if (InviteSessionStatus.ok == inviteInfo.getStatus()) {
            try {
                log.info("[停止点播/回放/下载] {}/{}", device.getGbId(), channel.getGbId());
                cmder.streamByeCmd(device, channel.getGbId(), "rtp", inviteInfo.getStream(), null, null);
            } catch (InvalidArgumentException | SipException | ParseException | SsrcTransactionNotFoundException e) {
                log.error("[命令发送失败] 停止点播/回放/下载， 发送BYE: {}", e.getMessage());
                throw new RuntimeException("命令发送失败: " + e.getMessage());
            }
        }

        if (inviteInfo.getType() == InviteSessionType.PLAY) {
            deviceChannelService.stopPlay(channel.getId());
        }
        if (inviteInfo.getStreamDetail() != null) {
            receiveRtpServerService.closeRTPServer(inviteInfo.getStreamDetail().getMediaServer(), inviteInfo.getSsrcInfo());
        }
    }


    @Override
    public void getSnap(String deviceGbId, String channelGbId, String fileName, ErrorCallback errorCallback) {
        MediaDevice device = deviceService.getByGbId(deviceGbId);
        Assert.notNull(device, "设备不存在");
        MediaDeviceChannel channel = deviceChannelService.getByGbId(deviceGbId, channelGbId);
        Assert.notNull(channel, "通道不存在");
        InviteInfo inviteInfo = inviteStreamService.getInviteInfoByDeviceAndChannel(InviteSessionType.PLAY, channel.getId());
        if (inviteInfo != null) {
            if (inviteInfo.getStreamDetail() != null) {
                // 已存在线直接截图
                MediaServer mediaServerItemInuse = inviteInfo.getStreamDetail().getMediaServer();
                String streamUrl;
                if (mediaServerItemInuse.getRtspPort() != 0) {
                    streamUrl = String.format("rtsp://127.0.0.1:%s/%s/%s", mediaServerItemInuse.getRtspPort(), "rtp",  inviteInfo.getStreamDetail().getStream());
                } else {
                    streamUrl = String.format("http://127.0.0.1:%s/%s/%s.live.mp4", mediaServerItemInuse.getHttpPort(), "rtp",  inviteInfo.getStreamDetail().getStream());
                }
                String path = "snap";
                // 请求截图
                log.info("[请求截图]: " + fileName);
                mediaServerService.getSnap(mediaServerItemInuse, streamUrl, 15, 1, path, fileName);
                File snapFile = new File(path + File.separator + fileName);
                if (snapFile.exists()) {
                    errorCallback.run(InviteResultCode.SUCCESS.getCode(), InviteResultCode.SUCCESS.getMsg(), snapFile.getAbsoluteFile());
                } else {
                    errorCallback.run(InviteResultCode.FAIL.getCode(), InviteResultCode.FAIL.getMsg(), null);
                }
                return;
            }
        }

        MediaServer newMediaServerItem = getAMediaServerItem(device);
        play(newMediaServerItem, device, channel, null, userSetting.getRecordSip(), (code, msg, data)->{
            if (code == InviteResultCode.SUCCESS.getCode()) {
                InviteInfo inviteInfoForPlay = inviteStreamService.getInviteInfoByDeviceAndChannel(InviteSessionType.PLAY, channel.getId());
                if (inviteInfoForPlay != null && inviteInfoForPlay.getStreamDetail() != null) {
                    getSnap(deviceGbId, channelGbId, fileName, errorCallback);
                } else {
                    errorCallback.run(InviteResultCode.FAIL.getCode(), InviteResultCode.FAIL.getMsg(), null);
                }
            } else {
                errorCallback.run(InviteResultCode.FAIL.getCode(), InviteResultCode.FAIL.getMsg(), null);
            }
        });
    }

    @Override
    public void pauseRtp(String streamId) throws InvalidArgumentException, ParseException, SipException {

        InviteInfo inviteInfo = inviteStreamService.getInviteInfoByStream(InviteSessionType.PLAYBACK, streamId);
        if (null == inviteInfo || inviteInfo.getStreamDetail() == null) {
            throw new CheckedException("streamId不存在");
        }
        MediaDevice device = deviceService.getByGbId(inviteInfo.getDeviceId());
        if (device == null) {
            throw new CheckedException("设备不存在");
        }

        inviteInfo.getStreamDetail().setPause(true);
        inviteStreamService.updateInviteInfo(inviteInfo);
        MediaServer mediaServerItem = inviteInfo.getStreamDetail().getMediaServer();
        if (null == mediaServerItem) {
            log.warn("mediaServer 不存在!");
            throw new CheckedException("mediaServer不存在");
        }
        // zlm 暂停RTP超时检查
        // 使用zlm中的流ID
        String streamKey = inviteInfo.getStream();
        if (!mediaServerItem.isRtpEnable()) {
            streamKey = Long.toHexString(Long.parseLong(inviteInfo.getSsrcInfo().getSsrc())).toUpperCase();
        }
        Boolean result = mediaServerService.pauseRtpCheck(mediaServerItem, streamKey);
        if (!result) {
            throw new CheckedException("暂停RTP接收失败");
        }

        MediaDeviceChannel channel = deviceChannelService.getById(inviteInfo.getChannelId());
        cmder.playPauseCmd(device, channel, inviteInfo.getStreamDetail());
    }

    @Override
    public void resumeRtp(String streamId) throws InvalidArgumentException, ParseException, SipException {
        InviteInfo inviteInfo = inviteStreamService.getInviteInfoByStream(InviteSessionType.PLAYBACK, streamId);
        if (null == inviteInfo || inviteInfo.getStreamDetail() == null) {
            throw new CheckedException("streamId不存在");
        }
        MediaDevice device = deviceService.getByGbId(inviteInfo.getDeviceId());
        if (device == null) {
            throw new CheckedException("设备不存在");
        }

        inviteInfo.getStreamDetail().setPause(false);
        inviteStreamService.updateInviteInfo(inviteInfo);
        MediaServer mediaServerItem = inviteInfo.getStreamDetail().getMediaServer();
        if (null == mediaServerItem) {
            log.warn("mediaServer 不存在!");
            throw new CheckedException("mediaServer不存在");
        }
        // 使用zlm中的流ID
        String streamKey = inviteInfo.getStream();
        if (!mediaServerItem.isRtpEnable()) {
            streamKey = Long.toHexString(Long.parseLong(inviteInfo.getSsrcInfo().getSsrc())).toUpperCase();
        }
        boolean result = mediaServerService.resumeRtpCheck(mediaServerItem, streamKey);
        if (!result) {
            throw new CheckedException("继续RTP接收失败");
        }
        MediaDeviceChannel channel = deviceChannelService.getById(inviteInfo.getChannelId());
        cmder.playResumeCmd(device, channel, inviteInfo.getStreamDetail());
    }

    @Override
    public void playBack(MediaDevice device, MediaDeviceChannel channel, String startTime,
                         String endTime, ErrorCallback<StreamDetail> callback) {

        MediaServer newMediaServerItem = getAMediaServerItem(device);
        Assert.notNull(newMediaServerItem, "未找到可用的媒体服务器");

        if (StreamModeType.TCP_ACTIVE.name().equalsIgnoreCase(device.getStreamMode()) && ! newMediaServerItem.isRtpEnable()) {
            log.warn("[录像回放] 单端口收流时不支持TCP主动方式收流 deviceId: {},channelId:{}", device.getGbId(), channel.getGbId());
            throw new CheckedException("单端口收流时不支持TCP主动方式收流");
        }

        playBack(newMediaServerItem, device, channel, startTime, endTime, callback);
    }

    private void playBack(MediaServer mediaServerItem,
                          MediaDevice device, MediaDeviceChannel channel, String startTime,
                          String endTime, ErrorCallback<StreamDetail> callback) {

        String startTimeStr = startTime.replace("-", "")
                .replace(":", "")
                .replace(" ", "");
        String endTimeTimeStr = endTime.replace("-", "")
                .replace(":", "")
                .replace(" ", "");

        String stream = device.getGbId() + "_" + channel.getGbId() + "_" + startTimeStr + "_" + endTimeTimeStr;
        int tcpMode = StreamModeType.valueOf(device.getStreamMode()).value();

        RTPServerParam rtpServerParam = new RTPServerParam();
        rtpServerParam.setMediaServerItem(mediaServerItem);
        rtpServerParam.setStreamId(stream);
        rtpServerParam.setSsrcCheck(device.getSsrcCheck());
        rtpServerParam.setPlayback(true);
        rtpServerParam.setPort(0);
        rtpServerParam.setTcpMode(tcpMode);
        rtpServerParam.setOnlyAuto(false);
        rtpServerParam.setDisableAudio(!channel.isHasAudio());
        SSRCInfo ssrcInfo = receiveRtpServerService.openRTPServer(rtpServerParam, (code, msg, result) -> {
            if (code == InviteResultCode.SUCCESS.getCode() && result != null && result.getHookData() != null) {
                // hook响应
                StreamDetail streamInfo = onPublishHandlerForPlayback(result.getHookData().getMediaServer(), result.getHookData().getMediaInfo(), device, channel, startTime, endTime);
                if (streamInfo == null) {
                    log.warn("设备回放API调用失败！");
                    callback.run(InviteResultCode.ERROR_FOR_STREAM_PARSING_EXCEPTIONS.getCode(),
                            InviteResultCode.ERROR_FOR_STREAM_PARSING_EXCEPTIONS.getMsg(), null);
                    return;
                }
                callback.run(InviteResultCode.SUCCESS.getCode(), InviteResultCode.SUCCESS.getMsg(), streamInfo);
                log.info("[录像回放] 成功 deviceId: {}, channelId: {},  开始时间: {}, 结束时间： {}", device.getGbId(), channel.getGbId(), startTime, endTime);
            } else {
                if (callback != null) {
                    callback.run(code, msg, null);
                }
                inviteStreamService.runCallback(InviteSessionType.PLAYBACK, channel.getId(), null, code, msg, null);
                inviteStreamService.removeInviteInfoByDeviceAndChannel(InviteSessionType.PLAYBACK, channel.getId());
                SsrcTransaction ssrcTransaction = sessionManager.getSsrcTransactionByStream("rtp", stream);
                if (ssrcTransaction != null) {
                    try {
                        cmder.streamByeCmd(device, channel.getGbId(), "rtp",  stream, null, null);
                    } catch (InvalidArgumentException | ParseException | SipException | SsrcTransactionNotFoundException e) {
                        log.error("[录像回放] 发送BYE失败 {}", e.getMessage());
                    } finally {
                        sessionManager.removeByStream("rtp", stream);
                    }
                }
            }
        });
        if (ssrcInfo == null || ssrcInfo.getPort() <= 0) {
            log.info("[回放端口/SSRC]获取失败，deviceId={},channelId={},ssrcInfo={}", device.getGbId(), channel.getGbId(), ssrcInfo);
            if (callback != null) {
                callback.run(InviteResultCode.ERROR_FOR_RESOURCE_EXHAUSTION.getCode(), "获取端口或者ssrc失败", null);
            }
            inviteStreamService.runCallback(InviteSessionType.PLAY, channel.getId(), null,
                    InviteResultCode.ERROR_FOR_RESOURCE_EXHAUSTION.getCode(),
                    InviteResultCode.ERROR_FOR_RESOURCE_EXHAUSTION.getMsg(),
                    null);
            return;
        }

        log.info("[录像回放] deviceId: {}, channelId: {}, 开始时间: {}, 结束时间： {}, 收流端口：{}, 收流模式：{}, SSRC: {}, SSRC校验：{}",
                device.getGbId(), channel.getGbId(), startTime, endTime, ssrcInfo.getPort(), device.getStreamMode(),
                ssrcInfo.getSsrc(), device.getSsrcCheck());
        // 初始化redis中的invite消息状态
        InviteInfo inviteInfo = InviteInfo.getInviteInfo(device.getGbId(), channel.getId(), ssrcInfo.getStream(), ssrcInfo, mediaServerItem.getServerId(),
                mediaServerItem.getSdpIp(), ssrcInfo.getPort(), device.getStreamMode(), InviteSessionType.PLAYBACK,
                InviteSessionStatus.ready, userSetting.getRecordSip());
        inviteStreamService.updateInviteInfo(inviteInfo);

        try {
            cmder.playbackStreamCmd(mediaServerItem, ssrcInfo, device, channel, startTime, endTime,
                    eventResult -> {
                        // 处理收到200ok后的TCP主动连接以及SSRC不一致的问题
                        inviteOKHandler(eventResult, ssrcInfo, mediaServerItem, device, channel,
                                callback, inviteInfo, InviteSessionType.PLAYBACK);
                    }, eventResult -> {
                        log.info("[录像回放] 失败，{} {}", eventResult.statusCode, eventResult.msg);
                        if (callback != null) {
                            callback.run(eventResult.statusCode, eventResult.msg, null);
                        }

                        receiveRtpServerService.closeRTPServer(mediaServerItem, ssrcInfo);
                        sessionManager.removeByStream(ssrcInfo.getApp(), ssrcInfo.getStream());
                        inviteStreamService.removeInviteInfo(inviteInfo);
                    }, userSetting.getPlayTimeout().longValue());
        } catch (InvalidArgumentException | SipException | ParseException e) {
            log.error("[命令发送失败] 录像回放: {}", e.getMessage());
            if (callback != null) {
                callback.run(InviteResultCode.FAIL.getCode(), e.getMessage(), null);
            }
            receiveRtpServerService.closeRTPServer(mediaServerItem, ssrcInfo);
            sessionManager.removeByStream(ssrcInfo.getApp(), ssrcInfo.getStream());
            inviteStreamService.removeInviteInfo(inviteInfo);
        }
    }

    private StreamDetail onPublishHandlerForPlayback(MediaServer mediaServerItem, MediaInfo mediaInfo, MediaDevice device,
                                                     MediaDeviceChannel channel, String startTime, String endTime) {
        StreamDetail streamInfo = onPublishHandler(mediaServerItem, mediaInfo, device, channel);
        if (streamInfo != null) {
            streamInfo.setStartTime(startTime);
            streamInfo.setEndTime(endTime);
            InviteInfo inviteInfo = inviteStreamService.getInviteInfoByStream(InviteSessionType.PLAYBACK, mediaInfo.getStream());
            if (inviteInfo != null) {
                inviteInfo.setStatus(InviteSessionStatus.ok);
                inviteInfo.setStreamDetail(streamInfo);
                inviteStreamService.updateInviteInfo(inviteInfo);
            }
        }
        return streamInfo;
    }


    private void tcpActiveHandler(MediaDevice device, MediaDeviceChannel channel, String contentString,
                                  MediaServer mediaServerItem, SSRCInfo ssrcInfo, ErrorCallback<StreamDetail> callback){
        if (!StreamModeType.TCP_ACTIVE.name().equalsIgnoreCase(device.getStreamMode())) {
            return;
        }

        String substring;
        if (contentString.indexOf("y=") > 0) {
            substring = contentString.substring(0, contentString.indexOf("y="));
        } else {
            substring = contentString;
        }
        try {
            SessionDescription sdp = SdpFactory.getInstance().createSessionDescription(substring);
            int port = -1;
            Vector mediaDescriptions = sdp.getMediaDescriptions(true);
            for (Object description : mediaDescriptions) {
                MediaDescription mediaDescription = (MediaDescription) description;
                Media media = mediaDescription.getMedia();

                Vector mediaFormats = media.getMediaFormats(false);
                if (mediaFormats.contains("96")) {
                    port = media.getMediaPort();
                    break;
                }
            }
            log.info("[TCP主动连接对方] deviceId: {}, channelId: {}, 连接对方的地址：{}:{}, 收流模式：{}, SSRC: {}, SSRC校验：{}", device.getGbId(), channel.getGbId(), sdp.getConnection().getAddress(), port, device.getStreamMode(), ssrcInfo.getSsrc(), device.getSsrcCheck());
            Boolean result = mediaServerService.connectRtpServer(mediaServerItem, sdp.getConnection().getAddress(), port, ssrcInfo.getStream());
            log.info("[TCP主动连接对方] 结果： {}" , result);
            if (!result) {
                // 主动连接失败，结束流程， 清理数据
                receiveRtpServerService.closeRTPServer(mediaServerItem, ssrcInfo);
                sessionManager.removeByStream(ssrcInfo.getApp(), ssrcInfo.getStream());
                callback.run(InviteResultCode.ERROR_FOR_TCP_ACTIVE_CONNECTION_REFUSED_ERROR.getCode(),
                        InviteResultCode.ERROR_FOR_TCP_ACTIVE_CONNECTION_REFUSED_ERROR.getMsg(), null);
                inviteStreamService.runCallback(InviteSessionType.BROADCAST, channel.getId(), null,
                        InviteResultCode.ERROR_FOR_TCP_ACTIVE_CONNECTION_REFUSED_ERROR.getCode(),
                        InviteResultCode.ERROR_FOR_TCP_ACTIVE_CONNECTION_REFUSED_ERROR.getMsg(), null);
            }
        } catch (SdpException e) {
            log.error("[TCP主动连接对方] deviceId: {}, channelId: {}, 解析200OK的SDP信息失败", device.getGbId(), channel.getGbId(), e);
            receiveRtpServerService.closeRTPServer(mediaServerItem, ssrcInfo);

            sessionManager.removeByStream(ssrcInfo.getApp(), ssrcInfo.getStream());

            callback.run(InviteResultCode.ERROR_FOR_SDP_PARSING_EXCEPTIONS.getCode(),
                    InviteResultCode.ERROR_FOR_SDP_PARSING_EXCEPTIONS.getMsg(), null);
            inviteStreamService.runCallback(InviteSessionType.BROADCAST, channel.getId(), null,
                    InviteResultCode.ERROR_FOR_SDP_PARSING_EXCEPTIONS.getCode(),
                    InviteResultCode.ERROR_FOR_SDP_PARSING_EXCEPTIONS.getMsg(), null);
        }
    }

    private void inviteOKHandler(SipSubscribe.EventResult eventResult, SSRCInfo ssrcInfo, MediaServer mediaServerItem,
                                 MediaDevice device, MediaDeviceChannel channel, ErrorCallback<StreamDetail> callback,
                                 InviteInfo inviteInfo, InviteSessionType inviteSessionType){
        inviteInfo.setStatus(InviteSessionStatus.ok);
        ResponseEvent responseEvent = (ResponseEvent) eventResult.event;
        String contentString = new String(responseEvent.getResponse().getRawContent());
        String ssrcInResponse = SipUtils.getSsrcFromSdp(contentString);
        // 兼容回复的消息中缺少ssrc(y字段)的情况
        if (ssrcInResponse == null) {
            ssrcInResponse = ssrcInfo.getSsrc();
        }
        if (ssrcInfo.getSsrc().equals(ssrcInResponse)) {
            // ssrc 一致
            if (mediaServerItem.isRtpEnable()) {
                // 多端口
                if (StreamModeType.TCP_ACTIVE.name().equalsIgnoreCase(device.getStreamMode())) {
                    tcpActiveHandler(device, channel, contentString, mediaServerItem, ssrcInfo, callback);
                }
            } else {
                // 单端口
                if (StreamModeType.TCP_ACTIVE.name().equalsIgnoreCase(device.getStreamMode())) {
                    log.warn("[Invite 200OK] 单端口收流模式不支持tcp主动模式收流");
                }

            }
        } else {
            log.info("[Invite 200OK] 收到invite 200, 发现下级自定义了ssrc: {}", ssrcInResponse);
            // ssrc 不一致
            if (mediaServerItem.isRtpEnable()) {
                // 多端口
                if (device.getSsrcCheck()) {
                    // ssrc检验
                    // 更新ssrc
                    log.info("[Invite 200OK] SSRC修正 {}->{}", ssrcInfo.getSsrc(), ssrcInResponse);
                    // 释放ssrc
                    mediaServerService.releaseSsrc(mediaServerItem.getServerId(), ssrcInfo.getSsrc());
                    Boolean result = mediaServerService.updateRtpServerSSRC(mediaServerItem, ssrcInfo.getStream(), ssrcInResponse);
                    if (!result) {
                        try {
                            log.warn("[Invite 200OK] 更新ssrc失败，停止点播 {}/{}", device.getGbId(), channel.getGbId());
                            cmder.streamByeCmd(device, channel.getGbId(), ssrcInfo.getApp(), ssrcInfo.getStream(), null, null);
                        } catch (InvalidArgumentException | SipException | ParseException | SsrcTransactionNotFoundException e) {
                            log.error("[命令发送失败] 停止播放， 发送BYE: {}", e.getMessage());
                        }

                        // 释放ssrc
                        mediaServerService.releaseSsrc(mediaServerItem.getServerId(), ssrcInfo.getSsrc());

                        sessionManager.removeByStream(ssrcInfo.getApp(), ssrcInfo.getStream());

                        callback.run(InviteResultCode.ERROR_FOR_RESET_SSRC.getCode(),
                                "下级自定义了ssrc,重新设置收流信息失败", null);
                        inviteStreamService.runCallback(inviteSessionType, channel.getId(), null,
                                InviteResultCode.ERROR_FOR_RESET_SSRC.getCode(),
                                "下级自定义了ssrc,重新设置收流信息失败", null);

                    } else {
                        ssrcInfo.setSsrc(ssrcInResponse);
                        inviteInfo.setSsrcInfo(ssrcInfo);
                        inviteInfo.setStream(ssrcInfo.getStream());
                        if (StreamModeType.TCP_ACTIVE.name().equalsIgnoreCase(device.getStreamMode())) {
                            if (mediaServerItem.isRtpEnable()) {
                                tcpActiveHandler(device, channel, contentString, mediaServerItem,  ssrcInfo, callback);
                            } else {
                                log.warn("[Invite 200OK] 单端口收流模式不支持tcp主动模式收流");
                            }
                        }
                        inviteStreamService.updateInviteInfo(inviteInfo);
                    }
                }
            } else {
                if (ssrcInResponse != null) {
                    // 单端口
                    // 重新订阅流上线
                    SsrcTransaction ssrcTransaction = sessionManager.getSsrcTransactionByStream("rtp", inviteInfo.getStream());
                    sessionManager.removeByStream("rtp", inviteInfo.getStream());
                    inviteStreamService.updateInviteInfoForSSRC(inviteInfo, ssrcInResponse);
                    ssrcTransaction.setDeviceId(device.getGbId());
                    ssrcTransaction.setChannelId(ssrcTransaction.getChannelId());
                    ssrcTransaction.setCallId(ssrcTransaction.getCallId());
                    ssrcTransaction.setSsrc(ssrcInResponse);
                    ssrcTransaction.setApp("rtp");
                    ssrcTransaction.setStream(inviteInfo.getStream());
                    ssrcTransaction.setMediaServerId(mediaServerItem.getServerId());
                    ssrcTransaction.setSipTransactionInfo(new SipTransactionInfo((SIPResponse) responseEvent.getResponse()));
                    ssrcTransaction.setType(inviteSessionType);

                    sessionManager.put(ssrcTransaction);
                }
            }
        }
    }

    @Override
    public AudioBroadcastResultVO audioBroadcast(String deviceGbId, String channelGbId, Boolean broadcastMode) {

        MediaDevice device = deviceService.getByGbId(deviceGbId);
        Assert.notNull(device, "设备不存在");

        MediaDeviceChannel deviceChannel = deviceChannelService.getChannelByGbId(device.getId(), channelGbId);
        Assert.notNull(deviceChannel, "通道不存在");

        log.info("[语音喊话] device： {}, channel: {}", device.getGbId(), deviceChannel.getGbId());
        MediaServer mediaServerItem = getAMediaServerItem(device);
        if (broadcastMode == null) {
            broadcastMode = true;
        }
        String app = broadcastMode ? "broadcast" : "talk";
        String stream = device.getGbId() + "_" + deviceChannel.getGbId();
        AudioBroadcastResultVO audioBroadcastResult = new AudioBroadcastResultVO();
        audioBroadcastResult.setApp(app);
        audioBroadcastResult.setStream(stream);
        audioBroadcastResult.setStreamInfo(mediaServerService.getStreamInfoByAppAndStream(mediaServerItem, app, stream, null, null, null, false).getStreamContent());
        audioBroadcastResult.setCodec("G.711");
        return audioBroadcastResult;
    }


    @Override
    public boolean audioBroadcastCmd(MediaDevice device, MediaDeviceChannel deviceChannel, MediaServer mediaServerItem,
                             String app, String stream, int timeout, boolean isFromPlatform, AudioBroadcastEvent event)
            throws InvalidArgumentException, ParseException, SipException {

        Assert.notNull(device, "设备不存在");
        Assert.notNull(deviceChannel, "通道不存在");
        log.info("[语音喊话] device： {}, channel: {}", device.getGbId(), deviceChannel.getGbId());
        // 查询通道使用状态
        if (audioBroadcastManager.exit(deviceChannel.getId())) {
            SendRtpInfo sendRtpInfo = sendRtpServerService.queryByChannelId(deviceChannel.getId(), device.getGbId());
            if (sendRtpInfo != null && sendRtpInfo.isOnlyAudio()) {
                // 查询流是否存在，不存在则认为是异常状态
                Boolean streamReady = mediaServerService.isStreamReady(mediaServerItem, sendRtpInfo.getApp(), sendRtpInfo.getStream());
                if (streamReady) {
                    log.warn("语音广播已经开启： {}", deviceChannel.getGbId());
                    event.call("语音广播已经开启");
                    return false;
                } else {
                    stopAudioBroadcast(device, deviceChannel);
                }
            }
        }

        // 发送通知
        cmder.audioBroadcastCmd(device, deviceChannel.getGbId(), eventResultForOk -> {
            // 发送成功
            AudioBroadcastCache audioBroadcastCache = new AudioBroadcastCache(device.getGbId(), deviceChannel.getId(), mediaServerItem, app, stream, event, AudioBroadcastCatchStatus.Ready, isFromPlatform);
            audioBroadcastManager.update(audioBroadcastCache);
            // 等待invite消息， 超时则结束
            String key = MediaConstants.BROADCAST_WAITE_INVITE +  device.getGbId();
            if (!SipUtils.isFrontEnd(device.getGbId())) {
                key += audioBroadcastCache.getChannelId();
            }
            dynamicTask.startDelay(key, ()->{
                log.info("[语音广播]等待invite消息超时：{}/{}", device.getGbId(), deviceChannel.getGbId());
                stopAudioBroadcast(device, deviceChannel);
            }, 10 * 1000);
        }, eventResultForError -> {
            // 发送失败
            log.error("语音广播发送失败： {}:{}", deviceChannel.getGbId(), eventResultForError.msg);
            event.call("语音广播发送失败");
            stopAudioBroadcast(device, deviceChannel);
        });
        return true;
    }

    @Override
    public void stopAudioBroadcast(MediaDevice device, MediaDeviceChannel channel) {
        log.info("[停止对讲] 设备：{}, 通道：{}", device.getGbId(), channel.getGbId());
        List<AudioBroadcastCache> audioBroadcastCatchList = new ArrayList<>();
        if (channel == null) {
            audioBroadcastCatchList.addAll(audioBroadcastManager.getByDeviceId(device.getGbId()));
        } else {
            audioBroadcastCatchList.addAll(audioBroadcastManager.getByDeviceId(device.getGbId()));
        }
        if (!audioBroadcastCatchList.isEmpty()) {
            for (AudioBroadcastCache audioBroadcastCatch : audioBroadcastCatchList) {
                if (audioBroadcastCatch == null) {
                    continue;
                }
                SendRtpInfo sendRtpInfo = sendRtpServerService.queryByChannelId(channel.getId(), device.getGbId());
                if (sendRtpInfo != null) {
                    sendRtpServerService.delete(sendRtpInfo);
                    MediaServer mediaServer = mediaServerService.getServerByServerId(sendRtpInfo.getMediaServerId());
                    mediaServerService.stopSendRtp(mediaServer, sendRtpInfo.getApp(), sendRtpInfo.getStream(), null);
                    try {
                        cmder.streamByeCmdForDeviceInvite(device, channel.getGbId(), audioBroadcastCatch.getSipTransactionInfo(), null);
                    } catch (InvalidArgumentException | ParseException | SipException |
                             SsrcTransactionNotFoundException e) {
                        log.error("[消息发送失败] 发送语音喊话BYE失败");
                    }
                }

                audioBroadcastManager.del(channel.getId());
            }
        }
    }


    @Override
    public void talkCmd(MediaDevice device, MediaDeviceChannel channel, MediaServer mediaServerItem, String stream, AudioBroadcastEvent event) {
        if (device == null || channel == null) {
            return;
        }
        // TODO 必须多端口模式才支持语音喊话和语音对讲
        log.info("[语音对讲] device： {}, channel: {}", device.getGbId(), channel.getGbId());
        // 查询通道使用状态
        if (audioBroadcastManager.exit(channel.getId())) {
            SendRtpInfo sendRtpInfo = sendRtpServerService.queryByChannelId(channel.getId(), device.getGbId());
            if (sendRtpInfo != null && sendRtpInfo.isOnlyAudio()) {
                // 查询流是否存在，不存在则认为是异常状态
                MediaServer mediaServer = mediaServerService.getServerByServerId(sendRtpInfo.getMediaServerId());
                Boolean streamReady = mediaServerService.isStreamReady(mediaServer, sendRtpInfo.getApp(), sendRtpInfo.getStream());
                if (streamReady) {
                    log.warn("[语音对讲] 正在语音广播，无法开启语音通话： {}", channel.getGbId());
                    event.call("正在语音广播");
                    return;
                } else {
                    stopAudioBroadcast(device, channel);
                }
            }
        }

        SendRtpInfo sendRtpInfo = sendRtpServerService.queryByChannelId(channel.getId(), device.getGbId());
        if (sendRtpInfo != null) {
            MediaServer mediaServer = mediaServerService.getServerByServerId(sendRtpInfo.getMediaServerId());
            Boolean streamReady = mediaServerService.isStreamReady(mediaServer, "rtp", sendRtpInfo.getReceiveStream());
            if (streamReady) {
                log.warn("[语音对讲] 进行中： {}", channel.getGbId());
                event.call("语音对讲进行中");
                return;
            } else {
                stopTalk(device, channel);
            }
        }

        talk(mediaServerItem, device, channel, stream, (hookData) -> {
            log.info("[语音对讲] 收到设备发来的流");
        }, eventResult -> {
            log.warn("[语音对讲] 失败，{}/{}, 错误码 {} {}", device.getGbId(), channel.getGbId(), eventResult.statusCode, eventResult.msg);
            event.call("失败，错误码 " + eventResult.statusCode + ", " + eventResult.msg);
        }, () -> {
            log.warn("[语音对讲] 失败，{}/{} 超时", device.getGbId(), channel.getGbId());
            event.call("失败，超时 ");
            stopTalk(device, channel);
        }, errorMsg -> {
            log.warn("[语音对讲] 失败，{}/{} {}", device.getGbId(), channel.getGbId(), errorMsg);
            event.call(errorMsg);
            stopTalk(device, channel);
        });
    }

    private void stopTalk(MediaDevice device, MediaDeviceChannel channel) {
        stopTalk(device, channel, null);
    }

    @Override
    public void stopTalk(MediaDevice device, MediaDeviceChannel channel, Boolean streamIsReady) {
        log.info("[语音对讲] 停止， {}/{}", device.getGbId(), channel.getGbId());
        SendRtpInfo sendRtpInfo = sendRtpServerService.queryByChannelId(channel.getId(), device.getGbId());
        if (sendRtpInfo == null) {
            log.info("[语音对讲] 停止失败， 未找到发送信息，可能已经停止");
            return;
        }
        // 停止向设备推流
        String mediaServerId = sendRtpInfo.getMediaServerId();
        if (mediaServerId == null) {
            return;
        }

        MediaServer mediaServer = mediaServerService.getServerByServerId(mediaServerId);

        if (streamIsReady == null || streamIsReady) {
            mediaServerService.stopSendRtp(mediaServer, sendRtpInfo.getApp(), sendRtpInfo.getStream(), sendRtpInfo.getSsrc());
        }

        ssrcFactory.releaseSsrc(mediaServerId, sendRtpInfo.getSsrc());

        SsrcTransaction ssrcTransaction = sessionManager.getSsrcTransactionByStream(sendRtpInfo.getApp(), sendRtpInfo.getStream());
        if (ssrcTransaction != null) {
            try {
                cmder.streamByeCmd(device, channel.getGbId(), sendRtpInfo.getApp(), sendRtpInfo.getStream(), null, null);
            } catch (InvalidArgumentException | ParseException | SipException | SsrcTransactionNotFoundException  e) {
                log.info("[语音对讲] 停止消息发送失败，可能已经停止");
            }
        }
        sendRtpServerService.deleteByChannel(channel.getId(), device.getGbId());
    }

    private void talk(MediaServer mediaServerItem, MediaDevice device, MediaDeviceChannel channel, String stream,
                      HookSubscribe.Event hookEvent, SipSubscribe.Event errorEvent,
                      Runnable timeoutCallback, AudioBroadcastEvent audioEvent) {

        String playSsrc = ssrcFactory.getPlaySsrc(mediaServerItem.getServerId());

        if (playSsrc == null) {
            audioEvent.call("ssrc已经用尽");
            return;
        }
        SendRtpInfo sendRtpInfo;
        try {
            sendRtpInfo = sendRtpServerService.createSendRtpInfo(mediaServerItem, null, null, playSsrc, device.getGbId(), "talk", stream,
                    channel.getId(), true, false);
            sendRtpInfo.setPlayType(InviteStreamType.TALK);
        } catch (Exception e) {
            log.info("[语音对讲]开始 获取发流端口失败 deviceId: {}, channelId: {},", device.getGbId(), channel.getGbId());
            return;
        }

        sendRtpInfo.setOnlyAudio(true);
        sendRtpInfo.setPt(8);
        sendRtpInfo.setStatus(1);
        sendRtpInfo.setTcpActive(false);
        sendRtpInfo.setUsePs(false);
        sendRtpInfo.setReceiveStream(stream + "_talk");

        String callId = SipUtils.getNewCallId();
        log.info("[语音对讲]开始 deviceId: {}, channelId: {},收流端口： {}, 收流模式：{}, SSRC: {}, SSRC校验：{}", device.getGbId(), channel.getGbId(), sendRtpInfo.getLocalPort(), device.getStreamMode(), sendRtpInfo.getSsrc(), false);
        // 超时处理
        String timeOutTaskKey = UUID.randomUUID().toString();
        dynamicTask.startDelay(timeOutTaskKey, () -> {

            log.info("[语音对讲] 收流超时 deviceId: {}, channelId: {}，端口：{}, SSRC: {}", device.getGbId(), channel.getGbId(), sendRtpInfo.getPort(), sendRtpInfo.getSsrc());
            timeoutCallback.run();
            // 点播超时回复BYE 同时释放ssrc以及此次点播的资源
            try {
                cmder.streamByeCmd(device, channel.getGbId(), null,  null, callId, null);
            } catch (InvalidArgumentException | ParseException | SipException | SsrcTransactionNotFoundException e) {
                log.error("[语音对讲]超时， 发送BYE失败 {}", e.getMessage());
            } finally {
                timeoutCallback.run();
                mediaServerService.releaseSsrc(mediaServerItem.getServerId(), sendRtpInfo.getSsrc());
                sessionManager.removeByStream(sendRtpInfo.getApp(), sendRtpInfo.getStream());
            }
        }, userSetting.getPlayTimeout());

        try {
            Integer localPort = mediaServerService.startSendRtpPassive(mediaServerItem, sendRtpInfo, userSetting.getPlayTimeout() * 1000);
            if (localPort == null || localPort <= 0) {
                timeoutCallback.run();
                mediaServerService.releaseSsrc(mediaServerItem.getServerId(), sendRtpInfo.getSsrc());
                sessionManager.removeByStream(sendRtpInfo.getApp(), sendRtpInfo.getStream());
                return;
            }
            sendRtpInfo.setPort(localPort);
        } catch (Exception e) {
            mediaServerService.releaseSsrc(mediaServerItem.getServerId(), sendRtpInfo.getSsrc());
            log.info("[语音对讲]失败 deviceId: {}, channelId: {}", device.getGbId(), channel.getGbId());
            audioEvent.call("失败, " + e.getMessage());
            // 查看是否已经建立了通道，存在则发送bye
            stopTalk(device, channel);
        }


        // 查看设备是否已经在推流
        try {
            cmder.talkStreamCmd(mediaServerItem, sendRtpInfo, device, channel, callId, (hookData) -> {
                log.info("[语音对讲] 流已生成， 开始推流： " + hookData);
                dynamicTask.stop(timeOutTaskKey);
                // TODO 暂不做处理
            }, (hookData) -> {
                log.info("[语音对讲] 设备开始推流： " + hookData);
                dynamicTask.stop(timeOutTaskKey);

            }, (event) -> {
                dynamicTask.stop(timeOutTaskKey);

                if (event.event instanceof ResponseEvent) {
                    ResponseEvent responseEvent = (ResponseEvent) event.event;
                    if (responseEvent.getResponse() instanceof SIPResponse) {
                        SIPResponse response = (SIPResponse) responseEvent.getResponse();
                        sendRtpInfo.setFromTag(response.getFromTag());
                        sendRtpInfo.setToTag(response.getToTag());
                        sendRtpInfo.setCallId(response.getCallIdHeader().getCallId());
                        sendRtpServerService.update(sendRtpInfo);

                        SsrcTransaction ssrcTransaction = SsrcTransaction.buildForDevice(device.getGbId(), sendRtpInfo.getChannelId(), response.getCallIdHeader().getCallId(), sendRtpInfo.getApp(),
                                sendRtpInfo.getStream(), sendRtpInfo.getSsrc(), sendRtpInfo.getMediaServerId(),
                                response, InviteSessionType.TALK);

                        sessionManager.put(ssrcTransaction);
                    } else {
                        log.error("[语音对讲]收到的消息错误，response不是SIPResponse");
                    }
                } else {
                    log.error("[语音对讲]收到的消息错误，event不是ResponseEvent");
                }

            }, (event) -> {
                dynamicTask.stop(timeOutTaskKey);
                mediaServerService.closeRTPServer(mediaServerItem, sendRtpInfo.getStream());
                // 释放ssrc
                mediaServerService.releaseSsrc(mediaServerItem.getServerId(), sendRtpInfo.getSsrc());
                sessionManager.removeByStream(sendRtpInfo.getApp(), sendRtpInfo.getStream());
                errorEvent.response(event);
            }, userSetting.getPlayTimeout().longValue());
        } catch (InvalidArgumentException | SipException | ParseException e) {

            log.error("[命令发送失败] 对讲消息: {}", e.getMessage());
            dynamicTask.stop(timeOutTaskKey);
            mediaServerService.closeRTPServer(mediaServerItem, sendRtpInfo.getStream());
            // 释放ssrc
            mediaServerService.releaseSsrc(mediaServerItem.getServerId(), sendRtpInfo.getSsrc());

            sessionManager.removeByStream(sendRtpInfo.getApp(), sendRtpInfo.getStream());
            SipSubscribe.EventResult eventResult = new SipSubscribe.EventResult();
            eventResult.type = SipSubscribe.EventResultType.cmdSendFailEvent;
            eventResult.statusCode = -1;
            eventResult.msg = "命令发送失败";
            errorEvent.response(eventResult);
        }

    }

    @Override
    public void startSendRtpStreamFailHand(SendRtpInfo sendRtpInfo, Platform platform, CallIdHeader callIdHeader) {
        if (sendRtpInfo.isOnlyAudio()) {
            MediaDevice device = deviceService.getByGbId(sendRtpInfo.getTargetId());
            MediaDeviceChannel deviceChannel = deviceChannelService.getById(sendRtpInfo.getChannelId());
            AudioBroadcastCache audioBroadcastCatch = audioBroadcastManager.get(sendRtpInfo.getChannelId());
            if (audioBroadcastCatch != null) {
                try {
                    cmder.streamByeCmd(device, deviceChannel.getGbId(), audioBroadcastCatch.getSipTransactionInfo(), null);
                } catch (SipException | ParseException | InvalidArgumentException |
                         SsrcTransactionNotFoundException exception) {
                    log.error("[命令发送失败] 停止语音对讲: {}", exception.getMessage());
                }
            }
        }
    }


    @Override
    public void startPushStream(SendRtpInfo sendRtpInfo, MediaDeviceChannel channel, SIPResponse sipResponse, Platform platform, CallIdHeader callIdHeader) {
        // 开始发流
        MediaServer mediaInfo = mediaServerService.getServerByServerId(sendRtpInfo.getMediaServerId());

        if (mediaInfo != null) {
            try {
                if (sendRtpInfo.isTcpActive()) {
                    mediaServerService.startSendRtpPassive(mediaInfo, sendRtpInfo, null);
                } else {
                    mediaServerService.startSendRtp(mediaInfo, sendRtpInfo);
                }
            } catch (Exception e) {
                log.error("RTP推流失败: {}", e.getMessage());
                startSendRtpStreamFailHand(sendRtpInfo, platform, callIdHeader);
                return;
            }

            log.info("RTP推流成功[ {}/{} ]，{}, ", sendRtpInfo.getApp(), sendRtpInfo.getStream(),
                    sendRtpInfo.isTcpActive()?"被动发流": sendRtpInfo.getIp() + ":" + sendRtpInfo.getPort());

        }
    }
}
