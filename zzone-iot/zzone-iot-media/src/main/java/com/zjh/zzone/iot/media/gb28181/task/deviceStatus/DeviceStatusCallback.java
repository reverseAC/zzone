package com.zjh.zzone.iot.media.gb28181.task.deviceStatus;

import com.ylg.iot.media.bo.SipTransactionInfo;

public interface DeviceStatusCallback {
    public void run(String deviceId, SipTransactionInfo transactionInfo);
}