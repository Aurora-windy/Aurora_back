-- liquibase formatted sql

-- changeset aurora:ai_agent_v1_schema_20260714 dbms:mysql
CREATE TABLE IF NOT EXISTS ai_model_provider (
  id BIGINT PRIMARY KEY,
  code VARCHAR(64) NOT NULL,
  name VARCHAR(100) NOT NULL,
  base_url VARCHAR(500) NOT NULL,
  api_key_cipher VARCHAR(1000) NULL,
  model VARCHAR(100) NOT NULL,
  temperature DECIMAL(4,2) NOT NULL DEFAULT 0.70,
  max_tokens INT NULL,
  timeout_seconds INT NOT NULL DEFAULT 60,
  enabled TINYINT NOT NULL DEFAULT 1,
  sort_order INT NOT NULL DEFAULT 0,
  create_user BIGINT NULL,
  create_time DATETIME NULL,
  update_user BIGINT NULL,
  update_time DATETIME NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_ai_model_provider_code_deleted(code, deleted),
  INDEX idx_ai_model_provider_enabled(enabled),
  INDEX idx_ai_model_provider_sort(sort_order)
);

CREATE TABLE IF NOT EXISTS ai_embedding_config (
  id BIGINT PRIMARY KEY,
  base_url VARCHAR(500) NOT NULL,
  api_key_cipher VARCHAR(1000) NULL,
  model VARCHAR(100) NOT NULL,
  dimension INT NOT NULL,
  timeout_seconds INT NOT NULL DEFAULT 60,
  enabled TINYINT NOT NULL DEFAULT 0,
  create_user BIGINT NULL,
  create_time DATETIME NULL,
  update_user BIGINT NULL,
  update_time DATETIME NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  INDEX idx_ai_embedding_config_enabled(enabled)
);

CREATE TABLE IF NOT EXISTS ai_knowledge_doc (
  id BIGINT PRIMARY KEY,
  title VARCHAR(200) NOT NULL,
  type VARCHAR(50) NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
  content MEDIUMTEXT NOT NULL,
  summary VARCHAR(1000) NULL,
  version INT NOT NULL DEFAULT 1,
  published_at DATETIME NULL,
  create_user BIGINT NULL,
  create_time DATETIME NULL,
  update_user BIGINT NULL,
  update_time DATETIME NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  INDEX idx_ai_knowledge_doc_status(status),
  INDEX idx_ai_knowledge_doc_type(type),
  INDEX idx_ai_knowledge_doc_published_at(published_at)
);

CREATE TABLE IF NOT EXISTS ai_knowledge_chunk (
  id BIGINT PRIMARY KEY,
  doc_id BIGINT NOT NULL,
  chunk_index INT NOT NULL,
  content TEXT NOT NULL,
  token_count INT NULL,
  embedding_json JSON NULL,
  embedding_model VARCHAR(100) NULL,
  embedding_dimension INT NULL,
  metadata_json JSON NULL,
  create_user BIGINT NULL,
  create_time DATETIME NULL,
  update_user BIGINT NULL,
  update_time DATETIME NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_ai_knowledge_chunk_doc_index_deleted(doc_id, chunk_index, deleted),
  INDEX idx_ai_knowledge_chunk_doc(doc_id),
  INDEX idx_ai_knowledge_chunk_embedding_model(embedding_model)
);

CREATE TABLE IF NOT EXISTS ai_chat_session (
  id BIGINT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  title VARCHAR(200) NOT NULL,
  provider_id BIGINT NULL,
  model VARCHAR(100) NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  last_message_at DATETIME NULL,
  create_user BIGINT NULL,
  create_time DATETIME NULL,
  update_user BIGINT NULL,
  update_time DATETIME NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  INDEX idx_ai_chat_session_user(user_id),
  INDEX idx_ai_chat_session_status(status),
  INDEX idx_ai_chat_session_last_message_at(last_message_at)
);

CREATE TABLE IF NOT EXISTS ai_chat_message (
  id BIGINT PRIMARY KEY,
  session_id BIGINT NOT NULL,
  role VARCHAR(32) NOT NULL,
  content MEDIUMTEXT NOT NULL,
  metadata_json JSON NULL,
  created_at DATETIME NULL,
  create_user BIGINT NULL,
  create_time DATETIME NULL,
  update_user BIGINT NULL,
  update_time DATETIME NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  INDEX idx_ai_chat_message_session(session_id),
  INDEX idx_ai_chat_message_role(role),
  INDEX idx_ai_chat_message_created_at(created_at)
);

CREATE TABLE IF NOT EXISTS ai_agent_action (
  id BIGINT PRIMARY KEY,
  session_id BIGINT NOT NULL,
  message_id BIGINT NULL,
  user_id BIGINT NOT NULL,
  action_type VARCHAR(64) NOT NULL,
  tool_name VARCHAR(100) NOT NULL,
  plan_summary VARCHAR(2000) NOT NULL,
  params_json JSON NULL,
  risk_summary VARCHAR(2000) NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'PENDING_CONFIRM',
  confirmed_at DATETIME NULL,
  executed_at DATETIME NULL,
  result_summary VARCHAR(2000) NULL,
  error_message VARCHAR(2000) NULL,
  create_user BIGINT NULL,
  create_time DATETIME NULL,
  update_user BIGINT NULL,
  update_time DATETIME NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  INDEX idx_ai_agent_action_session(session_id),
  INDEX idx_ai_agent_action_user_status(user_id, status),
  INDEX idx_ai_agent_action_tool_status(tool_name, status),
  INDEX idx_ai_agent_action_confirmed_at(confirmed_at),
  INDEX idx_ai_agent_action_executed_at(executed_at)
);

CREATE TABLE IF NOT EXISTS ai_tool_call_log (
  id BIGINT PRIMARY KEY,
  session_id BIGINT NULL,
  action_id BIGINT NULL,
  user_id BIGINT NOT NULL,
  provider_id BIGINT NULL,
  tool_name VARCHAR(100) NOT NULL,
  permission_code VARCHAR(100) NULL,
  params_summary VARCHAR(2000) NULL,
  success TINYINT NOT NULL DEFAULT 0,
  result_summary VARCHAR(2000) NULL,
  error_code VARCHAR(64) NULL,
  error_message VARCHAR(2000) NULL,
  started_at DATETIME NOT NULL,
  finished_at DATETIME NULL,
  create_user BIGINT NULL,
  create_time DATETIME NULL,
  update_user BIGINT NULL,
  update_time DATETIME NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  INDEX idx_ai_tool_call_log_session(session_id),
  INDEX idx_ai_tool_call_log_action(action_id),
  INDEX idx_ai_tool_call_log_user_started_at(user_id, started_at),
  INDEX idx_ai_tool_call_log_tool_started_at(tool_name, started_at),
  INDEX idx_ai_tool_call_log_success(success),
  INDEX idx_ai_tool_call_log_provider(provider_id)
);
-- rollback DROP TABLE IF EXISTS ai_tool_call_log;
-- rollback DROP TABLE IF EXISTS ai_agent_action;
-- rollback DROP TABLE IF EXISTS ai_chat_message;
-- rollback DROP TABLE IF EXISTS ai_chat_session;
-- rollback DROP TABLE IF EXISTS ai_knowledge_chunk;
-- rollback DROP TABLE IF EXISTS ai_knowledge_doc;
-- rollback DROP TABLE IF EXISTS ai_embedding_config;
-- rollback DROP TABLE IF EXISTS ai_model_provider;