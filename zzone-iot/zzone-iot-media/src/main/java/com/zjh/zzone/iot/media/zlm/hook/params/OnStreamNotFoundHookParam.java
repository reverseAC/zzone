package com.zjh.zzone.iot.media.zlm.hook.params;

import lombok.Getter;
import lombok.Setter;

/**
 * zlm hook事件中的on_stream_not_found事件的参数
 *
 * @author zjh
 * @since 2025-07-02 15:48
 */
@Getter
@Setter
public class OnStreamNotFoundHookParam extends HookParam {
    /**
     * 应用名
     */
    private String app;
    /**
     * TCP 链接唯一 ID
     */
    private String id;
    /**
     * 播放器 ip
     */
    private String ip;
    /**
     * 播放 url 参数
     */
    private String params;
    /**
     * 播放器端口号
     */
    private int port;
    /**
     * 播放的协议，可能是 rtsp、rtmp
     */
    private String schema;
    /**
     * 流id
     */
    private String stream;
    /**
     * 流虚拟主机
     */
    private String vhost;

    @Override
    public String toString() {
        return "OnStreamNotFoundHookParam{" +
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
