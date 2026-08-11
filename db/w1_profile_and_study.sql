-- 十月上线 · 第 1 周：员工档案地基 + 学习时长采集
--
-- 依据《十月上线技术方案.md》第三节。
-- 执行：mysql -uroncoo -p roncoo_education < db/w1_profile_and_study.sql

-- ============================================================
-- 一、班组字典
--
-- 用字典表而不是枚举：班组会变（除四个作业班组外，考证清单里还出现了
-- 「消控值班」「项目管理」），字典表后台可维护，不用改代码重新发版。
-- 班组是整套需求的分流依据：SOP 按班组推送、考证按班组适配、
-- 后台统计三维度之一就是班组。
-- ============================================================
CREATE TABLE IF NOT EXISTS team (
  id           BIGINT       NOT NULL PRIMARY KEY,
  gmt_create   DATETIME     DEFAULT CURRENT_TIMESTAMP,
  gmt_modified DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  status_id    TINYINT      DEFAULT 1,
  sort         INT          DEFAULT 0,
  team_name    VARCHAR(64)  NOT NULL COMMENT '班组名称',
  remark       VARCHAR(255) COMMENT '备注',
  UNIQUE KEY uk_team_name (team_name)
) COMMENT '班组字典';

INSERT INTO team (id, sort, team_name, remark) VALUES
  (1, 1, '强电组',     '高低压配电、发电机组、变压器、配电室值班'),
  (2, 2, '弱电组',     '网络布线、消防报警、视频监控、门禁楼宇自控、电话交换机'),
  (3, 3, '维修组',     '管道、空调、木工油漆、通用工具'),
  (4, 4, '客房维保组', '客房设备点检、预防性保养、报修响应'),
  (5, 5, '消控值班',   '消防控制室值班'),
  (6, 6, '项目管理',   '项目管理岗')
ON DUPLICATE KEY UPDATE team_name = VALUES(team_name), remark = VALUES(remark);

-- ============================================================
-- 二、员工档案字段
--
-- 这四个字段是地基：按班组统计、按人统计带工号、以及年底推送引擎的
-- D0/D60 计算全都依赖它们。
-- hire_date 尤其关键——整个「入职第N天推送」「入职满60天检查证照」
-- 「证书到期前60天预警」都以它为时间基准。
-- ============================================================
ALTER TABLE users
  ADD COLUMN emp_no    VARCHAR(64) COMMENT '工号',
  ADD COLUMN team_id   BIGINT      COMMENT '班组ID，关联 team.id',
  ADD COLUMN position  VARCHAR(64) COMMENT '岗位',
  ADD COLUMN hire_date DATE        COMMENT '入职日期';

CREATE UNIQUE INDEX uk_users_emp_no ON users (emp_no);
CREATE INDEX idx_users_team ON users (team_id);
CREATE INDEX idx_users_hire_date ON users (hire_date);

-- ============================================================
-- 三、证书类型字典
--
-- 数据来自《培训模块清单》的「考证管理」工作表。
-- 十月只用于「学员档案-证照信息」的下拉选择；年底的考证看板、
-- 到期预警、渠道推送直接复用这张表，不用返工。
-- 部分行的「取证要求」在原表中是合并单元格（空），留 NULL 由后台补填。
-- ============================================================
CREATE TABLE IF NOT EXISTS certificate_type (
  id            BIGINT       NOT NULL PRIMARY KEY,
  gmt_create    DATETIME     DEFAULT CURRENT_TIMESTAMP,
  gmt_modified  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  status_id     TINYINT      DEFAULT 1,
  sort          INT          DEFAULT 0,
  cert_name     VARCHAR(128) NOT NULL COMMENT '证书名称',
  apply_teams   VARCHAR(255) COMMENT '适用班组（逗号分隔的 team.id）',
  obtain_req    VARCHAR(255) COMMENT '取证要求（含报销规则）',
  push_timing   VARCHAR(64)  COMMENT '推送时间',
  apply_channel VARCHAR(255) COMMENT '报名渠道',
  valid_years   INT          COMMENT '有效期（年）',
  review_cycle  VARCHAR(64)  COMMENT '复审周期',
  remark        VARCHAR(255),
  UNIQUE KEY uk_cert_name (cert_name)
) COMMENT '证书类型字典';

INSERT INTO certificate_type (id, sort, cert_name, apply_teams, obtain_req, push_timing, apply_channel, valid_years, review_cycle, remark) VALUES
  (1, 1, '低压电工操作证',   '1,3', '入职满2个月未持证，不报销', '入职满60天', '应急管理局指定培训机构/线上报名', 6, '每3年复审', '特种作业操作证'),
  (2, 2, '高压电工操作证',   '1',   NULL,                        '入职满60天', '应急管理局指定培训机构/线上报名', 6, '每3年复审', '特种作业操作证'),
  (3, 3, '焊工操作证',       '3',   '看项目需求，公司报销',      NULL,         '应急管理局指定培训机构/线上报名', 6, '每3年复审', '特种作业操作证'),
  (4, 4, '高处作业操作证',   '3',   NULL,                        NULL,         '应急管理局指定培训机构/线上报名', 6, '每3年复审', '含登高架设与高处安装维护'),
  (5, 5, '消防设施操作员证', '2,5', NULL,                        NULL,         '消防职业技能鉴定中心/报名点',     3, '每3年复审', '消防控制室值班人员必备'),
  (6, 6, '特种设备安全管理员','6',  '看项目需求，不报销',        NULL,         '应急管理局指定培训机构/线上报名', 6, '每3年复审', NULL),
  (7, 7, '电梯安全管理员',   NULL,  '看项目需求，不报销',        NULL,         NULL,                              NULL, NULL, '原表未填适用班组与渠道，待业务确认')
ON DUPLICATE KEY UPDATE apply_teams = VALUES(apply_teams), obtain_req = VALUES(obtain_req);

-- ============================================================
-- 四、员工证照
-- ============================================================
CREATE TABLE IF NOT EXISTS user_certificate (
  id           BIGINT       NOT NULL PRIMARY KEY,
  gmt_create   DATETIME     DEFAULT CURRENT_TIMESTAMP,
  gmt_modified DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  status_id    TINYINT      DEFAULT 1,
  user_id      BIGINT       NOT NULL,
  cert_type_id BIGINT       NOT NULL COMMENT '关联 certificate_type.id',
  cert_no      VARCHAR(128) COMMENT '证书编号',
  issue_date   DATE         COMMENT '发证日期',
  expire_date  DATE         COMMENT '到期日期',
  file_url     VARCHAR(500) COMMENT '扫描件地址（走本地存储）',
  remark       VARCHAR(255),
  KEY idx_cert_user (user_id),
  KEY idx_cert_expire (expire_date)
) COMMENT '员工证照';

-- ============================================================
-- 五、学习时长日汇总  ★ 本周最关键的一张表
--
-- 为什么必须新建：user_study.current_duration 存的是**播放位置**而非
-- 累计观看时长——同一视频看两遍值不变，直接拖到结尾值就是满的。
-- 「这个员工学了多久」在现有数据里完全不存在，且**历史无法补算**。
--
-- 按天存，周/月由 SQL 聚合，满足「日/周/月汇总」要求。
-- 带 course_id 才能支持「按模块统计」（模块 = 课程分类）。
-- ============================================================
CREATE TABLE IF NOT EXISTS user_study_daily (
  id           BIGINT   NOT NULL PRIMARY KEY,
  gmt_create   DATETIME DEFAULT CURRENT_TIMESTAMP,
  gmt_modified DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  user_id      BIGINT   NOT NULL,
  course_id    BIGINT   NOT NULL,
  study_date   DATE     NOT NULL,
  duration_sec INT      DEFAULT 0 COMMENT '当日在该课程的有效学习秒数',
  UNIQUE KEY uk_user_course_date (user_id, course_id, study_date),
  KEY idx_study_date (study_date),
  KEY idx_study_user (user_id)
) COMMENT '学习时长日汇总';

-- ============================================================
-- 六、阅读签署记录（员工手册等需签署确认的课时）
-- ============================================================
CREATE TABLE IF NOT EXISTS user_agreement_sign (
  id        BIGINT      NOT NULL PRIMARY KEY,
  user_id   BIGINT      NOT NULL,
  period_id BIGINT      NOT NULL COMMENT '课时ID',
  sign_time DATETIME    DEFAULT CURRENT_TIMESTAMP,
  sign_ip   VARCHAR(64),
  UNIQUE KEY uk_sign_user_period (user_id, period_id)
) COMMENT '阅读签署记录';

-- ============================================================
-- 七、预留企业微信群机器人配置
--
-- 十月先预留配置项和接口，上线时把群主给的 Webhook 地址填进来即可，
-- 不用改代码。地址本身是密钥，放配置表而非硬编码。
-- ============================================================
INSERT INTO sys_config (id, config_type, content_type, config_show, config_name, config_key, config_value, sort) VALUES
  (40, 2, 1, 0, '企业微信群机器人Webhook', 'wecomRobotWebhook', '', 50)
ON DUPLICATE KEY UPDATE config_name = VALUES(config_name), content_type = VALUES(content_type);

-- ============================================================
-- 校验
-- ============================================================
SELECT '班组'   AS 字典, COUNT(*) AS 条数 FROM team
UNION ALL SELECT '证书类型', COUNT(*) FROM certificate_type;

SELECT COLUMN_NAME AS users新增字段
FROM information_schema.columns
WHERE table_schema = 'roncoo_education' AND table_name = 'users'
  AND COLUMN_NAME IN ('emp_no', 'team_id', 'position', 'hire_date');

SELECT TABLE_NAME AS 新建表
FROM information_schema.tables
WHERE table_schema = 'roncoo_education'
  AND TABLE_NAME IN ('team', 'certificate_type', 'user_certificate', 'user_study_daily', 'user_agreement_sign');
