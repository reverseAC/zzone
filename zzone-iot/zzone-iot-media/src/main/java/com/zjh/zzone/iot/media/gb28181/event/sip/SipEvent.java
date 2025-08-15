package com.zjh.zzone.iot.media.gb28181.event.sip;

import com.ylg.iot.media.bo.SipTransactionInfo;
import com.ylg.iot.media.callback.SipSubscribe;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

/**
 * TODO describe
 *
 * @author zjh
 * @since 2025-03-24 19:49
 */
@Data
public class SipEvent implements Delayed {

    private String key;

    /**
     * 成功的回调
     */
    private SipSubscribe.Event okEvent;

    /**
     * 错误的回调,包括超时
     */
    private SipSubscribe.Event errorEvent;

    /**
     * 超时时间(单位： 毫秒)
     */
    private long delay;

    private SipTransactionInfo sipTransactionInfo;

    public static SipEvent getInstance(String key, SipSubscribe.Event okEvent, SipSubscribe.Event errorEvent, long delay) {
        SipEvent sipEvent = new SipEvent();
        sipEvent.setKey(key);
        sipEvent.setOkEvent(okEvent);
        sipEvent.setErrorEvent(errorEvent);
        sipEvent.setDelay(System.currentTimeMillis() + delay);
        return sipEvent;
    }

    @Override
    public long getDelay(@NotNull TimeUnit unit) {
        return unit.convert(delay - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public int compareTo(@NotNull Delayed o) {
        return (int) (this.getDelay(TimeUnit.MILLISECONDS) - o.getDelay(TimeUnit.MILLISECONDS));
    }
}
