package com.zjh.zzone.iot.media.zlm.hook.params;

import lombok.Getter;
import lombok.Setter;

/**
 * zlm hook事件中的on_stream_none_reader事件的参数
 *
 * @author zjh
 * @since 2025-07-02 15:48
 */
@Getter
@Setter
public class OnStreamNoneReaderHookParam extends HookParam {

    /**
     * 流应用名
     */
    private String app;
    /**
     * 协议 rtsp 或 rtmp
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
        return "OnStreamNoneReaderHookParam{" +
                "schema='" + schema + '\'' +
                ", app='" + app + '\'' +
                ", stream='" + stream + '\'' +
                ", vhost='" + vhost + '\'' +
                '}';
    }
}
