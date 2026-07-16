package com.aurora.ai.provider.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.aurora.ai.provider.client.OpenAiClientFactory;
import com.aurora.ai.provider.entity.AiModelProviderDO;
import com.aurora.ai.provider.mapper.AiModelProviderMapper;
import com.aurora.ai.provider.model.req.ProviderEnabledReq;
import com.aurora.ai.provider.model.req.ProviderPageReq;
import com.aurora.ai.provider.model.req.ProviderSaveReq;
import com.aurora.ai.provider.model.resp.ProviderOptionResp;
import com.aurora.ai.provider.model.resp.ProviderResp;
import com.aurora.ai.provider.model.resp.ProviderTestResp;
import com.aurora.ai.provider.service.AiProviderService;
import com.aurora.common.exception.BizException;
import com.aurora.common.response.BizCode;
import com.aurora.common.response.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AiProviderServiceImpl implements AiProviderService {

    private final AiModelProviderMapper providerMapper;
    private final OpenAiClientFactory openAiClientFactory;

    @Override
    public List<ProviderOptionResp> listEnabled() {
        List<AiModelProviderDO> providers = providerMapper.selectList(Wrappers.<AiModelProviderDO>lambdaQuery()
                .eq(AiModelProviderDO::getEnabled, 1)
                .orderByAsc(AiModelProviderDO::getSortOrder)
                .orderByDesc(AiModelProviderDO::getCreateTime));
        return providers.stream().map(this::toOptionResp).toList();
    }

    @Override
    public PageResult<ProviderResp> page(ProviderPageReq req) {
        LambdaQueryWrapper<AiModelProviderDO> wrapper = Wrappers.<AiModelProviderDO>lambdaQuery()
                .like(StringUtils.hasText(req.getCode()), AiModelProviderDO::getCode, req.getCode())
                .like(StringUtils.hasText(req.getName()), AiModelProviderDO::getName, req.getName())
                .like(StringUtils.hasText(req.getModel()), AiModelProviderDO::getModel, req.getModel())
                .eq(req.getEnabled() != null, AiModelProviderDO::getEnabled, req.getEnabled())
                .orderByAsc(AiModelProviderDO::getSortOrder)
                .orderByDesc(AiModelProviderDO::getCreateTime);
        Page<AiModelProviderDO> page = providerMapper.selectPage(new Page<>(req.normalizedPageNum(), req.normalizedPageSize()), wrapper);
        return new PageResult<>(page.getRecords().stream().map(this::toResp).toList(), page.getTotal());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(ProviderSaveReq req) {
        ensureCodeUnique(req.getCode(), null);
        AiModelProviderDO provider = new AiModelProviderDO();
        fill(provider, req, true);
        providerMapper.insert(provider);
        return provider.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, ProviderSaveReq req) {
        AiModelProviderDO provider = requireProvider(id);
        ensureCodeUnique(req.getCode(), id);
        fill(provider, req, false);
        provider.setId(id);
        providerMapper.updateById(provider);
    }

    @Override
    public void setEnabled(Long id, ProviderEnabledReq req) {
        requireProvider(id);
        AiModelProviderDO provider = new AiModelProviderDO();
        provider.setId(id);
        provider.setEnabled(Boolean.TRUE.equals(req.getEnabled()) ? 1 : 0);
        providerMapper.updateById(provider);
    }

    @Override
    public ProviderTestResp test(Long id) {
        AiModelProviderDO provider = requireProvider(id);
        Instant startedAt = Instant.now();
        try {
            openAiClientFactory.testChatCompletion(provider);
            return ProviderTestResp.builder()
                    .success(Boolean.TRUE)
                    .message("Provider connection test succeeded")
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

    private void fill(AiModelProviderDO provider, ProviderSaveReq req, boolean create) {
        provider.setCode(req.getCode());
        provider.setName(req.getName());
        provider.setBaseUrl(req.getBaseUrl());
        if (create || StringUtils.hasText(req.getApiKey())) {
            provider.setApiKeyCipher(maskAndStoreApiKey(req.getApiKey()));
        }
        provider.setModel(req.getModel());
        provider.setTemperature(req.getTemperature() == null ? new BigDecimal("0.70") : req.getTemperature());
        provider.setMaxTokens(req.getMaxTokens());
        provider.setTimeoutSeconds(req.getTimeoutSeconds() == null ? 60 : req.getTimeoutSeconds());
        provider.setEnabled(Boolean.FALSE.equals(req.getEnabled()) ? 0 : 1);
        provider.setSortOrder(req.getSortOrder() == null ? 0 : req.getSortOrder());
    }

    private String maskAndStoreApiKey(String raw) {
        return StringUtils.hasText(raw) ? raw : null;
    }

    private AiModelProviderDO requireProvider(Long id) {
        AiModelProviderDO provider = providerMapper.selectById(id);
        if (provider == null) {
            throw new BizException(BizCode.DATA_NOT_FOUND);
        }
        return provider;
    }

    private void ensureCodeUnique(String code, Long excludeId) {
        LambdaQueryWrapper<AiModelProviderDO> wrapper = Wrappers.<AiModelProviderDO>lambdaQuery()
                .eq(AiModelProviderDO::getCode, code)
                .ne(excludeId != null, AiModelProviderDO::getId, excludeId);
        if (providerMapper.selectCount(wrapper) > 0) {
            throw new BizException(BizCode.DATA_EXISTS);
        }
    }

    private ProviderResp toResp(AiModelProviderDO provider) {
        return ProviderResp.builder()
                .id(provider.getId())
                .code(provider.getCode())
                .name(provider.getName())
                .baseUrl(provider.getBaseUrl())
                .model(provider.getModel())
                .temperature(provider.getTemperature())
                .maxTokens(provider.getMaxTokens())
                .timeoutSeconds(provider.getTimeoutSeconds())
                .enabled(provider.getEnabled() != null && provider.getEnabled() == 1)
                .sortOrder(provider.getSortOrder())
                .hasApiKey(StringUtils.hasText(provider.getApiKeyCipher()))
                .createTime(provider.getCreateTime())
                .build();
    }

    private ProviderOptionResp toOptionResp(AiModelProviderDO provider) {
        return ProviderOptionResp.builder()
                .id(provider.getId())
                .code(provider.getCode())
                .name(provider.getName())
                .baseUrl(provider.getBaseUrl())
                .model(provider.getModel())
                .temperature(provider.getTemperature())
                .maxTokens(provider.getMaxTokens())
                .timeoutSeconds(provider.getTimeoutSeconds())
                .sortOrder(provider.getSortOrder())
                .build();
    }
}
