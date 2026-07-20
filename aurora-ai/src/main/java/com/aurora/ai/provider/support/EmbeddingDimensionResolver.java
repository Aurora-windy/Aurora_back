package com.aurora.ai.provider.support;

import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.Map;

public final class EmbeddingDimensionResolver {

    private static final Map<String, Integer> KNOWN_DIMENSIONS = Map.of(
            "text-embedding-3-small", 1536,
            "text-embedding-3-large", 3072,
            "bge-m3", 1024,
            "nomic-embed-text", 768,
            "m3e-base", 768
    );

    private EmbeddingDimensionResolver() {
    }

    public static Integer resolve(String model, Integer configuredDimension) {
        if (configuredDimension != null && configuredDimension > 0) {
            return configuredDimension;
        }
        if (!StringUtils.hasText(model)) {
            return null;
        }
        return KNOWN_DIMENSIONS.get(normalize(model));
    }

    private static String normalize(String model) {
        return model.trim().toLowerCase(Locale.ROOT);
    }
}