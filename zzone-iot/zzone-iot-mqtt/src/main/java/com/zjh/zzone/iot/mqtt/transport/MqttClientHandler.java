package com.zjh.zzone.iot.mqtt.transport;

import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient;
import com.ylg.iot.message.dto.MqttTopicInfo;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;
import java.util.UUID;

/**
 * TODO describe
 *
 * @author zjh
 * @date 2025-09-10 12:35
 */
public class MqttClientHandler {

    private final Mqtt5AsyncClient asyncClient;

    @Getter
    private String clientId;

    private boolean status;

    @Value("${transport.mqtt.shared}")
    private boolean shared;

    private static final String SHARED_TOPIC_PREFIX = "$share/1/";

    /**
     * 构造函数：创建客户端
     */
    public MqttClientHandler(String ip, int port) {
        this(UUID.randomUUID().toString(), ip, port);
    }

    public MqttClientHandler(String clientId, String host, int port) {
        this.clientId = clientId;
        asyncClient = MqttClient.builder()
                .useMqttVersion5()
                .identifier(clientId)
                .serverHost(host)
                .serverPort(port)

//                .sslWithDefaultConfig() // 默认TLS连接
//
//                .sslConfig()    // 自定义TLS连接
//                    .keyManagerFactory(null)
//                    .trustManagerFactory(null)
//                    .applySslConfig()

                .buildAsync();
    }

    /**
     * 连接mqtt broker
     * @param username 用户名
     * @param password 密码
     */
    public void connect(String username, String password) {
        asyncClient.connectWith()
                .simpleAuth()
                    .username(username)
                    .password(password.getBytes())
                    .applySimpleAuth()
                .send()
                .whenComplete((connAck, throwable) -> {
                    if (throwable != null) {
                        System.out.println("连接失败");
                        throwable.printStackTrace();
                    } else {
                        System.out.println("连接成功");
                        // TODO 添加订阅
                    }
                });
    }

    /**
     * 订阅主题
     * @param topic 主题
     * @param qos 服务质量
     */
    public void subscribe(String topic, MqttQos qos) {
        asyncClient.subscribeWith()
                .topicFilter(shared ? SHARED_TOPIC_PREFIX + topic : topic)
                .qos(qos)
                .callback(publish -> {
                    System.out.println(publish.getTopic());
                    System.out.println(publish.getPayload());
                })
                .send()
                .whenComplete((subAck, throwable) -> {
                    if (throwable != null) {
                        // Handle failure to subscribe
                    } else {
                        // Handle successful subscription, e.g. logging or incrementing a metric
                    }
                });
    }

    /**
     * 批量订阅topic
     * @param topics 主题信息
     */
    public void subscribe(List<MqttTopicInfo> topics) {
        topics.forEach(topic -> subscribe(topic.getTopic(), topic.getQos()));
    }

    /**
     * 取消订阅topic
     * @param topic 主题
     */
    public void unsubscribe(String topic) {
        asyncClient.unsubscribeWith()
                .topicFilter(topic)
                .send();
    }

    /**
     * 设置遗嘱
     * @param topic 主题
     * @param payload 载荷
     */
    public void setWill(String topic, byte[] payload) {
        asyncClient.connectWith()
                .willPublish()
                .topic("my/will")
                .payload("payload".getBytes())
                .qos(MqttQos.AT_MOST_ONCE)
                .retain(true)
                .applyWillPublish()
                .send()
                .whenComplete((connAck, throwable) -> {
                    // Handle connection complete
                });

    }

    /**
     * 发布消息
     * @param topic 主题
     * @param payload 载荷
     * @param retain 是否保留
     */
    public void publish(String topic, byte[] payload, boolean retain) {
        asyncClient.publishWith()
                .topic(topic)
                .payload(payload)
                .qos(MqttQos.EXACTLY_ONCE)
                .retain(retain)
                .send()
                .whenComplete((mqtt3Publish, throwable) -> {
                    if (throwable != null) {
                        // Handle failure to publish
                    } else {
                        // Handle successful publish, e.g. logging or incrementing a metric
                    }
                });
    }

    // 断开连接
    public void disconnect() {
        asyncClient.disconnect();
    }


    public static void main(String[] args) {
        MqttClientHandler handler = new MqttClientHandler("192.168.2.109", 2883);
        handler.connect("emqx", "emqx");
        handler.subscribe("/zjh", MqttQos.AT_LEAST_ONCE);

        handler.publish("/zjh", "hello".getBytes(), false);

    }
}
