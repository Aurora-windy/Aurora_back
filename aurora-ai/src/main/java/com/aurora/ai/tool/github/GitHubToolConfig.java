package com.aurora.ai.tool.github;

import com.aurora.ai.tool.core.AiToolDefinition;
import com.aurora.ai.tool.core.AiToolDefinition.ParamField;
import com.aurora.ai.tool.core.AiToolDefinition.ParamType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class GitHubToolConfig {
    @Bean
    public AiToolDefinition githubRepositorySearchToolDefinition(GitHubService handler) {
        return AiToolDefinition.builder()
                .name("github.repository.search")
                .description("Search public GitHub repositories by keyword. Read-only external data.")
                .mutation(false)
                .paramSchema(List.of(
                        new ParamField("query", ParamType.STRING, true, "Repository search keywords"),
                        new ParamField("sort", ParamType.STRING, false, "stars, forks, help-wanted-issues, or updated"),
                        new ParamField("perPage", ParamType.INTEGER, false, "Number of results, from 1 to 10")))
                .handler(handler)
                .build();
    }
}
