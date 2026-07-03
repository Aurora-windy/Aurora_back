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

        // 配置了白名单 → 用具体 origin；否则开发期默认用通配符端口模式
        // （5173 占用时 vite 会顺延到 5174/5175...，硬编码端口会反复踩坑）
        if (allowedOrigins.isEmpty()) {
            config.setAllowedOriginPatterns(List.of("http://localhost:*", "http://127.0.0.1:*"));
        } else {
            config.setAllowedOrigins(allowedOrigins);
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
