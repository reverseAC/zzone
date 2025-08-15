package com.zjh.zzone.iot.media.zlm.hook.params;

import lombok.Getter;
import lombok.Setter;

/**
 * zlm hook事件中的on_publish事件的参数
 *
 * @author zjh
 * @since 2025-07-02 15:48
 */
@Getter
@Setter
public class OnPublishHookParam extends HookParam {

    /**
     * 流应用名
     */
    private String app;
    /**
     * TCP链接唯一ID
     */
    private String id;
    /**
     * 播放器ip
     */
    private String ip;
    /**
     * 播放url参数
     */
    private String params;
    /**
     * 播放器端口号
     */
    private int port;
    /**
     * 播放协议，可能是 rtsp、rtmp、http
     */
    private String schema;
    /**
     * 流ID
     */
    private String stream;
    /**
     * 流虚拟主机
     */
    private String vhost;


    @Override
    public String toString() {
        return "OnPublishHookParam{" +
                "id='" + id + '\'' +
                ", app='" + app + '\'' +
                ", stream='" + stream + '\'' +
                ", ip='" + ip + '\'' +
                ", params='" + params + '\'' +
                ", port=" + port +
                ", schema='" + schema + '\'' +
                ", vhost='" + vhost + '\'' +
                '}';
    }
}
