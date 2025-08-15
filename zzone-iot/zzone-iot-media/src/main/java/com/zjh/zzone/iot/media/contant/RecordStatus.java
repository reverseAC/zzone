package com.zjh.zzone.iot.media.contant;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 录像状态 枚举
 *
 * @author zjh
 * @since 2025-07-29 19:25
 */
@Getter
@AllArgsConstructor
public enum RecordStatus {

    RECORD("ON", "录像中"),
    STOP_RECORD("OFF", "停止录像");

    private final String code;
    private final String desc;

    public String value() {
        return code;
    }

    public static RecordStatus getByCode(String code) {
        for (RecordStatus recordStatus : values()) {
            if (recordStatus.code.equals(code)) {
                return recordStatus;
            }
        }
        return null;
    }
}
