package com.zjh.zzone.iot.media.service;

import com.ylg.iot.media.bo.AuthenticatePlayResult;
import com.ylg.iot.media.bo.ResultForOnPublish;
import com.ylg.iot.entity.MediaServer;

/**
 * 媒体信息业务
 */
public interface MediaService {

    /**
     * 播放鉴权
     *
     * @param app 应用名
     * @param stream 流id
     * @param callId 会话id
     * @return 鉴权结果
     */
    AuthenticatePlayResult authenticatePlay(MediaServer mediaServer, String app, String stream, String callId);

    /**
     * 推流鉴权
     *
     * @param mediaServer 流媒体服务器
     * @param app 应用名
     * @param stream 流id
     * @param params 其他参数
     * @return 鉴权结果
     */
    ResultForOnPublish authenticatePublish(MediaServer mediaServer, String app, String stream, String params);

    /**
     * 判断是否关闭无人观看的流
     *
     * @param mediaServerId 流媒体服务器id
     * @param app 应用名
     * @param stream 流id
     * @param schema 协议
     * @return 是否关闭
     */
    boolean closeStreamOnNoneReader(String mediaServerId, String app, String stream, String schema);
}
