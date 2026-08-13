-- =============================================================================
-- AURORA 知识库向量库初始化脚本（PostgreSQL + PgVector）
-- 目标库：aurora_vector（需提前创建：createdb aurora_vector）
-- 执行方式：首次部署手动执行一次（spec 2026-08-13-rag-pgvector 决策 B1）
--   psql -d aurora_vector -f init.sql
-- 前置：PG 已安装 pgvector 扩展（CREATE EXTENSION 需 superuser 权限）
--
-- ⚠️ 向量维度：本脚本默认 vector(1536)，对应 OpenAI text-embedding 系列。
--    若你的 embedding 模型维度不同（如 768 / 1024），请把下方所有
--    `vector(1536)` 替换为你的实际维度，必须与 ai_embedding_config.dimension 一致，
--    否则写入时 PG 会抛 "vector dimension mismatch"。
-- =============================================================================

CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE IF NOT EXISTS ai_knowledge_chunk (
    id                  BIGINT       PRIMARY KEY,                 -- 应用层雪花生成（MyBatis-Plus ASSIGN_ID），不自增
    doc_id              BIGINT       NOT NULL,
    chunk_index         INTEGER      NOT NULL,
    content             TEXT         NOT NULL,
    token_count         INTEGER      NULL,
    embedding           vector(1536) NULL,                        -- 向量列；维度见顶部说明
    embedding_model     VARCHAR(100) NULL,
    embedding_dimension INTEGER      NULL,
    metadata_json       JSONB        NULL,
    create_user         BIGINT       NULL,
    create_time         TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_user         BIGINT       NULL,
    update_time         TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    deleted             SMALLINT     NOT NULL DEFAULT 0           -- 逻辑删除 0未删/1已删（与 MySQL 业务库约定一致）
);

-- 余弦相似度 ANN 索引（向量检索核心）
CREATE INDEX IF NOT EXISTS idx_chunk_embedding_hnsw
    ON ai_knowledge_chunk USING hnsw (embedding vector_cosine_ops);

-- 按 doc_id 清理 / 过滤
CREATE INDEX IF NOT EXISTS idx_chunk_doc_id
    ON ai_knowledge_chunk (doc_id);
