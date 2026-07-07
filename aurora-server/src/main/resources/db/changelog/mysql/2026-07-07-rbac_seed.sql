-- liquibase formatted sql

-- changeset aurora:normalize_rbac_roles_20260707 dbms:mysql
-- 与 RoleCodeConst 对齐：admin / hr_admin / edu_teacher / mall_admin / student
INSERT INTO sys_role (id, name, code, data_scope, sort, status, remark, create_time, update_time, deleted)
VALUES
    (1, '超级管理员', 'admin',       1, 1, 1, '拥有所有系统全部权限', NOW(), NOW(), 0),
    (2, '人事管理员', 'hr_admin',    2, 2, 1, 'HR 模块管理员，本部门及下级数据权限', NOW(), NOW(), 0),
    (3, '教师',       'edu_teacher', 3, 3, 1, '教务教师角色，本人数据权限', NOW(), NOW(), 0),
    (5, '电商管理员', 'mall_admin',  1, 4, 1, '电商模块管理员', NOW(), NOW(), 0),
    (7, '学生',       'student',     3, 5, 1, '学生/普通用户角色', NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    code = VALUES(code),
    data_scope = VALUES(data_scope),
    sort = VALUES(sort),
    status = VALUES(status),
    remark = VALUES(remark),
    update_time = NOW(),
    deleted = 0;

UPDATE sys_role
   SET status = 0,
       remark = CONCAT(COALESCE(remark, ''), '（已被 AURORA 五角色体系替代）'),
       update_time = NOW()
 WHERE code IN ('edu_admin', 'oj_admin', 'ai_admin');
-- rollback UPDATE sys_role SET status=1 WHERE code IN ('edu_admin', 'oj_admin', 'ai_admin');

-- changeset aurora:seed_rbac_menus_20260707 dbms:mysql
INSERT INTO sys_menu (id, parent_id, title, type, name, path, component, icon, permission, sort, visible, status, create_time, update_time, deleted)
VALUES
    (10, 0, '工作台', 2, 'workbench', '/workbench', 'workbench/WorkbenchView', 'IconDashboard', NULL, 10, 1, 1, NOW(), NOW(), 0),

    (100, 0, '系统管理', 1, 'system', '/system', NULL, 'IconSettings', NULL, 100, 1, 1, NOW(), NOW(), 0),
    (110, 100, '用户管理', 2, 'system-users', '/system/users', 'system/user/index', 'IconUser', 'system:user:list', 110, 1, 1, NOW(), NOW(), 0),
    (111, 110, '用户查询', 3, NULL, NULL, NULL, NULL, 'system:user:list', 111, 0, 1, NOW(), NOW(), 0),
    (112, 110, '用户新增', 3, NULL, NULL, NULL, NULL, 'system:user:add', 112, 0, 1, NOW(), NOW(), 0),
    (113, 110, '用户编辑', 3, NULL, NULL, NULL, NULL, 'system:user:edit', 113, 0, 1, NOW(), NOW(), 0),
    (114, 110, '用户删除', 3, NULL, NULL, NULL, NULL, 'system:user:remove', 114, 0, 1, NOW(), NOW(), 0),
    (115, 110, '重置密码', 3, NULL, NULL, NULL, NULL, 'system:user:reset-password', 115, 0, 1, NOW(), NOW(), 0),
    (116, 110, '用户启停', 3, NULL, NULL, NULL, NULL, 'system:user:status', 116, 0, 1, NOW(), NOW(), 0),

    (120, 100, '角色管理', 2, 'system-roles', '/system/roles', 'system/role/index', 'IconUserGroup', 'system:role:list', 120, 1, 1, NOW(), NOW(), 0),
    (121, 120, '角色查询', 3, NULL, NULL, NULL, NULL, 'system:role:list', 121, 0, 1, NOW(), NOW(), 0),
    (122, 120, '角色新增', 3, NULL, NULL, NULL, NULL, 'system:role:add', 122, 0, 1, NOW(), NOW(), 0),
    (123, 120, '角色编辑', 3, NULL, NULL, NULL, NULL, 'system:role:edit', 123, 0, 1, NOW(), NOW(), 0),
    (124, 120, '角色删除', 3, NULL, NULL, NULL, NULL, 'system:role:remove', 124, 0, 1, NOW(), NOW(), 0),
    (125, 120, '分配菜单', 3, NULL, NULL, NULL, NULL, 'system:role:assign-menu', 125, 0, 1, NOW(), NOW(), 0),

    (130, 100, '菜单管理', 2, 'system-menus', '/system/menus', 'system/menu/index', 'IconMenu', 'system:menu:list', 130, 1, 1, NOW(), NOW(), 0),
    (131, 130, '菜单查询', 3, NULL, NULL, NULL, NULL, 'system:menu:list', 131, 0, 1, NOW(), NOW(), 0),
    (132, 130, '菜单新增', 3, NULL, NULL, NULL, NULL, 'system:menu:add', 132, 0, 1, NOW(), NOW(), 0),
    (133, 130, '菜单编辑', 3, NULL, NULL, NULL, NULL, 'system:menu:edit', 133, 0, 1, NOW(), NOW(), 0),
    (134, 130, '菜单删除', 3, NULL, NULL, NULL, NULL, 'system:menu:remove', 134, 0, 1, NOW(), NOW(), 0)
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
-- rollback DELETE FROM sys_menu WHERE id BETWEEN 10 AND 134;

-- changeset aurora:seed_rbac_role_menu_20260707 dbms:mysql
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id
  FROM sys_role r
  JOIN sys_menu m ON m.id IN (10, 100, 110, 111, 112, 113, 114, 115, 116, 120, 121, 122, 123, 124, 125, 130, 131, 132, 133, 134)
 WHERE r.code = 'admin';

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, 10
  FROM sys_role r
 WHERE r.code IN ('hr_admin', 'edu_teacher', 'mall_admin', 'student');
-- rollback DELETE FROM sys_role_menu WHERE menu_id IN (10, 100, 110, 111, 112, 113, 114, 115, 116, 120, 121, 122, 123, 124, 125, 130, 131, 132, 133, 134);
