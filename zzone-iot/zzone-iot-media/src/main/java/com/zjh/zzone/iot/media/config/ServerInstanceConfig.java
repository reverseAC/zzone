package com.zjh.zzone.iot.media.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 服务实例配置
 *
 * @author zjh
 * @since 2025-06-24 14:35
 */
@Data
@Component
@ConfigurationProperties(prefix = "spring.application", ignoreInvalidFields = true)
public class ServerInstanceConfig {

    private String instanceId;
}
