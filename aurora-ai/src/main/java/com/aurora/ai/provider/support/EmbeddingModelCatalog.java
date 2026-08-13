package com.aurora.ai.provider.support;

import com.aurora.ai.provider.model.resp.EmbeddingModelOptionResp;

import java.util.List;
import java.util.Locale;

/**
 * Embedding 模型权威清单（单一数据源）：预置常见本地 / 云 embedding 模型及其维度、来源、说明。
 * 供前端下拉选择与 {@link EmbeddingDimensionResolver} 维度解析共用，避免两处维度数字漂移。
 * <p>
 * 设计对齐语析知识库（yuxi-know）的 embed_model.choices：用户只选模型名，维度由模型决定，
 * 不把「1536 / 1024」这类数字暴露给管理员。
 */
public final class EmbeddingModelCatalog {

    public static final String SOURCE_LOCAL = "LOCAL";
    public static final String SOURCE_CLOUD = "CLOUD";

    private record Entry(String model, int dimension, String source, String hint) {
        EmbeddingModelOptionResp toResp() {
            return EmbeddingModelOptionResp.builder()
                    .model(model)
                    .dimension(dimension)
                    .source(source)
                    .hint(hint)
                    .build();
        }
    }

    private static final List<Entry> ENTRIES = List.of(
            new Entry("bge-m3", 1024, SOURCE_LOCAL, "Ollama 本地·免费·中文友好"),
            new Entry("nomic-embed-text", 768, SOURCE_LOCAL, "Ollama 本地·轻量"),
            new Entry("bge-large-zh-v1.5", 1024, SOURCE_LOCAL, "Ollama 本地·中文"),
            new Entry("m3e-base", 768, SOURCE_LOCAL, "sentence-transformers 本地"),
            new Entry("text-embedding-3-small", 1536, SOURCE_CLOUD, "OpenAI·便宜"),
            new Entry("text-embedding-3-large", 3072, SOURCE_CLOUD, "OpenAI·高精"),
            new Entry("text-embedding-ada-002", 1536, SOURCE_CLOUD, "OpenAI 经典"),
            new Entry("embedding-3", 2048, SOURCE_CLOUD, "智谱"),
            new Entry("text-embedding-v2", 1536, SOURCE_CLOUD, "通义千问")
    );

    private EmbeddingModelCatalog() {
    }

    /** 返回全量清单（前端下拉用）。 */
    public static List<EmbeddingModelOptionResp> options() {
        return ENTRIES.stream().map(Entry::toResp).toList();
    }

    /** 按模型名查维度，大小写、首尾空格不敏感；未命中返回 null。 */
    public static Integer dimensionOf(String model) {
        if (model == null || model.isBlank()) {
            return null;
        }
        String key = model.trim().toLowerCase(Locale.ROOT);
        return ENTRIES.stream()
                .filter(e -> e.model().toLowerCase(Locale.ROOT).equals(key))
                .map(Entry::dimension)
                .findFirst()
                .orElse(null);
    }
}
