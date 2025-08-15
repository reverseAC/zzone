package com.zjh.zzone.iot.media.bo;

import com.ylg.iot.entity.MediaServer;
import lombok.Data;

/**
 * RTP服务器参数
 *
 * @author zjh
 * @since 2025/04/01 10:58
 */
@Data
public class RTPServerParam {

    /**
     * 媒体服务器信息
     */
    private MediaServer mediaServerItem;
    /**
     * 流ID
     */
    private String streamId;
    /**
     * 预设ssrc
     */
    private String presetSsrc;
    /**
     * 是否开启ssrc校验
     */
    private boolean ssrcCheck;
    /**
     * 录像回放/实时直播
     */
    private boolean playback;
    /**
     * 端口
     */
    private Integer port;
    /**
     *
     */
    private boolean onlyAuto;
    /**
     * 是否禁用音频
     */
    private boolean disableAudio;
    /**
     * 是否重用端口
     */
    private boolean reUsePort;
    /**
     * tcp模式，0时为不启用tcp监听，1时为启用tcp监听，2时为tcp主动连接模式
     */
    private Integer tcpMode;


}
