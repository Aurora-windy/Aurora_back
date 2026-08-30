package com.aurora.ai.knowledge.support;

import com.aurora.ai.provider.entity.AiEmbeddingConfigDO;

import java.util.List;

public interface EmbeddingClient {
    AiEmbeddingConfigDO requireEnabledConfig();

    List<Double> embed(String text);

    /**
     * 用调用方已取好的配置做向量化——批量场景（如文档发布）应只取一次配置再复用，
     * 避免每个分块都回查数据库。
     */
    List<Double> embed(AiEmbeddingConfigDO config, String text);

    /**
     * 批量向量化：一次 HTTP 请求提交多条文本（OpenAI /embeddings 的 input 支持数组）。
     * 返回顺序与入参一致；供应商不支持批量时实现内部降级为逐条调用。
     */
    List<List<Double>> embedBatch(AiEmbeddingConfigDO config, List<String> texts);
}