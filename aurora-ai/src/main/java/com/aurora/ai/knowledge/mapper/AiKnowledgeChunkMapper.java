package com.aurora.ai.knowledge.mapper;

import com.aurora.ai.knowledge.entity.AiKnowledgeChunkDO;
import com.aurora.ai.knowledge.model.resp.ChunkSearchPO;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 知识库分块 Mapper —— 走 PostgreSQL（PgVector）数据源。
 * <p>业务库仍为 MySQL；仅本表迁至 PG 以支持向量列与 ANN 检索（见 spec 2026-08-13-rag-pgvector）。</p>
 * <p>embedding 为 PG vector 类型，不映射进 DO，写入/检索均通过下方自定义 SQL 完成。</p>
 */
@Mapper
@DS("pgvector")
public interface AiKnowledgeChunkMapper extends BaseMapper<AiKnowledgeChunkDO> {

    /**
     * 写入分块及其向量。
     *
     * @param chunk     分块业务字段（id 已由调用方雪花生成）
     * @param embedding PgVector 字面量字符串 "[v1,v2,...]"，由 PG CAST AS vector 解析；
     *                  维度与表 vector(N) 不一致时 PG 抛 "vector dimension mismatch"
     */
    @Insert("INSERT INTO ai_knowledge_chunk " +
            "(id, doc_id, chunk_index, content, token_count, embedding, embedding_model, embedding_dimension, create_time, update_time, deleted) " +
            "VALUES " +
            "(#{chunk.id}, #{chunk.docId}, #{chunk.chunkIndex}, #{chunk.content}, #{chunk.tokenCount}, " +
            "CAST(#{embedding} AS vector), #{chunk.embeddingModel}, #{chunk.embeddingDimension}, " +
            "CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)")
    int insertChunkWithVector(@Param("chunk") AiKnowledgeChunkDO chunk, @Param("embedding") String embedding);

    /**
     * 向量检索：余弦距离 {@code <=>} 升序（值越小越相似），取 topK。
     * score = 1 - distance，越大越相似，与历史内存检索 CosineSimilarity 语义一致。
     */
    @Select("<script>" +
            "SELECT c.id, c.doc_id AS docId, c.chunk_index AS chunkIndex, c.content, " +
            "c.embedding_model AS embeddingModel, 1 - (c.embedding &lt;=&gt; CAST(#{queryVec} AS vector)) AS score " +
            "FROM ai_knowledge_chunk c " +
            "WHERE c.deleted = 0 AND c.embedding IS NOT NULL " +
            "AND c.doc_id IN " +
            "<foreach collection='docIds' item='dId' open='(' separator=',' close=')'>#{dId}</foreach> " +
            "ORDER BY c.embedding &lt;=&gt; CAST(#{queryVec} AS vector) " +
            "LIMIT #{topK}" +
            "</script>")
    List<ChunkSearchPO> searchByVector(@Param("docIds") List<Long> docIds,
                                       @Param("queryVec") String queryVec,
                                       @Param("topK") int topK);

    /**
     * 按 doc 物理清理分块（重建 embedding 用）。chunk 为可重建的衍生数据，
     * 物理删除避免死记录堆积污染 HNSW 向量索引。
     */
    @Delete("DELETE FROM ai_knowledge_chunk WHERE doc_id = #{docId}")
    int deleteByDocId(@Param("docId") Long docId);
}
