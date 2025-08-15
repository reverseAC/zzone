package com.zjh.zzone.iot.media.contant;

import lombok.AllArgsConstructor;

/**
 * 流传输模式 枚举
 *
 * @author zjh
 * @since 2025-04-01 16:05
 */
@AllArgsConstructor
public enum StreamModeType {

    // UDP
    UDP(0),
    // TCP被动模式
    TCP_PASSIVE(1),
    // TCP主动模式
    TCP_ACTIVE(2);

    private final int value;

    public int value() {
        return value;
    }
}
