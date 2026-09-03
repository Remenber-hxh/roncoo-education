-- 按入职天数自动排课 + 顺序解锁
--
-- 需求（《线上入职培训系统—培训模块清单》· 培训模块总览）：
--   课程按「入职第 N 天」推送，班组技能 SOP 从第 7 天起按班组分流；
--   作业安全与班组 SOP 的考核方式标注为「测验+闯关」，闯关即按顺序解锁下一课。
--
-- 只对新入职员工自动推送。现有 40 名员工最早 2022 年入职，
-- 一开任务他们的「入职第 1 天」早就过了，若按历史补派会一次性压下 40 多门课、
-- 且全部立即逾期，看板第二天就是一片红。存量员工走后台手工批量指派。
--
-- 执行：mysql -uroncoo -p roncoo_education < db/w5_auto_assign.sql

-- ============ 一、课程的推送规则 ============
-- 用存储过程包一层，让脚本可以重复执行——
-- ALTER TABLE ADD COLUMN 没有 IF NOT EXISTS，重跑会直接报 Duplicate column
DROP PROCEDURE IF EXISTS add_col_if_missing;
DELIMITER $$
CREATE PROCEDURE add_col_if_missing(IN tb VARCHAR(64), IN col VARCHAR(64), IN ddl VARCHAR(500))
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                  WHERE table_schema = DATABASE() AND table_name = tb AND column_name = col) THEN
    SET @s = CONCAT('ALTER TABLE ', tb, ' ADD COLUMN ', ddl);
    PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
  END IF;
END$$
DELIMITER ;

CALL add_col_if_missing('course', 'push_day',
  "push_day int DEFAULT NULL COMMENT '入职后第几天推送；为空表示不自动推送，只能手工指派'");
CALL add_col_if_missing('course', 'push_scope',
  "push_scope tinyint DEFAULT 1 COMMENT '推送范围 1全员 2指定班组'");
CALL add_col_if_missing('course', 'push_team_ids',
  "push_team_ids varchar(255) DEFAULT NULL COMMENT 'push_scope=2 时生效，班组ID逗号分隔'");
CALL add_col_if_missing('course', 'deadline_days',
  "deadline_days int DEFAULT 7 COMMENT '推送后多少天内需完成，用于自动计算截止日期'");

-- ============ 二、课时的顺序解锁 ============
-- 「闯关」= 必须学完上一课时才能进下一课时。
-- 放在课程级而不是课时级：需求是按课程标注考核方式的，
-- 逐个课时配开关既繁琐、也容易配出「中间某节不锁」这种没意义的状态。
CALL add_col_if_missing('course', 'need_sequential',
  "need_sequential tinyint DEFAULT 0 COMMENT '是否按顺序解锁课时 0否 1是（对应需求里的「闯关」）'");

DROP PROCEDURE IF EXISTS add_col_if_missing;

-- 按入职天数捞课程，这个索引让任务不必全表扫
SET @has_idx = (SELECT COUNT(*) FROM information_schema.statistics
                 WHERE table_schema = DATABASE() AND table_name = 'course' AND index_name = 'idx_push_day');
SET @s = IF(@has_idx = 0, 'ALTER TABLE course ADD INDEX idx_push_day (push_day)', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- ============ 三、自动排课的开关与生效起点 ============
-- 生效起点是必须的：没有它，任务一开就会把所有历史入职的员工全捞出来。
-- 默认取执行本脚本的当天，即「从今天起入职的人才自动排课」。
-- config_type=6 单独归一类，避免和网站(2)/存储(4)/短信(5)混在一起；
-- config_show=1 表示在后台「参数配置」里可见可改
INSERT INTO sys_config (id, config_type, content_type, config_show, sort, config_name, config_key, config_value, remark) VALUES
  (900001, 6, 1, 1, 1, '自动排课开关', 'autoAssignEnable', '1', '1开启 0关闭。开启后每天按课程的「入职第N天」给新员工自动派课'),
  (900002, 6, 1, 1, 2, '自动排课生效起点', 'autoAssignStartDate', DATE_FORMAT(CURDATE(), '%Y-%m-%d'),
   '只给该日期（含）之后入职的员工自动排课。存量员工请走「课程指派」手工批量指派')
ON DUPLICATE KEY UPDATE config_name = VALUES(config_name), remark = VALUES(remark),
                        config_type = VALUES(config_type), config_show = VALUES(config_show);

-- ============ 校验 ============
SELECT COLUMN_NAME AS 新增字段, COLUMN_COMMENT AS 说明
FROM information_schema.columns
WHERE table_schema = DATABASE() AND table_name = 'course'
  AND COLUMN_NAME IN ('push_day','push_scope','push_team_ids','deadline_days','need_sequential');

SELECT config_key, config_value FROM sys_config WHERE config_key LIKE 'autoAssign%';

SELECT COUNT(*) AS 生效起点之后入职的人数
FROM users WHERE status_id = 1 AND hire_date >= CURDATE();
