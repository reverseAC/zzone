package com.zjh.zzone.iot.media.bo;

import com.ylg.iot.entity.MediaDevice;
import lombok.Data;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * @author lin
 */
@Data
public class CatalogData {
    /**
     * 命令序列号
     */
    private int sn;
    private Integer total;
    private Instant time;
    private MediaDevice device;
    private String errorMsg;
    private Set<String> redisKeysForChannel = new HashSet<>();
    private Set<String> errorChannel = new HashSet<>();

    public enum CatalogDataStatus{
        ready, running, end
    }
    private CatalogDataStatus status;

}
