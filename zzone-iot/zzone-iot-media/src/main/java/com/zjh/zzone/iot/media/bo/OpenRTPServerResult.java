package com.zjh.zzone.iot.media.bo;

import com.ylg.iot.media.callback.HookData;
import lombok.Data;

@Data
public class OpenRTPServerResult {

    private SSRCInfo ssrcInfo;
    private HookData hookData;
}
