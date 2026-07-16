package com.aurora.ai.provider.service;

import com.aurora.ai.provider.model.req.EmbeddingConfigSaveReq;
import com.aurora.ai.provider.model.resp.EmbeddingConfigResp;
import com.aurora.ai.provider.model.resp.ProviderTestResp;

public interface AiEmbeddingConfigService {
    EmbeddingConfigResp get();

    Long save(EmbeddingConfigSaveReq req);

    ProviderTestResp test(EmbeddingConfigSaveReq req);
}