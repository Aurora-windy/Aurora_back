-- liquibase formatted sql

-- changeset aurora:local_workspace_capability_20260830 dbms:mysql
-- 会话级本地工作区只读能力，默认关闭。历史会话通过 DEFAULT 兼容升级。
ALTER TABLE ai_chat_session
  ADD COLUMN local_files_enabled TINYINT NOT NULL DEFAULT 0
  COMMENT '本地工作区只读能力：0=关闭 1=开启';
-- rollback ALTER TABLE ai_chat_session DROP COLUMN local_files_enabled;
