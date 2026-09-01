package com.aurora.ai.mcp.server;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.util.StringUtils;

import java.io.IOException;

/**
 * MCP Server Bearer Token 安全过滤（T-M3）。
 * <p>
 * 仅拦截 MCP 端点（/mcp/sse, /mcp/message）。
 * 无 token 或 token 错误 → 401。
 * 与 Sa-Token 体系完全独立（MCP client 不走登录流程）。
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "aurora.ai.mcp.server.enabled", havingValue = "true")
@RequiredArgsConstructor
public class McpServerSecurityConfig {

    private final McpServerProperties properties;

    /**
     * 注册安全过滤器，仅匹配 MCP 端点。
     */
    @Bean
    public FilterRegistrationBean<Filter> mcpSecurityFilter() {
        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new McpBearerTokenFilter());
        registration.addUrlPatterns(properties.getSseEndpoint(), properties.getMessageEndpoint());
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        registration.setName("mcpBearerTokenFilter");
        return registration;
    }

    /**
     * Bearer Token 校验过滤器。
     */
    private class McpBearerTokenFilter implements Filter {

        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                throws IOException, ServletException {

            HttpServletRequest httpReq = (HttpServletRequest) request;
            HttpServletResponse httpResp = (HttpServletResponse) response;

            String authHeader = httpReq.getHeader("Authorization");
            if (!StringUtils.hasText(authHeader)) {
                log.warn("mcp.server auth failed: missing Authorization header uri={}", httpReq.getRequestURI());
                httpResp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                httpResp.setContentType("application/json");
                httpResp.getWriter().write("{\"error\":\"Missing Authorization header\"}");
                return;
            }

            // 解析 Bearer token
            String token;
            if (authHeader.startsWith("Bearer ")) {
                token = authHeader.substring(7).trim();
            } else {
                token = authHeader.trim();
            }

            String expectedToken = properties.resolveToken();
            if (!expectedToken.equals(token)) {
                log.warn("mcp.server auth failed: invalid token uri={}", httpReq.getRequestURI());
                httpResp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                httpResp.setContentType("application/json");
                httpResp.getWriter().write("{\"error\":\"Invalid bearer token\"}");
                return;
            }

            chain.doFilter(request, response);
        }
    }
}
