package com.aurora.ai.knowledge.support;

import com.aurora.ai.provider.entity.AiEmbeddingConfigDO;

import java.util.List;

public interface EmbeddingClient {
    AiEmbeddingConfigDO requireEnabledConfig();

    List<Double> embed(String text);
}