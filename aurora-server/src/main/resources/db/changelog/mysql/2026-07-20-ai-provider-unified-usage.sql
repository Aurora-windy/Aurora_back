-- liquibase formatted sql

-- changeset aurora:ai_provider_unified_usage_20260720 dbms:mysql
ALTER TABLE ai_model_provider
  ADD COLUMN usage_type VARCHAR(20) NOT NULL DEFAULT 'CHAT' AFTER model,
  ADD COLUMN embedding_dimension INT NULL AFTER usage_type,
  ADD INDEX idx_ai_model_provider_usage_enabled(usage_type, enabled);
-- rollback ALTER TABLE ai_model_provider DROP INDEX idx_ai_model_provider_usage_enabled, DROP COLUMN embedding_dimension, DROP COLUMN usage_type;