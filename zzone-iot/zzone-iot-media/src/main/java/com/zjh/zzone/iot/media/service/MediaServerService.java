package com.zjh.zzone.iot.media.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ylg.iot.media.bo.SendRtpInfo;
import com.ylg.iot.media.bo.StreamDetail;
import com.ylg.iot.vo.MediaInfo;
import com.ylg.iot.entity.MediaServer;

import java.util.List;

/**
 * 媒体服务节点
 */
public interface MediaServerService extends IService<MediaServer> {
    /**
     * 根据流媒体服务id获取流媒体服务器
     *
     * @param serverId 服务器id
     * @return 流媒体服务器信息
     */
    MediaServer getServerByServerId(String serverId);

    MediaServer getMediaServerForMinimumLoad(Boolean hasAssist);

    void closeRTPServer(MediaServer mediaServerItem, String streamId);

    void closeRTPServer(String mediaServerId, String streamId);

    Boolean updateRtpServerSSRC(MediaServer mediaServerItem, String streamId, String ssrc);

    void clearRTPServer(MediaServer mediaServerItem);

    Integer startSendRtpPassive(MediaServer mediaServer, SendRtpInfo sendRtpItem, Integer timeout);

    void startSendRtp(MediaServer mediaServer, SendRtpInfo sendRtpItem);

    /**
     * 删除录制目录
     * @param mediaServerItem
     * @param app
     * @param stream
     * @param date
     * @param fileName
     * @return
     */
    boolean deleteRecordDirectory(MediaServer mediaServerItem, String app, String stream, String date, String fileName);

    /**
     * 更新流媒体服务信息
     *
     * @param mediaSer 流媒体服务信息
     */
    void updateServer(MediaServer mediaSer);

    void releaseSsrc(String mediaServerItemId, String ssrc);

    /**
     * 清空在线流媒体服务缓存
     */
    void clearMediaServerForOnline();

    /**
     * 新增流媒体服务信息
     *
     * @param mediaSer 流媒体服务信息
     */
    void addServer(MediaServer mediaSer);

    boolean stopSendRtp(MediaServer mediaInfo, String app, String stream, String ssrc);

    Boolean connectRtpServer(MediaServer mediaServerItem, String address, int port, String stream);

    void getSnap(MediaServer mediaServerItemInuse, String streamUrl, int timeoutSec, int expireSec, String path, String fileName);

    Boolean pauseRtpCheck(MediaServer mediaServerItem, String streamKey);

    boolean resumeRtpCheck(MediaServer mediaServerItem, String streamKey);

    /**
     * 根据应用名和流ID获取播放地址, 只是地址拼接
     *
     * @param app
     * @param stream
     * @return
     */
    StreamDetail getStreamInfoByAppAndStream(MediaServer mediaServerItem, String app, String stream, MediaInfo mediaInfo, String callId);

    /**
     * 根据应用名和流ID获取播放地址, 只是地址拼接，返回的ip使用远程访问ip，适用与zlm与wvp在一台主机的情况
     *
     * @param app 应用名
     * @param stream 流id
     * @return 流详情
     */
    StreamDetail getStreamInfoByAppAndStream(MediaServer mediaServer, String app, String stream, MediaInfo mediaInfo, String addr, String callId, boolean isPlay);

    /**
     * 判断流是否就绪
     *
     * @param mediaServer 流媒体服务器
     * @param rtp 协议
     * @param stream 流id
     * @return 是否就绪
     */
    Boolean isStreamReady(MediaServer mediaServer, String rtp, String stream);

    /**
     * 修改下载进度
     *
     * @param mediaServerItem 流媒体服务
     * @param app 应用名
     * @param stream 流id
     * @return 下载进度
     */
    Long updateDownloadProcess(MediaServer mediaServerItem, String app, String stream);

    int createRTPServer(MediaServer mediaServerItem, String streamId, long ssrc, Integer port,
                        boolean onlyAuto, boolean disableAudio, boolean reUsePort, Integer tcpMode);


    StreamDetail loadMP4File(MediaServer mediaServer, String app, String stream, String datePath);


    MediaInfo getMediaInfo(MediaServer mediaServerItem, String app, String stream);


    List<String> listRtpServer(MediaServer mediaServer);
}
