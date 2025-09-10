package com.zjh.zzone.iot.mqtt.support;

import com.ylg.iot.constant.TransportEnum;
import com.ylg.iot.message.ProtocolSupportDefinition;
import com.ylg.iot.protocol.DeviceMessageCodec;

/**
 * 设备协议实例顶级接口
 *
 * @author zjh
 * @since 2025-01-02 19:06
 */
public interface ProtocolSupport {
    /**
     * 获取协议id
     * @return Long
     */
    Long getId();

    /**
     * 协议是否变更
     * @param definition 协议配置
     * @return 是否变更
     */
    boolean isChanged (ProtocolSupportDefinition definition);

    /**
     * 获取消息编解码器
     * @param transport 传输协议
     * @return 消息编解码器
     */
    DeviceMessageCodec getMessageCodec(TransportEnum transport);

}
