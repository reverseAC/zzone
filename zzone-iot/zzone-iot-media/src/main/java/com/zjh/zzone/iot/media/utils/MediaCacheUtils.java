package com.zjh.zzone.iot.media.utils;

import com.ylg.core.utils.StringUtils;
import com.ylg.iot.constant.MediaCacheConstants;
import com.ylg.iot.entity.MediaDevice;
import com.ylg.redis.util.RedisUtils;
import org.springframework.stereotype.Component;

/**
 * 流媒体缓存
 *
 * @author zjh
 * @since 2025-03-31 16:35
 */
@Component
public class MediaCacheUtils extends RedisUtils {

    /**
     * 修改设备媒体信息
     * @param mediaInfo 字典列表
     */
    public static void setDeviceMediaInfo(MediaDevice mediaInfo) {
        setObject(MediaCacheConstants.DEVICE_PREFIX + mediaInfo.getGbId(), mediaInfo);
    }

    /**
     * 获取设备媒体信息
     * @param gbId 设备国标id
     */
    public static MediaDevice getDeviceMediaInfo(String gbId) {
        String key = MediaCacheConstants.DEVICE_PREFIX + gbId;
        return getObject(key, MediaDevice.class);
    }

    /**
     * 删除设备媒体信息缓存
     * @param gbId 设备国标id
     */
    public static void delDeviceMediaInfo(String gbId) {
        if (StringUtils.isNotEmpty(gbId)) {
            deleteObject(MediaCacheConstants.DEVICE_PREFIX + gbId);
        }
    }

}
