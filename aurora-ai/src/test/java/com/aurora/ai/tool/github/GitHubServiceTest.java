package com.aurora.ai.tool.github;

import com.aurora.ai.tool.model.AiToolRequest;
import com.aurora.ai.tool.model.AiToolResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GitHubServiceTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void missingQuery_returnsValidationError() {
        GitHubService service = new GitHubService((uri, token) -> "{}", objectMapper, "");
        AiToolResult result = service.execute(AiToolRequest.builder().params(Map.of()).build());
        assertThat(result.getSuccess()).isFalse();
        assertThat(result.getErrorCode()).isEqualTo("GITHUB_INVALID_ARGUMENT");
    }

    @Test
    void validResponse_returnsRepositorySummaryAndPassesToken() {
        GitHubService service = new GitHubService((uri, token) -> {
            assertThat(uri).contains("q=spring").contains("per_page=5");
            assertThat(token).isEqualTo("secret");
            return "{\"total_count\":1,\"items\":[{\"full_name\":\"spring-projects/spring-boot\",\"description\":\"Spring Boot\",\"html_url\":\"https://github.com/spring-projects/spring-boot\",\"stargazers_count\":75000,\"forks_count\":40000,\"language\":\"Java\"}]}";
        }, objectMapper, "secret");

        AiToolResult result = service.execute(AiToolRequest.builder().params(Map.of("query", "spring")).build());
        assertThat(result.getSuccess()).isTrue();
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.getData();
        assertThat(data).containsEntry("totalCount", 1);
        assertThat(result.getSummary()).contains("1");
    }

    @Test
    void upstreamFailure_returnsStableError() {
        GitHubService service = new GitHubService((uri, token) -> { throw new IOException("403"); }, objectMapper, "");
        AiToolResult result = service.execute(AiToolRequest.builder().params(Map.of("query", "spring")).build());
        assertThat(result.getSuccess()).isFalse();
        assertThat(result.getErrorCode()).isEqualTo("GITHUB_UPSTREAM_ERROR");
    }
}
