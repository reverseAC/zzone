package com.zjh.zzone.iot.media.vo;

import com.ylg.iot.entity.RecordShareLog;
import com.ylg.iot.media.contant.RecordShareDurationUnit;
import lombok.Data;

/**
 * 录像分享记录 响应实体
 *
 * @author zjh
 * @since 2025-07-24 14:42
 */
@Data
public class RecordShareLogVO extends RecordShareLog {
    /**
     * 分享路径
     */
    private String path;
    /**
     * 有效期单位描述
     */
    private String validDurationUnitName;

    public String getValidDurationUnitName() {
        RecordShareDurationUnit unit = RecordShareDurationUnit.getByCode(getValidDurationUnit());
        if (unit != null) {
            return unit.getDesc();
        }
        return null;
    }
}
