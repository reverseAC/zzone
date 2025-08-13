package com.zjh.zzone.common.core.enums;

import lombok.Getter;

/**
 * 通用响应枚举
 *
 * @author zjh
 * @date 2025/7/13 16:38
 */
@Getter
public enum ResultCode {

    // 成功
    SUCCESS(200, "成功"),

    // 参数错误
    PARAM_ERROR(400, "请求参数错误"),
    VALIDATION_ERROR(401, "参数校验失败"),

    // 认证 & 权限错误
    UNAUTHORIZED(403, "未授权或登录失效"),
    FORBIDDEN(40301, "无操作权限"),

    // 资源不存在
    NOT_FOUND(404, "资源未找到"),

    // 业务异常
    BUSINESS_ERROR(1000, "业务处理失败"),

    // 系统异常
    SERVER_ERROR(500, "服务器内部错误"),
    SERVICE_UNAVAILABLE(503, "服务暂不可用"),

    // 数据异常
    DATA_CONFLICT(409, "数据冲突"),
    DATA_NOT_EXIST(410, "数据不存在"),
    DATA_ALREADY_EXIST(411, "数据已存在");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
