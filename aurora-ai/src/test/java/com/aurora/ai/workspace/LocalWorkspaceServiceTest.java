package com.aurora.ai.workspace;

import com.aurora.ai.chat.entity.AiChatSessionDO;
import com.aurora.ai.chat.mapper.AiChatSessionMapper;
import com.aurora.ai.tool.model.AiToolResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocalWorkspaceServiceTest {

    @TempDir
    Path workspaceRoot;

    @Mock
    private AiChatSessionMapper sessionMapper;

    private LocalWorkspaceService service;

    @BeforeEach
    void setUp() throws Exception {
        LocalWorkspaceProperties properties = new LocalWorkspaceProperties();
        properties.setWorkspaceRoot(workspaceRoot.toString());
        properties.setWorkspaceMaxReadChars(100);
        properties.setWorkspaceMaxEntries(20);
        properties.setWorkspaceMaxSearchResults(20);
        service = new LocalWorkspaceService(properties, sessionMapper);

        AiChatSessionDO enabledSession = new AiChatSessionDO();
        enabledSession.setLocalFilesEnabled(1);
        when(sessionMapper.selectById(any())).thenReturn(enabledSession);
    }

    @Test
    void read_returnsTextWithinWorkspace() throws Exception {
        Files.writeString(workspaceRoot.resolve("README.md"), "hello aurora", StandardCharsets.UTF_8);

        AiToolResult result = service.read(1L, Map.of("path", "README.md"));

        assertThat(result.getSuccess()).isTrue();
        assertThat(result.getData().toString()).contains("hello aurora");
    }

    @Test
    void traversal_isRejected() {
        AiToolResult result = service.read(1L, Map.of("path", "../outside.txt"));

        assertThat(result.getSuccess()).isFalse();
        assertThat(result.getErrorCode()).isEqualTo("LOCAL_PATH_OUTSIDE_WORKSPACE");
    }

    @Test
    void sensitiveAndBinaryFiles_areRejected() throws Exception {
        Files.writeString(workspaceRoot.resolve(".env"), "SECRET=value", StandardCharsets.UTF_8);
        Files.write(workspaceRoot.resolve("image.bin"), new byte[]{1, 2, 0, 3});

        AiToolResult sensitive = service.read(1L, Map.of("path", ".env"));
        AiToolResult binary = service.read(1L, Map.of("path", "image.bin"));

        assertThat(sensitive.getErrorCode()).isEqualTo("LOCAL_SENSITIVE_FILE");
        assertThat(binary.getErrorCode()).isEqualTo("LOCAL_BINARY_FILE");
    }

    @Test
    void disabledSession_cannotRead() throws Exception {
        AiChatSessionDO disabledSession = new AiChatSessionDO();
        disabledSession.setLocalFilesEnabled(0);
        when(sessionMapper.selectById(any())).thenReturn(disabledSession);
        Files.writeString(workspaceRoot.resolve("README.md"), "hello", StandardCharsets.UTF_8);

        AiToolResult result = service.read(1L, Map.of("path", "README.md"));

        assertThat(result.getSuccess()).isFalse();
        assertThat(result.getErrorCode()).isEqualTo("LOCAL_FILES_DISABLED");
    }
}
