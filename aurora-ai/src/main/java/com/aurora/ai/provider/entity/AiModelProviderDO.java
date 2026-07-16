package com.aurora.ai.provider.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.aurora.common.base.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_model_provider")
public class AiModelProviderDO extends BaseDO {
    private String code;
    private String name;
    private String baseUrl;
    private String apiKeyCipher;
    private String model;
    private BigDecimal temperature;
    private Integer maxTokens;
    private Integer timeoutSeconds;
    private Integer enabled;
    private Integer sortOrder;
}