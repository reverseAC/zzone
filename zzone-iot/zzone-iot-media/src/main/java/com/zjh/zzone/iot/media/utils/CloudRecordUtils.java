package com.zjh.zzone.iot.media.utils;

import com.ylg.iot.entity.MediaServer;
import com.ylg.iot.vo.DownloadFileInfo;

public class CloudRecordUtils {

    public static DownloadFileInfo getDownloadFilePath(MediaServer mediaServerItem, String filePath) {
        DownloadFileInfo downloadFileInfo = new DownloadFileInfo();

        String pathTemplate = "%s://%s:%s/index/api/downloadFile?file_path=" + filePath;

        downloadFileInfo.setHttpPath(String.format(pathTemplate, "http", mediaServerItem.getStreamIp(),
                mediaServerItem.getHttpPort()));

        if (mediaServerItem.getHttpSSlPort() > 0) {
            downloadFileInfo.setHttpsPath(String.format(pathTemplate, "https", mediaServerItem.getStreamIp(),
                    mediaServerItem.getHttpSSlPort()));
        }
        return downloadFileInfo;
    }
}
