package com.zjh.zzone.iot.mqtt;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * mqtt传输服务启动类
 *
 * @author zjh
 * @date 2025-08-08 17:19
 */
@EnableAsync
@EnableScheduling
@SpringBootApplication(scanBasePackages = {})
public class MqttTransportApplication {

    public static void main(String[] args) {
        SpringApplication.run(MqttTransportApplication.class, args);
    }
}
