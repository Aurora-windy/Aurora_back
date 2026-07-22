-- liquibase formatted sql

-- changeset aurora:update_oj_zh_labels_20260722 dbms:mysql
UPDATE sys_role
   SET name = 'OJ 管理员',
       remark = '在线判题系统管理员',
       update_time = NOW()
 WHERE id = 4
   AND code = 'oj_admin'
   AND deleted = 0;

UPDATE sys_menu
   SET title = CASE id
       WHEN 400 THEN '在线判题'
       WHEN 410 THEN '题目管理'
       WHEN 411 THEN '题目查询'
       WHEN 412 THEN '题目详情'
       WHEN 413 THEN '题目新增'
       WHEN 414 THEN '题目编辑'
       WHEN 415 THEN '题目删除'
       WHEN 420 THEN '题库练习'
       WHEN 421 THEN '练习详情'
       WHEN 422 THEN '提交代码'
       WHEN 430 THEN '提交管理'
       WHEN 431 THEN '提交查询'
       WHEN 440 THEN '我的提交'
       WHEN 441 THEN '我的提交查询'
       ELSE title
       END,
       update_time = NOW()
 WHERE id BETWEEN 400 AND 441
   AND deleted = 0;

UPDATE oj_problem
   SET title = '两数之和演示',
       description = '读取两个整数并输出它们的和。第一期使用简化判题：源码中包含 // AC 可得到演示通过结果。',
       update_time = NOW()
 WHERE id = 4001
   AND deleted = 0;

UPDATE oj_problem
   SET title = '反转字符串演示',
       description = '读取一个字符串并输出反转后的字符串。该题用于演示 OJ 第一期题库和提交闭环。',
       update_time = NOW()
 WHERE id = 4002
   AND deleted = 0;
-- rollback UPDATE sys_role SET name = 'OJ Admin', remark = 'OJ system administrator', update_time = NOW() WHERE id = 4 AND code = 'oj_admin';
-- rollback UPDATE sys_menu SET title = CASE id WHEN 400 THEN 'Online Judge' WHEN 410 THEN 'Problem Management' WHEN 411 THEN 'Problem Query' WHEN 412 THEN 'Problem Detail' WHEN 413 THEN 'Problem Add' WHEN 414 THEN 'Problem Edit' WHEN 415 THEN 'Problem Delete' WHEN 420 THEN 'Problem Set' WHEN 421 THEN 'Practice Detail' WHEN 422 THEN 'Submit Code' WHEN 430 THEN 'Submission Management' WHEN 431 THEN 'Submission Query' WHEN 440 THEN 'My Submissions' WHEN 441 THEN 'My Submission Query' ELSE title END, update_time = NOW() WHERE id BETWEEN 400 AND 441;
-- rollback UPDATE oj_problem SET title = 'A + B Demo', description = 'Read two integers and output their sum. Phase 1 uses simplified judging: include // AC in source code to force an accepted demo result.', update_time = NOW() WHERE id = 4001;
-- rollback UPDATE oj_problem SET title = 'Reverse String Demo', description = 'Read one string and output the reversed string. This is demo data for the OJ first-batch flow.', update_time = NOW() WHERE id = 4002;
