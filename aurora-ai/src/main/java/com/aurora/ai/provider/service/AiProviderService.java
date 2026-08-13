package com.aurora.ai.provider.service;

import com.aurora.ai.provider.model.req.ProviderEnabledReq;
import com.aurora.ai.provider.model.req.ProviderPageReq;
import com.aurora.ai.provider.model.req.ProviderSaveReq;
import com.aurora.ai.provider.model.resp.EmbeddingModelOptionResp;
import com.aurora.ai.provider.model.resp.ProviderOptionResp;
import com.aurora.ai.provider.model.resp.ProviderResp;
import com.aurora.ai.provider.model.resp.ProviderTestResp;
import com.aurora.common.response.PageResult;

import java.util.List;

public interface AiProviderService {
    List<ProviderOptionResp> listEnabled();

    PageResult<ProviderResp> page(ProviderPageReq req);

    Long create(ProviderSaveReq req);

    void update(Long id, ProviderSaveReq req);

    void setEnabled(Long id, ProviderEnabledReq req);

    ProviderTestResp test(Long id);

    void delete(Long id);

    /** 预置 embedding 模型清单（前端下拉用，维度随模型决定）。 */
    List<EmbeddingModelOptionResp> listEmbeddingModelOptions();
}