-- liquibase formatted sql

-- changeset aurora:ai_agent_v1_menu_20260716 dbms:mysql
-- validCheckSum: 9:6c8103b3dd84ce6a6c9a358a14a407d1
INSERT INTO sys_menu (id, parent_id, title, type, name, path, component, icon, permission, sort, visible, status, create_time, update_time, deleted)
VALUES
    (600, 0, 'AI Agent', 1, 'ai', '/ai', NULL, 'IconRobot', NULL, 600, 1, 1, NOW(), NOW(), 0),
    (610, 600, 'AI Chat', 2, 'ai-chat', '/ai/chat', 'ai/chat/index', 'IconMessage', 'ai:chat:use', 610, 0, 1, NOW(), NOW(), 0),
    (611, 610, 'AI Chat Use', 3, NULL, NULL, NULL, NULL, 'ai:chat:use', 611, 0, 1, NOW(), NOW(), 0),

    (620, 600, 'AI Providers', 2, 'ai-providers', '/ai/providers', 'ai/provider/index', 'IconCloud', 'ai:provider:list', 620, 1, 1, NOW(), NOW(), 0),
    (621, 620, 'Provider List', 3, NULL, NULL, NULL, NULL, 'ai:provider:list', 621, 0, 1, NOW(), NOW(), 0),
    (622, 620, 'Provider Create', 3, NULL, NULL, NULL, NULL, 'ai:provider:create', 622, 0, 1, NOW(), NOW(), 0),
    (623, 620, 'Provider Update', 3, NULL, NULL, NULL, NULL, 'ai:provider:update', 623, 0, 1, NOW(), NOW(), 0),
    (624, 620, 'Provider Test', 3, NULL, NULL, NULL, NULL, 'ai:provider:test', 624, 0, 1, NOW(), NOW(), 0),

    (630, 600, 'Knowledge Base', 2, 'ai-knowledge', '/ai/knowledge', 'ai/knowledge/index', 'IconBook', 'ai:knowledge:list', 630, 1, 1, NOW(), NOW(), 0),
    (631, 630, 'Knowledge List', 3, NULL, NULL, NULL, NULL, 'ai:knowledge:list', 631, 0, 1, NOW(), NOW(), 0),
    (632, 630, 'Knowledge Create', 3, NULL, NULL, NULL, NULL, 'ai:knowledge:create', 632, 0, 1, NOW(), NOW(), 0),
    (633, 630, 'Knowledge Update', 3, NULL, NULL, NULL, NULL, 'ai:knowledge:update', 633, 0, 1, NOW(), NOW(), 0),
    (634, 630, 'Knowledge Delete', 3, NULL, NULL, NULL, NULL, 'ai:knowledge:delete', 634, 0, 1, NOW(), NOW(), 0),
    (635, 630, 'Knowledge Publish', 3, NULL, NULL, NULL, NULL, 'ai:knowledge:publish', 635, 0, 1, NOW(), NOW(), 0),

    (640, 600, 'AI Audit', 3, NULL, NULL, NULL, NULL, 'ai:audit:list', 640, 0, 1, NOW(), NOW(), 0),
    (641, 600, 'AI Session List', 3, NULL, NULL, NULL, NULL, 'ai:session:list', 641, 0, 1, NOW(), NOW(), 0)
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
-- rollback DELETE FROM sys_menu WHERE id BETWEEN 600 AND 641;

-- changeset aurora:ai_agent_v1_admin_role_menu_20260716 dbms:mysql
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id
  FROM sys_role r
  JOIN sys_menu m ON m.id BETWEEN 600 AND 641
 WHERE r.code = 'admin'
   AND r.deleted = 0;
-- rollback DELETE rm FROM sys_role_menu rm JOIN sys_role r ON r.id = rm.role_id WHERE r.code = 'admin' AND rm.menu_id BETWEEN 600 AND 641;

-- changeset aurora:ai_agent_v1_student_chat_menu_20260716 dbms:mysql
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id
  FROM sys_role r
  JOIN sys_menu m ON m.id IN (600, 610, 611)
 WHERE r.code = 'student'
   AND r.deleted = 0;
-- rollback DELETE rm FROM sys_role_menu rm JOIN sys_role r ON r.id = rm.role_id WHERE r.code = 'student' AND rm.menu_id IN (600, 610, 611);

-- changeset aurora:ai_agent_v1_menu_zh_titles_20260716 dbms:mysql
UPDATE sys_menu SET title = 'AI 对话', update_time = NOW() WHERE id = 610;
UPDATE sys_menu SET title = '使用 AI 对话', update_time = NOW() WHERE id = 611;
UPDATE sys_menu SET title = '模型配置', update_time = NOW() WHERE id = 620;
UPDATE sys_menu SET title = '模型配置列表', update_time = NOW() WHERE id = 621;
UPDATE sys_menu SET title = '新增模型配置', update_time = NOW() WHERE id = 622;
UPDATE sys_menu SET title = '更新模型配置', update_time = NOW() WHERE id = 623;
UPDATE sys_menu SET title = '测试模型配置', update_time = NOW() WHERE id = 624;
UPDATE sys_menu SET title = '知识库', update_time = NOW() WHERE id = 630;
UPDATE sys_menu SET title = '知识库列表', update_time = NOW() WHERE id = 631;
UPDATE sys_menu SET title = '新增知识', update_time = NOW() WHERE id = 632;
UPDATE sys_menu SET title = '更新知识', update_time = NOW() WHERE id = 633;
UPDATE sys_menu SET title = '删除知识', update_time = NOW() WHERE id = 634;
UPDATE sys_menu SET title = '发布知识', update_time = NOW() WHERE id = 635;
UPDATE sys_menu SET title = 'AI 审计列表', update_time = NOW() WHERE id = 640;
UPDATE sys_menu SET title = 'AI 会话列表', update_time = NOW() WHERE id = 641;
-- rollback UPDATE sys_menu SET title = CASE id WHEN 610 THEN 'AI Chat' WHEN 611 THEN 'AI Chat Use' WHEN 620 THEN 'AI Providers' WHEN 621 THEN 'Provider List' WHEN 622 THEN 'Provider Create' WHEN 623 THEN 'Provider Update' WHEN 624 THEN 'Provider Test' WHEN 630 THEN 'Knowledge Base' WHEN 631 THEN 'Knowledge List' WHEN 632 THEN 'Knowledge Create' WHEN 633 THEN 'Knowledge Update' WHEN 634 THEN 'Knowledge Delete' WHEN 635 THEN 'Knowledge Publish' WHEN 640 THEN 'AI Audit' WHEN 641 THEN 'AI Session List' ELSE title END WHERE id BETWEEN 610 AND 641;

-- changeset aurora:ai_agent_v1_audit_menu_page_20260716 dbms:mysql
UPDATE sys_menu
   SET title = 'AI 审计',
       type = 2,
       name = 'ai-audit',
       path = '/ai/audit',
       component = 'ai/audit/index',
       icon = 'IconHistory',
       permission = 'ai:audit:list',
       sort = 640,
       visible = 1,
       status = 1,
       update_time = NOW(),
       deleted = 0
 WHERE id = 640;
-- rollback UPDATE sys_menu SET title = 'AI Audit', type = 3, name = NULL, path = NULL, component = NULL, icon = NULL, permission = 'ai:audit:list', sort = 640, visible = 0, update_time = NOW() WHERE id = 640;

-- changeset aurora:ai_agent_v1_session_menu_page_20260716 dbms:mysql
UPDATE sys_menu
   SET title = 'AI 会话',
       type = 2,
       name = 'ai-session',
       path = '/ai/sessions',
       component = 'ai/session/index',
       icon = 'IconMessage',
       permission = 'ai:session:list',
       sort = 650,
       visible = 1,
       status = 1,
       update_time = NOW(),
       deleted = 0
 WHERE id = 641;
-- rollback UPDATE sys_menu SET title = 'AI Session List', type = 3, name = NULL, path = NULL, component = NULL, icon = NULL, permission = 'ai:session:list', sort = 641, visible = 0, update_time = NOW() WHERE id = 641;
