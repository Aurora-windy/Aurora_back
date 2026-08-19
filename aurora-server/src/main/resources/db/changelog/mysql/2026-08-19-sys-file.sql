-- liquibase formatted sql

-- changeset aurora:sys_file_20260819 dbms:mysql
CREATE TABLE IF NOT EXISTS sys_file (
  id BIGINT PRIMARY KEY,
  name VARCHAR(255) NOT NULL COMMENT '原文件名',
  url VARCHAR(500) NOT NULL COMMENT '访问地址（/uploads/**）',
  relative_path VARCHAR(500) NULL COMMENT '存储相对路径',
  file_type VARCHAR(100) NULL COMMENT '扩展名',
  size BIGINT NULL COMMENT '文件大小（字节）',
  create_user BIGINT NULL,
  create_time DATETIME NULL,
  update_time DATETIME NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  INDEX idx_sys_file_name(name),
  INDEX idx_sys_file_create_time(create_time)
) COMMENT '文件管理';
-- rollback DROP TABLE IF EXISTS sys_file;

-- changeset aurora:sys_file_menu_20260819 dbms:mysql
INSERT INTO sys_menu (id, parent_id, title, type, name, path, component, icon, permission, sort, visible, status, create_time, update_time, deleted)
VALUES
    (150, 100, '文件管理', 2, 'system-file', '/system/file', 'system/file/index', 'IconFile', 'system:file:list', 150, 1, 1, NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE
    parent_id = VALUES(parent_id),
    title = VALUES(title),
    type = VALUES(type),
    name = VALUES(name),
    path = VALUES(path),
    component = VALUES(component),
    icon = VALUES(icon),
    permission = VALUES(permission),
    sort = VALUES(sort),
    visible = VALUES(visible),
    status = VALUES(status),
    update_time = NOW(),
    deleted = 0;
-- rollback DELETE FROM sys_menu WHERE id = 150;

-- changeset aurora:sys_file_admin_role_menu_20260819 dbms:mysql
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, 150
  FROM sys_role r
 WHERE r.code = 'admin'
   AND r.deleted = 0;
-- rollback DELETE rm FROM sys_role_menu rm JOIN sys_role r ON r.id = rm.role_id WHERE r.code = 'admin' AND rm.menu_id = 150;
