package com.zjh.zzone.iot.mqtt.support;

import com.ylg.iot.constant.TransportEnum;
import com.ylg.iot.message.ProtocolSupportDefinition;
import com.ylg.iot.protocol.DeviceMessageCodec;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 协议实例
 *
 * @author zjh
 * @since 2025-01-04 11:26
 */
@Getter
@Setter
public class DefaultProtocolSupport implements ProtocolSupport {
    /**
     * 协议id
     */
    private Long id;
    /**
     * 解码器集合
     * key: 协议类型
     * value: 解码器
     */
    private final Map<String, DeviceMessageCodec> messageCodecSupports = new ConcurrentHashMap<>();
    /**
     * 协议配置
     */
    private ProtocolSupportDefinition protocolSupportDefinition;

    public DefaultProtocolSupport(ProtocolSupportDefinition protocolSupportDefinition) {
        this.protocolSupportDefinition = protocolSupportDefinition;
        this.id = protocolSupportDefinition.getId();
    }

    /**
     * 批量添加编解码器
     * @param codecList 编解码器
     */
    public void addMessageCodecSupports(List<DeviceMessageCodec> codecList) {
        codecList.forEach(this::addMessageCodecSupport);
    }

    /**
     * 添加编解码器
     * @param codec 编解码器
     */
    public void addMessageCodecSupport(DeviceMessageCodec codec) {
        this.addMessageCodecSupport(codec.getSupportTransport(), codec);
    }

    /**
     * 添加编解码器
     * @param transport 传输协议
     * @param codec 编解码器
     */
    public void addMessageCodecSupport(TransportEnum transport, DeviceMessageCodec codec) {
        this.messageCodecSupports.put(transport.name(), codec);
    }

    /**
     * 移除编解码器
     * @param transport 传输协议
     */
    public void removeMessageCodecSupport(TransportEnum transport) {
        this.messageCodecSupports.remove(transport.name());
    }

    /**
     * 获取设备协议支持的传输协议
     * @return 支持的传输协议
     */
    public List<TransportEnum> getSupportedTransport() {
        return this.messageCodecSupports.values().stream()
                .map(DeviceMessageCodec::getSupportTransport).distinct().collect(Collectors.toList());
    }

    /**
     * 获取解码器实例
     * @param transport 传输协议
     * @return 编解码实例
     */
    public DeviceMessageCodec getMessageCodec(TransportEnum transport) {
        return getMessageCodec(transport.name());
    }

    /**
     * 获取解码器实例
     * @param transport 传输协议
     * @return 编解码实例
     */
    public DeviceMessageCodec getMessageCodec(String transport) {
        return this.messageCodecSupports.get(transport);
    }

    /**
     * 协议是否变更
     * @param definition 协议配置
     * @return 是否变更
     */
    @Override
    public boolean isChanged(ProtocolSupportDefinition definition) {
        return protocolSupportDefinition.equals(definition);
    }
}
