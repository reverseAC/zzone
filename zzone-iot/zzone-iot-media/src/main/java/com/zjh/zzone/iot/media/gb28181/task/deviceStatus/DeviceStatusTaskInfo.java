package com.zjh.zzone.iot.media.gb28181.task.deviceStatus;

import com.ylg.iot.media.bo.SipTransactionInfo;
import lombok.Data;

@Data
public class DeviceStatusTaskInfo{

    private String deviceId;

    private SipTransactionInfo transactionInfo;

    /**
     * 过期时间
     */
    private long expireTime;
}
