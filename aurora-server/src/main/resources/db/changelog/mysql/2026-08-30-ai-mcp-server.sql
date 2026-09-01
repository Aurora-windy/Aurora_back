-- liquibase formatted sql

-- changeset aurora:ai_mcp_server_20260830 dbms:mysql
-- MCP Server 配置表：存储外部 MCP server 的连接信息，Client 方向（AURORA 连接外部 MCP server）。
-- v1 仅支持 HTTP 型（baseUrl），stdio 协议层 SDK 自带但不开放配置入口（spec 2026-08-29 决策 #5）。
CREATE TABLE IF NOT EXISTS ai_mcp_server (
  id BIGINT PRIMARY KEY,
  code VARCHAR(64) NOT NULL COMMENT '唯一标识，用于工具名前缀 mcp.<code>.<tool>',
  name VARCHAR(100) NOT NULL COMMENT '显示名称',
  description VARCHAR(500) NULL COMMENT '描述',
  base_url VARCHAR(500) NOT NULL COMMENT 'MCP server 的 HTTP SSE 端点',
  timeout_seconds INT NOT NULL DEFAULT 10 COMMENT '单次 tools/call 超时上限（秒）',
  enabled TINYINT NOT NULL DEFAULT 1 COMMENT '启用状态：0=禁用 1=启用',
  tool_count INT NULL COMMENT '最近一次同步的工具数量（同步后回填）',
  last_sync_at DATETIME NULL COMMENT '最近一次成功同步的时间',
  last_error VARCHAR(1000) NULL COMMENT '最近一次同步/调用的错误信息（降级记录）',
  create_user BIGINT NULL,
  create_time DATETIME NULL,
  update_user BIGINT NULL,
  update_time DATETIME NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_ai_mcp_server_code_deleted(code, deleted),
  INDEX idx_ai_mcp_server_enabled(enabled)
);
-- rollback DROP TABLE IF EXISTS ai_mcp_server;
