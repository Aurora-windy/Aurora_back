-- liquibase formatted sql

-- changeset aurora:seed_sys_role dbms:mysql
INSERT INTO sys_role (id, name, code, data_scope, sort, status, remark, create_time, update_time, deleted)
VALUES
    (1, '超级管理员',  'admin',      1, 1, 1, '拥有所有系统全部权限，程序逻辑跳过权限检查',         NOW(), NOW(), 0),
    (2, '人事管理员',  'hr_admin',   1, 2, 1, 'HR 系统管理员（部门级权限 Phase 2 实装时细化）',     NOW(), NOW(), 0),
    (3, '教务管理员',  'edu_admin',  1, 3, 1, '教务系统管理员',                                     NOW(), NOW(), 0),
    (4, 'OJ 管理员',   'oj_admin',   1, 4, 1, '在线判题系统管理员',                                 NOW(), NOW(), 0),
    (5, '电商管理员',  'mall_admin', 1, 5, 1, '电商系统管理员',                                     NOW(), NOW(), 0),
    (6, 'AI 管理员',   'ai_admin',   1, 6, 1, 'AI 智能体系统管理员',                                NOW(), NOW(), 0);
-- rollback DELETE FROM sys_role WHERE id IN (1, 2, 3, 4, 5, 6);

-- changeset aurora:seed_sys_user_admin dbms:mysql
INSERT INTO sys_user (id, username, password, nickname, gender, status, create_time, update_time, deleted)
VALUES (
    1,
    'admin',
    '$2a$10$PlM2MunWgdxrJXx.jMXT4.TrTjjB8xzBv3RJofsypHuS2hrU9TW2S',
    '超级管理员',
    0,
    1,
    NOW(),
    NOW(),
    0
);
-- rollback DELETE FROM sys_user WHERE id = 1;

-- changeset aurora:seed_sys_user_role_admin dbms:mysql
INSERT INTO sys_user_role (user_id, role_id) VALUES (1, 1);
-- rollback DELETE FROM sys_user_role WHERE user_id = 1 AND role_id = 1;

-- changeset aurora:fix_sys_user_admin_password dbms:mysql
-- 修复：原 seed 的 BCrypt hash 来源不可考（非 admin123 也非常见密码），登录时一律"用户名或密码错误"
-- 用 admin123 重新生成的 hash 替换；BCryptPasswordEncoder.matches("admin123", hash) == true 已验证
UPDATE sys_user
SET password = '$2a$10$ul4m2.GN0aLRnORIt0w8keHFo38m81xG0EfQvJ0ZJ5IzHsc6aSyhK',
    update_time = NOW()
WHERE id = 1 AND username = 'admin';
-- rollback UPDATE sys_user SET password='$2a$10$PlM2MunWgdxrJXx.jMXT4.TrTjjB8xzBv3RJofsypHuS2hrU9TW2S' WHERE id=1;