package com.aurora.ai.tool.github;

import java.io.IOException;

/** Small transport boundary so GitHub parsing can be tested without network access. */
public interface GitHubHttpClient {
    String get(String uri, String bearerToken) throws IOException, InterruptedException;
}
