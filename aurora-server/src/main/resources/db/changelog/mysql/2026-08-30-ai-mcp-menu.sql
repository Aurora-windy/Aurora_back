-- liquibase formatted sql

-- changeset aurora:ai_mcp_server_menu_20260830 dbms:mysql
-- MCP Server 管理菜单（T-M2）：挂在 AI Agent (600) 下，sort=660。
INSERT INTO sys_menu (id, parent_id, title, type, name, path, component, icon, permission, sort, visible, status, create_time, update_time, deleted)
VALUES
    (660, 600, 'MCP Server', 2, 'ai-mcp-servers', '/ai/mcp-servers', 'ai/mcp/index', 'IconLink', 'ai:mcp:list', 660, 1, 1, NOW(), NOW(), 0),
    (661, 660, 'MCP List', 3, NULL, NULL, NULL, NULL, 'ai:mcp:list', 661, 0, 1, NOW(), NOW(), 0),
    (662, 660, 'MCP Create', 3, NULL, NULL, NULL, NULL, 'ai:mcp:create', 662, 0, 1, NOW(), NOW(), 0),
    (663, 660, 'MCP Update', 3, NULL, NULL, NULL, NULL, 'ai:mcp:update', 663, 0, 1, NOW(), NOW(), 0),
    (664, 660, 'MCP Delete', 3, NULL, NULL, NULL, NULL, 'ai:mcp:delete', 664, 0, 1, NOW(), NOW(), 0),
    (665, 660, 'MCP Sync', 3, NULL, NULL, NULL, NULL, 'ai:mcp:sync', 665, 0, 1, NOW(), NOW(), 0)
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
-- rollback DELETE FROM sys_menu WHERE id BETWEEN 660 AND 665;

-- changeset aurora:ai_mcp_server_admin_role_menu_20260830 dbms:mysql
-- 管理员角色自动关联 MCP 菜单
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id
  FROM sys_role r
  JOIN sys_menu m ON m.id BETWEEN 660 AND 665
 WHERE r.code = 'admin'
   AND r.deleted = 0;
-- rollback DELETE rm FROM sys_role_menu rm JOIN sys_role r ON r.id = rm.role_id WHERE r.code = 'admin' AND rm.menu_id BETWEEN 660 AND 665;
