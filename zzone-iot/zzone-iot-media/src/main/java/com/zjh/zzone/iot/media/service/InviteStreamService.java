package com.zjh.zzone.iot.media.service;

import com.ylg.iot.media.bo.StreamDetail;
import com.ylg.iot.enums.media.InviteSessionType;
import com.ylg.iot.media.bo.InviteInfo;
import com.ylg.iot.media.callback.ErrorCallback;

import java.util.List;

/**
 * 点播状态管理 服务接口
 *
 * @author zjh
 * @since 2025-03-31 13:46
 */
public interface InviteStreamService {

    /**
     * 获取通道或流指定类型的点播状态信息
     *
     * @param type 点播类型
     * @param channelId 通道id
     * @param stream 流id
     * @return 点播状态信息
     */
    InviteInfo getInviteInfo(InviteSessionType type, Long channelId, String stream);

    /**
     * 获取通道指定类型的点播状态信息
     *
     * @param type 点播类型
     * @param channelId 通道id
     * @return 点播状态信息
     */
    InviteInfo getInviteInfoByDeviceAndChannel(InviteSessionType type, Long channelId);

    /**
     * 获取流指定类型的点播状态信息
     *
     * @param type 点播类型
     * @param stream 流id
     * @return 点播状态信息
     */
    InviteInfo getInviteInfoByStream(InviteSessionType type, String stream);

    /**
     * 获取所有点播状态信息
     *
     * @return 点播状态信息
     */
    List<InviteInfo> getAllInviteInfo();

    /**
     * 更新点播的状态信息
     *
     * @param inviteInfo 点播状态信息
     */
    void updateInviteInfo(InviteInfo inviteInfo);

    /**
     * 更新点播的状态信息
     *
     * @param inviteInfo 点播状态信息
     * @param time 有效时间
     */
    void updateInviteInfo(InviteInfo inviteInfo, Long time);

    /**
     * 移除通道或流指定类型的点播状态信息
     *
     * @param type 点播类型
     * @param channelId 通道id
     * @param stream 流id
     */
    void removeInviteInfo(InviteSessionType type, Long channelId, String stream);

    /**
     * 移除通道指定类型的点播状态信息
     *
     * @param inviteSessionType 点播类型
     * @param channelId 通道id
     */
    void removeInviteInfoByDeviceAndChannel(InviteSessionType inviteSessionType, Long channelId);

    /**
     * 移除指定点播状态信息
     *
     * @param inviteInfo 点播状态信息
     */
    void removeInviteInfo(InviteInfo inviteInfo);

    /**
     * 清空一个设备的所有invite信息
     *
     * @param deviceGbId 设备国标id
     */
    void clearInviteInfo(String deviceGbId);

    /**
     * 添加一个invite回调
     */
    void addCallback(InviteSessionType type, Long channelId, String stream, ErrorCallback<StreamDetail> callback);

    /**
     * 调用一个invite回调
     */
    void runCallback(InviteSessionType type, Long channelId, String stream, int code, String msg, StreamDetail data);

    /**
     * 获取MediaServer下的流信息
     */
    InviteInfo getInviteInfoBySSRC(String ssrc);

    /**
     * 更新ssrc
     */
    InviteInfo updateInviteInfoForSSRC(InviteInfo inviteInfo, String ssrcInResponse);
}
