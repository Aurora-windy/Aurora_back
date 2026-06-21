package com.aurora.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

/**
 * 跨域配置（SRS §4.3.6）
 *
 * <p>白名单通过 application.yml 中 {@code aurora.cors.allowed-origins} 配置。</p>
 */
@Configuration
public class CorsConfig {

    @Value("${aurora.cors.allowed-origins:#{T(java.util.Collections).emptyList()}}")
    private List<String> allowedOrigins;

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();

        // 开发期默认允许本地 5173，生产由 yml 白名单控制
        if (allowedOrigins.isEmpty()) {
            config.addAllowedOrigin("http://localhost:5173");
            config.addAllowedOrigin("http://127.0.0.1:5173");
        } else {
            allowedOrigins.forEach(config::addAllowedOrigin);
        }

        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        config.addExposedHeader("token");
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
