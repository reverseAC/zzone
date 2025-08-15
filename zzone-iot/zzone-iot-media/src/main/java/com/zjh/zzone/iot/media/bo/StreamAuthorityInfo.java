package com.zjh.zzone.iot.media.bo;

import com.ylg.iot.media.event.media.MediaArrivalEvent;
import lombok.Getter;
import lombok.Setter;

/**
 * 流的鉴权信息
 *
 * @author zjh
 * @date 2025-07-03 11:13
 */
@Getter
@Setter
public class StreamAuthorityInfo {
    /**
     * id
     */
    private String id;
    /**
     * 应用名
     */
    private String app;
    /**
     * 流id
     */
    private String stream;
    /**
     * 产生源类型，
     * unknown = 0,
     * rtmp_push=1,
     * rtsp_push=2,
     * rtp_push=3,
     * pull=4,
     * ffmpeg_pull=5,
     * mp4_vod=6,
     * device_chn=7
     */
    private int originType;
    /**
     * 产生源类型的字符串描述
     */
    private String originTypeStr;
    /**
     * 推流时自定义的播放鉴权ID
     */
    private String callId;
    /**
     * 推流的鉴权签名
     */
    private String sign;


    public static StreamAuthorityInfo getInstanceByHook(String app, String stream, String id) {
        StreamAuthorityInfo streamAuthorityInfo = new StreamAuthorityInfo();
        streamAuthorityInfo.setApp(app);
        streamAuthorityInfo.setStream(stream);
        streamAuthorityInfo.setId(id);
        return streamAuthorityInfo;
    }

    public static StreamAuthorityInfo getInstanceByHook(MediaArrivalEvent event) {
        StreamAuthorityInfo streamAuthorityInfo = new StreamAuthorityInfo();
        streamAuthorityInfo.setApp(event.getApp());
        streamAuthorityInfo.setStream(event.getStream());
        streamAuthorityInfo.setId(event.getMediaServer().getServerId());
        if (event.getMediaInfo() != null) {
            streamAuthorityInfo.setOriginType(event.getMediaInfo().getOriginType());
        }

        return streamAuthorityInfo;
    }
}
