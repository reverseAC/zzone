package com.zjh.zzone.iot.media.gb28181.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 异常
 *
 * @author zjh
 * @since 2025-04-01 11:16
 */
@Getter
@AllArgsConstructor
public class SsrcTransactionNotFoundException extends Exception{
    private String deviceId;
    private String channelId;
    private String callId;
    private String stream;

    @Override
    public String getMessage() {
        StringBuffer msg = new StringBuffer();
        msg.append(String.format("缓存事务信息未找到，device：%s channel: %s ",  deviceId, channelId));
        if (callId != null) {
            msg.append(",callId: " + callId);
        }
        if (stream != null) {
            msg.append(",stream: " + stream);
        }
        return msg.toString();
    }
}
