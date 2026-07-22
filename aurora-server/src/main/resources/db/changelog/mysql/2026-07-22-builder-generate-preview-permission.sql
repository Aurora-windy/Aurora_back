-- liquibase formatted sql

-- changeset aurora:builder_generate_preview_permission_20260722 dbms:mysql
INSERT INTO sys_menu (id, parent_id, title, type, name, path, component, icon, permission, sort, visible, status, create_time, update_time, deleted)
VALUES
    (713, 710, '生成预览', 3, NULL, NULL, NULL, NULL, 'builder:generate:preview', 713, 0, 1, NOW(), NOW(), 0)
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
-- rollback DELETE FROM sys_menu WHERE id = 713;

-- changeset aurora:builder_generate_preview_admin_role_menu_20260722 dbms:mysql
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, 713
  FROM sys_role r
 WHERE r.code = 'admin'
   AND r.deleted = 0;
-- rollback DELETE rm FROM sys_role_menu rm JOIN sys_role r ON r.id = rm.role_id WHERE r.code = 'admin' AND rm.menu_id = 713;
