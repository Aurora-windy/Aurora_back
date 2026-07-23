-- liquibase formatted sql
--
-- 修复：HR seed（2026-07-23-hr-core-org.sql）误用 AI 占用的 600-641 段，
-- ON DUPLICATE KEY UPDATE 把 AI 菜单整行覆盖成 HR 内容，并在 610 下留下 612/613/614
-- 孤儿按钮，还把 hr_admin 错绑到 6xx。
-- 处理：恢复 AI 600-641 原值（含 zh_titles 与 audit/session 升级为菜单页后的最终状态），
-- 删孤儿 612/613/614，清 hr_admin 对 6xx 的错绑，HR 菜单整体迁到空闲的 800-834 段。
-- 幂等：所有 changeset 重复执行结果不变；不改动已执行的 hr-core-org.sql / ai-menu.sql，
-- 避免 Liquibase checksum mismatch。
-- 前端无需改动：组件 key（hr/dept|position|employee/index）不变，菜单 id 只在后端。

-- changeset aurora:restore_ai_menu_600_641_20260723 dbms:mysql
INSERT INTO sys_menu (id, parent_id, title, type, name, path, component, icon, permission, sort, visible, status, create_time, update_time, deleted)
VALUES
    (600, 0, 'AI Agent', 1, 'ai', '/ai', NULL, 'IconRobot', NULL, 600, 1, 1, NOW(), NOW(), 0),
    (610, 600, 'AI 对话', 2, 'ai-chat', '/ai/chat', 'ai/chat/index', 'IconMessage', 'ai:chat:use', 610, 0, 1, NOW(), NOW(), 0),
    (611, 610, '使用 AI 对话', 3, NULL, NULL, NULL, NULL, 'ai:chat:use', 611, 0, 1, NOW(), NOW(), 0),
    (620, 600, '模型配置', 2, 'ai-providers', '/ai/providers', 'ai/provider/index', 'IconCloud', 'ai:provider:list', 620, 1, 1, NOW(), NOW(), 0),
    (621, 620, '模型配置列表', 3, NULL, NULL, NULL, NULL, 'ai:provider:list', 621, 0, 1, NOW(), NOW(), 0),
    (622, 620, '新增模型配置', 3, NULL, NULL, NULL, NULL, 'ai:provider:create', 622, 0, 1, NOW(), NOW(), 0),
    (623, 620, '更新模型配置', 3, NULL, NULL, NULL, NULL, 'ai:provider:update', 623, 0, 1, NOW(), NOW(), 0),
    (624, 620, '测试模型配置', 3, NULL, NULL, NULL, NULL, 'ai:provider:test', 624, 0, 1, NOW(), NOW(), 0),
    (630, 600, '知识库', 2, 'ai-knowledge', '/ai/knowledge', 'ai/knowledge/index', 'IconBook', 'ai:knowledge:list', 630, 1, 1, NOW(), NOW(), 0),
    (631, 630, '知识库列表', 3, NULL, NULL, NULL, NULL, 'ai:knowledge:list', 631, 0, 1, NOW(), NOW(), 0),
    (632, 630, '新增知识', 3, NULL, NULL, NULL, NULL, 'ai:knowledge:create', 632, 0, 1, NOW(), NOW(), 0),
    (633, 630, '更新知识', 3, NULL, NULL, NULL, NULL, 'ai:knowledge:update', 633, 0, 1, NOW(), NOW(), 0),
    (634, 630, '删除知识', 3, NULL, NULL, NULL, NULL, 'ai:knowledge:delete', 634, 0, 1, NOW(), NOW(), 0),
    (635, 630, '发布知识', 3, NULL, NULL, NULL, NULL, 'ai:knowledge:publish', 635, 0, 1, NOW(), NOW(), 0),
    (640, 600, 'AI 审计', 2, 'ai-audit', '/ai/audit', 'ai/audit/index', 'IconHistory', 'ai:audit:list', 640, 1, 1, NOW(), NOW(), 0),
    (641, 600, 'AI 会话', 2, 'ai-session', '/ai/sessions', 'ai/session/index', 'IconMessage', 'ai:session:list', 650, 1, 1, NOW(), NOW(), 0)
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
-- rollback SELECT 'no-op: AI menu reconciliation' FROM dual;

-- changeset aurora:cleanup_hr_orphan_menu_20260723 dbms:mysql
DELETE FROM sys_role_menu WHERE menu_id IN (612, 613, 614);
DELETE FROM sys_menu WHERE id IN (612, 613, 614);
-- rollback SELECT 'no-op: orphan cleanup' FROM dual;

-- changeset aurora:cleanup_hr_admin_ai_role_menu_20260723 dbms:mysql
DELETE rm FROM sys_role_menu rm
JOIN sys_role r ON r.id = rm.role_id
WHERE r.code = 'hr_admin' AND rm.menu_id BETWEEN 600 AND 641;
-- rollback SELECT 'no-op: hr_admin ai role-menu cleanup' FROM dual;

-- changeset aurora:relocate_hr_menu_to_800_20260723 dbms:mysql
INSERT INTO sys_menu (id, parent_id, title, type, name, path, component, icon, permission, sort, visible, status, create_time, update_time, deleted)
VALUES
    (800, 0, '人事管理', 1, 'hr', '/hr', NULL, 'IconUserGroup', NULL, 800, 1, 1, NOW(), NOW(), 0),
    (810, 800, '部门管理', 2, 'hr-depts', '/hr/depts', 'hr/dept/index', 'IconCommon', 'hr:dept:list', 810, 1, 1, NOW(), NOW(), 0),
    (811, 810, '部门查询', 3, NULL, NULL, NULL, NULL, 'hr:dept:list', 811, 0, 1, NOW(), NOW(), 0),
    (812, 810, '部门新增', 3, NULL, NULL, NULL, NULL, 'hr:dept:add', 812, 0, 1, NOW(), NOW(), 0),
    (813, 810, '部门编辑', 3, NULL, NULL, NULL, NULL, 'hr:dept:edit', 813, 0, 1, NOW(), NOW(), 0),
    (814, 810, '部门删除', 3, NULL, NULL, NULL, NULL, 'hr:dept:remove', 814, 0, 1, NOW(), NOW(), 0),
    (820, 800, '岗位管理', 2, 'hr-positions', '/hr/positions', 'hr/position/index', 'IconStorage', 'hr:position:list', 820, 1, 1, NOW(), NOW(), 0),
    (821, 820, '岗位查询', 3, NULL, NULL, NULL, NULL, 'hr:position:list', 821, 0, 1, NOW(), NOW(), 0),
    (822, 820, '岗位新增', 3, NULL, NULL, NULL, NULL, 'hr:position:add', 822, 0, 1, NOW(), NOW(), 0),
    (823, 820, '岗位编辑', 3, NULL, NULL, NULL, NULL, 'hr:position:edit', 823, 0, 1, NOW(), NOW(), 0),
    (824, 820, '岗位删除', 3, NULL, NULL, NULL, NULL, 'hr:position:remove', 824, 0, 1, NOW(), NOW(), 0),
    (830, 800, '员工管理', 2, 'hr-employees', '/hr/employees', 'hr/employee/index', 'IconUser', 'hr:employee:list', 830, 1, 1, NOW(), NOW(), 0),
    (831, 830, '员工查询', 3, NULL, NULL, NULL, NULL, 'hr:employee:list', 831, 0, 1, NOW(), NOW(), 0),
    (832, 830, '员工新增', 3, NULL, NULL, NULL, NULL, 'hr:employee:add', 832, 0, 1, NOW(), NOW(), 0),
    (833, 830, '员工编辑', 3, NULL, NULL, NULL, NULL, 'hr:employee:edit', 833, 0, 1, NOW(), NOW(), 0),
    (834, 830, '员工删除', 3, NULL, NULL, NULL, NULL, 'hr:employee:remove', 834, 0, 1, NOW(), NOW(), 0)
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
-- rollback DELETE FROM sys_menu WHERE id BETWEEN 800 AND 834;

-- changeset aurora:relocate_hr_role_menu_to_800_20260723 dbms:mysql
DELETE FROM sys_role_menu WHERE menu_id BETWEEN 800 AND 834;
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id
  FROM sys_role r
  JOIN sys_menu m ON m.id BETWEEN 800 AND 834
 WHERE r.code IN ('admin', 'hr_admin')
   AND r.deleted = 0;
-- rollback DELETE FROM sys_role_menu WHERE menu_id BETWEEN 800 AND 834;
