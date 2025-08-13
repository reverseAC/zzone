package com.zjh.zzone.common.security.config;

import com.zjh.zzone.common.security.service.OAuth2AuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

/**
 * TODO describe
 *
 * @author zjh
 * @date 2025-08-13 17:02
 */
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SpringSecurityConfig {

    /**
     * 放行URL配置
     */
    private final PermitIgnoreUrlsConfig permitAllUrl;

    /**
     * 自定义不透明令牌解析器
     */
    private final CustomOpaqueTokenIntrospector customOpaqueTokenIntrospector;

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        PathPatternRequestMatcher[] permitMatchers = permitAllUrl.getUrls()
                .stream()
                .map(url -> PathPatternRequestMatcher.withDefaults().matcher(url))
                .toList()
                .toArray(new PathPatternRequestMatcher[] {});

        http.authorizeHttpRequests(authorizeRequests -> authorizeRequests.requestMatchers(permitMatchers)
                        .permitAll()
                        .anyRequest()
                        .authenticated())  // 放行不需要认证的请求路径
                .oauth2ResourceServer(
                        oauth2 -> oauth2.opaqueToken(token -> token.introspector(customOpaqueTokenIntrospector))
                                .authenticationEntryPoint(resourceAuthExceptionEntryPoint)
                                .bearerTokenResolver(pigBearerTokenExtractor))
                .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable)) // 禁用 X-Frame-Options
                .csrf(AbstractHttpConfigurer::disable); // 关闭 CSRF 防护

        return http.build();
    }
}
