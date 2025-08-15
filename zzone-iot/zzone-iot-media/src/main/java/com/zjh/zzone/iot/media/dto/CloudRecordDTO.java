package com.zjh.zzone.iot.media.dto;

import com.baomidou.mybatisplus.annotation.TableField;
import com.ylg.iot.entity.CloudRecord;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 云端录像查询实体
 *
 * @author zjh
 * @date 2025-07-21 11:08
 */
@Getter
@Setter
public class CloudRecordDTO extends CloudRecord {
    /**
     * 检索内容
     */
    private String query;
    /**
     * 开始时间（yyyy-MM-dd HH:mm:ss）
     */
    private String startTimeStr;
    /**
     * 结束时间（yyyy-MM-dd HH:mm:ss）
     */
    private String endTimeStr;
    /**
     * 流媒体服务
     */
    private List<String> mediaServerItems;
    /**
     * 录像id列表
     */
    private List<Long> ids;

}
