package com.aurora.ai.tool.github;

import com.aurora.ai.tool.core.AiToolHandler;
import com.aurora.ai.tool.model.AiToolRequest;
import com.aurora.ai.tool.model.AiToolResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class GitHubService implements AiToolHandler {
    private static final String SEARCH_ENDPOINT = "https://api.github.com/search/repositories";
    private final GitHubHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String bearerToken;

    public GitHubService(GitHubHttpClient httpClient, ObjectMapper objectMapper,
                         @Value("${aurora.ai.github.token:}") String bearerToken) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.bearerToken = bearerToken;
    }

    @Override
    public AiToolResult execute(AiToolRequest request) {
        String query = textParam(request, "query");
        if (!StringUtils.hasText(query)) {
            return AiToolResult.fail("GITHUB_INVALID_ARGUMENT", "query is required");
        }
        int perPage = intParam(request, "perPage", 5);
        if (perPage < 1 || perPage > 10) {
            return AiToolResult.fail("GITHUB_INVALID_ARGUMENT", "perPage must be between 1 and 10");
        }
        String sort = textParam(request, "sort");
        if (!StringUtils.hasText(sort)) {
            sort = "stars";
        }
        if (!List.of("stars", "forks", "help-wanted-issues", "updated").contains(sort)) {
            return AiToolResult.fail("GITHUB_INVALID_ARGUMENT", "sort is not supported");
        }
        String uri = SEARCH_ENDPOINT + "?q=" + encode(query.trim()) + "&sort=" + encode(sort)
                + "&order=desc&per_page=" + perPage;
        try {
            JsonNode root = objectMapper.readTree(httpClient.get(uri, bearerToken));
            JsonNode items = root == null ? null : root.get("items");
            if (items == null || !items.isArray()) {
                return AiToolResult.fail("GITHUB_INVALID_RESPONSE", "GitHub response did not contain repositories");
            }
            List<Map<String, Object>> repositories = new ArrayList<>();
            for (JsonNode item : items) {
                Map<String, Object> repo = new LinkedHashMap<>();
                putText(repo, "name", item, "full_name");
                putText(repo, "description", item, "description");
                putText(repo, "url", item, "html_url");
                putNumber(repo, "stars", item, "stargazers_count");
                putNumber(repo, "forks", item, "forks_count");
                putText(repo, "language", item, "language");
                repositories.add(repo);
            }
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("query", query.trim());
            data.put("sort", sort);
            data.put("totalCount", root.path("total_count").asInt(repositories.size()));
            data.put("repositories", repositories);
            return AiToolResult.ok(data, "Found " + repositories.size() + " GitHub repositories");
        } catch (JsonProcessingException ex) {
            return AiToolResult.fail("GITHUB_INVALID_RESPONSE", "GitHub response was not valid JSON");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return AiToolResult.fail("GITHUB_NETWORK_ERROR", "GitHub request was interrupted");
        } catch (IOException | RuntimeException ex) {
            return AiToolResult.fail("GITHUB_UPSTREAM_ERROR", "GitHub is unavailable or rate limited");
        }
    }

    private static void putText(Map<String, Object> target, String key, JsonNode node, String source) {
        JsonNode value = node.get(source);
        if (value != null && !value.isNull()) target.put(key, value.asText());
    }

    private static void putNumber(Map<String, Object> target, String key, JsonNode node, String source) {
        JsonNode value = node.get(source);
        if (value != null && value.isNumber()) target.put(key, value.numberValue());
    }

    private static String textParam(AiToolRequest request, String name) {
        if (request == null || request.getParams() == null) return null;
        Object value = request.getParams().get(name);
        return value == null ? null : String.valueOf(value);
    }

    private static int intParam(AiToolRequest request, String name, int fallback) {
        Object value = request == null || request.getParams() == null ? null : request.getParams().get(name);
        if (value instanceof Number number) return number.intValue();
        if (value != null) {
            try { return Integer.parseInt(String.valueOf(value)); } catch (NumberFormatException ignored) { }
        }
        return fallback;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
