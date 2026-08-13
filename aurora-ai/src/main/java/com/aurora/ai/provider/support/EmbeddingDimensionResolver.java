package com.aurora.ai.provider.support;

/**
 * 按 embedding 模型名解析向量维度。维度数据来自 {@link EmbeddingModelCatalog}（单一数据源），
 * configuredDimension 非空时优先用配置值，否则按模型名查清单。
 */
public final class EmbeddingDimensionResolver {

    private EmbeddingDimensionResolver() {
    }

    public static Integer resolve(String model, Integer configuredDimension) {
        if (configuredDimension != null && configuredDimension > 0) {
            return configuredDimension;
        }
        return EmbeddingModelCatalog.dimensionOf(model);
    }
}
