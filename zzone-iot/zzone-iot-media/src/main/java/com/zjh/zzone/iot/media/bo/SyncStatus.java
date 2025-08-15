package com.zjh.zzone.iot.media.bo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 摄像机同步状态
 * @author lin
 */
@Data
//@Schema(description = "摄像机同步状态")
@NoArgsConstructor
@AllArgsConstructor
public class SyncStatus {

    //@Schema(description = "总数")
    private Integer total;

    //@Schema(description = "当前更新多少")
    private Integer current;

    //@Schema(description = "错误描述")
    private String errorMsg;

    //@Schema(description = "是否同步中")
    private Boolean syncIng;

    //@Schema(description = "时间")
    private Instant time;

}
