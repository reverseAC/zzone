package com.zjh.zzone.iot.media.service.impl;

import com.ylg.iot.constant.MediaCacheConstants;
import com.ylg.iot.media.bo.SendRtpInfo;
import com.ylg.iot.entity.MediaServer;
import com.ylg.iot.media.config.ServerInstanceConfig;
import com.ylg.iot.media.service.SendRtpServerService;
import com.ylg.redis.util.RedisUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.support.atomic.RedisAtomicInteger;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Slf4j
public class SendRtpServerServiceImpl implements SendRtpServerService {

    @Autowired
    private ServerInstanceConfig serverInstance;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;


    @Override
    public SendRtpInfo createSendRtpInfo(MediaServer mediaServer, String ip, Integer port, String ssrc, String requesterId,
                                         String deviceId, Long channelId, Boolean isTcp, Boolean rtcp) {
        int localPort = getNextPort(mediaServer);
        if (localPort <= 0) {
            return null;
        }
        return SendRtpInfo.getInstance(localPort, mediaServer, ip, port, ssrc, deviceId, null, channelId,
                isTcp, rtcp, serverInstance.getInstanceId());
    }

    @Override
    public SendRtpInfo createSendRtpInfo(MediaServer mediaServer, String ip, Integer port, String ssrc, String platformId,
                                         String app, String stream, Long channelId, Boolean tcp, Boolean rtcp){

        int localPort = getNextPort(mediaServer);
        if (localPort <= 0) {
            throw new RuntimeException("server internal error");
        }
        SendRtpInfo sendRtpInfo = SendRtpInfo.getInstance(localPort, mediaServer, ip, port, ssrc, null, platformId, channelId,
                tcp, rtcp, serverInstance.getInstanceId());
        if (sendRtpInfo == null) {
            return null;
        }
        sendRtpInfo.setApp(app);
        sendRtpInfo.setStream(stream);
        return sendRtpInfo;
    }

    @Override
    public void update(SendRtpInfo sendRtpItem) {
        RedisUtils.setObject(MediaCacheConstants.SEND_RTP_INFO_CALLID + ":" + sendRtpItem.getCallId(), sendRtpItem);
        RedisUtils.setObject(MediaCacheConstants.SEND_RTP_INFO_STREAM + ":" + sendRtpItem.getStream() + ":" +sendRtpItem.getTargetId(), sendRtpItem);
        RedisUtils.setObject(MediaCacheConstants.SEND_RTP_INFO_CHANNEL + ":" + sendRtpItem.getChannelId() + ":" +sendRtpItem.getTargetId(), sendRtpItem);
    }

    @Override
    public SendRtpInfo queryByChannelId(Long channelId, String targetId) {
        String key = MediaCacheConstants.SEND_RTP_INFO_CHANNEL + channelId;
//        return JsonUtil.redisHashJsonToObject(redisTemplate, key, targetId, SendRtpInfo.class); TODO 从hash获取数据并转换为对象
        return null;
    }

    @Override
    public SendRtpInfo queryByCallId(String callId) {
        String key = MediaCacheConstants.SEND_RTP_INFO_CALLID + ":" + callId;
        return RedisUtils.getObject(key, SendRtpInfo.class);
    }

    @Override
    public SendRtpInfo queryByStream(String stream, String targetId) {
        String key = MediaCacheConstants.SEND_RTP_INFO_STREAM + stream;
//        return JsonUtil.redisHashJsonToObject(redisTemplate, key, targetId, SendRtpInfo.class); TODO 从hash获取数据并转换为对象
        return null;
    }

    @Override
    public List<SendRtpInfo> queryByStream(String stream) {
        String key = MediaCacheConstants.SEND_RTP_INFO_STREAM + stream;
        return RedisUtils.getCacheList(key, SendRtpInfo.class);
    }

    /**
     * 删除RTP推送信息缓存
     */
    @Override
    public void delete(SendRtpInfo sendRtpInfo) {
        if (sendRtpInfo == null) {
            return;
        }
        RedisUtils.deleteObject(MediaCacheConstants.SEND_RTP_INFO_CALLID + ":" + sendRtpInfo.getCallId());
        RedisUtils.deleteObject(MediaCacheConstants.SEND_RTP_INFO_STREAM + ":" + sendRtpInfo.getStream() + ":" + sendRtpInfo.getTargetId());
        RedisUtils.deleteObject(MediaCacheConstants.SEND_RTP_INFO_CHANNEL + ":" + sendRtpInfo.getChannelId() + ":" + sendRtpInfo.getTargetId());
    }
    @Override
    public void deleteByCallId(String callId) {
        SendRtpInfo sendRtpInfo = queryByCallId(callId);
        if (sendRtpInfo == null) {
            return;
        }
        delete(sendRtpInfo);
    }
    @Override
    public void deleteByStream(String stream, String targetId) {
        SendRtpInfo sendRtpInfo = queryByStream(stream, targetId);
        if (sendRtpInfo == null) {
            return;
        }
        delete(sendRtpInfo);
    }

    @Override
    public void deleteByStream(String stream) {
        List<SendRtpInfo> sendRtpInfos = queryByStream(stream);
        for (SendRtpInfo sendRtpInfo : sendRtpInfos) {
            delete(sendRtpInfo);
        }
    }

    @Override
    public void deleteByChannel(Long channelId, String targetId) {
        SendRtpInfo sendRtpInfo = queryByChannelId(channelId, targetId);
        if (sendRtpInfo == null) {
            return;
        }
        delete(sendRtpInfo);
    }

    @Override
    public List<SendRtpInfo> queryByChannelId(Long channelId) {
        String key = MediaCacheConstants.SEND_RTP_INFO_CHANNEL + channelId;
        return RedisUtils.getCacheList(key, SendRtpInfo.class);
    }

    @Override
    public List<SendRtpInfo> queryAll() {
        String key = MediaCacheConstants.SEND_RTP_INFO_CALLID;
        return RedisUtils.getCacheList(key, SendRtpInfo.class);
    }

    /**
     * 查询某个通道是否存在上级点播（RTP推送）
     */
    @Override
    public boolean isChannelSendingRTP(Long channelId) {
        List<SendRtpInfo> sendRtpInfoList = queryByChannelId(channelId);
        return !sendRtpInfoList.isEmpty();
    }

    @Override
    public List<SendRtpInfo> queryForPlatform(String platformId) {
        List<SendRtpInfo> sendRtpInfos = queryAll();
        if (!sendRtpInfos.isEmpty()) {
            sendRtpInfos.removeIf(sendRtpInfo -> !sendRtpInfo.isSendToPlatform() || !sendRtpInfo.getTargetId().equals(platformId));
        }
        return sendRtpInfos;
    }

    private Set<Integer> getAllSendRtpPort() {
        String key = MediaCacheConstants.SEND_RTP_INFO_CALLID;
        List<SendRtpInfo> values = RedisUtils.getCacheList(key, SendRtpInfo.class);
        Set<Integer> result = new HashSet<>();
        if (values != null) {
            for (SendRtpInfo value : values) {
                result.add(value.getPort());
            }
        }
        return result;
    }


    @Override
    public synchronized int getNextPort(MediaServer mediaServer) {
        if (mediaServer == null) {
            log.warn("[发送端口管理] 参数错误，mediaServer为NULL");
            return -1;
        }
        String sendIndexKey = MediaCacheConstants.SEND_RTP_PORT + serverInstance.getInstanceId() + ":" +  mediaServer.getId();
        Set<Integer> sendRtpSet = getAllSendRtpPort();
        String sendRtpPortRange = mediaServer.getSendRtpPortRange();
        int startPort;
        int endPort;
        if (sendRtpPortRange != null) {
            String[] portArray = sendRtpPortRange.split(",");
            if (portArray.length != 2 || !NumberUtils.isParsable(portArray[0]) || !NumberUtils.isParsable(portArray[1])) {
                log.warn("{}发送端口配置格式错误，自动使用50000-60000作为端口范围", mediaServer.getId());
                startPort = 50000;
                endPort = 60000;
            } else {
                if ( Integer.parseInt(portArray[1]) - Integer.parseInt(portArray[0]) < 1) {
                    log.warn("{}发送端口配置错误,结束端口至少比开始端口大一，自动使用50000-60000作为端口范围", mediaServer.getId());
                    startPort = 50000;
                    endPort = 60000;
                } else {
                    startPort = Integer.parseInt(portArray[0]);
                    endPort = Integer.parseInt(portArray[1]);
                }
            }
        } else {
            log.warn("{}未设置发送端口默认值，自动使用50000-60000作为端口范围", mediaServer.getId());
            startPort = 50000;
            endPort = 60000;
        }
        RedisAtomicInteger redisAtomicInteger = new RedisAtomicInteger(sendIndexKey , redisTemplate.getConnectionFactory());
        if (redisAtomicInteger.get() < startPort) {
            redisAtomicInteger.set(startPort);
            return startPort;
        } else {
            for (int i = 0; i < endPort - startPort; i++) {
                int port = redisAtomicInteger.getAndIncrement();
                if (port > endPort) {
                    redisAtomicInteger.set(startPort);
                    if (sendRtpSet.contains(startPort)) {
                        continue;
                    } else {
                        return startPort;
                    }
                }
                if (!sendRtpSet.contains(port)) {
                    return port;
                }
            }
        }
        log.warn("{}获取发送端口失败, 无可用端口", mediaServer.getId());
        return -1;
    }
}
