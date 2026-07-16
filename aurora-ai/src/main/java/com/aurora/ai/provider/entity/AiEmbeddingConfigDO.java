package com.aurora.ai.provider.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.aurora.common.base.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_embedding_config")
public class AiEmbeddingConfigDO extends BaseDO {
    private String baseUrl;
    private String apiKeyCipher;
    private String model;
    private Integer dimension;
    private Integer timeoutSeconds;
    private Integer enabled;
}