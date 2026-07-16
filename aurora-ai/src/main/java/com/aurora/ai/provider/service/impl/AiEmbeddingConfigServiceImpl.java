package com.aurora.ai.provider.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.aurora.ai.provider.entity.AiEmbeddingConfigDO;
import com.aurora.ai.provider.mapper.AiEmbeddingConfigMapper;
import com.aurora.ai.provider.client.OpenAiClientFactory;
import com.aurora.ai.provider.model.req.EmbeddingConfigSaveReq;
import com.aurora.ai.provider.model.resp.EmbeddingConfigResp;
import com.aurora.ai.provider.model.resp.ProviderTestResp;
import com.aurora.ai.provider.service.AiEmbeddingConfigService;
import com.aurora.common.exception.BizException;
import com.aurora.common.response.BizCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AiEmbeddingConfigServiceImpl implements AiEmbeddingConfigService {

    private final AiEmbeddingConfigMapper embeddingConfigMapper;
    private final OpenAiClientFactory openAiClientFactory;

    @Override
    public EmbeddingConfigResp get() {
        AiEmbeddingConfigDO config = embeddingConfigMapper.selectOne(Wrappers.<AiEmbeddingConfigDO>lambdaQuery()
                .orderByDesc(AiEmbeddingConfigDO::getEnabled)
                .orderByDesc(AiEmbeddingConfigDO::getCreateTime)
                .last("LIMIT 1"));
        return config == null ? null : toResp(config);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long save(EmbeddingConfigSaveReq req) {
        validate(req);
        if (Boolean.TRUE.equals(req.getEnabled())) {
            disableAll();
        }
        AiEmbeddingConfigDO config = new AiEmbeddingConfigDO();
        config.setBaseUrl(req.getBaseUrl());
        config.setApiKeyCipher(maskAndStoreApiKey(req.getApiKey()));
        config.setModel(req.getModel());
        config.setDimension(req.getDimension());
        config.setTimeoutSeconds(req.getTimeoutSeconds() == null ? 60 : req.getTimeoutSeconds());
        config.setEnabled(Boolean.TRUE.equals(req.getEnabled()) ? 1 : 0);
        embeddingConfigMapper.insert(config);
        return config.getId();
    }

    @Override
    public ProviderTestResp test(EmbeddingConfigSaveReq req) {
        Instant startedAt = Instant.now();
        try {
            validate(req);
            AiEmbeddingConfigDO config = new AiEmbeddingConfigDO();
            config.setBaseUrl(req.getBaseUrl());
            config.setApiKeyCipher(req.getApiKey());
            config.setModel(req.getModel());
            config.setDimension(req.getDimension());
            config.setTimeoutSeconds(req.getTimeoutSeconds());
            openAiClientFactory.testEmbedding(config);
            return ProviderTestResp.builder()
                    .success(Boolean.TRUE)
                    .message("Embedding 连通性测试成功")
                    .durationMs(Duration.between(startedAt, Instant.now()).toMillis())
                    .build();
        } catch (Exception ex) {
            return ProviderTestResp.builder()
                    .success(Boolean.FALSE)
                    .message(ex.getMessage())
                    .durationMs(Duration.between(startedAt, Instant.now()).toMillis())
                    .build();
        }
    }

    private void validate(EmbeddingConfigSaveReq req) {
        if (Boolean.TRUE.equals(req.getEnabled()) && (req.getDimension() == null || req.getDimension() < 1)) {
            throw new BizException(BizCode.PARAM_ERROR);
        }
    }

    private void disableAll() {
        AiEmbeddingConfigDO update = new AiEmbeddingConfigDO();
        update.setEnabled(0);
        embeddingConfigMapper.update(update, Wrappers.<AiEmbeddingConfigDO>lambdaUpdate().eq(AiEmbeddingConfigDO::getEnabled, 1));
    }

    private String maskAndStoreApiKey(String raw) {
        return StringUtils.hasText(raw) ? raw : null;
    }

    private EmbeddingConfigResp toResp(AiEmbeddingConfigDO config) {
        return EmbeddingConfigResp.builder()
                .id(config.getId())
                .baseUrl(config.getBaseUrl())
                .model(config.getModel())
                .dimension(config.getDimension())
                .timeoutSeconds(config.getTimeoutSeconds())
                .enabled(config.getEnabled() != null && config.getEnabled() == 1)
                .hasApiKey(StringUtils.hasText(config.getApiKeyCipher()))
                .createTime(config.getCreateTime())
                .build();
    }
}
