package com.aurora.ai.workspace;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** 本地工作区工具配置。工作区由服务端配置，不接受客户端传入。 */
@Data
@Component
@ConfigurationProperties(prefix = "aurora.ai.agent")
public class LocalWorkspaceProperties {
    private String workspaceRoot = "";
    private int workspaceMaxReadChars = 20_000;
    private int workspaceMaxEntries = 200;
    private int workspaceMaxSearchResults = 100;
}
