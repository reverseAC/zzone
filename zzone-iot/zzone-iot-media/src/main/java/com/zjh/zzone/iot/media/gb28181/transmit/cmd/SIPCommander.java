package com.zjh.zzone.iot.media.gb28181.transmit.cmd;

import com.ylg.iot.media.bo.*;
import com.ylg.iot.media.callback.ErrorCallback;
import com.ylg.iot.media.gb28181.common.SsrcTransactionNotFoundException;
import com.ylg.iot.entity.MediaServer;
import com.ylg.iot.entity.MediaDeviceChannel;
import com.ylg.iot.entity.MediaDevice;
import com.ylg.iot.media.callback.SipSubscribe;
import com.ylg.iot.media.callback.HookSubscribe;

import javax.sip.InvalidArgumentException;
import javax.sip.SipException;
import java.text.ParseException;

/**
 * 平台下发请求（播放请求、设备控制、查询信息）
 *
 * @author zjh
 * @since 2025-03-26 11:35
 */
public interface SIPCommander {

    /**
     * 云台控制，支持方向与缩放控制
     *
     * @param device  控制设备
     * @param channelId  预览通道
     * @param leftRight  镜头左移右移 0:停止 1:左移 2:右移
     * @param upDown     镜头上移下移 0:停止 1:上移 2:下移
     * @param inOut      镜头放大缩小 0:停止 1:缩小 2:放大
     * @param moveSpeed  镜头移动速度
     * @param zoomSpeed  镜头缩放速度
     */
    void ptzCmd(MediaDevice device, String channelId, int leftRight, int upDown, int inOut, int moveSpeed,
                int zoomSpeed) throws InvalidArgumentException, SipException, ParseException;

    /**
     * 前端控制，包括PTZ指令、FI指令、预置位指令、巡航指令、扫描指令和辅助开关指令
     *
     * @param device  		控制设备
     * @param channelId		预览通道
     * @param cmdCode		指令码
     * @param parameter1	数据1
     * @param parameter2	数据2
     * @param combineCode2	组合码2
     */
    void frontEndCmd(MediaDevice device, String channelId, int cmdCode, int parameter1, int parameter2, int combineCode2) throws SipException, InvalidArgumentException, ParseException;

    /**
     * 请求预览视频流
     * @param device  视频设备
     * @param channel  预览通道
     */
    void playStreamCmd(MediaServer mediaServerItem, SSRCInfo ssrcInfo, MediaDevice device, MediaDeviceChannel channel,
                       SipSubscribe.Event okEvent, SipSubscribe.Event errorEvent, Long timeout)
            throws InvalidArgumentException, SipException, ParseException;

    /**
     * 视频流停止:
     */
    void streamByeCmd(MediaDevice device, String channelId, String app, String stream, String callId, SipSubscribe.Event okEvent) throws InvalidArgumentException, SipException, ParseException, SsrcTransactionNotFoundException;

    void streamByeCmd(MediaDevice device, String channelId, SipTransactionInfo sipTransactionInfo, SipSubscribe.Event okEvent) throws InvalidArgumentException, SipException, ParseException, SsrcTransactionNotFoundException;

    void streamByeCmdForDeviceInvite(MediaDevice device, String channelId, SipTransactionInfo sipTransactionInfo, SipSubscribe.Event okEvent) throws InvalidArgumentException, SipException, ParseException, SsrcTransactionNotFoundException;


    /**
     * 请求回放视频流
     *
     * @param device  视频设备
     * @param channel  预览通道
     * @param startTime 开始时间,格式要求：yyyy-MM-dd HH:mm:ss
     * @param endTime 结束时间,格式要求：yyyy-MM-dd HH:mm:ss
     */
    void playbackStreamCmd(MediaServer mediaServerItem, SSRCInfo ssrcInf, MediaDevice device, MediaDeviceChannel channel, String startTime, String endTime, SipSubscribe.Event okEvent, SipSubscribe.Event errorEvent, Long timeout) throws InvalidArgumentException, SipException, ParseException;

    /**
     * 请求历史媒体下载
     *
     * @param device  视频设备
     * @param channel  预览通道
     * @param startTime 开始时间,格式要求：yyyy-MM-dd HH:mm:ss
     * @param endTime 结束时间,格式要求：yyyy-MM-dd HH:mm:ss
     * @param downloadSpeed 下载倍速参数
     */
    void downloadStreamCmd(MediaServer mediaServerItem, SSRCInfo ssrcInfo, MediaDevice device, MediaDeviceChannel channel,
                           String startTime, String endTime, int downloadSpeed,
                           SipSubscribe.Event errorEvent, SipSubscribe.Event okEvent, Long timeout)
            throws InvalidArgumentException, SipException, ParseException;

    /**
     * 语音喊话
     *
     * @param device 视频设备
     */
    void talkStreamCmd(MediaServer mediaServerItem, SendRtpInfo sendRtpItem, MediaDevice device, MediaDeviceChannel channelId, String callId, HookSubscribe.Event event, HookSubscribe.Event eventForPush, SipSubscribe.Event okEvent, SipSubscribe.Event errorEvent, Long timeout) throws InvalidArgumentException, SipException, ParseException;


    /**
     * 语音广播
     *
     * @param device 视频设备
     */
    void audioBroadcastCmd(MediaDevice device, String channelId, SipSubscribe.Event okEvent, SipSubscribe.Event errorEvent) throws InvalidArgumentException, SipException, ParseException;

    /**
     * 回放暂停
     */
    void playPauseCmd(MediaDevice device, MediaDeviceChannel channel, StreamDetail streamInfo) throws InvalidArgumentException, ParseException, SipException;

    /**
     * 回放恢复
     */
    void playResumeCmd(MediaDevice device, MediaDeviceChannel channel, StreamDetail streamInfo) throws InvalidArgumentException, ParseException, SipException;

    /**
     * 回放拖动播放
     */
    void playSeekCmd(MediaDevice device, MediaDeviceChannel channel, StreamDetail streamInfo, long seekTime) throws InvalidArgumentException, ParseException, SipException;

    /**
     * 回放倍速播放
     */
    void playSpeedCmd(MediaDevice device, MediaDeviceChannel channel, StreamDetail streamInfo, Double speed) throws InvalidArgumentException, ParseException, SipException;

    /**
     * 回放控制
     * @param device
     * @param streamInfo
     * @param content
     */
    void playbackControlCmd(MediaDevice device, MediaDeviceChannel channel, StreamDetail streamInfo, String content, SipSubscribe.Event errorEvent, SipSubscribe.Event okEvent) throws SipException, InvalidArgumentException, ParseException;

    void playbackControlCmd(MediaDevice device, MediaDeviceChannel channel, String stream,
                            String content, SipSubscribe.Event errorEvent, SipSubscribe.Event okEvent) throws SipException, InvalidArgumentException, ParseException;

    /**
     * 音视频录像控制
     *
     * @param device  		视频设备
     * @param channelId  	预览通道
     * @param recordCmdStr	录像命令：Record / StopRecord
     */
    void recordCmd(MediaDevice device, String channelId, String recordCmdStr, ErrorCallback<String> callback) throws InvalidArgumentException, SipException, ParseException;

    /**
     * 查询设备状态
     *
     * @param device 视频设备
     */
    void deviceStatusQuery(MediaDevice device, MediaDeviceChannel channel, ErrorCallback<DeviceStatus> callback) throws InvalidArgumentException, SipException, ParseException;

    /**
     * 查询设备信息
     *
     * @param device 视频设备
     */
    void deviceInfoQuery(MediaDevice device) throws InvalidArgumentException, SipException, ParseException;

    /**
     * 查询目录列表
     *
     * @param device 视频设备
     */
    void catalogQuery(MediaDevice device, int sn, SipSubscribe.Event errorEvent) throws SipException, InvalidArgumentException, ParseException;

    /**
     * 查询录像信息
     *
     * @param device 视频设备
     * @param channelId 通道编码
     * @param startTime 开始时间,格式要求：yyyy-MM-dd HH:mm:ss
     * @param endTime 结束时间,格式要求：yyyy-MM-dd HH:mm:ss
     * @param seq 查询序号
     */
    void recordInfoQuery(MediaDevice device, String channelId, String startTime, String endTime, int seq, Integer Secrecy, String type, SipSubscribe.Event okEvent, SipSubscribe.Event errorEvent) throws InvalidArgumentException, SipException, ParseException;

    /**
     * 查询设备配置
     *
     * @param device 		视频设备
     * @param channelId		通道编码（可选）
     * @param configType	配置类型：
     */
    void deviceConfigQuery(MediaDevice device, String channelId, String configType, SipSubscribe.Event errorEvent) throws InvalidArgumentException, SipException, ParseException;
}
