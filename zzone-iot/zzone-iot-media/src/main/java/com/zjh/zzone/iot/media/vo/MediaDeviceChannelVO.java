package com.zjh.zzone.iot.media.vo;

import com.ylg.iot.entity.MediaDeviceChannel;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 视频设备通道信息 响应实体
 *
 * @author zjh
 * @since 2025-07-01 16:01
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MediaDeviceChannelVO extends MediaDeviceChannel {
    private static final long serialVersionUID = 1L;

}
