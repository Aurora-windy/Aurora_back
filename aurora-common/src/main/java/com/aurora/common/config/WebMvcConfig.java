package com.aurora.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/**
 * Web MVC 配置
 *
 * <p>Phase 1 在此注册 JWT 拦截器（{@code aurora-system} 提供），
 * 同时排除登录、验证码、Knife4j 等公开路径。</p>
 * <p>本地文件上传的静态访问：{@code /uploads/**} 映射到 {@code aurora.upload.path} 目录
 * （参考 ContiNew Admin 本地存储方案，零第三方依赖）。</p>
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${aurora.upload.path:./uploads}")
    private String uploadPath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = "file:" + Paths.get(uploadPath).toAbsolutePath().normalize() + "/";
        registry.addResourceHandler("/uploads/**").addResourceLocations(location);
    }

    // TODO Phase 1 Task 1.3: 注入 JwtAuthInterceptor 并 override addInterceptors
}
