package com.zjh.zzone.common.core.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 数据状态枚举
 *
 * @author zjh
 * @date 2025-08-13 11:53
 */
@Getter
@AllArgsConstructor
public enum DataStatus {

    PENDING("待激活"),	// 尚未激活，需要进一步操作
    ACTIVE("启用"),	// 可以正常使用
    DISABLED("禁用"),	// 不允许使用
    LOCKED("锁定"),	// 因安全或异常被锁定
    SUSPENDED("暂停使用"),	// 临时中止，可能因违规或系统原因
    EXPIRED("过期"),	// 账号或状态已过期
    FROZEN("冻结"),	// 类似锁定，通常和资金相关
    ARCHIVED("归档");	// 不再活跃，存档保存;

    private final String desc;
}
