package com.zjh.zzone.iot.media.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ylg.iot.constant.MediaCacheConstants;
import com.ylg.iot.entity.RecordShareLog;
import com.ylg.iot.media.bo.*;
import com.ylg.iot.entity.MediaDeviceChannel;
import com.ylg.iot.entity.MediaServer;
import com.ylg.iot.enums.media.InviteSessionStatus;
import com.ylg.iot.enums.media.InviteSessionType;
import com.ylg.iot.media.config.ServerInstanceConfig;
import com.ylg.iot.media.config.UserSetting;
import com.ylg.iot.media.gb28181.session.SipInviteSessionManager;
import com.ylg.iot.media.service.InviteStreamService;
import com.ylg.iot.media.service.MediaDeviceChannelService;
import com.ylg.iot.media.service.MediaService;
import com.ylg.iot.media.service.RecordShareLogService;
import com.ylg.iot.media.utils.DateUtil;
import com.ylg.iot.media.zlm.ZLMServerFactory;
import com.ylg.redis.util.RedisUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Slf4j
@Service
public class MediaServiceImpl implements MediaService {

    @Autowired
    private InviteStreamService inviteStreamService;

    @Autowired
    private MediaDeviceChannelService deviceChannelService;

    @Autowired
    private RecordShareLogService recordShareLogService;

    @Autowired
    private SipInviteSessionManager sessionManager;

    @Autowired
    private UserSetting userSetting;

    @Autowired
    private ServerInstanceConfig serverInstance;

    @Autowired
    private ZLMServerFactory zlmServerFactory;


    @Override
    public AuthenticatePlayResult authenticatePlay(MediaServer mediaServer, String app, String stream, String callId) {
        if (app == null || stream == null) {
            return AuthenticatePlayResult.fail("Unauthorized");
        }

        if ("rtp".equals(app)) {
            int totalReader = zlmServerFactory.totalReaderCount(mediaServer, app, stream);
            if (totalReader < 5) {
                return AuthenticatePlayResult.ok();
            } else {
                return AuthenticatePlayResult.fail("Too many readers");
            }
        }

        // 平台固定使用fmp4协议进行录像分享
        if ("mp4_record".equals(app)) {
            List<RecordShareLog> shareLogList = recordShareLogService.list(new QueryWrapper<RecordShareLog>().eq("stream", stream.split("_")[1]));
            if (!CollectionUtils.isEmpty(shareLogList)) {
                RecordShareLog shareLog = shareLogList.get(0);
                if (-1 == shareLog.getMaxConcurrentReaders()) {
                    return AuthenticatePlayResult.ok();
                }
                // 获取在线人数
                int totalReader = zlmServerFactory.totalReaderCount(mediaServer, app, stream);
                if(shareLog.getMaxConcurrentReaders() != -1 && totalReader < shareLog.getMaxConcurrentReaders()) {
                    return AuthenticatePlayResult.ok();
                } else {
                    return AuthenticatePlayResult.fail("Too many readers");
                }
            }
        }

        StreamAuthorityInfo streamAuthorityInfo = RedisUtils.getHashKey(
                MediaCacheConstants.MEDIA_STREAM_AUTHORITY + serverInstance.getInstanceId(),
                app + "_" + stream,
                StreamAuthorityInfo.class);
        if (streamAuthorityInfo == null || streamAuthorityInfo.getCallId() == null) {
            return AuthenticatePlayResult.ok();
        }
        if (!streamAuthorityInfo.getCallId().equals(callId)) {
            return AuthenticatePlayResult.fail("Unauthorized");
        }
        return AuthenticatePlayResult.ok();
    }

    @Override
    public ResultForOnPublish authenticatePublish(MediaServer mediaServer, String app, String stream, String params) {
        // 推流鉴权的处理
        if (!"rtp".equals(app)) {
            if ("talk".equals(app) && stream.endsWith("_talk")) {
                ResultForOnPublish result = new ResultForOnPublish();
                result.setEnable_mp4(false);
                result.setEnable_audio(true);
                return result;
            }
            // TODO 按需实现推流/拉流代理鉴权
        }

        ResultForOnPublish result = new ResultForOnPublish();
        result.setEnable_audio(true);

        // 国标流
        if ("rtp".equals(app)) {
            InviteInfo inviteInfo = inviteStreamService.getInviteInfoByStream(null, stream);

            if (inviteInfo != null) {
                result.setEnable_mp4(inviteInfo.getRecord());
            } else {
                result.setEnable_mp4(userSetting.getRecordSip());
            }

            // 单端口模式下修改流 ID
            if (!mediaServer.isRtpEnable() && inviteInfo == null) {
                String ssrc = String.format("%010d", Long.parseLong(stream, 16));
                inviteInfo = inviteStreamService.getInviteInfoBySSRC(ssrc);
                if (inviteInfo != null) {
                    result.setStream_replace(inviteInfo.getStream());
                    log.info("[ZLM HOOK]推流鉴权 stream: {} 替换为 {}", stream, inviteInfo.getStream());
                    stream = inviteInfo.getStream();
                }
            }

            // 设置音频信息及录制信息
            SsrcTransaction ssrcTransaction = sessionManager.getSsrcTransactionByStream(app, stream);
            if (ssrcTransaction != null ) {

                // 为录制国标模拟一个鉴权信息, 方便后续写入录像文件时使用
                StreamAuthorityInfo streamAuthorityInfo = StreamAuthorityInfo.getInstanceByHook(app, stream, mediaServer.getServerId());
                streamAuthorityInfo.setApp(app);
                streamAuthorityInfo.setStream(ssrcTransaction.getStream());
                streamAuthorityInfo.setCallId(ssrcTransaction.getSipTransactionInfo().getCallId());

                RedisUtils.setHashKey(MediaCacheConstants.MEDIA_STREAM_AUTHORITY + serverInstance.getInstanceId(), app + "_" + ssrcTransaction.getStream(), streamAuthorityInfo);

                Long channelId = ssrcTransaction.getChannelId();
                MediaDeviceChannel deviceChannel = deviceChannelService.getById(channelId);
                if (deviceChannel != null) {
                    result.setEnable_audio(deviceChannel.isHasAudio());
                }
                // 如果是录像下载就设置视频间隔十秒
                if (ssrcTransaction.getType() == InviteSessionType.DOWNLOAD) {
                    // 获取录像的总时长，然后设置为这个视频的时长
                    InviteInfo inviteInfoForDownload = inviteStreamService.getInviteInfo(InviteSessionType.DOWNLOAD, channelId, stream);
                    if (inviteInfoForDownload != null) {
                        String startTime = inviteInfoForDownload.getStartTime();
                        String endTime = inviteInfoForDownload.getEndTime();
                        long difference = DateUtil.getDifference(startTime, endTime) / 1000;
                        result.setMp4_max_second((int) difference);
                        result.setEnable_mp4(true);
                        // 设置为2保证得到的mp4的时长是正常的
                        result.setModify_stamp(2);
                    }
                }
                // 如果是talk对讲，则默认获取声音
                if (ssrcTransaction.getType() == InviteSessionType.TALK) {
                    result.setEnable_audio(true);
                }
            }
        } else if (app.equals("broadcast")) { // 广播
            result.setEnable_audio(true);
            result.setEnable_mp4(userSetting.getRecordSip());
        } else if (app.equals("talk")) { // 对讲
            result.setEnable_audio(true);
            result.setEnable_mp4(userSetting.getRecordSip());
        } else { // 非国标流 推流/拉流代理
            result.setEnable_mp4(userSetting.getRecordPushLive());
        }
        if (app.equalsIgnoreCase("rtp")) {
//            String receiveKey = MediaCacheConstants.WVP_OTHER_RECEIVE_RTP_INFO + userSetting.getServerId() + "_" + stream;
//            OtherRtpSendInfo otherRtpSendInfo = (OtherRtpSendInfo) redisTemplate.opsForValue().get(receiveKey);
//
//            String receiveKeyForPS = VideoManagerConstants.WVP_OTHER_RECEIVE_PS_INFO + userSetting.getServerId() + "_" + stream;
//            OtherPsSendInfo otherPsSendInfo = (OtherPsSendInfo) redisTemplate.opsForValue().get(receiveKeyForPS);
//            if (otherRtpSendInfo != null || otherPsSendInfo != null) {
//                result.setEnable_mp4(true);
//            }
        }
        return result;
    }

    @Override
    public boolean closeStreamOnNoneReader(String mediaServerId, String app, String stream, String schema) {
        boolean result = false;
//        if (recordPlanService.recording(app, stream) != null) {
//            return false;
//        }
        // 国标类型的流
        if ("rtp".equals(app)) {
            result = userSetting.getStreamOnDemand();
            // 国标流， 点播/录像回放/录像下载
            InviteInfo inviteInfo = inviteStreamService.getInviteInfoByStream(null, stream);
            // 点播
            if (inviteInfo != null && inviteInfo.getStatus() == InviteSessionStatus.ok) {
                // 录像下载
                if (inviteInfo.getType() == InviteSessionType.DOWNLOAD) {
                    return false;
                }
                MediaDeviceChannel deviceChannel = deviceChannelService.getById(inviteInfo.getChannelId());
                if (deviceChannel == null) {
                    return false;
                }
                return result;
            } else {
                return false;
            }
        } else if ("talk".equals(app) || "broadcast".equals(app)) {
            return false;
        } else {
            return false;
        }
    }
}
