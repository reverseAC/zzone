package com.zjh.zzone.iot.media.callback;

import com.ylg.iot.entity.MediaServer;
import com.ylg.iot.media.bo.HookRecordInfo;
import com.ylg.iot.media.event.media.MediaArrivalEvent;
import com.ylg.iot.media.event.media.MediaPublishEvent;
import com.ylg.iot.media.event.media.MediaRecordMp4Event;
import com.ylg.iot.media.gb28181.event.MediaEvent;
import com.ylg.iot.vo.MediaInfo;
import lombok.Data;

/**
 * Hook返回的内容
 */
@Data
public class HookData {
    /**
     * 应用名
     */
    private String app;
    /**
     * 流ID
     */
    private String stream;
    /**
     * 流媒体节点
     */
    private MediaServer mediaServer;
    /**
     * 协议
     */
    private String schema;

    /**
     * 流信息
     */
    private MediaInfo mediaInfo;

    /**
     * 录像信息
     */
    private HookRecordInfo recordInfo;

    /**
     * 推流的额外参数
     */
    private String params;

    public static HookData getInstance(MediaEvent mediaEvent) {
        HookData hookData = new HookData();
        if (mediaEvent instanceof MediaPublishEvent) {
            MediaPublishEvent event = (MediaPublishEvent) mediaEvent;
            hookData.setApp(event.getApp());
            hookData.setStream(event.getStream());
            hookData.setSchema(event.getSchema());
            hookData.setMediaServer(event.getMediaServer());
            hookData.setParams(event.getParams());
        } else if (mediaEvent instanceof MediaArrivalEvent) {
            MediaArrivalEvent event = (MediaArrivalEvent) mediaEvent;
            hookData.setApp(event.getApp());
            hookData.setStream(event.getStream());
            hookData.setSchema(event.getSchema());
            hookData.setMediaServer(event.getMediaServer());
            hookData.setMediaInfo(event.getMediaInfo());
        } else if (mediaEvent instanceof MediaRecordMp4Event) {
            MediaRecordMp4Event event = (MediaRecordMp4Event) mediaEvent;
            hookData.setApp(event.getApp());
            hookData.setStream(event.getStream());
            hookData.setSchema(event.getSchema());
            hookData.setMediaServer(event.getMediaServer());
            hookData.setRecordInfo(event.getRecordInfo());
        } else {
            hookData.setApp(mediaEvent.getApp());
            hookData.setStream(mediaEvent.getStream());
            hookData.setSchema(mediaEvent.getSchema());
            hookData.setMediaServer(mediaEvent.getMediaServer());
        }
        return hookData;
    }
}
