package com.aurora.ai.knowledge.service.impl;

import com.aurora.ai.knowledge.entity.AiKnowledgeChunkDO;
import com.aurora.ai.knowledge.entity.AiKnowledgeDocDO;
import com.aurora.ai.knowledge.mapper.AiKnowledgeChunkMapper;
import com.aurora.ai.knowledge.mapper.AiKnowledgeDocMapper;
import com.aurora.ai.knowledge.model.req.KnowledgeDocPageReq;
import com.aurora.ai.knowledge.model.req.KnowledgeDocSaveReq;
import com.aurora.ai.knowledge.model.resp.KnowledgeDocResp;
import com.aurora.ai.knowledge.model.resp.KnowledgePublishResp;
import com.aurora.ai.knowledge.service.KnowledgeDocService;
import com.aurora.ai.knowledge.support.EmbeddingClient;
import com.aurora.ai.knowledge.support.KnowledgeChunker;
import com.aurora.ai.knowledge.support.KnowledgeDocStatus;
import com.aurora.ai.provider.entity.AiEmbeddingConfigDO;
import com.aurora.common.exception.BizException;
import com.aurora.common.response.BizCode;
import com.aurora.common.response.PageResult;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class KnowledgeDocServiceImpl implements KnowledgeDocService {

    private final AiKnowledgeDocMapper docMapper;
    private final AiKnowledgeChunkMapper chunkMapper;
    private final KnowledgeChunker knowledgeChunker;
    private final EmbeddingClient embeddingClient;
    private final ObjectMapper objectMapper;

    @Override
    public PageResult<KnowledgeDocResp> page(KnowledgeDocPageReq req) {
        LambdaQueryWrapper<AiKnowledgeDocDO> wrapper = Wrappers.<AiKnowledgeDocDO>lambdaQuery()
                .like(StringUtils.hasText(req.getTitle()), AiKnowledgeDocDO::getTitle, req.getTitle())
                .eq(StringUtils.hasText(req.getType()), AiKnowledgeDocDO::getType, req.getType())
                .eq(StringUtils.hasText(req.getStatus()), AiKnowledgeDocDO::getStatus, req.getStatus())
                .orderByDesc(AiKnowledgeDocDO::getCreateTime);
        Page<AiKnowledgeDocDO> page = docMapper.selectPage(new Page<>(req.normalizedPageNum(), req.normalizedPageSize()), wrapper);
        return new PageResult<>(page.getRecords().stream().map(this::toResp).toList(), page.getTotal());
    }

    @Override
    public KnowledgeDocResp detail(Long id) {
        return toResp(requireDoc(id));
    }

    @Override
    public Long create(KnowledgeDocSaveReq req) {
        AiKnowledgeDocDO doc = new AiKnowledgeDocDO();
        fill(doc, req);
        doc.setStatus(KnowledgeDocStatus.DRAFT);
        doc.setVersion(1);
        docMapper.insert(doc);
        return doc.getId();
    }

    @Override
    public void update(Long id, KnowledgeDocSaveReq req) {
        AiKnowledgeDocDO doc = requireDoc(id);
        fill(doc, req);
        if (KnowledgeDocStatus.PUBLISHED.equals(doc.getStatus())) {
            doc.setStatus(KnowledgeDocStatus.DRAFT);
            doc.setPublishedAt(null);
        }
        doc.setVersion(doc.getVersion() == null ? 1 : doc.getVersion() + 1);
        docMapper.updateById(doc);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        AiKnowledgeDocDO doc = requireDoc(id);
        if (KnowledgeDocStatus.PUBLISHED.equals(doc.getStatus())) {
            offline(id);
            return;
        }
        deleteChunks(id);
        docMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KnowledgePublishResp publish(Long id) {
        AiKnowledgeDocDO doc = requireDoc(id);
        embeddingClient.requireEnabledConfig();
        List<String> chunks = knowledgeChunker.chunk(doc.getContent());
        if (chunks.isEmpty()) {
            throw new BizException(BizCode.PARAM_ERROR, "Knowledge document content cannot be empty");
        }
        deleteChunks(id);
        AiEmbeddingConfigDO config = embeddingClient.requireEnabledConfig();
        int embeddedCount = 0;
        int skippedCount = 0;
        for (int i = 0; i < chunks.size(); i++) {
            String content = chunks.get(i);
            List<Double> embedding = embeddingClient.embed(content);
            AiKnowledgeChunkDO chunk = new AiKnowledgeChunkDO();
            chunk.setDocId(id);
            chunk.setChunkIndex(i);
            chunk.setContent(content);
            chunk.setTokenCount(content.length());
            chunk.setEmbeddingJson(toJson(embedding));
            chunk.setEmbeddingModel(config.getModel());
            chunk.setEmbeddingDimension(config.getDimension());
            chunkMapper.insert(chunk);
            embeddedCount++;
        }
        doc.setStatus(KnowledgeDocStatus.PUBLISHED);
        doc.setPublishedAt(LocalDateTime.now());
        docMapper.updateById(doc);
        return KnowledgePublishResp.builder()
                .docId(id)
                .chunkCount(chunks.size())
                .embeddedCount(embeddedCount)
                .skippedCount(skippedCount)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KnowledgePublishResp rebuildEmbedding(Long id) {
        return publish(id);
    }

    @Override
    public void offline(Long id) {
        AiKnowledgeDocDO doc = requireDoc(id);
        doc.setStatus(KnowledgeDocStatus.OFFLINE);
        docMapper.updateById(doc);
    }

    private void fill(AiKnowledgeDocDO doc, KnowledgeDocSaveReq req) {
        doc.setTitle(req.getTitle());
        doc.setType(req.getType());
        doc.setContent(req.getContent());
        doc.setSummary(req.getSummary());
    }

    private AiKnowledgeDocDO requireDoc(Long id) {
        AiKnowledgeDocDO doc = docMapper.selectById(id);
        if (doc == null) {
            throw new BizException(BizCode.DOC_NOT_FOUND);
        }
        return doc;
    }

    private void deleteChunks(Long docId) {
        chunkMapper.delete(Wrappers.<AiKnowledgeChunkDO>lambdaQuery().eq(AiKnowledgeChunkDO::getDocId, docId));
    }

    private String toJson(List<Double> embedding) {
        try {
            return objectMapper.writeValueAsString(embedding);
        } catch (JsonProcessingException ex) {
            throw new BizException(BizCode.DOC_PROCESSING_FAILED, "Failed to serialize embedding vector");
        }
    }

    private KnowledgeDocResp toResp(AiKnowledgeDocDO doc) {
        return KnowledgeDocResp.builder()
                .id(doc.getId())
                .title(doc.getTitle())
                .type(doc.getType())
                .status(doc.getStatus())
                .content(doc.getContent())
                .summary(doc.getSummary())
                .version(doc.getVersion())
                .publishedAt(doc.getPublishedAt())
                .createTime(doc.getCreateTime())
                .build();
    }
}