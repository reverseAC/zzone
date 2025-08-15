package com.zjh.zzone.iot.media.zlm.hook.params;

/**
 * zlm hook事件中的on_send_rtp_stopped事件的参数
 *
 * @author zjh
 * @since 2025-07-02 15:48
 */
public class OnSendRtpStoppedHookParam extends HookParam {
    private String app;
    private String stream;


    public String getApp() {
        return app;
    }

    public void setApp(String app) {
        this.app = app;
    }

    public String getStream() {
        return stream;
    }

    public void setStream(String stream) {
        this.stream = stream;
    }

    @Override
    public String toString() {
        return "OnSendRtpStoppedHookParam{" +
                "app='" + app + '\'' +
                ", stream='" + stream + '\'' +
                '}';
    }
}
