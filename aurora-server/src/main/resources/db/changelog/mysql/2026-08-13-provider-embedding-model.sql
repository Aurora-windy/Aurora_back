-- liquibase formatted sql

-- changeset aurora:provider_embedding_model_20260813 dbms:mysql
-- ai_model_provider 增加 embedding_model 字段：同一 provider 的 chat 模型与 embedding 模型可分别指定，
-- 让 embedding 配置并入大模型 provider 表单，前端无需第二套配置。为空时调用方 fallback 到 model。
ALTER TABLE ai_model_provider ADD COLUMN embedding_model VARCHAR(100) NULL AFTER model;

-- rollback ALTER TABLE ai_model_provider DROP COLUMN embedding_model;
