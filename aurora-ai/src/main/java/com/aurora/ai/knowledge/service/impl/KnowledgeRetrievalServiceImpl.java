package com.aurora.ai.knowledge.service.impl;

import com.aurora.ai.knowledge.entity.AiKnowledgeChunkDO;
import com.aurora.ai.knowledge.entity.AiKnowledgeDocDO;
import com.aurora.ai.knowledge.mapper.AiKnowledgeChunkMapper;
import com.aurora.ai.knowledge.mapper.AiKnowledgeDocMapper;
import com.aurora.ai.knowledge.model.resp.KnowledgeCitation;
import com.aurora.ai.knowledge.service.KnowledgeRetrievalService;
import com.aurora.ai.knowledge.support.CosineSimilarity;
import com.aurora.ai.knowledge.support.EmbeddingClient;
import com.aurora.ai.knowledge.support.KnowledgeDocStatus;
import com.aurora.ai.provider.entity.AiEmbeddingConfigDO;
import com.aurora.common.exception.BizException;
import com.aurora.common.response.BizCode;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class KnowledgeRetrievalServiceImpl implements KnowledgeRetrievalService {

    private static final int DEFAULT_TOP_K = 5;

    private final AiKnowledgeDocMapper docMapper;
    private final AiKnowledgeChunkMapper chunkMapper;
    private final EmbeddingClient embeddingClient;
    private final ObjectMapper objectMapper;

    @Override
    public List<KnowledgeCitation> search(String query, int topK) {
        if (!StringUtils.hasText(query)) {
            return List.of();
        }
        AiEmbeddingConfigDO config = embeddingClient.requireEnabledConfig();
        List<Double> queryVector = embeddingClient.embed(query);
        List<AiKnowledgeDocDO> docs = docMapper.selectList(Wrappers.<AiKnowledgeDocDO>lambdaQuery()
                .eq(AiKnowledgeDocDO::getStatus, KnowledgeDocStatus.PUBLISHED));
        if (docs.isEmpty()) {
            return List.of();
        }
        Map<Long, AiKnowledgeDocDO> docMap = new HashMap<>();
        docs.forEach(doc -> docMap.put(doc.getId(), doc));
        List<AiKnowledgeChunkDO> chunks = chunkMapper.selectList(Wrappers.<AiKnowledgeChunkDO>lambdaQuery()
                .in(AiKnowledgeChunkDO::getDocId, docMap.keySet())
                .isNotNull(AiKnowledgeChunkDO::getEmbeddingJson));
        List<KnowledgeCitation> citations = new ArrayList<>();
        for (AiKnowledgeChunkDO chunk : chunks) {
            if (chunk.getEmbeddingDimension() == null || !chunk.getEmbeddingDimension().equals(config.getDimension())) {
                continue;
            }
            List<Double> chunkVector = parseVector(chunk.getEmbeddingJson());
            double score = CosineSimilarity.score(queryVector, chunkVector);
            if (score <= 0D) {
                continue;
            }
            AiKnowledgeDocDO doc = docMap.get(chunk.getDocId());
            if (doc == null) {
                continue;
            }
            citations.add(KnowledgeCitation.builder()
                    .docId(doc.getId())
                    .docTitle(doc.getTitle())
                    .docType(doc.getType())
                    .chunkId(chunk.getId())
                    .snippet(snippet(chunk.getContent()))
                    .score(score)
                    .build());
        }
        int limit = topK < 1 ? DEFAULT_TOP_K : topK;
        return citations.stream()
                .sorted(Comparator.comparing(KnowledgeCitation::getScore).reversed())
                .limit(limit)
                .toList();
    }

    private List<Double> parseVector(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception ex) {
            throw new BizException(BizCode.DOC_PROCESSING_FAILED, "解析已存储向量数据失败");
        }
    }

    private String snippet(String content) {
        if (content == null || content.length() <= 240) {
            return content;
        }
        return content.substring(0, 240);
    }
}
