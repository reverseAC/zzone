package com.zjh.zzone.iot.mqtt.support;

import com.ylg.iot.message.ProtocolSupportDefinition;

/**
 * 协议包加载器顶级接口
 *
 * @author zjh
 * @since 2025-01-02 19:05
 */
public interface ProtocolSupportLoader {

    /**
     * 加载协议包
     * @param supportDefinition 协议配置
     * @return 协议实例
     */
    ProtocolSupport load(ProtocolSupportDefinition supportDefinition);

    /**
     * 关闭类加载器
     * @param loader 类加载器id
     */
    void closeLoader(Long loader);

}
