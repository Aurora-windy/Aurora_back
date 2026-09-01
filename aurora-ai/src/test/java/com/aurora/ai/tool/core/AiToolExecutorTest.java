package com.aurora.ai.tool.core;

import cn.dev33.satoken.stp.StpUtil;
import com.aurora.ai.audit.service.AiToolAuditService;
import com.aurora.ai.tool.model.AiToolRequest;
import com.aurora.ai.tool.model.AiToolResult;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiToolExecutorTest {

    private final AiToolRegistry registry = mock(AiToolRegistry.class);
    private final AiToolAuditService auditService = mock(AiToolAuditService.class);
    private final AiToolExecutor executor = new AiToolExecutor(registry, auditService);

    @Test
    void execute_shouldNotExposeToolExceptionMessageToBrowserOrAuditResult() {
        AiToolRequest request = AiToolRequest.builder()
                .toolName("edu.test")
                .params(Map.of("apiKey", "sk-tool-secret"))
                .build();
        AiToolDefinition definition = AiToolDefinition.builder()
                .name("edu.test")
                .permissionCode("edu:test")
                .handler(req -> {
                    throw new RuntimeException("Authorization: Bearer sk-tool-secret failed");
                })
                .build();
        when(registry.get("edu.test")).thenReturn(definition);

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(() -> StpUtil.checkPermission("edu:test")).thenAnswer(invocation -> null);

            AiToolResult result = executor.execute(request);

            assertThat(result.getSuccess()).isFalse();
            assertThat(result.getErrorMessage()).isEqualTo("tool execution failed");
            assertThat(result.getErrorMessage()).doesNotContain("sk-tool-secret");
            assertThat(result.getErrorMessage()).doesNotContain("Authorization");
            verify(auditService).record(any(), any(), any(), any(LocalDateTime.class), any(LocalDateTime.class));
        }
    }

    @Test
    void execute_withoutPermissionCode_allowsPublicReadOnlyTool() {
        AiToolRequest request = AiToolRequest.builder()
                .toolName("weather.current")
                .params(Map.of("city", "Beijing"))
                .build();
        AiToolDefinition definition = AiToolDefinition.builder()
                .name("weather.current")
                .mutation(false)
                .handler(req -> AiToolResult.ok(Map.of("temperature", 28.4), "ok"))
                .build();
        when(registry.get("weather.current")).thenReturn(definition);

        AiToolResult result = executor.execute(request);

        assertThat(result.getSuccess()).isTrue();
        verify(auditService).record(any(), any(), any(), any(LocalDateTime.class), any(LocalDateTime.class));
    }
}
