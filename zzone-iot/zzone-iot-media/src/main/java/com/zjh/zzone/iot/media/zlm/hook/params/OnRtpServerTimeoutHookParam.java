package com.zjh.zzone.iot.media.zlm.hook.params;

import lombok.Getter;
import lombok.Setter;

/**
 * zlm hook事件中的on_send_rtp_stopped事件的参数
 *
 * @author zjh
 * @since 2025-07-02 15:48
 */
@Getter
@Setter
public class OnRtpServerTimeoutHookParam extends HookParam {
    /**
     * openRtpServer 输入的参数
     */
    private int local_port;
    /**
     * openRtpServer 输入的参数
     */
    private String stream_id;
    /**
     * openRtpServer 输入的参数
     */
    private int tcpMode;
    /**
     * openRtpServer 输入的参数
     */
    private boolean re_use_port;
    /**
     * openRtpServer 输入的参数
     */
    private String ssrc;

    @Override
    public String toString() {
        return "OnRtpServerTimeoutHookParam{" +
                "local_port=" + local_port +
                ", stream_id='" + stream_id + '\'' +
                ", tcpMode=" + tcpMode +
                ", re_use_port=" + re_use_port +
                ", ssrc='" + ssrc + '\'' +
                '}';
    }
}
