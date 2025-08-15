package com.zjh.zzone.iot.media.contant;

import lombok.Getter;

/**
 * 业务处理结果响应码枚举
 *
 * @author zjh
 */
@Getter
public enum ResultCode {
    SUCCESS(200, "成功"),
    ERROR400(400, "参数或方法错误"),
    ERROR404(404, "资源未找到"),
    ERROR403(403, "无权限操作"),
    ERROR401(401, "请登录后重新请求"),
    ERROR500(500, "系统异常");

    private final int code;
    private final String msg;

    ResultCode(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }
}
