package com.zjh.zzone.iot.media.bo;

import com.ylg.iot.entity.MediaDeviceChannel;
import lombok.Getter;
import lombok.Setter;

/**
 * 通道CRUD信息对象
 * @author zjh
 * @since 2025-06-19 20:36
 */
@Getter
@Setter
public class NotifyCatalogChannel {

    /**
     * CRUD类型
     */
    private Type type;

    /**
     * 通道信息
     */
    private MediaDeviceChannel channel;


    public enum Type {
        ADD, DELETE, UPDATE, STATUS_CHANGED
    }

    public static NotifyCatalogChannel getInstance(Type type, MediaDeviceChannel channel) {
        NotifyCatalogChannel notifyCatalogChannel = new NotifyCatalogChannel();
        notifyCatalogChannel.setType(type);
        notifyCatalogChannel.setChannel(channel);
        return notifyCatalogChannel;
    }
}
