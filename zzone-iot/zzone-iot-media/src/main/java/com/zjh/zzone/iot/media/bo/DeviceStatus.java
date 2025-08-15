package com.zjh.zzone.iot.media.bo;

import lombok.Getter;
import lombok.Setter;

/**
 * 设备状态(SIP:MESSAGE:DeviceStatus上报)
 *
 * @author zjh
 * @date 2025-07-29 18:56
 */
@Getter
@Setter
public class DeviceStatus {
    /**
     * DeviceID
     */
    private String deviceId;
    /**
     * 在线状态
     */
    private String online;
    /**
     * 录像状态
     */
    private String record;
}
