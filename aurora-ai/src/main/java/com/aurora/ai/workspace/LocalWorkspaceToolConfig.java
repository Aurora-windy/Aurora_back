package com.aurora.ai.workspace;

import com.aurora.ai.tool.core.AiToolDefinition;
import com.aurora.ai.tool.core.AiToolDefinition.ParamField;
import com.aurora.ai.tool.core.AiToolDefinition.ParamType;
import com.aurora.ai.tool.model.AiToolRequest;
import com.aurora.common.constant.PermCodeConst;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/** 将本地工作区只读服务注册为统一 Agent 工具。 */
@Configuration
public class LocalWorkspaceToolConfig {

    private static final String CAPABILITY = "LOCAL_FILES_READ";

    @Bean
    public AiToolDefinition localWorkspaceListToolDefinition(LocalWorkspaceService service) {
        return definition("local.fs.list", "列出工作区内的文件和目录。path 使用工作区相对路径，默认根目录。",
                List.of(new ParamField("path", ParamType.STRING, false, "工作区相对目录路径"),
                        new ParamField("depth", ParamType.INTEGER, false, "目录展示深度，0-8")),
                request -> service.list(request.getSessionId(), request.getParams()));
    }

    @Bean
    public AiToolDefinition localWorkspaceSearchToolDefinition(LocalWorkspaceService service) {
        return definition("local.fs.search", "按文件名或相对路径关键词搜索工作区文件，不读取文件内容。",
                List.of(new ParamField("query", ParamType.STRING, true, "文件名或路径关键词"),
                        new ParamField("path", ParamType.STRING, false, "限定搜索的工作区相对目录")),
                request -> service.search(request.getSessionId(), request.getParams()));
    }

    @Bean
    public AiToolDefinition localWorkspaceReadToolDefinition(LocalWorkspaceService service) {
        return definition("local.fs.read", "读取工作区内的文本文件。二进制文件和敏感文件不可读。",
                List.of(new ParamField("path", ParamType.STRING, true, "工作区相对文件路径"),
                        new ParamField("maxChars", ParamType.INTEGER, false, "最多返回字符数")),
                request -> service.read(request.getSessionId(), request.getParams()));
    }

    private AiToolDefinition definition(String name, String description, List<ParamField> schema,
                                        java.util.function.Function<AiToolRequest, com.aurora.ai.tool.model.AiToolResult> function) {
        return AiToolDefinition.builder()
                .name(name)
                .description(description)
                .permissionCode(PermCodeConst.Ai.Chat.USE)
                .mutation(false)
                .paramSchema(schema)
                .requiredCapability(CAPABILITY)
                .handler(function::apply)
                .build();
    }
}
