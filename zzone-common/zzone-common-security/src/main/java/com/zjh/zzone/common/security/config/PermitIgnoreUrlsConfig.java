package com.zjh.zzone.common.security.config;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 忽略鉴权的URL配置
 *
 * @author zjh
 * @date 2025-08-13 17:26
 */
@Slf4j
@ConfigurationProperties(prefix = "security.ignore")
public class PermitIgnoreUrlsConfig implements InitializingBean {

    @Getter
    @Setter
    private List<String> urls = new ArrayList<>();

    // 默认忽略的URL
    private static final String[] DEFAULT_IGNORE_URLS = new String[] { "/actuator/**", "/error", "/v3/api-docs" };

    @Override
    public void afterPropertiesSet() {
        urls.addAll(Arrays.asList(DEFAULT_IGNORE_URLS));
    }
}
