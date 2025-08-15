package com.zjh.zzone.iot.media.vo;

import lombok.Data;

/**
 * 视频平台接入信息 响应实体
 *
 * @author zjh
 * @since 2025-07-01 14:42
 */
@Data
public class PlatformInfoVO {

    /**
     * 平台ID
     */
    private String id;
    /**
     * 平台域
     */
    private String domain;
    /**
     * 接入ip
     */
    private String ip;
    /**
     * 接入端口
     */
    private Integer port;
    /**
     * 密码
     */
    private String password;

}
