-- liquibase formatted sql

-- changeset aurora:create_edu_tables_20260708 dbms:mysql
CREATE TABLE IF NOT EXISTS edu_student (
  id BIGINT PRIMARY KEY,
  user_id BIGINT NULL,
  student_no VARCHAR(32) NOT NULL,
  name VARCHAR(50) NOT NULL,
  gender TINYINT NULL,
  phone VARCHAR(20) NULL,
  major VARCHAR(80) NULL,
  class_name VARCHAR(80) NULL,
  status TINYINT NOT NULL DEFAULT 1,
  create_user BIGINT NULL,
  create_time DATETIME NULL,
  update_user BIGINT NULL,
  update_time DATETIME NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_student_no_deleted(student_no, deleted),
  INDEX idx_user(user_id)
);

CREATE TABLE IF NOT EXISTS edu_teacher (
  id BIGINT PRIMARY KEY,
  user_id BIGINT NULL,
  teacher_no VARCHAR(32) NOT NULL,
  name VARCHAR(50) NOT NULL,
  title TINYINT NOT NULL DEFAULT 3,
  phone VARCHAR(20) NULL,
  college VARCHAR(80) NULL,
  status TINYINT NOT NULL DEFAULT 1,
  create_user BIGINT NULL,
  create_time DATETIME NULL,
  update_user BIGINT NULL,
  update_time DATETIME NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_teacher_no_deleted(teacher_no, deleted),
  INDEX idx_user(user_id)
);

CREATE TABLE IF NOT EXISTS edu_course (
  id BIGINT PRIMARY KEY,
  course_code VARCHAR(32) NOT NULL,
  name VARCHAR(100) NOT NULL,
  teacher_id BIGINT NOT NULL,
  category TINYINT NOT NULL DEFAULT 2,
  credit DECIMAL(4,1) NOT NULL DEFAULT 1.0,
  capacity INT NOT NULL,
  selected_count INT NOT NULL DEFAULT 0,
  selection_start_time DATETIME NULL,
  selection_end_time DATETIME NULL,
  status TINYINT NOT NULL DEFAULT 1,
  create_user BIGINT NULL,
  create_time DATETIME NULL,
  update_user BIGINT NULL,
  update_time DATETIME NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_course_code_deleted(course_code, deleted),
  INDEX idx_teacher(teacher_id),
  INDEX idx_status(status)
);

CREATE TABLE IF NOT EXISTS edu_selection (
  id BIGINT PRIMARY KEY,
  student_id BIGINT NOT NULL,
  course_id BIGINT NOT NULL,
  status TINYINT NOT NULL DEFAULT 1,
  selected_time DATETIME NULL,
  dropped_time DATETIME NULL,
  create_user BIGINT NULL,
  create_time DATETIME NULL,
  update_user BIGINT NULL,
  update_time DATETIME NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_student_course(student_id, course_id),
  INDEX idx_course(course_id),
  INDEX idx_student(student_id)
);
-- rollback DROP TABLE IF EXISTS edu_selection;
-- rollback DROP TABLE IF EXISTS edu_course;
-- rollback DROP TABLE IF EXISTS edu_teacher;
-- rollback DROP TABLE IF EXISTS edu_student;

-- changeset aurora:seed_edu_menus_20260708 dbms:mysql
INSERT INTO sys_menu (id, parent_id, title, type, name, path, component, icon, permission, sort, visible, status, create_time, update_time, deleted)
VALUES
    (300, 0, '教务管理', 1, 'edu', '/edu', NULL, 'IconBook', NULL, 300, 1, 1, NOW(), NOW(), 0),
    (310, 300, '学生管理', 2, 'edu-students', '/edu/students', 'edu/student/index', 'IconUser', 'edu:student:list', 310, 1, 1, NOW(), NOW(), 0),
    (311, 310, '学生查询', 3, NULL, NULL, NULL, NULL, 'edu:student:list', 311, 0, 1, NOW(), NOW(), 0),
    (312, 310, '学生新增', 3, NULL, NULL, NULL, NULL, 'edu:student:add', 312, 0, 1, NOW(), NOW(), 0),
    (313, 310, '学生编辑', 3, NULL, NULL, NULL, NULL, 'edu:student:edit', 313, 0, 1, NOW(), NOW(), 0),
    (314, 310, '学生删除', 3, NULL, NULL, NULL, NULL, 'edu:student:remove', 314, 0, 1, NOW(), NOW(), 0),
    (320, 300, '教师管理', 2, 'edu-teachers', '/edu/teachers', 'edu/teacher/index', 'IconUserGroup', 'edu:teacher:list', 320, 1, 1, NOW(), NOW(), 0),
    (321, 320, '教师查询', 3, NULL, NULL, NULL, NULL, 'edu:teacher:list', 321, 0, 1, NOW(), NOW(), 0),
    (322, 320, '教师新增', 3, NULL, NULL, NULL, NULL, 'edu:teacher:add', 322, 0, 1, NOW(), NOW(), 0),
    (323, 320, '教师编辑', 3, NULL, NULL, NULL, NULL, 'edu:teacher:edit', 323, 0, 1, NOW(), NOW(), 0),
    (324, 320, '教师删除', 3, NULL, NULL, NULL, NULL, 'edu:teacher:remove', 324, 0, 1, NOW(), NOW(), 0),
    (330, 300, '课程管理', 2, 'edu-courses', '/edu/courses', 'edu/course/index', 'IconCalendar', 'edu:course:list', 330, 1, 1, NOW(), NOW(), 0),
    (331, 330, '课程查询', 3, NULL, NULL, NULL, NULL, 'edu:course:list', 331, 0, 1, NOW(), NOW(), 0),
    (332, 330, '课程新增', 3, NULL, NULL, NULL, NULL, 'edu:course:add', 332, 0, 1, NOW(), NOW(), 0),
    (333, 330, '课程编辑', 3, NULL, NULL, NULL, NULL, 'edu:course:edit', 333, 0, 1, NOW(), NOW(), 0),
    (334, 330, '课程删除', 3, NULL, NULL, NULL, NULL, 'edu:course:remove', 334, 0, 1, NOW(), NOW(), 0),
    (340, 300, '选课中心', 2, 'edu-selection', '/edu/selection', 'edu/selection/index', 'IconList', 'edu:selection:select', 340, 1, 1, NOW(), NOW(), 0),
    (341, 340, '选课', 3, NULL, NULL, NULL, NULL, 'edu:selection:select', 341, 0, 1, NOW(), NOW(), 0),
    (342, 340, '退课', 3, NULL, NULL, NULL, NULL, 'edu:selection:drop', 342, 0, 1, NOW(), NOW(), 0)
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
-- rollback DELETE FROM sys_menu WHERE id BETWEEN 300 AND 342;

-- changeset aurora:seed_edu_role_menu_20260708 dbms:mysql
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id
  FROM sys_role r
  JOIN sys_menu m ON m.id BETWEEN 300 AND 342
 WHERE r.code = 'admin';

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id
  FROM sys_role r
  JOIN sys_menu m ON m.id IN (300, 330, 331, 333, 340, 341, 342)
 WHERE r.code = 'edu_teacher';

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id
  FROM sys_role r
  JOIN sys_menu m ON m.id IN (300, 340, 341, 342)
 WHERE r.code = 'student';
-- rollback DELETE FROM sys_role_menu WHERE menu_id BETWEEN 300 AND 342;

-- changeset aurora:restore_edu_admin_role_20260708 dbms:mysql
INSERT INTO sys_role (id, name, code, data_scope, sort, status, remark, create_time, update_time, deleted)
VALUES (8, 'Teacher', 'edu_teacher_migrating', 3, 8, 1, 'EDU teacher role', NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    code = VALUES(code),
    data_scope = VALUES(data_scope),
    sort = VALUES(sort),
    status = VALUES(status),
    remark = VALUES(remark),
    update_time = NOW(),
    deleted = 0;

INSERT IGNORE INTO sys_user_role (user_id, role_id)
SELECT ur.user_id, 8
  FROM sys_user_role ur
  JOIN sys_role r ON r.id = ur.role_id
 WHERE ur.role_id = 3
   AND r.code = 'edu_teacher';

DELETE ur
  FROM sys_user_role ur
  JOIN sys_role r ON r.id = ur.role_id
 WHERE ur.role_id = 3
   AND r.code = 'edu_teacher';

UPDATE sys_role
   SET name = 'EDU Admin',
       code = 'edu_admin',
       data_scope = 1,
       sort = 3,
       status = 1,
       remark = 'EDU system administrator',
       update_time = NOW(),
       deleted = 0
 WHERE id = 3;

UPDATE sys_role
   SET code = 'edu_teacher',
       update_time = NOW(),
       deleted = 0
 WHERE id = 8;

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id
  FROM sys_role r
  JOIN sys_menu m ON m.id BETWEEN 300 AND 342
 WHERE r.code IN ('admin', 'edu_admin');

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id
  FROM sys_role r
  JOIN sys_menu m ON m.id IN (300, 330, 331, 333, 340, 341, 342)
 WHERE r.code = 'edu_teacher';

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, 331
  FROM sys_role r
 WHERE r.code = 'student';
-- rollback DELETE FROM sys_role_menu WHERE role_id IN (SELECT id FROM sys_role WHERE code = 'edu_admin') AND menu_id BETWEEN 300 AND 342;

-- changeset aurora:seed_edu_demo_data_20260708 dbms:mysql
INSERT INTO sys_user (id, username, password, nickname, gender, status, create_time, update_time, deleted)
VALUES
    (3001, 'teacher1', '$2a$10$ul4m2.GN0aLRnORIt0w8keHFo38m81xG0EfQvJ0ZJ5IzHsc6aSyhK', '演示教师', 0, 1, NOW(), NOW(), 0),
    (3002, 'student1', '$2a$10$ul4m2.GN0aLRnORIt0w8keHFo38m81xG0EfQvJ0ZJ5IzHsc6aSyhK', '演示学生', 0, 1, NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE
    nickname = VALUES(nickname),
    status = VALUES(status),
    update_time = NOW(),
    deleted = 0;

INSERT IGNORE INTO sys_user_role (user_id, role_id)
SELECT 3001, id FROM sys_role WHERE code = 'edu_teacher';

INSERT IGNORE INTO sys_user_role (user_id, role_id)
SELECT 3002, id FROM sys_role WHERE code = 'student';

INSERT INTO edu_teacher (id, user_id, teacher_no, name, title, phone, college, status, create_time, update_time, deleted)
VALUES (3001, 3001, 'T2026001', '演示教师', 3, '13800003001', '计算机学院', 1, NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE
    user_id = VALUES(user_id),
    name = VALUES(name),
    title = VALUES(title),
    phone = VALUES(phone),
    college = VALUES(college),
    status = VALUES(status),
    update_time = NOW(),
    deleted = 0;

INSERT INTO edu_student (id, user_id, student_no, name, gender, phone, major, class_name, status, create_time, update_time, deleted)
VALUES (3002, 3002, 'S2026001', '演示学生', 0, '13800003002', '软件工程', '软工 2601', 1, NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE
    user_id = VALUES(user_id),
    name = VALUES(name),
    gender = VALUES(gender),
    phone = VALUES(phone),
    major = VALUES(major),
    class_name = VALUES(class_name),
    status = VALUES(status),
    update_time = NOW(),
    deleted = 0;

INSERT INTO edu_course (id, course_code, name, teacher_id, category, credit, capacity, selected_count, selection_start_time, selection_end_time, status, create_time, update_time, deleted)
VALUES (3001, 'CS-SECKILL-001', '高并发抢课演示课', 3001, 2, 2.0, 5, 0, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 30 DAY), 1, NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    teacher_id = VALUES(teacher_id),
    category = VALUES(category),
    credit = VALUES(credit),
    capacity = VALUES(capacity),
    selection_start_time = VALUES(selection_start_time),
    selection_end_time = VALUES(selection_end_time),
    status = VALUES(status),
    update_time = NOW(),
    deleted = 0;
-- rollback DELETE FROM edu_course WHERE id = 3001;
-- rollback DELETE FROM edu_student WHERE id = 3002;
-- rollback DELETE FROM edu_teacher WHERE id = 3001;
-- rollback DELETE FROM sys_user_role WHERE user_id IN (3001, 3002);
-- rollback DELETE FROM sys_user WHERE id IN (3001, 3002);
