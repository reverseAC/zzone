package com.zjh.zzone.iot.media.service.impl;

import com.ylg.core.exception.CheckedException;
import com.ylg.iot.constant.DeviceStatusEnum;
import com.ylg.iot.media.bo.SendRtpInfo;
import com.ylg.iot.media.config.ServerInstanceConfig;
import com.ylg.iot.media.bo.StreamDetail;
import com.ylg.iot.media.config.MediaConfig;
import com.ylg.iot.media.config.UserSetting;
import com.ylg.iot.constant.MediaCacheConstants;
import com.ylg.iot.vo.MediaInfo;
import com.ylg.iot.entity.MediaServer;
import com.ylg.iot.media.event.MediaServerOfflineEvent;
import com.ylg.iot.media.event.MediaServerOnlineEvent;
import com.ylg.iot.media.event.media.MediaArrivalEvent;
import com.ylg.iot.media.event.media.MediaDepartureEvent;
import com.ylg.iot.media.mapper.MediaServerMapper;
import com.ylg.iot.media.service.InviteStreamService;
import com.ylg.iot.media.service.MediaNodeServerService;
import com.ylg.iot.media.service.MediaServerService;
import com.ylg.iot.media.gb28181.session.SSRCFactory;
import com.ylg.iot.media.service.PlayService;
import com.ylg.mybatis.base.BaseServiceImpl;
import com.ylg.redis.util.RedisUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.util.*;

/**
 * 媒体服务器节点管理
 */
@Slf4j
@Service
public class MediaServerServiceImpl extends BaseServiceImpl<MediaServerMapper, MediaServer> implements MediaServerService {

    @Autowired
    private SSRCFactory ssrcFactory;

    @Autowired
    private UserSetting userSetting;

    @Autowired
    private InviteStreamService inviteStreamService;

    @Autowired
    private PlayService playService;

    @Autowired
    private Map<String, MediaNodeServerService> nodeServerServiceMap;

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    private MediaConfig mediaConfig;

    @Autowired
    private ServerInstanceConfig serverInstanceConfig;

    /**
     * 流到来的处理
     */
    @Async("taskExecutor")
    @EventListener
    public void onApplicationEvent(MediaArrivalEvent event) {
        // 维护流媒体服务器负载
    }

    /**
     * 流离开的处理
     */
    @Async("taskExecutor")
    @EventListener
    public void onApplicationEvent(MediaDepartureEvent event) {
        if ("rtsp".equals(event.getSchema())) {
//            log.info("流变化：注销, app->{}, stream->{}", event.getApp(), event.getStream());
//            removeCount(event.getMediaServer().getServerId());
//            MediaInfo mediaInfo = redisCatchStorage.getStreamInfo(
//                    event.getApp(), event.getStream(), event.getMediaServer().getId());
//            if (mediaInfo == null) {
//                return;
//            }
//            String type = OriginType.values()[mediaInfo.getOriginType()].getType();
//            redisCatchStorage.removeStream(mediaInfo.getMediaServer().getId(), type, event.getApp(), event.getStream());
        }
    }

    @Override
    public MediaServer getServerByServerId(String mediaServerId) {
        if (mediaServerId == null) {
            return null;
        }
        String key = MediaCacheConstants.MEDIA_SERVER_PREFIX + serverInstanceConfig.getInstanceId();
        MediaServer mediaServer = RedisUtils.getObject(key + ":" + mediaServerId, MediaServer.class);

        if (mediaServer == null) {
            mediaServer = baseMapper.selectByServerId(mediaServerId);
        }

        return mediaServer;
    }

    @Override
    public int createRTPServer(MediaServer mediaServer, String streamId, long ssrc, Integer port, boolean onlyAuto, boolean disableAudio, boolean reUsePort, Integer tcpMode) {
        int rtpServerPort;
        if (mediaServer.isRtpEnable()) {
            MediaNodeServerService mediaNodeServerService = nodeServerServiceMap.get(mediaServer.getType());
            if (mediaNodeServerService == null) {
                log.info("[openRTPServer] 失败, mediaServer的类型： {}，未找到对应的实现类", mediaServer.getType());
                return 0;
            }
            rtpServerPort = mediaNodeServerService.createRTPServer(mediaServer, streamId, ssrc, port, onlyAuto, disableAudio, reUsePort, tcpMode);
        } else {
            rtpServerPort = mediaServer.getRtpProxyPort();
        }
        return rtpServerPort;
    }

    @Override
    public StreamDetail loadMP4File(MediaServer mediaServer, String app, String stream, String datePath) {
        MediaNodeServerService mediaNodeServerService = nodeServerServiceMap.get(mediaServer.getType());
        if (mediaNodeServerService == null) {
            log.info("[loadMP4File] 失败, mediaServer的类型： {}，未找到对应的实现类", mediaServer.getType());
            throw new CheckedException("未找到mediaServer对应的实现类");
        }
        mediaNodeServerService.loadMP4File(mediaServer, app, stream, datePath);
        return getStreamInfoByAppAndStream(mediaServer, app, stream, null, null);
    }

    @Override
    public MediaInfo getMediaInfo(MediaServer mediaServer, String app, String stream) {
        MediaNodeServerService mediaNodeServerService = nodeServerServiceMap.get(mediaServer.getType());
        if (mediaNodeServerService == null) {
            log.info("[getMediaInfo] 失败, mediaServer的类型： {}，未找到对应的实现类", mediaServer.getType());
            return null;
        }
        return mediaNodeServerService.getMediaInfo(mediaServer, app, stream);
    }

    @Override
    public List<String> listRtpServer(MediaServer mediaServer) {
        MediaNodeServerService mediaNodeServerService = nodeServerServiceMap.get(mediaServer.getType());
        if (mediaNodeServerService == null) {
            log.info("[openRTPServer] 失败, mediaServer的类型： {}，未找到对应的实现类", mediaServer.getType());
            return new ArrayList<>();
        }
        return mediaNodeServerService.listRtpServer(mediaServer);
    }

    @Override
    public void closeRTPServer(MediaServer mediaServer, String streamId) {
        if (mediaServer == null) {
            return;
        }
        MediaNodeServerService mediaNodeServerService = nodeServerServiceMap.get(mediaServer.getType());
        if (mediaNodeServerService == null) {
            log.info("[closeRTPServer] 失败, mediaServer的类型： {}，未找到对应的实现类", mediaServer.getType());
            return;
        }
        mediaNodeServerService.closeRtpServer(mediaServer, streamId);
    }

    @Override
    public void closeRTPServer(String mediaServerId, String streamId) {
        MediaServer mediaServer = this.getServerByServerId(mediaServerId);
        if (mediaServer == null) {
            return;
        }
        if (mediaServer.isRtpEnable()) {
            closeRTPServer(mediaServer, streamId);
        }
        MediaNodeServerService mediaNodeServerService = nodeServerServiceMap.get(mediaServer.getType());
        if (mediaNodeServerService == null) {
            log.info("[closeRTPServer] 失败, mediaServer的类型： {}，未找到对应的实现类", mediaServer.getType());
            return;
        }
        mediaNodeServerService.closeStreams(mediaServer, "rtp", streamId);
    }

    @Override
    public Boolean updateRtpServerSSRC(MediaServer mediaServer, String streamId, String ssrc) {
        if (mediaServer == null) {
            return false;
        }
        MediaNodeServerService mediaNodeServerService = nodeServerServiceMap.get(mediaServer.getType());
        if (mediaNodeServerService == null) {
            log.info("[updateRtpServerSSRC] 失败, mediaServer的类型： {}，未找到对应的实现类", mediaServer.getType());
            return false;
        }
        return mediaNodeServerService.updateRtpServerSSRC(mediaServer, streamId, ssrc);
    }

    @Override
    public void releaseSsrc(String mediaServerId, String ssrc) {
        MediaServer mediaServer = getServerByServerId(mediaServerId);
        if (mediaServer == null || ssrc == null) {
            return;
        }
        ssrcFactory.releaseSsrc(mediaServerId, ssrc);
    }

    @Override
    public void clearRTPServer(MediaServer mediaServer) {
        ssrcFactory.reset(mediaServer.getServerId());
    }

    @Override
    public Integer startSendRtpPassive(MediaServer mediaServer, SendRtpInfo sendRtpItem, Integer timeout) {
        MediaNodeServerService mediaNodeServerService = nodeServerServiceMap.get(mediaServer.getType());
        if (mediaNodeServerService == null) {
            log.info("[startSendRtpPassive] 失败, mediaServer的类型： {}，未找到对应的实现类", mediaServer.getType());
            throw new RuntimeException("未找到mediaServer对应的实现类");
        }
        return mediaNodeServerService.startSendRtpPassive(mediaServer, sendRtpItem, timeout);
    }

    @Override
    public void startSendRtp(MediaServer mediaServer, SendRtpInfo sendRtpItem) {
        MediaNodeServerService mediaNodeServerService = nodeServerServiceMap.get(mediaServer.getType());
        if (mediaNodeServerService == null) {
            log.info("[startSendRtpStream] 失败, mediaServer的类型： {}，未找到对应的实现类", mediaServer.getType());
            throw new RuntimeException("未找到mediaServer对应的实现类");
        }
        sendRtpItem.setRtcp(true);

        log.info("[开始推流] {}/{}, 目标={}:{}，SSRC={}, RTCP={}", sendRtpItem.getApp(), sendRtpItem.getStream(),
                sendRtpItem.getIp(), sendRtpItem.getPort(), sendRtpItem.getSsrc(), sendRtpItem.isRtcp());
        mediaNodeServerService.startSendRtpStream(mediaServer, sendRtpItem);
    }

    @Override
    public boolean deleteRecordDirectory(MediaServer mediaServer, String app, String stream, String date, String fileName) {
        MediaNodeServerService mediaNodeServerService = nodeServerServiceMap.get(mediaServer.getType());
        if (mediaNodeServerService == null) {
            log.info("[stopSendRtp] 失败, mediaServer的类型： {}，未找到对应的实现类", mediaServer.getType());
            return false;
        }
        return mediaNodeServerService.deleteRecordDirectory(mediaServer, app, stream, date, fileName);
    }

    @Override
    public void updateServer(MediaServer mediaSerItem) {
        // 1.更新DB
        baseMapper.updateById(mediaSerItem);

        // 2. 重新生成ssrc
        if(!ssrcFactory.hasMediaServerSSRC(mediaSerItem.getServerId())) {
            ssrcFactory.initMediaServerSSRC(mediaSerItem.getServerId(),null);
        }

        // 3.更新缓存
        String key = MediaCacheConstants.MEDIA_SERVER_PREFIX + serverInstanceConfig.getInstanceId();
        RedisUtils.setObject(key + ":" + mediaSerItem.getServerId(), mediaSerItem);
    }

    @Override
    public void clearMediaServerForOnline() {
        String key = MediaCacheConstants.ONLINE_MEDIA_SERVERS_PREFIX + serverInstanceConfig.getInstanceId();
        RedisUtils.batchDelete(key);
    }

    @Override
    public void addServer(MediaServer mediaServer) {
        if (mediaServer.getHookAliveInterval() == null || mediaServer.getHookAliveInterval() == 0) {
            mediaServer.setHookAliveInterval(10);
        }
        if (mediaServer.getType() == null) {
            log.info("[添加媒体节点] 失败, mediaServer的类型：为空");
            return;
        }

        MediaNodeServerService mediaNodeServerService = nodeServerServiceMap.get(mediaServer.getType());
        if (mediaNodeServerService == null) {
            log.info("[添加媒体节点] 失败, mediaServer的类型： {}，未找到对应的实现类", mediaServer.getType());
            return;
        }

        this.save(mediaServer);
        if (DeviceStatusEnum.ONLINE.getCode().equals(mediaServer.getOnline())) {
            mediaNodeServerService.online(mediaServer);
        }
    }

    @Override
    public MediaServer getMediaServerForMinimumLoad(Boolean hasAssist) {
        String key = MediaCacheConstants.ONLINE_MEDIA_SERVERS_PREFIX + serverInstanceConfig.getInstanceId();
//        Long size = redisTemplate.opsForZSet().zCard(key);
        Long size = 0L;
        if (size  == null || size == 0) {
            log.info("获取负载最低的节点时无在线节点");
            return null;
        }

        // 获取分数最低的，及并发最低的
//        Set<Object> objects = redisTemplate.opsForZSet().range(key, 0, -1);
        Set<Object> objects = null;
        ArrayList<Object> mediaServerObjects = new ArrayList<>(objects);
        MediaServer mediaServer = null;
        if (hasAssist == null) {
            String mediaServerId = (String)mediaServerObjects.get(0);
            mediaServer = getServerByServerId(mediaServerId);
        } else if (hasAssist) {
            for (Object mediaServerObject : mediaServerObjects) {
                String mediaServerId = (String)mediaServerObject;
                MediaServer serverItem = getServerByServerId(mediaServerId);
                if (serverItem.getRecordAssistPort() > 0) {
                    mediaServer = serverItem;
                    break;
                }
            }
        } else {
            for (Object mediaServerObject : mediaServerObjects) {
                String mediaServerId = (String)mediaServerObject;
                MediaServer serverItem = getServerByServerId(mediaServerId);
                if (serverItem.getRecordAssistPort() == 0) {
                    mediaServer = serverItem;
                    break;
                }
            }
        }

        return mediaServer;
    }

    @Override
    public boolean stopSendRtp(MediaServer mediaInfo, String app, String stream, String ssrc) {
        MediaNodeServerService mediaNodeServerService = nodeServerServiceMap.get(mediaInfo.getType());
        if (mediaNodeServerService == null) {
            log.info("[stopSendRtp] 失败, mediaServer的类型： {}，未找到对应的实现类", mediaInfo.getType());
            return false;
        }
        return mediaNodeServerService.stopSendRtp(mediaInfo, app, stream, ssrc);
    }

    @Override
    public Boolean connectRtpServer(MediaServer mediaServer, String address, int port, String stream) {
        MediaNodeServerService mediaNodeServerService = nodeServerServiceMap.get(mediaServer.getType());
        if (mediaNodeServerService == null) {
            log.info("[connectRtpServer] 失败, mediaServer的类型： {}，未找到对应的实现类", mediaServer.getType());
            return false;
        }
        return mediaNodeServerService.connectRtpServer(mediaServer, address, port, stream);
    }

    @Override
    public void getSnap(MediaServer mediaServer, String streamUrl, int timeoutSec, int expireSec, String path, String fileName) {
        MediaNodeServerService mediaNodeServerService = nodeServerServiceMap.get(mediaServer.getType());
        if (mediaNodeServerService == null) {
            log.info("[getSnap] 失败, mediaServer的类型： {}，未找到对应的实现类", mediaServer.getType());
            return;
        }
        mediaNodeServerService.getSnap(mediaServer, streamUrl, timeoutSec, expireSec, path, fileName);
    }

    @Override
    public Boolean pauseRtpCheck(MediaServer mediaServer, String streamKey) {
        MediaNodeServerService mediaNodeServerService = nodeServerServiceMap.get(mediaServer.getType());
        if (mediaNodeServerService == null) {
            log.info("[pauseRtpCheck] 失败, mediaServer的类型： {}，未找到对应的实现类", mediaServer.getType());
            return false;
        }
        return mediaNodeServerService.pauseRtpCheck(mediaServer, streamKey);
    }

    @Override
    public boolean resumeRtpCheck(MediaServer mediaServer, String streamKey) {
        MediaNodeServerService mediaNodeServerService = nodeServerServiceMap.get(mediaServer.getType());
        if (mediaNodeServerService == null) {
            log.info("[pauseRtpCheck] 失败, mediaServer的类型： {}，未找到对应的实现类", mediaServer.getType());
            return false;
        }
        return mediaNodeServerService.resumeRtpCheck(mediaServer, streamKey);
    }

    @Override
    public StreamDetail getStreamInfoByAppAndStream(MediaServer mediaServerItem, String app, String stream, MediaInfo mediaInfo, String callId) {
        return getStreamInfoByAppAndStream(mediaServerItem, app, stream, mediaInfo, null, callId, true);
    }

    @Override
    public StreamDetail getStreamInfoByAppAndStream(MediaServer mediaServer, String app, String stream, MediaInfo mediaInfo, String addr, String callId, boolean isPlay) {
        StreamDetail streamInfoResult = new StreamDetail();
        streamInfoResult.setStream(stream);
        streamInfoResult.setApp(app);
        if (addr == null) {
            addr = mediaServer.getStreamIp();
        }

        streamInfoResult.setIp(addr);
        if (mediaInfo != null) {
            streamInfoResult.setServerId(mediaInfo.getServerId());
        } else {
            streamInfoResult.setServerId(serverInstanceConfig.getInstanceId());
        }

        streamInfoResult.setMediaServer(mediaServer);

        Map<String, String> param = new HashMap<>();
        if (!ObjectUtils.isEmpty(callId)) {
            param.put("callId", callId);
        }
        if (mediaInfo != null && !ObjectUtils.isEmpty(mediaInfo.getOriginTypeStr()))  {
            param.put("originTypeStr", mediaInfo.getOriginTypeStr());
        }
        StringBuilder callIdParamBuilder = new StringBuilder();
        if (!param.isEmpty()) {
            callIdParamBuilder.append("?");
            for (Map.Entry<String, String> entry : param.entrySet()) {
                callIdParamBuilder.append(entry.getKey()).append("=").append(entry.getValue());
                callIdParamBuilder.append("&");
            }
            callIdParamBuilder.deleteCharAt(callIdParamBuilder.length() - 1);
        }
        String callIdParam = callIdParamBuilder.toString();

        streamInfoResult.setRtmp(addr, mediaServer.getRtmpPort(), mediaServer.getRtmpSSlPort(), app, stream, callIdParam);
        streamInfoResult.setRtsp(addr, mediaServer.getRtspPort(), mediaServer.getRtspSSLPort(), app, stream, callIdParam);

        String flvFile;
        if ("abl".equals(mediaServer.getType())) {
            flvFile = String.format("%s/%s.flv%s", app, stream, callIdParam);
        } else {
            flvFile = String.format("%s/%s.live.flv%s", app, stream, callIdParam);
        }
        streamInfoResult.setFlv(addr, mediaServer.getFlvPort(),mediaServer.getFlvSSLPort(), flvFile);
        streamInfoResult.setWsFlv(addr, mediaServer.getWsFlvPort(),mediaServer.getWsFlvSSLPort(), flvFile);

        streamInfoResult.setFmp4(addr, mediaServer.getHttpPort(),mediaServer.getHttpSSlPort(), app, stream, callIdParam);
        streamInfoResult.setHls(addr, mediaServer.getHttpPort(),mediaServer.getHttpSSlPort(), app, stream, callIdParam);
        streamInfoResult.setTs(addr, mediaServer.getHttpPort(),mediaServer.getHttpSSlPort(), app, stream, callIdParam);
        streamInfoResult.setRtc(addr, mediaServer.getHttpPort(),mediaServer.getHttpSSlPort(), app, stream, callIdParam, isPlay);

        streamInfoResult.setMediaInfo(mediaInfo);

        if (!"broadcast".equalsIgnoreCase(app) && !ObjectUtils.isEmpty(mediaServer.getTranscodeSuffix()) && !"null".equalsIgnoreCase(mediaServer.getTranscodeSuffix())) {
            String newStream = stream + "_" + mediaServer.getTranscodeSuffix();
            mediaServer.setTranscodeSuffix(null);
            StreamDetail transcodeStreamInfo = getStreamInfoByAppAndStream(mediaServer, app, newStream, null, addr, callId, isPlay);
            streamInfoResult.setTranscodeStream(transcodeStreamInfo);
        }
        return streamInfoResult;
    }

    @Override
    public Boolean isStreamReady(MediaServer mediaServer, String app, String streamId) {
        MediaNodeServerService mediaNodeServerService = nodeServerServiceMap.get(mediaServer.getType());
        if (mediaNodeServerService == null) {
            log.info("[isStreamReady] 失败, mediaServer的类型： {}，未找到对应的实现类", mediaServer.getType());
            return false;
        }
        MediaInfo mediaInfo = mediaNodeServerService.getMediaInfo(mediaServer, app, streamId);
        return mediaInfo != null;
    }

    @Override
    public Long updateDownloadProcess(MediaServer mediaServer, String app, String stream) {
        MediaNodeServerService mediaNodeServerService = nodeServerServiceMap.get(mediaServer.getType());
        if (mediaNodeServerService == null) {
            log.info("[updateDownloadProcess] 失败, mediaServer的类型： {}，未找到对应的实现类", mediaServer.getType());
            throw new CheckedException("未找到mediaServer对应的实现类");
        }
        return mediaNodeServerService.updateDownloadProcess(mediaServer, app, stream);
    }

    /**
     * 处理ZLM上线
     *
     * @param event 上线事件
     */
    @Async("taskExecutor")
    @EventListener
    public void onApplicationEvent(MediaServerOnlineEvent event) {
        log.info("[媒体节点] 上线 ID：{}", event.getMediaServer().getId());
        playService.zlmServerOnline(event.getMediaServer());
    }

    /**
     * 处理ZLM下线
     *
     * @param event 下线事件
     */
    @Async("taskExecutor")
    @EventListener
    public void onApplicationEvent(MediaServerOfflineEvent event) {
        log.info("[媒体节点] 离线，ID：{}", event.getMediaServer().getId());
        playService.zlmServerOffline(event.getMediaServer());
    }
}
