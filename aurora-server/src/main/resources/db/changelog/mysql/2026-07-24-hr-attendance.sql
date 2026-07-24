-- liquibase formatted sql

-- changeset aurora:create_hr_attendance_tables_20260724 dbms:mysql
CREATE TABLE IF NOT EXISTS hr_attendance (
  id BIGINT PRIMARY KEY,
  emp_id BIGINT NOT NULL,
  attendance_date DATE NOT NULL,
  clock_in_time DATETIME NULL,
  clock_out_time DATETIME NULL,
  status TINYINT NOT NULL DEFAULT 0 COMMENT '0缺卡 1正常 2迟到 3早退 4正常(已审批)',
  remark VARCHAR(255) NULL,
  create_user BIGINT NULL,
  create_time DATETIME NULL,
  update_user BIGINT NULL,
  update_time DATETIME NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_emp_date_deleted(emp_id, attendance_date, deleted),
  INDEX idx_emp_date(emp_id, attendance_date),
  INDEX idx_date(attendance_date),
  INDEX idx_status(status)
);

CREATE TABLE IF NOT EXISTS hr_attendance_appeal (
  id BIGINT PRIMARY KEY,
  attendance_id BIGINT NOT NULL,
  emp_id BIGINT NOT NULL,
  reason VARCHAR(500) NOT NULL,
  status TINYINT NOT NULL DEFAULT 0 COMMENT '0待审核 1通过 2驳回',
  audit_user_id BIGINT NULL,
  audit_remark VARCHAR(255) NULL,
  audit_time DATETIME NULL,
  create_user BIGINT NULL,
  create_time DATETIME NULL,
  update_user BIGINT NULL,
  update_time DATETIME NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  INDEX idx_attendance(attendance_id),
  INDEX idx_emp(emp_id),
  INDEX idx_status(status)
);
-- rollback DROP TABLE IF EXISTS hr_attendance_appeal;
-- rollback DROP TABLE IF EXISTS hr_attendance;

-- changeset aurora:hr_attendance_menu_seed_20260724 dbms:mysql
-- HR 考勤菜单 840-849 段（紧接一期 800-834）
INSERT INTO sys_menu (id, title, parent_id, type, path, name, component, permission, icon, sort, status, create_time, deleted)
VALUES
-- 840: 我的考勤（菜单页）
(840, '我的考勤', 800, 2, '/hr/my-attendance', 'HrMyAttendance', 'hr/my-attendance/index', 'hr:attendance:clock-in', 'icon-calendar', 40, 1, NOW(), 0),
-- 841: 打卡按钮
(841, '打卡', 840, 3, NULL, NULL, NULL, 'hr:attendance:clock-in', NULL, 1, 1, NOW(), 0),
-- 842: 查看我的考勤
(842, '查看我的考勤', 840, 3, NULL, NULL, NULL, 'hr:attendance:clock-in', NULL, 2, 1, NOW(), 0),
-- 843: 提交申诉
(843, '提交申诉', 840, 3, NULL, NULL, NULL, 'hr:attendance:clock-in', NULL, 3, 1, NOW(), 0),
-- 850: 考勤管理（菜单页）
(850, '考勤管理', 800, 2, '/hr/attendance-manage', 'HrAttendanceManage', 'hr/attendance-manage/index', 'hr:attendance:audit', 'icon-list', 50, 1, NOW(), 0),
-- 851: 查看考勤记录
(851, '查看考勤记录', 850, 3, NULL, NULL, NULL, 'hr:attendance:audit', NULL, 1, 1, NOW(), 0),
-- 852: 审核申诉
(852, '审核申诉', 850, 3, NULL, NULL, NULL, 'hr:attendance:audit', NULL, 2, 1, NOW(), 0)
ON DUPLICATE KEY UPDATE title=VALUES(title), parent_id=VALUES(parent_id), type=VALUES(type),
  path=VALUES(path), name=VALUES(name), component=VALUES(component), permission=VALUES(permission),
  icon=VALUES(icon), sort=VALUES(sort), status=VALUES(status), update_time=NOW();

-- 绑定 admin 和 hr_admin 角色
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r, sys_menu m
WHERE r.code IN ('admin', 'hr_admin') AND m.id IN (840, 841, 842, 843, 850, 851, 852)
AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = r.id AND rm.menu_id = m.id);
-- rollback DELETE FROM sys_role_menu WHERE menu_id IN (840,841,842,843,850,851,852);
-- rollback DELETE FROM sys_menu WHERE id IN (840,841,842,843,850,851,852);
