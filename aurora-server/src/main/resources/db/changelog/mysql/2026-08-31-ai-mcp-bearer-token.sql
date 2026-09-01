-- liquibase formatted sql

-- changeset aurora:ai_mcp_bearer_token_20260831 dbms:mysql
-- MCP Client 连接远程 Server 使用的 Bearer Token，按项目密钥约定保存密文。
ALTER TABLE ai_mcp_server
  ADD COLUMN bearer_token_cipher VARCHAR(1200) NULL COMMENT '远程 MCP server Bearer Token 密文' AFTER base_url;

-- rollback ALTER TABLE ai_mcp_server DROP COLUMN bearer_token_cipher;
