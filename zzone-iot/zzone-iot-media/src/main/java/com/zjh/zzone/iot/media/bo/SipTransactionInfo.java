package com.zjh.zzone.iot.media.bo;

import gov.nist.javax.sip.message.SIPResponse;
import lombok.Getter;
import lombok.Setter;

/**
 * SIP事务信息
 *
 * @author zjh
 * @since 2025-06-24 14:27:12
 */
@Getter
@Setter
public class SipTransactionInfo {

    private String callId;
    private String fromTag;
    private String toTag;
    private String viaBranch;
    private int expires;
    private String user;

    // 自己是否媒体流发送者
    private boolean asSender;

    public SipTransactionInfo(SIPResponse response, boolean asSender) {
        this.callId = response.getCallIdHeader().getCallId();
        this.fromTag = response.getFromTag();
        this.toTag = response.getToTag();
        this.viaBranch = response.getTopmostViaHeader().getBranch();
        this.asSender = asSender;
    }

    public SipTransactionInfo(SIPResponse response) {
        this(response, false);
    }

    public SipTransactionInfo() {
    }
}
