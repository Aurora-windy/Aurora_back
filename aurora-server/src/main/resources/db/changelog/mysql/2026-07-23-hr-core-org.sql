-- liquibase formatted sql

-- changeset aurora:create_hr_core_org_tables_20260723 dbms:mysql
CREATE TABLE IF NOT EXISTS hr_department (
  id BIGINT PRIMARY KEY,
  parent_id BIGINT NULL,
  dept_name VARCHAR(100) NOT NULL,
  dept_code VARCHAR(50) NULL,
  sort INT NOT NULL DEFAULT 0,
  status TINYINT NOT NULL DEFAULT 1,
  create_user BIGINT NULL,
  create_time DATETIME NULL,
  update_user BIGINT NULL,
  update_time DATETIME NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  INDEX idx_parent(parent_id),
  INDEX idx_status(status)
);

CREATE TABLE IF NOT EXISTS hr_position (
  id BIGINT PRIMARY KEY,
  dept_id BIGINT NOT NULL,
  position_name VARCHAR(100) NOT NULL,
  position_code VARCHAR(50) NULL,
  sort INT NOT NULL DEFAULT 0,
  status TINYINT NOT NULL DEFAULT 1,
  create_user BIGINT NULL,
  create_time DATETIME NULL,
  update_user BIGINT NULL,
  update_time DATETIME NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  INDEX idx_dept(dept_id),
  INDEX idx_status(status)
);

CREATE TABLE IF NOT EXISTS hr_employee (
  id BIGINT PRIMARY KEY,
  emp_no VARCHAR(50) NOT NULL,
  user_id BIGINT NULL,
  dept_id BIGINT NOT NULL,
  position_id BIGINT NOT NULL,
  name VARCHAR(50) NOT NULL,
  gender TINYINT NOT NULL DEFAULT 0,
  phone VARCHAR(20) NULL,
  entry_date DATE NULL,
  status TINYINT NOT NULL DEFAULT 1,
  create_user BIGINT NULL,
  create_time DATETIME NULL,
  update_user BIGINT NULL,
  update_time DATETIME NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_emp_no_deleted(emp_no, deleted),
  INDEX idx_dept(dept_id),
  INDEX idx_position(position_id),
  INDEX idx_user(user_id),
  INDEX idx_status(status)
);
-- rollback DROP TABLE IF EXISTS hr_employee;
-- rollback DROP TABLE IF EXISTS hr_position;
-- rollback DROP TABLE IF EXISTS hr_department;

-- changeset aurora:seed_hr_core_org_menu_20260723 dbms:mysql
INSERT INTO sys_menu (id, parent_id, title, type, name, path, component, icon, permission, sort, visible, status, create_time, update_time, deleted)
VALUES
    (600, 0, '人事管理', 1, 'hr', '/hr', NULL, 'IconUserGroup', NULL, 600, 1, 1, NOW(), NOW(), 0),
    (610, 600, '部门管理', 2, 'hr-depts', '/hr/depts', 'hr/dept/index', 'IconCommon', 'hr:dept:list', 610, 1, 1, NOW(), NOW(), 0),
    (611, 610, '部门查询', 3, NULL, NULL, NULL, NULL, 'hr:dept:list', 611, 0, 1, NOW(), NOW(), 0),
    (612, 610, '部门新增', 3, NULL, NULL, NULL, NULL, 'hr:dept:add', 612, 0, 1, NOW(), NOW(), 0),
    (613, 610, '部门编辑', 3, NULL, NULL, NULL, NULL, 'hr:dept:edit', 613, 0, 1, NOW(), NOW(), 0),
    (614, 610, '部门删除', 3, NULL, NULL, NULL, NULL, 'hr:dept:remove', 614, 0, 1, NOW(), NOW(), 0),
    (620, 600, '岗位管理', 2, 'hr-positions', '/hr/positions', 'hr/position/index', 'IconStorage', 'hr:position:list', 620, 1, 1, NOW(), NOW(), 0),
    (621, 620, '岗位查询', 3, NULL, NULL, NULL, NULL, 'hr:position:list', 621, 0, 1, NOW(), NOW(), 0),
    (622, 620, '岗位新增', 3, NULL, NULL, NULL, NULL, 'hr:position:add', 622, 0, 1, NOW(), NOW(), 0),
    (623, 620, '岗位编辑', 3, NULL, NULL, NULL, NULL, 'hr:position:edit', 623, 0, 1, NOW(), NOW(), 0),
    (624, 620, '岗位删除', 3, NULL, NULL, NULL, NULL, 'hr:position:remove', 624, 0, 1, NOW(), NOW(), 0),
    (630, 600, '员工管理', 2, 'hr-employees', '/hr/employees', 'hr/employee/index', 'IconUser', 'hr:employee:list', 630, 1, 1, NOW(), NOW(), 0),
    (631, 630, '员工查询', 3, NULL, NULL, NULL, NULL, 'hr:employee:list', 631, 0, 1, NOW(), NOW(), 0),
    (632, 630, '员工新增', 3, NULL, NULL, NULL, NULL, 'hr:employee:add', 632, 0, 1, NOW(), NOW(), 0),
    (633, 630, '员工编辑', 3, NULL, NULL, NULL, NULL, 'hr:employee:edit', 633, 0, 1, NOW(), NOW(), 0),
    (634, 630, '员工删除', 3, NULL, NULL, NULL, NULL, 'hr:employee:remove', 634, 0, 1, NOW(), NOW(), 0)
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
-- rollback DELETE FROM sys_menu WHERE id BETWEEN 600 AND 634;

-- changeset aurora:seed_hr_core_org_role_menu_20260723 dbms:mysql
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id
  FROM sys_role r
  JOIN sys_menu m ON m.id BETWEEN 600 AND 634
 WHERE r.code IN ('admin', 'hr_admin')
   AND r.deleted = 0;
-- rollback DELETE FROM sys_role_menu WHERE menu_id BETWEEN 600 AND 634;

-- changeset aurora:seed_hr_core_org_demo_20260723 dbms:mysql
INSERT INTO hr_department (id, parent_id, dept_name, dept_code, sort, status, create_time, update_time, deleted)
VALUES
    (6001, NULL, '总裁办', 'HQ', 1, 1, NOW(), NOW(), 0),
    (6002, 6001, '技术部', 'TECH', 2, 1, NOW(), NOW(), 0),
    (6003, 6001, '人事部', 'HR', 3, 1, NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE
    parent_id = VALUES(parent_id),
    dept_name = VALUES(dept_name),
    dept_code = VALUES(dept_code),
    sort = VALUES(sort),
    status = VALUES(status),
    update_time = NOW(),
    deleted = 0;

INSERT INTO hr_position (id, dept_id, position_name, position_code, sort, status, create_time, update_time, deleted)
VALUES
    (6101, 6002, '研发工程师', 'ENG', 1, 1, NOW(), NOW(), 0),
    (6102, 6002, '架构师', 'ARCH', 2, 1, NOW(), NOW(), 0),
    (6103, 6003, '人事专员', 'HRA', 1, 1, NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE
    dept_id = VALUES(dept_id),
    position_name = VALUES(position_name),
    position_code = VALUES(position_code),
    sort = VALUES(sort),
    status = VALUES(status),
    update_time = NOW(),
    deleted = 0;

INSERT INTO hr_employee (id, emp_no, user_id, dept_id, position_id, name, gender, phone, entry_date, status, create_time, update_time, deleted)
VALUES
    (6201, 'E001', NULL, 6002, 6101, '张三', 1, '13800000001', '2025-03-01', 1, NOW(), NOW(), 0),
    (6202, 'E002', NULL, 6003, 6103, '李四', 2, '13800000002', '2025-04-15', 2, NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE
    emp_no = VALUES(emp_no),
    dept_id = VALUES(dept_id),
    position_id = VALUES(position_id),
    name = VALUES(name),
    gender = VALUES(gender),
    phone = VALUES(phone),
    entry_date = VALUES(entry_date),
    status = VALUES(status),
    update_time = NOW(),
    deleted = 0;
-- rollback DELETE FROM hr_employee WHERE id IN (6201, 6202);
-- rollback DELETE FROM hr_position WHERE id IN (6101, 6102, 6103);
-- rollback DELETE FROM hr_department WHERE id IN (6001, 6002, 6003);
