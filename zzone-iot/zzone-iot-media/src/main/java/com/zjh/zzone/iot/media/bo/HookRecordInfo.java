package com.zjh.zzone.iot.media.bo;

import com.ylg.iot.media.zlm.hook.params.OnRecordMp4HookParam;
import lombok.Getter;
import lombok.Setter;

/**
 * zlm hook发送的录像信息的按需获取和驼峰转换
 *
 * @author zjh
 */
@Getter
@Setter
public class HookRecordInfo {
    private String fileName;
    private String filePath;
    private long fileSize;
    private String folder;
    private String url;
    private long startTime;
    private double timeLen;
    private String params;

    public static HookRecordInfo getInstance(OnRecordMp4HookParam hookParam) {
        HookRecordInfo recordInfo = new HookRecordInfo();
        recordInfo.setFileName(hookParam.getFile_name());
        recordInfo.setUrl(hookParam.getUrl());
        recordInfo.setFolder(hookParam.getFolder());
        recordInfo.setFilePath(hookParam.getFile_path());
        recordInfo.setFileSize(hookParam.getFile_size());
        recordInfo.setStartTime(hookParam.getStart_time());
        recordInfo.setTimeLen(hookParam.getTime_len());
        return recordInfo;
    }

    @Override
    public String toString() {
        return "RecordInfo{" +
                "文件名称='" + fileName + '\'' +
                ", 文件路径='" + filePath + '\'' +
                ", 文件大小=" + fileSize +
                ", 开始时间=" + startTime +
                ", 时长=" + timeLen +
                ", params=" + params +
                '}';
    }
}
