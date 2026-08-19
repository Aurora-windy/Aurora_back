-- liquibase formatted sql

-- changeset aurora:ai_knowledge_doc_file_url_20260819 dbms:mysql
ALTER TABLE ai_knowledge_doc
    ADD COLUMN file_url VARCHAR(500) NULL COMMENT '原文件访问地址（本地文件上传保存后）' AFTER type;
-- rollback ALTER TABLE ai_knowledge_doc DROP COLUMN file_url;
