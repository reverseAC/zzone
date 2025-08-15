package com.zjh.zzone.iot.media.vo;

import com.ylg.iot.vo.StreamContent;
import lombok.Getter;
import lombok.Setter;

/**
 * @author lin
 */
@Getter
@Setter
public class AudioBroadcastResultVO {
    /**
     * 推流的各个方式流地址
     */
    private StreamContent streamInfo;

    /**
     * 编码格式
     */
    private String codec;

    /**
     * 向zlm推流的应用名
     */
    private String app;

    /**
     * 向zlm推流的流ID
     */
    private String stream;

}
