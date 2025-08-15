package com.zjh.zzone.iot.media.gb28181.session;

import com.ylg.iot.constant.MediaCacheConstants;
import com.ylg.iot.media.config.ServerInstanceConfig;
import com.ylg.iot.media.config.SipConfig;
import com.ylg.redis.util.RedisUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * ssrc生成器
 *
 * @author zjh
 * @since 2025-04-01 16:28
 */
@Component
public class SSRCFactory {

    /**
     * RTP流最大并发数
     */
    private static final Integer MAX_STREAM_COUNT = 10000;

    @Autowired
    private SipConfig sipConfig;

    @Autowired
    private ServerInstanceConfig serverInstanceConfig;

    /**
     * 初始化ssrc：生成ssrc并放入redis中
     *
     * @param mediaServerId 媒体服务id
     * @param usedSet 已使用的ssrc集合
     */
    public void initMediaServerSSRC(String mediaServerId, Set<String> usedSet) {
        String sipDomain = sipConfig.getDomain();
        String ssrcPrefix = sipDomain.length() >= 8 ? sipDomain.substring(3, 8) : sipDomain;
        String redisKey = MediaCacheConstants.MEDIA_SERVER_SSRC_INFO_PREFIX + serverInstanceConfig.getInstanceId() + ":" + mediaServerId;
        List<String> ssrcList = new ArrayList<>();
        for (int i = 1; i < MAX_STREAM_COUNT; i++) {
            String ssrc = String.format("%s%04d", ssrcPrefix, i);
            if (null == usedSet || !usedSet.contains(ssrc)) {
                ssrcList.add(ssrc);
            }
        }
        if (RedisUtils.getSetSize(redisKey) != null) {
            RedisUtils.deleteObject(redisKey);
        }
        RedisUtils.addSet(redisKey, ssrcList.toArray(new String[0]));
    }

    /**
     * 获取视频预览的ssrc值,第一位固定为0
     *
     * @param mediaServerId 媒体服务id
     * @return ssrc
     */
    public String getPlaySsrc(String mediaServerId) {
        return "0" + getSsrc(mediaServerId);
    }

    /**
     * 获取录像回放的ssrc值,第一位固定为1
     *
     * @param mediaServerId 媒体服务id
     * @return ssrc
     */
    public String getPlayBackSsrc(String mediaServerId) {
        return "1" + getSsrc(mediaServerId);
    }

    /**
     * 释放ssrc：将使用结束的ssrc重新放入redis（主要用完的ssrc一定要释放，否则会耗尽）
     *
     * @param mediaServerId 媒体服务id
     * @param ssrc 需要重置的ssrc
     */
    public void releaseSsrc(String mediaServerId, String ssrc) {
        if (ssrc == null) {
            return;
        }
        String sn = ssrc.substring(1);
        String redisKey = MediaCacheConstants.MEDIA_SERVER_SSRC_INFO_PREFIX + serverInstanceConfig.getInstanceId() + ":" + mediaServerId;
        RedisUtils.addSet(redisKey, sn);
    }

    /**
     * 随机获取ssrc
     *
     * @param mediaServerId 媒体服务id
     */
    private String getSsrc(String mediaServerId) {
        String redisKey = MediaCacheConstants.MEDIA_SERVER_SSRC_INFO_PREFIX + serverInstanceConfig.getInstanceId() + ":" + mediaServerId;
        Long setSize = RedisUtils.getSetSize(redisKey);
        if (setSize == null || setSize == 0) {
            throw new RuntimeException("ssrc已经用完");
        } else {
            // 在集合中移除并返回一个随机成员。
            return RedisUtils.popSet(redisKey, String.class);
        }
    }

    /**
     * 重置一个流媒体服务的所有ssrc
     *
     * @param mediaServerId 流媒体服务ID
     */
    public void reset(String mediaServerId) {
        this.initMediaServerSSRC(mediaServerId, null);
    }

    /**
     * 是否已经存在了当前MediaServer的SSRC信息
     *
     * @param mediaServerId 流媒体服务ID
     */
    public boolean hasMediaServerSSRC(String mediaServerId) {
        String redisKey = MediaCacheConstants.MEDIA_SERVER_SSRC_INFO_PREFIX + serverInstanceConfig.getInstanceId() + ":" + mediaServerId;
        return Boolean.TRUE.equals(RedisUtils.hasKey(redisKey));
    }

}
