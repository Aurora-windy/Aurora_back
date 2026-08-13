package com.aurora.ai.knowledge.service.impl;

import com.aurora.ai.knowledge.entity.AiKnowledgeDocDO;
import com.aurora.ai.knowledge.mapper.AiKnowledgeChunkMapper;
import com.aurora.ai.knowledge.mapper.AiKnowledgeDocMapper;
import com.aurora.ai.knowledge.model.resp.ChunkSearchPO;
import com.aurora.ai.knowledge.model.resp.KnowledgeCitation;
import com.aurora.ai.knowledge.service.KnowledgeRetrievalService;
import com.aurora.ai.knowledge.support.EmbeddingClient;
import com.aurora.ai.knowledge.support.KnowledgeDocStatus;
import com.aurora.common.exception.BizException;
import com.aurora.common.response.BizCode;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 知识库检索服务。
 * <p>架构：业务库 MySQL（ai_knowledge_doc）+ 向量库 PostgreSQL/PgVector（ai_knowledge_chunk）。
 * docMapper 走默认 master=MySQL 源；chunkMapper 标注 @DS("pgvector") 走 PG 源。</p>
 * <p>检索改为 PG 侧余弦距离 ANN（HNSW 索引），取代此前的内存全量余弦计算。
 * 旧实现见 {@link com.aurora.ai.knowledge.support.CosineSimilarity}（保留备用）。</p>
 */
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
        embeddingClient.requireEnabledConfig();
        // 1. 查询向量化，拼成 PgVector 字面量 "[v1,v2,...]"
        String queryVecLiteral = toVectorLiteral(embeddingClient.embed(query));
        // 2. 取已发布文档（MySQL；若 embedding 维度与表 vector(N) 漂移导致 PG 报错，
        //    上层 AgentOrchestrator#safeSearch 会兜底返回空引用，问答降级而非崩溃）
        List<AiKnowledgeDocDO> docs = docMapper.selectList(Wrappers.<AiKnowledgeDocDO>lambdaQuery()
                .eq(AiKnowledgeDocDO::getStatus, KnowledgeDocStatus.PUBLISHED));
        if (docs.isEmpty()) {
            return List.of();
        }
        Map<Long, AiKnowledgeDocDO> docMap = new HashMap<>();
        docs.forEach(doc -> docMap.put(doc.getId(), doc));
        // 3. 向量检索（PG + HNSW 余弦索引，ORDER BY embedding <=> queryVec LIMIT topK）
        int limit = topK < 1 ? DEFAULT_TOP_K : topK;
        List<ChunkSearchPO> hits = chunkMapper.searchByVector(new ArrayList<>(docMap.keySet()), queryVecLiteral, limit);
        // 4. 回填文档元信息组装引用（score = 1 - 余弦距离，越大越相似，由 PG 计算）
        List<KnowledgeCitation> citations = new ArrayList<>();
        for (ChunkSearchPO hit : hits) {
            if (hit.getScore() == null || hit.getScore() <= 0D) {
                continue;
            }
            AiKnowledgeDocDO doc = docMap.get(hit.getDocId());
            if (doc == null) {
                continue;
            }
            citations.add(KnowledgeCitation.builder()
                    .docId(doc.getId())
                    .docTitle(doc.getTitle())
                    .docType(doc.getType())
                    .chunkId(hit.getId())
                    .snippet(snippet(hit.getContent()))
                    .score(hit.getScore())
                    .build());
        }
        return citations;
    }

    private String toVectorLiteral(List<Double> embedding) {
        try {
            return objectMapper.writeValueAsString(embedding);
        } catch (JsonProcessingException ex) {
            throw new BizException(BizCode.DOC_PROCESSING_FAILED, "序列化查询向量失败");
        }
    }

    private String snippet(String content) {
        if (content == null || content.length() <= 240) {
            return content;
        }
        return content.substring(0, 240);
    }
}
