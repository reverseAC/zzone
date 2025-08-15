package com.zjh.zzone.iot.media.bo;

import lombok.Data;

/**
 * 同步源标识符信息，标识RTP视频流的唯一性
 *
 * @author zjh
 * @since 2025-06-26 9:49
 */
@Data
public class SSRCInfo {
    /**
     * RTP服务端口
     */
    private int port;
    /**
     * 同步源标识，标识RTP数据流
     */
    private String ssrc;
    /**
     * 应用名
     */
    private String app;
    /**
     * 流标识
     */
    private String Stream;
    /**
     * 超时任务标识
     */
    private String timeOutTaskKey;


    public SSRCInfo(int port, String ssrc, String stream, String timeOutTaskKey) {
        this(port, ssrc, "rtp", stream, timeOutTaskKey);
    }

    public SSRCInfo(int port, String ssrc, String app, String stream, String timeOutTaskKey) {
        this.port = port;
        this.ssrc = ssrc;
        this.app = app;
        this.Stream = stream;
        this.timeOutTaskKey = timeOutTaskKey;
    }
}
