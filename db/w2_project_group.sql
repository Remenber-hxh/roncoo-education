-- W2：项目组维度
--
-- 班组和项目组是两个正交的维度，一个字段装不下：
--   班组   = 干什么活（强电/弱电/维修/技工/IT/项目管理/职能）
--   项目组 = 在哪个点上班（总部/巡塘片区/大修房/无锡国际会议中心）
-- 例：周瑞鳌是仓管（职能组），但人在无锡国际会议中心；
--     大修房 8 人里 7 个维修技工 + 1 个强电技工。
-- 所以两个都要能单独统计，也要能交叉看。
--
-- 数据来源：《璟邑科技人员信息登记表》「备注」列（合并单元格）
--   总部 8 | 巡塘片区 4 | 大修房 8 | 无锡国际会议中心 22 = 42 人
--
-- 执行：mysql -uroncoo -p roncoo_education < db/w2_project_group.sql

-- 一、项目组字典
CREATE TABLE IF NOT EXISTS project_group (
  id           BIGINT       NOT NULL COMMENT '主键',
  gmt_create   DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  gmt_modified DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  status_id    TINYINT      DEFAULT 1 COMMENT '状态(1:正常，0:停用)',
  sort         INT          DEFAULT 0 COMMENT '排序',
  group_name   VARCHAR(64)  NOT NULL COMMENT '项目组名称',
  remark       VARCHAR(255) COMMENT '备注',
  PRIMARY KEY (id),
  UNIQUE KEY uk_group_name (group_name)
) COMMENT '项目组字典';

INSERT INTO project_group (id, status_id, sort, group_name, remark) VALUES
  (1, 1, 1, '总部',           '总经理、运营、行政、财务、文员等，含派驻紫菡雅集的文员'),
  (2, 1, 2, '巡塘片区',       '巡塘片区项目点'),
  (3, 1, 3, '大修房',         '大修房项目点'),
  (4, 1, 4, '无锡国际会议中心', '无锡国际会议中心项目点，人数最多')
ON DUPLICATE KEY UPDATE group_name = VALUES(group_name), remark = VALUES(remark), sort = VALUES(sort);

-- 二、users 增加项目组外键
SET @exists := (SELECT COUNT(*) FROM information_schema.columns
                WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'project_group_id');
SET @sql := IF(@exists = 0,
  'ALTER TABLE users ADD COLUMN project_group_id BIGINT COMMENT ''项目组ID，关联 project_group''',
  'SELECT ''project_group_id 已存在，跳过''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx := (SELECT COUNT(*) FROM information_schema.statistics
             WHERE table_schema = DATABASE() AND table_name = 'users' AND index_name = 'idx_project_group');
SET @sql2 := IF(@idx = 0,
  'ALTER TABLE users ADD KEY idx_project_group (project_group_id)',
  'SELECT ''idx_project_group 已存在，跳过''');
PREPARE stmt2 FROM @sql2; EXECUTE stmt2; DEALLOCATE PREPARE stmt2;

-- 三、班组「总部」改名「职能组」
--    上一轮把职能岗的班组命名为「总部」，而「总部」同时是项目组的取值，
--    两个维度撞名会让人分不清。周瑞鳌就是反例：他班组是职能岗，
--    项目组却在无锡国际会议中心，班组叫「总部」会读成他在总部上班。
UPDATE team SET team_name = '职能组',
       remark = '总经理、运营负责人、行政主管、财务专员、文员、仓管等职能岗（不分工种）'
WHERE team_name = '总部';

-- 校验
SELECT id, group_name, remark FROM project_group ORDER BY sort;
SELECT COLUMN_NAME, COLUMN_TYPE, COLUMN_COMMENT FROM information_schema.columns
WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'project_group_id';
SELECT id, team_name FROM team ORDER BY sort;
