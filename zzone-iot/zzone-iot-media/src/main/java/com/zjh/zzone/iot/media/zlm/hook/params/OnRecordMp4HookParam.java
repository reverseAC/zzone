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
public class OnRecordMp4HookParam extends HookParam {
    /**
     * 录制的流应用名
     */
    private String app;
    /**
     * 录制的流 ID
     */
    private String stream;
    /**
     * 文件名
     */
    private String file_name;
    /**
     * 文件绝对路径
     */
    private String file_path;
    /**
     * 文件大小，单位字节
     */
    private long file_size;
    /**
     * 文件所在目录路径
     */
    private String folder;
    /**
     * http/rtsp/rtmp 点播相对 url 路径
     */
    private String url;
    /**
     * 流虚拟主机
     */
    private String vhost;
    /**
     * 开始录制时间戳
     */
    private long start_time;
    /**
     * 录制时长，单位秒
     */
    private double time_len;
    /**
     * 其他参数 TODO 确定有无
     */
    private String params;

    @Override
    public String toString() {
        return "OnRecordMp4HookParam{" +
                "app='" + app + '\'' +
                ", stream='" + stream + '\'' +
                ", file_name='" + file_name + '\'' +
                ", file_path='" + file_path + '\'' +
                ", file_size='" + file_size + '\'' +
                ", folder='" + folder + '\'' +
                ", url='" + url + '\'' +
                ", vhost='" + vhost + '\'' +
                ", start_time=" + start_time +
                ", time_len=" + time_len +
                ", params=" + params +
                '}';
    }
}
