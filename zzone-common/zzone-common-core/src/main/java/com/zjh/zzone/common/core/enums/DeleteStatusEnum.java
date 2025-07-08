package com.zjh.zzone.common.core.enums;

import com.baomidou.mybatisplus.annotation.IEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 删除状态枚举
 *
 * @author zjh
 * @date 2025/7/8 22:07
 */
@Getter
@RequiredArgsConstructor
public enum DeleteStatusEnum implements IEnum<Boolean> {
    NOT_DELETED(false, "未删除"),
    DELETED(true, "已删除");

    private final Boolean value;
    private final String desc;

}
