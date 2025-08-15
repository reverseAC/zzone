package com.zjh.zzone.iot.media.event.media;

import com.ylg.iot.entity.CloudRecord;
import com.ylg.iot.media.bo.HookRecordInfo;
import com.ylg.iot.entity.MediaServer;
import com.ylg.iot.media.gb28181.event.MediaEvent;
import com.ylg.iot.media.zlm.hook.params.OnRecordMp4HookParam;
import com.ylg.iot.util.MediaServerUtils;

import java.util.Map;

/**
 * 录像文件生成事件
 */
public class MediaRecordMp4Event extends MediaEvent {
    public MediaRecordMp4Event(Object source) {
        super(source);
    }

    private HookRecordInfo recordInfo;

    public static MediaRecordMp4Event getInstance(Object source, OnRecordMp4HookParam hookParam, MediaServer mediaServer){
        MediaRecordMp4Event mediaRecordMp4Event = new MediaRecordMp4Event(source);
        mediaRecordMp4Event.setApp(hookParam.getApp());
        mediaRecordMp4Event.setStream(hookParam.getStream());
        HookRecordInfo recordInfo = HookRecordInfo.getInstance(hookParam);
        mediaRecordMp4Event.setRecordInfo(recordInfo);
        mediaRecordMp4Event.setMediaServer(mediaServer);
        return mediaRecordMp4Event;
    }

    public HookRecordInfo getRecordInfo() {
        return recordInfo;
    }

    public void setRecordInfo(HookRecordInfo recordInfo) {
        this.recordInfo = recordInfo;
    }

    public CloudRecord getCloudRecord() {
        CloudRecord cloudRecordItem = new CloudRecord();
        cloudRecordItem.setApp(this.getApp());
        cloudRecordItem.setStream(this.getStream());
        cloudRecordItem.setStartTime(this.getRecordInfo().getStartTime() * 1000);
        cloudRecordItem.setFileName(this.getRecordInfo().getFileName());
        cloudRecordItem.setFolder(this.getRecordInfo().getFolder());
        cloudRecordItem.setFileSize(this.getRecordInfo().getFileSize());
        cloudRecordItem.setFilePath(this.getRecordInfo().getFilePath());
        cloudRecordItem.setMediaServerId(this.getMediaServer().getServerId());
        cloudRecordItem.setTimeLen(this.getRecordInfo().getTimeLen() * 1000);
        cloudRecordItem.setEndTime((this.getRecordInfo().getStartTime() + (long)this.getRecordInfo().getTimeLen()) * 1000);
        Map<String, String> paramsMap = MediaServerUtils.urlParamToMap(this.getRecordInfo().getParams());
        if (paramsMap.get("callId") != null) {
            cloudRecordItem.setCallId(paramsMap.get("callId"));
        }
        return cloudRecordItem;
    }

}
