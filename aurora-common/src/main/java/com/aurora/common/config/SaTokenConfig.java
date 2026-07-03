package com.aurora.common.config;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.stp.StpUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sa-Token 拦截器配置
 *
 * <p>注册两个拦截器（顺序敏感）：</p>
 * <ol>
 *   <li>{@link SaInterceptor}：先执行，校验登录（白名单路径放行 + CORS 预检 OPTIONS 放行）</li>
 *   <li>{@link SecurityContextInterceptor}：后执行，把 SaSession 的 username/roles 同步到
 *       {@link com.aurora.common.util.SecurityUtil} 的 ThreadLocal（供 MetaObjectHandlerImpl 用）</li>
 * </ol>
 */
@Configuration
@RequiredArgsConstructor
public class SaTokenConfig implements WebMvcConfigurer {

    private final SecurityContextInterceptor securityContextInterceptor;

    /** 公开路径（无需登录） */
    private static final String[] WHITELIST = {
            "/auth/captcha",
            "/auth/login",
            "/error",
            "/doc.html",
            "/doc.html/**",
            "/swagger-ui/**",
            "/swagger-resources/**",
            "/v3/api-docs/**",
            "/webjars/**",
            "/favicon.ico",
            "/actuator/**",
    };

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 1. Sa-Token 登录校验（先）
        //    - 白名单路径放行（excludePathPatterns）
        //    - CORS 预检 OPTIONS 直接放行（OPTIONS 不带 Authorization，会触发 NotLoginException 导致预检失败）
        registry.addInterceptor(new SaInterceptor(handler -> {
            if ("OPTIONS".equals(SaHolder.getRequest().getMethod())) {
                return;
            }
            StpUtil.checkLogin();
        }))
                .addPathPatterns("/**")
                .excludePathPatterns(WHITELIST)
                .order(100);

        // 2. SecurityUtil ThreadLocal 同步（后）
        registry.addInterceptor(securityContextInterceptor)
                .addPathPatterns("/**")
                .order(200);
    }
}
