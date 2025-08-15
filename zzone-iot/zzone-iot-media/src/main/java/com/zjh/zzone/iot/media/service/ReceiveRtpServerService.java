package com.zjh.zzone.iot.media.service;

import com.ylg.iot.media.callback.ErrorCallback;
import com.ylg.iot.media.bo.OpenRTPServerResult;
import com.ylg.iot.media.bo.RTPServerParam;
import com.ylg.iot.media.bo.SSRCInfo;
import com.ylg.iot.entity.MediaServer;

/**
 * 接收RTP服务
 *
 * @author zjh
 * @since 2025-07-21 10:56
 */
public interface ReceiveRtpServerService {

    SSRCInfo openRTPServer(RTPServerParam rtpServerParam, ErrorCallback<OpenRTPServerResult> callback);

    void closeRTPServer(MediaServer mediaServer, SSRCInfo ssrcInfo);
}
