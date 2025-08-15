package com.zjh.zzone.iot.media.bo;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResultForOnPublish {

    /**
     * 是否开启音频
     */
    private boolean enable_audio;
    /**
     * 是否允许 mp4 录制
     */
    private boolean enable_mp4;
    /**
     * mp4 录制切片大小，单位秒
     */
    private int mp4_max_second;
    /**
     * mp4 录制文件保存根目录，置空使用默认
     */
    private String mp4_save_path;
    /**
     * 是否修改流 id, 通过此参数可以自定义流 id(譬如替换 ssrc)
     */
    private String stream_replace;
    /**
     * 该流是否开启时间戳覆盖(0:绝对时间戳/1:系统时间戳/2:相对时间戳)
     */
    private Integer modify_stamp;

}
