package com.zjh.zzone.iot.media.vo;

import com.ylg.iot.entity.MediaDevice;
import com.ylg.iot.media.bo.SipTransactionInfo;
import lombok.Getter;
import lombok.Setter;

/**
 * 视频设备信息 响应实体
 *
 * @author zjh
 * @since 2025-07-01 15:10
 */
@Getter
@Setter
public class MediaDeviceVO extends MediaDevice {

    /**
     * SIP事务信息
     */
    private SipTransactionInfo sipTransactionInfo;

    /**
     * 通道个数
     */
    private int channelCount;
}
