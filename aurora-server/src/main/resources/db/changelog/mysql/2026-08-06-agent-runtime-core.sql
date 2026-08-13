-- liquibase formatted sql

-- changeset aurora:agent_runtime_core_20260806 dbms:mysql
-- Agent Runtime 核心表：任务、事件和可观测 Trace。
CREATE TABLE IF NOT EXISTS ai_agent_task (
  id BIGINT PRIMARY KEY,
  session_id BIGINT NULL,
  user_id BIGINT NOT NULL,
  task_type VARCHAR(100) NOT NULL,
  input_json JSON NULL,
  state VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  idempotency_key VARCHAR(128) NULL,
  retry_count INT NOT NULL DEFAULT 0,
  max_retries INT NOT NULL DEFAULT 1,
  started_at DATETIME NULL,
  finished_at DATETIME NULL,
  cancelled_at DATETIME NULL,
  failure_code VARCHAR(64) NULL,
  failure_message VARCHAR(2000) NULL,
  create_user BIGINT NULL,
  create_time DATETIME NULL,
  update_user BIGINT NULL,
  update_time DATETIME NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_ai_agent_task_user_idempotency(user_id, idempotency_key, deleted),
  INDEX idx_ai_agent_task_user_state(user_id, state),
  INDEX idx_ai_agent_task_session(session_id),
  INDEX idx_ai_agent_task_create_time(create_time)
);

CREATE TABLE IF NOT EXISTS ai_agent_runtime_event (
  id BIGINT PRIMARY KEY,
  task_id BIGINT NOT NULL,
  sequence BIGINT NOT NULL,
  event_type VARCHAR(64) NOT NULL,
  payload_json JSON NULL,
  occurred_at DATETIME NOT NULL,
  create_user BIGINT NULL,
  create_time DATETIME NULL,
  update_user BIGINT NULL,
  update_time DATETIME NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_ai_agent_runtime_event_task_sequence_deleted(task_id, sequence, deleted),
  INDEX idx_ai_agent_runtime_event_task_sequence(task_id, sequence),
  INDEX idx_ai_agent_runtime_event_type(event_type)
);

CREATE TABLE IF NOT EXISTS ai_agent_trace (
  id BIGINT PRIMARY KEY,
  task_id BIGINT NOT NULL,
  parent_trace_id BIGINT NULL,
  span_type VARCHAR(32) NOT NULL,
  span_name VARCHAR(128) NOT NULL,
  state VARCHAR(32) NOT NULL,
  input_summary VARCHAR(2000) NULL,
  output_summary VARCHAR(2000) NULL,
  error_code VARCHAR(64) NULL,
  started_at DATETIME NOT NULL,
  finished_at DATETIME NULL,
  create_user BIGINT NULL,
  create_time DATETIME NULL,
  update_user BIGINT NULL,
  update_time DATETIME NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  INDEX idx_ai_agent_trace_task_started(task_id, started_at),
  INDEX idx_ai_agent_trace_parent(parent_trace_id),
  INDEX idx_ai_agent_trace_state(state)
);

-- rollback DROP TABLE IF EXISTS ai_agent_trace;
-- rollback DROP TABLE IF EXISTS ai_agent_runtime_event;
-- rollback DROP TABLE IF EXISTS ai_agent_task;
