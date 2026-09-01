package com.aurora.ai.mcp.service.impl;

import com.aurora.ai.mcp.client.McpClientManager;
import com.aurora.ai.mcp.entity.AiMcpServerDO;
import com.aurora.ai.mcp.mapper.AiMcpServerMapper;
import com.aurora.ai.mcp.model.req.McpServerEnabledReq;
import com.aurora.ai.tool.core.AiToolDefinition;
import com.aurora.ai.tool.core.AiToolRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AiMcpServerServiceImplTest {

    private final AiMcpServerMapper mapper = mock(AiMcpServerMapper.class);
    private final McpClientManager clientManager = mock(McpClientManager.class);
    private final AiToolRegistry toolRegistry = mock(AiToolRegistry.class);
    private final AiMcpServerServiceImpl service = new AiMcpServerServiceImpl(mapper, clientManager, toolRegistry);

    @Test
    void enable_syncsAndRegistersRemoteTools() {
        AiMcpServerDO server = server("weather");
        AiToolDefinition tool = AiToolDefinition.builder().name("mcp.weather.current").build();
        when(mapper.selectById(7L)).thenReturn(server);
        when(clientManager.getToolNames("weather")).thenReturn(List.of());
        when(clientManager.connectAndSync(server)).thenReturn(List.of(tool));

        McpServerEnabledReq request = new McpServerEnabledReq();
        request.setEnabled(1);
        service.setEnabled(7L, request);

        verify(clientManager).connectAndSync(server);
        verify(toolRegistry).registerMcpTools(List.of(tool));
        verify(mapper, times(2)).updateById(server);
        assertThat(server.getEnabled()).isEqualTo(1);
        assertThat(server.getToolCount()).isEqualTo(1);
        assertThat(server.getLastError()).isNull();
    }

    @Test
    void enableFailure_recordsDegradedStateWithoutThrowing() {
        AiMcpServerDO server = server("offline");
        when(mapper.selectById(8L)).thenReturn(server);
        when(clientManager.getToolNames("offline")).thenReturn(List.of());
        when(clientManager.connectAndSync(server)).thenThrow(new IllegalStateException("connection refused"));

        McpServerEnabledReq request = new McpServerEnabledReq();
        request.setEnabled(1);
        service.setEnabled(8L, request);

        verify(mapper, times(2)).updateById(server);
        verifyNoInteractions(toolRegistry);
        assertThat(server.getEnabled()).isEqualTo(1);
        assertThat(server.getLastError()).isEqualTo("connection refused");
    }

    private static AiMcpServerDO server(String code) {
        AiMcpServerDO server = new AiMcpServerDO();
        server.setId(1L);
        server.setCode(code);
        server.setBaseUrl("http://localhost:3000/sse");
        server.setEnabled(0);
        server.setTimeoutSeconds(10);
        return server;
    }
}
