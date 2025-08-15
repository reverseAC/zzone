package com.zjh.zzone.iot.media.contant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 视频分享时长单位
 *
 * @author zjh
 * @date 2025-07-24 16:34
 */
@Getter
@RequiredArgsConstructor
public enum RecordShareDurationUnit {

    DAY("0", "天") {
        @Override
        public Long getTimeMills(int duration) {
            return duration * 24 * 60 * 60 * 1000L;
        }
    },
    HOUR("1", "小时") {
        @Override
        public Long getTimeMills(int duration) {
            return duration * 60 * 60 * 1000L;
        }
    },
    MINUTE("2", "分钟") {
        @Override
        public Long getTimeMills(int duration) {
            return duration * 60 * 1000L;
        }
    };

    private final String code;
    private final String desc;

    public static RecordShareDurationUnit getByCode(String code) {
        for (RecordShareDurationUnit unit : values()) {
            if (unit.getCode().equals(code)) {
                return unit;
            }
        }
        return null;
    }

    public abstract Long getTimeMills(int duration);

}
