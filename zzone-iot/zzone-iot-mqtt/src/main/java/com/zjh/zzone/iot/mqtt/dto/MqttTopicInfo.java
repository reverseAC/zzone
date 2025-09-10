package com.zjh.zzone.iot.mqtt.dto;

import com.hivemq.client.mqtt.datatypes.MqttQos;
import lombok.Getter;
import lombok.Setter;

/**
 * 订阅主题信息
 *
 * @author zjh
 * @date 2025-09-10 16:05
 */
@Getter
@Setter
public class MqttTopicInfo {

    /**
     * 主题
     */
    private String topic;

    /**
     * qos 服务质量
     * @see MqttQos
     */
    private String qos;

    public MqttQos getQos() {
        return MqttQos.valueOf(qos);
    }
}
