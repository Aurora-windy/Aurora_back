-- liquibase formatted sql

-- changeset aurora:create_oj_core_judge_tables_20260722 dbms:mysql
CREATE TABLE IF NOT EXISTS oj_problem (
  id BIGINT PRIMARY KEY,
  title VARCHAR(120) NOT NULL,
  description TEXT NOT NULL,
  difficulty TINYINT NOT NULL,
  time_limit_ms INT NOT NULL DEFAULT 1000,
  memory_limit_mb INT NOT NULL DEFAULT 128,
  sample_input TEXT NULL,
  sample_output TEXT NULL,
  test_input TEXT NULL,
  expected_output TEXT NULL,
  status TINYINT NOT NULL DEFAULT 1,
  create_user BIGINT NULL,
  create_time DATETIME NULL,
  update_user BIGINT NULL,
  update_time DATETIME NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  INDEX idx_difficulty(difficulty),
  INDEX idx_status(status),
  INDEX idx_title(title)
);

CREATE TABLE IF NOT EXISTS oj_submission (
  id BIGINT PRIMARY KEY,
  problem_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  language TINYINT NOT NULL,
  source_code TEXT NOT NULL,
  status TINYINT NOT NULL DEFAULT 0,
  time_used_ms INT NULL,
  memory_used_kb INT NULL,
  error_message VARCHAR(500) NULL,
  create_user BIGINT NULL,
  create_time DATETIME NULL,
  update_user BIGINT NULL,
  update_time DATETIME NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  INDEX idx_problem(problem_id),
  INDEX idx_user(user_id),
  INDEX idx_status(status)
);
-- rollback DROP TABLE IF EXISTS oj_submission;
-- rollback DROP TABLE IF EXISTS oj_problem;

-- changeset aurora:seed_oj_admin_role_20260722 dbms:mysql
INSERT INTO sys_role (id, name, code, data_scope, sort, status, remark, create_time, update_time, deleted)
VALUES (4, 'OJ Admin', 'oj_admin', 1, 4, 1, 'OJ system administrator', NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    code = VALUES(code),
    data_scope = VALUES(data_scope),
    sort = VALUES(sort),
    status = VALUES(status),
    remark = VALUES(remark),
    update_time = NOW(),
    deleted = 0;
-- rollback DELETE FROM sys_role WHERE id = 4 AND code = 'oj_admin';

-- changeset aurora:seed_oj_core_judge_menu_20260722 dbms:mysql
INSERT INTO sys_menu (id, parent_id, title, type, name, path, component, icon, permission, sort, visible, status, create_time, update_time, deleted)
VALUES
    (400, 0, 'Online Judge', 1, 'oj', '/oj', NULL, 'IconCode', NULL, 400, 1, 1, NOW(), NOW(), 0),
    (410, 400, 'Problem Management', 2, 'oj-problems', '/oj/problems', 'oj/problem/index', 'IconApps', 'oj:problem:list', 410, 1, 1, NOW(), NOW(), 0),
    (411, 410, 'Problem Query', 3, NULL, NULL, NULL, NULL, 'oj:problem:list', 411, 0, 1, NOW(), NOW(), 0),
    (412, 410, 'Problem Detail', 3, NULL, NULL, NULL, NULL, 'oj:problem:detail', 412, 0, 1, NOW(), NOW(), 0),
    (413, 410, 'Problem Add', 3, NULL, NULL, NULL, NULL, 'oj:problem:add', 413, 0, 1, NOW(), NOW(), 0),
    (414, 410, 'Problem Edit', 3, NULL, NULL, NULL, NULL, 'oj:problem:edit', 414, 0, 1, NOW(), NOW(), 0),
    (415, 410, 'Problem Delete', 3, NULL, NULL, NULL, NULL, 'oj:problem:remove', 415, 0, 1, NOW(), NOW(), 0),
    (420, 400, 'Problem Set', 2, 'oj-practice', '/oj/practice', 'oj/practice/index', 'IconBook', 'oj:problem:list', 420, 1, 1, NOW(), NOW(), 0),
    (421, 420, 'Practice Detail', 3, NULL, NULL, NULL, NULL, 'oj:problem:detail', 421, 0, 1, NOW(), NOW(), 0),
    (422, 420, 'Submit Code', 3, NULL, NULL, NULL, NULL, 'oj:submission:submit', 422, 0, 1, NOW(), NOW(), 0),
    (430, 400, 'Submission Management', 2, 'oj-submissions', '/oj/submissions', 'oj/submission/index', 'IconFile', 'oj:problem:list', 430, 1, 1, NOW(), NOW(), 0),
    (431, 430, 'Submission Query', 3, NULL, NULL, NULL, NULL, 'oj:problem:list', 431, 0, 1, NOW(), NOW(), 0),
    (440, 400, 'My Submissions', 2, 'oj-my-submissions', '/oj/my-submissions', 'oj/my-submission/index', 'IconHistory', 'oj:submission:view-my', 440, 1, 1, NOW(), NOW(), 0),
    (441, 440, 'My Submission Query', 3, NULL, NULL, NULL, NULL, 'oj:submission:view-my', 441, 0, 1, NOW(), NOW(), 0)
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
-- rollback DELETE FROM sys_menu WHERE id BETWEEN 400 AND 441;

-- changeset aurora:seed_oj_core_judge_role_menu_20260722 dbms:mysql
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id
  FROM sys_role r
  JOIN sys_menu m ON m.id BETWEEN 400 AND 441
 WHERE r.code IN ('admin', 'oj_admin')
   AND r.deleted = 0;

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id
  FROM sys_role r
  JOIN sys_menu m ON m.id IN (400, 420, 421, 422, 440, 441)
 WHERE r.code = 'student'
   AND r.deleted = 0;
-- rollback DELETE FROM sys_role_menu WHERE menu_id BETWEEN 400 AND 441;

-- changeset aurora:seed_oj_demo_problems_20260722 dbms:mysql
INSERT INTO oj_problem (id, title, description, difficulty, time_limit_ms, memory_limit_mb, sample_input, sample_output, test_input, expected_output, status, create_time, update_time, deleted)
VALUES
    (4001, 'A + B Demo', 'Read two integers and output their sum. Phase 1 uses simplified judging: include // AC in source code to force an accepted demo result.', 1, 1000, 128, '1 2', '3', '1 2', '3', 1, NOW(), NOW(), 0),
    (4002, 'Reverse String Demo', 'Read one string and output the reversed string. This is demo data for the OJ first-batch flow.', 2, 1000, 128, 'aurora', 'arorua', 'aurora', 'arorua', 1, NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE
    title = VALUES(title),
    description = VALUES(description),
    difficulty = VALUES(difficulty),
    time_limit_ms = VALUES(time_limit_ms),
    memory_limit_mb = VALUES(memory_limit_mb),
    sample_input = VALUES(sample_input),
    sample_output = VALUES(sample_output),
    test_input = VALUES(test_input),
    expected_output = VALUES(expected_output),
    status = VALUES(status),
    update_time = NOW(),
    deleted = 0;
-- rollback DELETE FROM oj_problem WHERE id IN (4001, 4002);
