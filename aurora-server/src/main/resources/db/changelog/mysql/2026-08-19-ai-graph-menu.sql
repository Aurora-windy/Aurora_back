-- liquibase formatted sql

-- changeset aurora:ai_graph_menu_20260819 dbms:mysql
INSERT INTO sys_menu (id, parent_id, title, type, name, path, component, icon, permission, sort, visible, status, create_time, update_time, deleted)
VALUES
    (660, 600, '知识图谱', 2, 'ai-graph', '/ai/graph', 'ai/graph/index', 'IconGraph', 'ai:graph:list', 660, 1, 1, NOW(), NOW(), 0)
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
-- rollback DELETE FROM sys_menu WHERE id = 660;

-- changeset aurora:ai_graph_admin_role_menu_20260819 dbms:mysql
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, 660
  FROM sys_role r
 WHERE r.code = 'admin'
   AND r.deleted = 0;
-- rollback DELETE rm FROM sys_role_menu rm JOIN sys_role r ON r.id = rm.role_id WHERE r.code = 'admin' AND rm.menu_id = 660;

-- changeset aurora:ai_graph_student_role_menu_20260819 dbms:mysql
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, 660
  FROM sys_role r
 WHERE r.code = 'student'
   AND r.deleted = 0;
-- rollback DELETE rm FROM sys_role_menu rm JOIN sys_role r ON r.id = rm.role_id WHERE r.code = 'student' AND rm.menu_id = 660;
