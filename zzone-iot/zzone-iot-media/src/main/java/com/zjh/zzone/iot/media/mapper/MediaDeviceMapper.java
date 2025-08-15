package com.zjh.zzone.iot.media.mapper;

import com.ylg.iot.entity.MediaDevice;
import com.ylg.iot.media.vo.MediaDeviceVO;
import com.ylg.mybatis.base.YlgBaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 设备媒体信息 Mapper接口
 *
 * @author zjh
 * @since 2025-03-31 10:19
 */
@Mapper
public interface MediaDeviceMapper extends YlgBaseMapper<MediaDevice> {

    /**
     * 根据国标id查询设备媒体信息
     *
     * @param gbId 设备国标id
     * @return MediaDeviceVO
     */
    MediaDeviceVO selectByGbId(@Param("gbId") String gbId);

    /**
     * 根据通道国标id查询设备媒体信息
     *
     * @param channelGbId 设备国标id
     * @return MediaDeviceVO
     */
    MediaDeviceVO selectByChannelGbId(@Param("channelGbId") String channelGbId);

    /**
     * 根据在线状态查询设备媒体信息
     * @param status 在线状态
     * @return List<DeviceMediaInfo>
     */
    List<MediaDeviceVO> getByOnline(String status);
}
