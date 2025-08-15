package com.zjh.zzone.iot.media.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ylg.iot.media.callback.ErrorCallback;
import com.ylg.iot.media.bo.SipTransactionInfo;
import com.ylg.iot.entity.MediaDevice;
import com.ylg.iot.media.vo.MediaDeviceVO;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 设备媒体信息
 *
 * @author zjh
 * @since 2025-03-28 10:14
 */
public interface MediaDeviceService extends IService<MediaDevice> {

    /**
     * 设备上线
     *
     * @param device 设备信息
     * @param sipTransactionInfo SIP事务信息
     */
    void online(MediaDeviceVO device, SipTransactionInfo sipTransactionInfo);

    /**
     * 根据通道国标id查询设备信息
     *
     * @param channelGbId 通道国标id
     * @return 设备信息
     */
    MediaDevice getDeviceByChannelGbId(String channelGbId);

    /**
     * 设备下线
     *
     * @param gbId 设备国标id
     */
    void offline(String gbId, String reason);

    /**
     * 根据设备国标id查询
     *
     * @param gbId 设备id
     * @return DeviceMediaInfo
     */
    MediaDeviceVO getByGbId(String gbId);

    /**
     * 检查设备状态
     *
     * @param device 设备信息
     */
    Boolean getDeviceStatus(MediaDevice device);

    /**
     * 获取所有在线设备
     *
     * @return 设备列表
     */
    List<MediaDeviceVO> getAllOnlineDevice();

    /**
     * 更新设备
     *
     * @param device 设备信息
     */
    void updateDevice(MediaDevice device);

    @Transactional
    void updateDeviceList(List<MediaDevice> deviceList);

    /**
     * 删除设备
     *
     * @param deviceGbId 设备国标编码
     */
    void delete(String deviceGbId);

    /**
     * 通道同步
     * @param device 设备信息
     */
    void sync(MediaDevice device);

    /**
     * 录像控制
     *
     * @param device 设备信息
     * @param channelId 通道国标id
     * @param recordCmdStr 录像命令
     * @param callback 回调
     */
    void record(MediaDevice device, String channelId, String recordCmdStr, ErrorCallback<String> callback);

    /**
     * 修改设备的心跳配置
     */
    void updateDeviceHeartInfo(MediaDevice device);

    /**
     * 校验设备是否启用
     *
     * @param deviceGbId 设备国标id
     */
    void checkDeviceEnable(String deviceGbId);
}