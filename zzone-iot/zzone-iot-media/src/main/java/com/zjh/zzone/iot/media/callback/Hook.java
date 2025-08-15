package com.zjh.zzone.iot.media.callback;

import com.ylg.iot.enums.media.HookType;
import lombok.Data;

/**
 * zlm hook事件的参数
 *
 * @author zjh
 * @date 2022-11-24 11:52
 */
@Data
public class Hook {

    private HookType hookType;

    private String app;

    private String stream;

    private Long expireTime;

    public static Hook getInstance(HookType hookType, String app, String stream) {
        Hook hookSubscribe = new Hook();
        hookSubscribe.setApp(app);
        hookSubscribe.setStream(stream);
        hookSubscribe.setHookType(hookType);
        hookSubscribe.setExpireTime(System.currentTimeMillis() + 5 * 60 * 1000);
        return hookSubscribe;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Hook) {
            Hook param = (Hook) obj;
            return param.getHookType().equals(this.hookType)
                    && param.getApp().equals(this.app)
                    && param.getStream().equals(this.stream);
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return this.getHookType() + this.getApp() + this.getStream();
    }
}
