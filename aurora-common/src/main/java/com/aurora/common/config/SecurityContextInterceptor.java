package com.aurora.common.config;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import com.aurora.common.util.SecurityUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;

/**
 * SecurityUtil ThreadLocal 同步拦截器
 *
 * <p>为什么是 Interceptor 而不是 Filter：Sa-Token 的 StpUtil.isLogin / getSession 依赖
 * SpringMVC 的 RequestContext（DispatcherServlet 内建立）。Filter 在 DispatcherServlet
 * <b>之前</b>执行，RequestContext 还没建立，调用会抛 NotWebContextException。
 * HandlerInterceptor 的 preHandle/afterCompletion 都在 DispatcherServlet 内，安全。</p>
 *
 * <p>顺序：必须排在 SaInterceptor 之后（SaInterceptor 校验登录、写 SaSession，本拦截器再读）。
 * 见 {@link SaTokenConfig#addInterceptors}。</p>
 */
@Component
public class SecurityContextInterceptor implements HandlerInterceptor {

    @SuppressWarnings("unchecked")
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 此时 SaInterceptor 已经校验过登录（白名单路径不会 checkLogin，但仍可能携带 token）
        if (StpUtil.isLogin()) {
            Long userId = StpUtil.getLoginIdAsLong();
            SaSession session = StpUtil.getSession();
            String username = (String) session.get("username");
            Object rolesObj = session.get("roles");
            List<String> roles = rolesObj instanceof List ? (List<String>) rolesObj : List.of();
            SecurityUtil.setCurrent(new SecurityUtil.LoginUser(userId, username, roles));
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler,
                                Exception ex) {
        // 无论是否抛异常都清理，防容器线程池串号（codemap §5.1）
        SecurityUtil.clear();
    }
}
