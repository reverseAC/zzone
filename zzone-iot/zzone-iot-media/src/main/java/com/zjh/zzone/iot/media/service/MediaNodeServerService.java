package com.zjh.zzone.iot.media.service;

import com.ylg.iot.vo.MediaInfo;
import com.ylg.iot.media.bo.SendRtpInfo;
import com.ylg.iot.entity.MediaServer;

import java.util.List;

/**
 * 流媒体服务器 服务接口
 *
 * @author zjh
 * @since 2025-04-01 19:23
 */
public interface MediaNodeServerService {

    int createRTPServer(MediaServer mediaServer, String streamId, long ssrc, Integer port, Boolean onlyAuto, Boolean disableAudio, Boolean reUsePort, Integer tcpMode);

    void closeRtpServer(MediaServer mediaServer, String streamId);

    void closeStreams(MediaServer mediaServer, String app, String stream);

    Boolean updateRtpServerSSRC(MediaServer mediaServer, String stream, String ssrc);

    void online(MediaServer mediaServer);

    boolean stopSendRtp(MediaServer mediaInfo, String app, String stream, String ssrc);

    boolean deleteRecordDirectory(MediaServer mediaServer, String app, String stream, String date, String fileName);

    Boolean connectRtpServer(MediaServer mediaServer, String address, int port, String stream);

    void getSnap(MediaServer mediaServer, String streamUrl, int timeoutSec, int expireSec, String path, String fileName);


    /**
     * 获取视频信息
     *
     * @param mediaServer 流媒体服务
     * @param app 应用名
     * @param stream 流id
     * @return 视频信息
     */
    MediaInfo getMediaInfo(MediaServer mediaServer, String app, String stream);

    Boolean pauseRtpCheck(MediaServer mediaServer, String streamKey);

    Boolean resumeRtpCheck(MediaServer mediaServer, String streamKey);

    Integer startSendRtpPassive(MediaServer mediaServer, SendRtpInfo sendRtpItem, Integer timeout);

    void startSendRtpStream(MediaServer mediaServer, SendRtpInfo sendRtpItem);

    Long updateDownloadProcess(MediaServer mediaServer, String app, String stream);

    void loadMP4File(MediaServer mediaServer, String app, String stream, String datePath);

    /**
     * zlm开启中的RTP Server
     *
     * @param mediaServer 流媒体服务
     * @return RTP Server id
     */
    List<String> listRtpServer(MediaServer mediaServer);

}
