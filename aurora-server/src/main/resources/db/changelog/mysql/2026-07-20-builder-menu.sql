-- liquibase formatted sql

-- changeset aurora:builder_batch1_menu_20260720 dbms:mysql
INSERT INTO sys_menu (id, parent_id, title, type, name, path, component, icon, permission, sort, visible, status, create_time, update_time, deleted)
VALUES
    (700, 0, 'AURORA Builder', 1, 'builder-root', '/builder', NULL, 'IconCode', NULL, 700, 1, 1, NOW(), NOW(), 0),
    (710, 700, '项目生成器', 2, 'builder', '/builder', 'builder/index', 'IconCode', 'builder:module:list', 710, 1, 1, NOW(), NOW(), 0),
    (711, 710, '模块列表', 3, NULL, NULL, NULL, NULL, 'builder:module:list', 711, 0, 1, NOW(), NOW(), 0),
    (712, 710, '需求解析', 3, NULL, NULL, NULL, NULL, 'builder:parse:use', 712, 0, 1, NOW(), NOW(), 0)
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
-- rollback DELETE FROM sys_menu WHERE id BETWEEN 700 AND 712;

-- changeset aurora:builder_batch1_admin_role_menu_20260720 dbms:mysql
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id
  FROM sys_role r
  JOIN sys_menu m ON m.id BETWEEN 700 AND 712
 WHERE r.code = 'admin'
   AND r.deleted = 0;
-- rollback DELETE rm FROM sys_role_menu rm JOIN sys_role r ON r.id = rm.role_id WHERE r.code = 'admin' AND rm.menu_id BETWEEN 700 AND 712;
