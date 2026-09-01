package com.aurora.ai.tool.weather;

import java.io.IOException;

/** Small transport boundary so weather parsing can be tested without network access. */
public interface WeatherHttpClient {
    String get(String uri) throws IOException, InterruptedException;
}
