package com.zjh.zzone.iot.media.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.ylg.iot.media.bo.StreamDetail;
import com.ylg.iot.media.config.UserSetting;
import com.ylg.iot.constant.MediaCacheConstants;
import com.ylg.iot.enums.media.InviteSessionStatus;
import com.ylg.iot.enums.media.InviteSessionType;
import com.ylg.iot.media.bo.InviteInfo;
import com.ylg.iot.media.callback.ErrorCallback;
import com.ylg.iot.media.service.InviteStreamService;
import com.ylg.redis.util.RedisUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 点播状态管理 服务实现类
 *
 * @author zjh
 * @since 2025-03-31 13:48
 */
@Slf4j
@Service
public class InviteStreamServiceImpl implements InviteStreamService {

    private final Map<String, List<ErrorCallback<StreamDetail>>> inviteErrorCallbackMap = new ConcurrentHashMap<>();

    @Autowired
    private UserSetting userSetting;


    @Override
    public InviteInfo getInviteInfo(InviteSessionType type, Long channelId, String stream) {
        String key = MediaCacheConstants.SIP_INVITE_PREFIX;
        String keyPattern = (type != null ? type : "*") +
                ":" + (channelId != null ? channelId : "*") +
                ":" + (stream != null ? stream : "*");
        ScanOptions options = ScanOptions.scanOptions().match(keyPattern).count(20).build();
        try (Cursor<Map.Entry<Object, Object>> cursor = RedisUtils.scanHash(key, options)) {
            if (cursor.hasNext()) {
                InviteInfo inviteInfo = JSONObject.parseObject(JSONObject.toJSONString(cursor.next().getValue()), InviteInfo.class) ;
                cursor.close();
                return inviteInfo;
            }
        } catch (Exception e) {
            log.error("[Redis-InviteInfo] 查询异常: ", e);
        }
        return null;
    }

    @Override
    public InviteInfo getInviteInfoByDeviceAndChannel(InviteSessionType type, Long channelId) {
        return getInviteInfo(type, channelId, null);
    }

    @Override
    public InviteInfo getInviteInfoByStream(InviteSessionType type, String stream) {
        return getInviteInfo(type, null, stream);
    }

    @Override
    public List<InviteInfo> getAllInviteInfo() {
        return new ArrayList<>(RedisUtils.getHashMap(MediaCacheConstants.SIP_INVITE_PREFIX, InviteInfo.class).values());
    }

    @Override
    public void updateInviteInfo(InviteInfo inviteInfo) {
        if (InviteSessionStatus.ready == inviteInfo.getStatus()) {
            updateInviteInfo(inviteInfo, Long.valueOf(userSetting.getPlayTimeout()) * 2);
        } else {
            updateInviteInfo(inviteInfo, null);
        }
    }

    @Override
    public void updateInviteInfo(InviteInfo inviteInfo, Long time) {
        if (inviteInfo == null || (inviteInfo.getDeviceId() == null || inviteInfo.getChannelId() == null)) {
            log.warn("[更新Invite信息]，参数不全： {}", JSON.toJSON(inviteInfo));
            return;
        }
        InviteInfo inviteInfoForUpdate;

        if (InviteSessionStatus.ready == inviteInfo.getStatus()) {
            if (inviteInfo.getDeviceId() == null || inviteInfo.getChannelId() == null
                    || inviteInfo.getType() == null || inviteInfo.getStream() == null
            ) {
                return;
            }
            inviteInfoForUpdate = inviteInfo;
        } else {
            InviteInfo inviteInfoInRedis = getInviteInfo(inviteInfo.getType(), inviteInfo.getChannelId(), inviteInfo.getStream());
            if (inviteInfoInRedis == null) {
                log.warn("[更新Invite信息]，未从缓存中读取到Invite信息： deviceId: {}, channel: {}, stream: {}",
                        inviteInfo.getDeviceId(), inviteInfo.getChannelId(), inviteInfo.getStream());
                return;
            }
            if (inviteInfo.getStreamDetail() != null) {
                inviteInfoInRedis.setStreamDetail(inviteInfo.getStreamDetail());
            }
            if (inviteInfo.getSsrcInfo() != null) {
                inviteInfoInRedis.setSsrcInfo(inviteInfo.getSsrcInfo());
            }
            if (inviteInfo.getStreamMode() != null) {
                inviteInfoInRedis.setStreamMode(inviteInfo.getStreamMode());
            }
            if (inviteInfo.getReceiveIp() != null) {
                inviteInfoInRedis.setReceiveIp(inviteInfo.getReceiveIp());
            }
            if (inviteInfo.getReceivePort() != null) {
                inviteInfoInRedis.setReceivePort(inviteInfo.getReceivePort());
            }
            if (inviteInfo.getStatus() != null) {
                inviteInfoInRedis.setStatus(inviteInfo.getStatus());
            }

            inviteInfoForUpdate = inviteInfoInRedis;

        }
        if (inviteInfoForUpdate.getCreateTime() == null) {
            inviteInfoForUpdate.setCreateTime(System.currentTimeMillis());
        }
        String key = MediaCacheConstants.SIP_INVITE_PREFIX;
        String objectKey = inviteInfoForUpdate.getType() +
                ":" + inviteInfoForUpdate.getChannelId() +
                ":" + inviteInfoForUpdate.getStream();
        if (time != null && time > 0) {
            inviteInfoForUpdate.setExpirationTime(time);
        }
        RedisUtils.setHashKey(key, objectKey, inviteInfoForUpdate);
    }

    @Override
    public void removeInviteInfo(InviteSessionType type, Long channelId, String stream) {
        String key = MediaCacheConstants.SIP_INVITE_PREFIX;
        if (type == null && channelId == null && stream == null) {
            RedisUtils.deleteObject(key);
            return;
        }
        InviteInfo inviteInfo = getInviteInfo(type, channelId, stream);
        if (inviteInfo != null) {
            String objectKey = inviteInfo.getType() +
                    ":" + inviteInfo.getChannelId() +
                    ":" + inviteInfo.getStream();
            RedisUtils.deleteHashKey(key, objectKey);
        }
    }

    @Override
    public void removeInviteInfoByDeviceAndChannel(InviteSessionType inviteSessionType, Long channelId) {
        removeInviteInfo(inviteSessionType, channelId, null);
    }

    @Override
    public void removeInviteInfo(InviteInfo inviteInfo) {
        removeInviteInfo(inviteInfo.getType(), inviteInfo.getChannelId(), inviteInfo.getStream());
    }

    @Override
    public void clearInviteInfo(String deviceId) {
        List<InviteInfo> inviteInfoList = getAllInviteInfo();
        for (InviteInfo inviteInfo : inviteInfoList) {
            if (inviteInfo.getDeviceId().equals(deviceId)) {
                removeInviteInfo(inviteInfo);
            }
        }
    }

    @Override
    public void addCallback(InviteSessionType type, Long channelId, String stream, ErrorCallback<StreamDetail> callback) {
        String key = buildKey(type, channelId, stream);
        List<ErrorCallback<StreamDetail>> callbacks = inviteErrorCallbackMap.computeIfAbsent(key, k -> new CopyOnWriteArrayList<>());
        callbacks.add(callback);
    }

    private String buildKey(InviteSessionType type, Long channelId, String stream) {
        String key = type + ":" + channelId;
        // 如果ssrc未null那么可以实现一个通道只能一次操作，ssrc不为null则可以支持一个通道多次invite
        if (stream != null) {
            key += (":" + stream);
        }
        return key;
    }

    @Override
    public void runCallback(InviteSessionType type, Long channelId, String stream, int code, String msg, StreamDetail data) {
        String key = buildSubStreamKey(type, channelId, stream);
        List<ErrorCallback<StreamDetail>> callbacks = inviteErrorCallbackMap.get(key);
        if (callbacks == null || callbacks.isEmpty()) {
            return;
        }
        for (ErrorCallback<StreamDetail> callback : callbacks) {
            if (callback != null) {
                callback.run(code, msg, data);
            }
        }
        inviteErrorCallbackMap.remove(key);
    }

    private String buildSubStreamKey(InviteSessionType type, Long channelId, String stream) {
        String key = type + ":" + channelId;
        if (stream != null) {
            key += (":" + stream);
        }
        return key;
    }

    @Override
    public InviteInfo getInviteInfoBySSRC(String ssrc) {
        List<InviteInfo> inviteInfoList = getAllInviteInfo();
        if (inviteInfoList.isEmpty()) {
            return null;
        }
        for (InviteInfo inviteInfo : inviteInfoList) {
            if (inviteInfo.getSsrcInfo() != null && ssrc.equals(inviteInfo.getSsrcInfo().getSsrc())) {
                return inviteInfo;
            }
        }
        return null;
    }

    @Override
    public InviteInfo updateInviteInfoForSSRC(InviteInfo inviteInfo, String ssrc) {
        InviteInfo inviteInfoInDb = getInviteInfo(inviteInfo.getType(), inviteInfo.getChannelId(), inviteInfo.getStream());
        if (inviteInfoInDb == null) {
            return null;
        }
        removeInviteInfo(inviteInfoInDb);
        String key = MediaCacheConstants.SIP_INVITE_PREFIX;
        String objectKey = inviteInfo.getType() +
                ":" + inviteInfo.getChannelId() +
                ":" + inviteInfo.getStream();
        if (inviteInfoInDb.getSsrcInfo() != null) {
            inviteInfoInDb.getSsrcInfo().setSsrc(ssrc);
        }
        RedisUtils.setHashKey(key, objectKey, inviteInfoInDb);
        return inviteInfoInDb;
    }

    @Scheduled(fixedRate = 10000)   // 定时检测,清理错误的redis数据,防止因为错误数据导致的点播不可用
    public void execute(){
        log.debug("Scheduled: InviteStreamServiceImpl...");
        String key = MediaCacheConstants.SIP_INVITE_PREFIX;

        Map<String, InviteInfo> values = RedisUtils.getHashMap(key, InviteInfo.class);
        for (InviteInfo inviteInfo : values.values()) {
            if (inviteInfo.getStreamDetail() != null) {
                continue;
            }
            if (inviteInfo.getCreateTime() == null || inviteInfo.getExpirationTime() == 0) {
                removeInviteInfo(inviteInfo);
            }
            long time = inviteInfo.getCreateTime() + inviteInfo.getExpirationTime();
            if (System.currentTimeMillis() > time) {
                removeInviteInfo(inviteInfo);
            }
        }
    }
}
