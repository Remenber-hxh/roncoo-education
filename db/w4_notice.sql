-- 站内消息（催办）
--
-- 逾期催办要能真正触达员工。短信平台没配、企业微信要十月才接，
-- 现阶段唯一跑得通的是站内消息：后台批量生成，员工在员工端看到。
-- 十月接企业微信时，同一条记录直接拿去推送即可，不用重做。
--
-- 表归 course 服务管：催办的数据来源（课程指派、逾期判定）全在 course 服务，
-- 跨服务写会把简单的事情搞复杂。roncoo 本身也把 user_course、user_study
-- 这类 user_ 前缀的表放在 course 服务，与既有惯例一致。
--
-- 执行：mysql -uroncoo -p roncoo_education < db/w4_notice.sql

CREATE TABLE IF NOT EXISTS `user_notice` (
  `id` bigint NOT NULL,
  `gmt_create` datetime DEFAULT CURRENT_TIMESTAMP,
  `gmt_modified` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `status_id` tinyint DEFAULT '1',
  `user_id` bigint NOT NULL COMMENT '接收人',
  `notice_type` tinyint DEFAULT '1' COMMENT '1学习催办 2系统通知',
  `title` varchar(128) DEFAULT NULL,
  `content` varchar(512) DEFAULT NULL,
  `course_id` bigint DEFAULT NULL COMMENT '关联课程，催办时带上，员工可直接点进去学',
  `is_read` tinyint(1) DEFAULT '0',
  `read_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  -- 员工端每页都要查未读数，这个索引是必须的
  KEY `idx_user_read` (`user_id`, `is_read`),
  -- 消息列表按时间倒序翻页
  KEY `idx_user_gmt` (`user_id`, `gmt_create`),
  -- 防重复催办要按「人+课程+时间」回查最近一条
  KEY `idx_dedup` (`user_id`, `course_id`, `notice_type`, `gmt_create`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='站内消息';

-- 催办按钮的权限位，挂在「学习统计」菜单（id=700，见 db/w3_stat_menu.sql）下面。
-- menu_type=3 是按钮权限，is_show=0 不进侧边栏，只供前端 hasPermission 判断。
INSERT INTO sys_menu (id, status_id, sort, parent_id, is_show, menu_type, menu_name, menu_icon, path, component, permission, remark) VALUES
  (70001, 1, 1, 700, 0, 3, '催办', NULL, NULL, NULL, 'stat:remind', '对逾期未完成的员工批量发送催办消息')
ON DUPLICATE KEY UPDATE menu_name = VALUES(menu_name), permission = VALUES(permission),
                        parent_id = VALUES(parent_id), menu_type = VALUES(menu_type),
                        is_show = VALUES(is_show);

-- 绑到与「学习统计」同一个角色，否则按钮权限取不到
INSERT INTO sys_menu_role (id, status_id, sort, menu_id, role_id)
SELECT 70001, 1, 0, 70001, (SELECT role_id FROM sys_role_user ORDER BY id LIMIT 1)
ON DUPLICATE KEY UPDATE menu_id = VALUES(menu_id), role_id = VALUES(role_id);

-- 校验
SELECT COUNT(*) AS 消息表已建, IF(COUNT(*) = 1, '就绪', '未创建!!') AS 结论
FROM information_schema.tables
WHERE table_schema = DATABASE() AND table_name = 'user_notice';

SELECT m.id, m.menu_name, m.permission, IF(r.id IS NULL, '未授权!!', '已授权') AS 权限
FROM sys_menu m LEFT JOIN sys_menu_role r ON r.menu_id = m.id
WHERE m.id = 70001;
