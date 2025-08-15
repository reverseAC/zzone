package com.zjh.zzone.iot.media.mapper;

import com.ylg.iot.entity.MediaDeviceChannel;
import com.ylg.iot.media.vo.MediaDeviceChannelVO;
import com.ylg.mybatis.base.YlgBaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 设备媒体通道信息 Mapper接口
 *
 * @author zjh
 * @since 2025-03-31 10:19
 */
@Mapper
public interface MediaDeviceChannelMapper extends YlgBaseMapper<MediaDeviceChannel> {

    /**
     * 根据设备媒体信息id和通道id查询设备媒体通道信息
     *
     * @param deviceMediaId 设备id
     * @param channelId 通道id
     * @return DeviceMediaInfo
     */
    MediaDeviceChannel getByDeviceMediaIdAndId(Long deviceMediaId, Long channelId); // TODO 需要确认是否自动添加删除条件

    /**
     * 修改通道音频状态
     *
     * @param channelId 通道id
     * @param audio 是否开启音频
     */
    void changeAudio(@Param("channelId") int channelId, @Param("audio") boolean audio);

    /**
     * 根据设备id查询通道信息
     *
     * @param parentId 设备id
     * @return 通道信息
     */
    List<MediaDeviceChannelVO> selectByParentId(@Param("parentId") Long parentId);
}
