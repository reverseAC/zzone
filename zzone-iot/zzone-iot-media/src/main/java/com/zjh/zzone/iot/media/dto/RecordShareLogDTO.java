package com.zjh.zzone.iot.media.dto;

import com.ylg.iot.entity.RecordShareLog;
import lombok.Getter;
import lombok.Setter;

/**
 * 录像分享记录拓展实体
 *
 * @author zjh
 * @date 2025-07-24 11:08
 */
@Getter
@Setter
public class RecordShareLogDTO extends RecordShareLog {

    /**
     * 分享状态(true:生效中, false:已失效)
     */
    private Boolean isValid;

}
