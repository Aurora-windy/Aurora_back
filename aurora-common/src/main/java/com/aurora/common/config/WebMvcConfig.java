package com.aurora.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置
 *
 * <p>Phase 1 在此注册 JWT 拦截器（{@code aurora-system} 提供），
 * 同时排除登录、验证码、Knife4j 等公开路径。</p>
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    // TODO Phase 1 Task 1.3: 注入 JwtAuthInterceptor 并 override addInterceptors
}
