package com.zjh.zzone.iot.media.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ylg.iot.media.callback.ErrorCallback;
import com.ylg.iot.vo.RecordInfo;
import com.ylg.iot.entity.MediaDevice;
import com.ylg.iot.entity.MediaDeviceChannel;

import java.util.List;

/**
 * 国标通道业务类
 * @author lin
 */
public interface MediaDeviceChannelService extends IService<MediaDeviceChannel> {

    /**
     * 批量添加设备通道
     *
     * @param device 设备信息
     * @param channels 通道列表
     */
    void updateChannels(MediaDevice device, List<MediaDeviceChannel> channels);

    /**
     *  根据国标编码获取通道
     *
     * @param deviceGbId 设备国标编码
     * @param channelGbId 通道国标编码
     * @return 通道信息
     */
    MediaDeviceChannel getByGbId(String deviceGbId, String channelGbId);

    /**
     * 获取设备的指定通道
     *
     * @param parentId 父节点id
     * @param channelGbId 国标id
     * @return 通道信息
     */
    MediaDeviceChannel getByGbId(Long parentId, String channelGbId);

    /**
     * 修改通道的码流类型
     *
     * @param channel 通道信息
     */
    void updateChannelStreamIdentification(MediaDeviceChannel channel);

    /**
     * 开始点播
     *
     * @param channelId 通道id
     * @param stream 流id
     */
    void startPlay(Long channelId, String stream);

    /**
     * 停止点播
     *
     * @param channelId 通道id
     */
    void stopPlay(Long channelId);

    /**
     * 重置设备通道
     *
     * @param deviceId 设备id
     * @param deviceChannels 设备通道列表
     * @return 结果
     */
    boolean resetChannels(Long deviceId, List<MediaDeviceChannel> deviceChannels);

    /**
     * 查询设备的通道列表
     *
     * @param deviceGbId 设备国标id
     * @return 设备通道列表信息
     */
    List<MediaDeviceChannel> getDeviceChannels(String deviceGbId);

    /**
     * 查询设备的通道列表
     *
     * @param deviceId 设备id
     * @return 设备通道列表信息
     */
    List<MediaDeviceChannel> getDeviceChannels(Long deviceId);

    /**
     * 获取设备通道的录像状态
     *
     * @param deviceGbId 设备国标编码
     * @param channelGbId 通道国标编码
     */
    void getRecordStatus(String deviceGbId, String channelGbId, ErrorCallback<String> rollback);

    /**
     * 获取设备的广播通道
     *
     * @param deviceDbId 设备id
     * @return 广播通道信息
     */
    MediaDeviceChannel getBroadcastChannel(Long deviceDbId);

    /**
     * 开启/关闭通道的音频
     *
     * @param channelId 通道id
     * @param audio 是否开启音频
     */
    void changeAudio(Long channelId, Boolean audio);

    void updateChannelStatus(MediaDeviceChannel channel);

    void addChannel(MediaDeviceChannel channel);

    void updateChannelForNotify(MediaDeviceChannel channel);

    /**
     * 根据父节点id和通道国标id获取通道
     * @param parentId 父节点id
     * @param gbId 通道国标id
     * @return 通道信息
     */
    MediaDeviceChannel getChannelByGbId(Long parentId, String gbId);

    /**
     * 获取录像文件列表
     *
     * @param device 设备信息
     * @param channel 通道信息
     * @param startTime 开始事件
     * @param endTime 结束时间
     * @param rollback 异常回调
     */
    void queryRecordInfo(MediaDevice device, MediaDeviceChannel channel, String startTime, String endTime, ErrorCallback<RecordInfo> rollback);

    /**
     * 删除设备通道
     *
     * @param deviceId 设备id
     */
    void deleteDeviceChannels(Long deviceId);
}
