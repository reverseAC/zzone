package com.zjh.zzone.iot.media.service;

import com.ylg.iot.enums.media.InviteSessionType;
import com.ylg.iot.entity.MediaServer;
import com.ylg.iot.entity.MediaDeviceChannel;
import com.ylg.iot.entity.MediaDevice;
import com.ylg.iot.media.bo.*;
import com.ylg.iot.media.callback.ErrorCallback;
import com.ylg.iot.media.gb28181.event.AudioBroadcastEvent;
import com.ylg.iot.media.vo.AudioBroadcastResultVO;
import com.ylg.iot.vo.MediaInfo;
import gov.nist.javax.sip.message.SIPResponse;

import javax.sip.InvalidArgumentException;
import javax.sip.SipException;
import javax.sip.header.CallIdHeader;
import java.text.ParseException;

/**
 * 国标视频点播 服务类接口
 *
 * @author zjh
 * @since 2025-03-25 20:09
 */
public interface PlayService {

    /**
     * 视频点播
     *
     * @param device 设备
     * @param channel 通道
     * @param callback 异常回调
     * @return 视频流标识信息
     */
    SSRCInfo play(MediaDevice device, MediaDeviceChannel channel, ErrorCallback<StreamDetail> callback);

    /**
     * 停止点播
     *
     * @param type 点播类型
     * @param device 设备信息
     * @param channel 通道信息
     * @param stream 流id
     */
    void stop(InviteSessionType type, MediaDevice device, MediaDeviceChannel channel, String stream);

    /**
     * 获取截图
     *
     * @param deviceGbId 设备国标id
     * @param channelGbId 通道国标id
     * @param fileName 文件名
     * @param errorCallback 异常回调
     */
    void getSnap(String deviceGbId, String channelGbId, String fileName, ErrorCallback errorCallback);

    /**
     * 暂停视频播放
     *
     * @param streamId 流id
     * @throws InvalidArgumentException
     * @throws ParseException
     * @throws SipException
     */
    void pauseRtp(String streamId) throws InvalidArgumentException, ParseException, SipException;

    /**
     * 恢复视频播放
     *
     * @param streamId 流id
     * @throws InvalidArgumentException
     * @throws ParseException
     * @throws SipException
     */
    void resumeRtp(String streamId) throws InvalidArgumentException, ParseException, SipException;

    /**
     * 视频回放
     *
     * @param device 设备信息
     * @param channel 通道信息
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param callback 回调
     */
    void playBack(MediaDevice device, MediaDeviceChannel channel, String startTime, String endTime, ErrorCallback<StreamDetail> callback);

    /**
     * 语音广播
     *
     * @param deviceGbId 设备国标id
     * @param channelGbId 通道国标id
     * @param broadcastMode 广播/对讲
     * @return 语音广播结果
     */
    AudioBroadcastResultVO audioBroadcast(String deviceGbId, String channelGbId, Boolean broadcastMode);

    /**
     * 发送语音广播命令
     *
     * @param device 设备国标id
     * @param channel 通道国标id
     * @param mediaServerItem 广播/对讲
     * @return 语音广播结果
     */
    boolean audioBroadcastCmd(MediaDevice device, MediaDeviceChannel channel, MediaServer mediaServerItem, String app, String stream, int timeout, boolean isFromPlatform, AudioBroadcastEvent event) throws InvalidArgumentException, ParseException, SipException;

    /**
     * 停止语音广播
     *
     * @param device 设备信息
     * @param channel 通道信息
     */
    void stopAudioBroadcast(MediaDevice device, MediaDeviceChannel channel);

    /**
     * 语音对讲
     *
     * @param device 设备信息
     * @param channel 通道信息
     * @param mediaServerItem 流媒体服务器
     * @param stream 流id
     * @param event 事件
     */
    void talkCmd(MediaDevice device, MediaDeviceChannel channel, MediaServer mediaServerItem, String stream, AudioBroadcastEvent event);

    /**
     * 停止对讲
     * @param device 设备信息
     * @param channel 通道信息
     * @param streamIsReady 流是否准备好
     */
    void stopTalk(MediaDevice device, MediaDeviceChannel channel, Boolean streamIsReady);

    /**
     * 下载录像文件
     *
     * @param device 设备信息
     * @param channel 通道信息
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param downloadSpeed 下载速度
     * @param callback 异常回调
     */
    void download(MediaDevice device, MediaDeviceChannel channel, String startTime, String endTime, int downloadSpeed, ErrorCallback<StreamDetail> callback);

    /**
     * 获取下载进度
     *
     * @param device 设备信息
     * @param channel 通道信息
     * @param stream 流id
     * @return 流信息
     */
    StreamDetail getDownLoadInfo(MediaDevice device, MediaDeviceChannel channel, String stream);

    /**
     * 流媒体服务上线
     *
     * @param mediaServer 流媒体服务信息
     */
    void zlmServerOnline(MediaServer mediaServer);
    /**
     * 流媒体服务离线
     *
     * @param mediaServer 流媒体服务信息
     */
    void zlmServerOffline(MediaServer mediaServer);

    /**
     * 获取一个流媒体服务器
     *
     * @param device 设备信息
     * @return 流媒体服务器
     */
    MediaServer getAMediaServerItem(MediaDevice device);

    /**
     * 推流失败处理
     *
     * @param sendRtpItem 推流信息
     * @param platform 平台
     * @param callIdHeader sip消息头
     */
    void startSendRtpStreamFailHand(SendRtpInfo sendRtpItem, Platform platform, CallIdHeader callIdHeader);

    /**
     * 通过zlm的on_publish事件获取流信息
     *
     * @param mediaServerItem 流媒体服务器
     * @param mediaInfo 流媒体信息
     * @param device 设备信息
     * @param channel 通道信息
     * @return 流信息
     */
    StreamDetail onPublishHandlerForPlay(MediaServer mediaServerItem, MediaInfo mediaInfo, MediaDevice device, MediaDeviceChannel channel);

    void startPushStream(SendRtpInfo sendRtpItem, MediaDeviceChannel channel, SIPResponse sipResponse, Platform platform, CallIdHeader callIdHeader);
}
