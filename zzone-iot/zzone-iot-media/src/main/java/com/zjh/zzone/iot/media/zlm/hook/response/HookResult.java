package com.zjh.zzone.iot.media.zlm.hook.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * ZLM钩子函数通用返回结果
 *
 * @author zjh
 * @since 2025-07-10 20:06
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class HookResult {

    private int code;
    private String msg;

    public static HookResult SUCCESS(){
        return new HookResult(0, "success");
    }

    public static HookResultForOnPublish Fail(){
        return new HookResultForOnPublish(-1, "fail");
    }
}
