-- roncoo-education 数据库结构（从 MyBatis Mapper XML 的 resultMap 反向重建）
-- 生成时间: 2026-07-16T05:54:19.560Z
-- 说明: 字段名/类型来自 resultMap；主键由应用生成(无 AUTO_INCREMENT)；
--       字段长度/索引/默认值/初始数据为重建推断，非 roncoo 原版，需边跑边校对。
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS=0;

-- category (9 字段)
DROP TABLE IF EXISTS `category`;
CREATE TABLE `category` (
  `id` bigint NOT NULL,
  `gmt_create` datetime DEFAULT CURRENT_TIMESTAMP,
  `gmt_modified` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `status_id` tinyint DEFAULT NULL,
  `sort` int DEFAULT NULL,
  `parent_id` bigint DEFAULT NULL,
  `category_type` tinyint DEFAULT NULL,
  `category_name` varchar(255) DEFAULT NULL,
  `remark` text DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='category';

-- course (19 字段)
DROP TABLE IF EXISTS `course`;
CREATE TABLE `course` (
  `id` bigint NOT NULL,
  `gmt_create` datetime DEFAULT CURRENT_TIMESTAMP,
  `gmt_modified` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `status_id` tinyint DEFAULT NULL,
  `sort` int DEFAULT NULL,
  `lecturer_id` bigint DEFAULT NULL,
  `category_id` bigint DEFAULT NULL,
  `course_name` varchar(255) DEFAULT NULL,
  `course_logo` varchar(255) DEFAULT NULL,
  `is_free` tinyint DEFAULT NULL,
  `ruling_price` decimal(12,2) DEFAULT NULL,
  `course_price` decimal(12,2) DEFAULT NULL,
  `is_putaway` tinyint DEFAULT NULL,
  `course_sort` int DEFAULT NULL,
  `count_buy` int DEFAULT NULL,
  `count_study` int DEFAULT NULL,
  `speed_double` tinyint DEFAULT NULL,
  `speed_drag` tinyint DEFAULT NULL,
  `introduce` text DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='course';

-- course_chapter (9 字段)
DROP TABLE IF EXISTS `course_chapter`;
CREATE TABLE `course_chapter` (
  `id` bigint NOT NULL,
  `gmt_create` datetime DEFAULT CURRENT_TIMESTAMP,
  `gmt_modified` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `status_id` tinyint DEFAULT NULL,
  `sort` int DEFAULT NULL,
  `course_id` bigint DEFAULT NULL,
  `chapter_name` varchar(255) DEFAULT NULL,
  `chapter_desc` varchar(255) DEFAULT NULL,
  `is_free` tinyint DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='course_chapter';

-- course_chapter_period (13 字段)
DROP TABLE IF EXISTS `course_chapter_period`;
CREATE TABLE `course_chapter_period` (
  `id` bigint NOT NULL,
  `gmt_create` datetime DEFAULT CURRENT_TIMESTAMP,
  `gmt_modified` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `status_id` tinyint DEFAULT NULL,
  `sort` int DEFAULT NULL,
  `course_id` bigint DEFAULT NULL,
  `chapter_id` bigint DEFAULT NULL,
  `period_name` varchar(255) DEFAULT NULL,
  `period_desc` varchar(255) DEFAULT NULL,
  `is_free` tinyint DEFAULT NULL,
  `resource_id` bigint DEFAULT NULL,
  `period_type` tinyint DEFAULT NULL,
  `live_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='course_chapter_period';

-- lecturer (11 字段)
DROP TABLE IF EXISTS `lecturer`;
CREATE TABLE `lecturer` (
  `id` bigint NOT NULL,
  `gmt_create` datetime DEFAULT CURRENT_TIMESTAMP,
  `gmt_modified` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `status_id` tinyint DEFAULT NULL,
  `sort` int DEFAULT NULL,
  `user_id` bigint DEFAULT NULL,
  `lecturer_name` varchar(255) DEFAULT NULL,
  `lecturer_mobile` varchar(255) DEFAULT NULL,
  `lecturer_position` varchar(255) DEFAULT NULL,
  `lecturer_head` varchar(255) DEFAULT NULL,
  `introduce` text DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='lecturer';

-- live (18 字段)
DROP TABLE IF EXISTS `live`;
CREATE TABLE `live` (
  `id` bigint NOT NULL,
  `gmt_create` datetime DEFAULT CURRENT_TIMESTAMP,
  `gmt_modified` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `status_id` tinyint DEFAULT NULL,
  `sort` int DEFAULT NULL,
  `lecturer_id` bigint DEFAULT NULL,
  `live_name` varchar(255) DEFAULT NULL,
  `live_model` tinyint DEFAULT NULL,
  `live_delay` tinyint DEFAULT NULL,
  `begin_time` datetime DEFAULT NULL,
  `live_duration` int DEFAULT NULL,
  `live_status` tinyint DEFAULT NULL,
  `live_platform` tinyint DEFAULT NULL,
  `channel_id` varchar(255) DEFAULT NULL,
  `channel_pwd` varchar(255) DEFAULT NULL,
  `playback_save` tinyint DEFAULT NULL,
  `resource_id` bigint DEFAULT NULL,
  `live_introduce` text DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='live';

-- live_log (10 字段)
DROP TABLE IF EXISTS `live_log`;
CREATE TABLE `live_log` (
  `id` bigint NOT NULL,
  `gmt_create` datetime DEFAULT CURRENT_TIMESTAMP,
  `live_id` bigint DEFAULT NULL,
  `live_platform` tinyint DEFAULT NULL,
  `live_status` tinyint DEFAULT NULL,
  `channel_id` varchar(255) DEFAULT NULL,
  `session_id` varchar(255) DEFAULT NULL,
  `start_time` datetime DEFAULT NULL,
  `end_time` datetime DEFAULT NULL,
  `playback_save` tinyint DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='live_log';

-- msg (13 字段)
DROP TABLE IF EXISTS `msg`;
CREATE TABLE `msg` (
  `id` bigint NOT NULL,
  `gmt_create` datetime DEFAULT CURRENT_TIMESTAMP,
  `gmt_modified` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `status_id` tinyint DEFAULT NULL,
  `sort` int DEFAULT NULL,
  `msg_type` tinyint DEFAULT NULL,
  `msg_title` varchar(255) DEFAULT NULL,
  `is_time_send` tinyint DEFAULT NULL,
  `send_time` datetime DEFAULT NULL,
  `is_send` tinyint DEFAULT NULL,
  `is_top` tinyint DEFAULT NULL,
  `remark` text DEFAULT NULL,
  `msg_text` text DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='msg';

-- msg_user (9 字段)
DROP TABLE IF EXISTS `msg_user`;
CREATE TABLE `msg_user` (
  `id` bigint NOT NULL,
  `gmt_create` datetime DEFAULT CURRENT_TIMESTAMP,
  `gmt_modified` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `status_id` tinyint DEFAULT NULL,
  `sort` int DEFAULT NULL,
  `msg_id` bigint DEFAULT NULL,
  `user_id` bigint DEFAULT NULL,
  `is_read` tinyint DEFAULT NULL,
  `is_top` tinyint DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='msg_user';

-- order_info (15 字段)
DROP TABLE IF EXISTS `order_info`;
CREATE TABLE `order_info` (
  `id` bigint NOT NULL,
  `gmt_create` datetime DEFAULT CURRENT_TIMESTAMP,
  `gmt_modified` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `order_no` bigint DEFAULT NULL,
  `user_id` bigint DEFAULT NULL,
  `mobile` varchar(255) DEFAULT NULL,
  `register_time` datetime DEFAULT NULL,
  `course_id` bigint DEFAULT NULL,
  `ruling_price` decimal(12,2) DEFAULT NULL,
  `course_price` decimal(12,2) DEFAULT NULL,
  `pay_type` tinyint DEFAULT NULL,
  `order_status` tinyint DEFAULT NULL,
  `remark_cus` text DEFAULT NULL,
  `remark` text DEFAULT NULL,
  `pay_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='order_info';

-- order_pay (10 字段)
DROP TABLE IF EXISTS `order_pay`;
CREATE TABLE `order_pay` (
  `id` bigint NOT NULL,
  `gmt_create` datetime DEFAULT CURRENT_TIMESTAMP,
  `order_no` bigint DEFAULT NULL,
  `serial_number` bigint DEFAULT NULL,
  `ruling_price` decimal(12,2) DEFAULT NULL,
  `course_price` decimal(12,2) DEFAULT NULL,
  `pay_type` tinyint DEFAULT NULL,
  `order_status` tinyint DEFAULT NULL,
  `remark_cus` text DEFAULT NULL,
  `pay_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='order_pay';

-- region (14 字段)
DROP TABLE IF EXISTS `region`;
CREATE TABLE `region` (
  `id` bigint NOT NULL,
  `parent_id` bigint DEFAULT NULL,
  `level` int DEFAULT NULL,
  `province_code` varchar(255) DEFAULT NULL,
  `center_lng` decimal(12,2) DEFAULT NULL,
  `center_lat` decimal(12,2) DEFAULT NULL,
  `province_id` int DEFAULT NULL,
  `province_name` varchar(255) DEFAULT NULL,
  `city_id` int DEFAULT NULL,
  `city_code` varchar(255) DEFAULT NULL,
  `city_name` varchar(255) DEFAULT NULL,
  `region_name` varchar(255) DEFAULT NULL,
  `district_name` varchar(255) DEFAULT NULL,
  `merger_name` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='region';

-- resource (18 字段)
DROP TABLE IF EXISTS `resource`;
CREATE TABLE `resource` (
  `id` bigint NOT NULL,
  `gmt_create` datetime DEFAULT CURRENT_TIMESTAMP,
  `gmt_modified` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `status_id` tinyint DEFAULT NULL,
  `sort` int DEFAULT NULL,
  `category_id` bigint DEFAULT NULL,
  `resource_name` varchar(255) DEFAULT NULL,
  `resource_type` tinyint DEFAULT NULL,
  `resource_size` bigint DEFAULT NULL,
  `resource_url` varchar(255) DEFAULT NULL,
  `vod_platform` tinyint DEFAULT NULL,
  `storage_platform` tinyint DEFAULT NULL,
  `video_status` tinyint DEFAULT NULL,
  `video_length` int DEFAULT NULL,
  `video_vid` varchar(255) DEFAULT NULL,
  `doc_page` int DEFAULT NULL,
  `img_width` int DEFAULT NULL,
  `img_height` int DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='resource';

-- sys_config (11 字段)
DROP TABLE IF EXISTS `sys_config`;
CREATE TABLE `sys_config` (
  `id` bigint NOT NULL,
  `config_type` tinyint DEFAULT NULL,
  `content_type` tinyint DEFAULT NULL,
  `config_name` varchar(255) DEFAULT NULL,
  `config_key` varchar(255) DEFAULT NULL,
  `config_show` tinyint(1) DEFAULT NULL,
  `remark` text DEFAULT NULL,
  `sort` int DEFAULT NULL,
  `gmt_create` datetime DEFAULT CURRENT_TIMESTAMP,
  `gmt_modified` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `config_value` text DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='sys_config';

-- sys_log (13 字段)
DROP TABLE IF EXISTS `sys_log`;
CREATE TABLE `sys_log` (
  `id` bigint NOT NULL,
  `gmt_create` datetime DEFAULT CURRENT_TIMESTAMP,
  `user_id` bigint DEFAULT NULL,
  `operation` varchar(255) DEFAULT NULL,
  `method` varchar(255) DEFAULT NULL,
  `path` varchar(255) DEFAULT NULL,
  `content` text DEFAULT NULL,
  `login_ip` varchar(255) DEFAULT NULL,
  `country` varchar(255) DEFAULT NULL,
  `province` varchar(255) DEFAULT NULL,
  `city` varchar(255) DEFAULT NULL,
  `browser` varchar(255) DEFAULT NULL,
  `os` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='sys_log';

-- sys_menu (15 字段)
DROP TABLE IF EXISTS `sys_menu`;
CREATE TABLE `sys_menu` (
  `id` bigint NOT NULL,
  `gmt_create` datetime DEFAULT CURRENT_TIMESTAMP,
  `gmt_modified` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `status_id` tinyint DEFAULT NULL,
  `sort` int DEFAULT NULL,
  `parent_id` bigint DEFAULT NULL,
  `is_show` tinyint(1) DEFAULT NULL,
  `menu_type` tinyint DEFAULT NULL,
  `menu_name` varchar(255) DEFAULT NULL,
  `menu_icon` varchar(255) DEFAULT NULL,
  `path` varchar(255) DEFAULT NULL,
  `component` varchar(255) DEFAULT NULL,
  `apis` varchar(255) DEFAULT NULL,
  `permission` varchar(255) DEFAULT NULL,
  `remark` text DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='sys_menu';

-- sys_menu_role (7 字段)
DROP TABLE IF EXISTS `sys_menu_role`;
CREATE TABLE `sys_menu_role` (
  `id` bigint NOT NULL,
  `gmt_create` datetime DEFAULT CURRENT_TIMESTAMP,
  `gmt_modified` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `status_id` tinyint DEFAULT NULL,
  `sort` int DEFAULT NULL,
  `menu_id` bigint DEFAULT NULL,
  `role_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='sys_menu_role';

-- sys_role (7 字段)
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role` (
  `id` bigint NOT NULL,
  `gmt_create` datetime DEFAULT CURRENT_TIMESTAMP,
  `gmt_modified` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `status_id` tinyint DEFAULT NULL,
  `sort` int DEFAULT NULL,
  `role_name` varchar(255) DEFAULT NULL,
  `remark` text DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='sys_role';

-- sys_role_user (7 字段)
DROP TABLE IF EXISTS `sys_role_user`;
CREATE TABLE `sys_role_user` (
  `id` bigint NOT NULL,
  `gmt_create` datetime DEFAULT CURRENT_TIMESTAMP,
  `gmt_modified` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `status_id` tinyint DEFAULT NULL,
  `sort` int DEFAULT NULL,
  `role_id` bigint DEFAULT NULL,
  `user_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='sys_role_user';

-- sys_user (10 字段)
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
  `id` bigint NOT NULL,
  `gmt_create` datetime DEFAULT CURRENT_TIMESTAMP,
  `gmt_modified` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `status_id` tinyint DEFAULT NULL,
  `sort` int DEFAULT NULL,
  `mobile` varchar(255) DEFAULT NULL,
  `mobile_salt` varchar(255) DEFAULT NULL,
  `mobile_psw` varchar(255) DEFAULT NULL,
  `real_name` varchar(255) DEFAULT NULL,
  `remark` text DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='sys_user';

-- user_course (8 字段)
DROP TABLE IF EXISTS `user_course`;
CREATE TABLE `user_course` (
  `id` bigint NOT NULL,
  `gmt_create` datetime DEFAULT CURRENT_TIMESTAMP,
  `gmt_modified` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `status_id` tinyint DEFAULT NULL,
  `sort` int DEFAULT NULL,
  `user_id` bigint DEFAULT NULL,
  `course_id` bigint DEFAULT NULL,
  `buy_type` tinyint DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='user_course';

-- user_course_collect (7 字段)
DROP TABLE IF EXISTS `user_course_collect`;
CREATE TABLE `user_course_collect` (
  `id` bigint NOT NULL,
  `gmt_create` datetime DEFAULT CURRENT_TIMESTAMP,
  `gmt_modified` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `status_id` tinyint DEFAULT NULL,
  `sort` int DEFAULT NULL,
  `user_id` bigint DEFAULT NULL,
  `course_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='user_course_collect';

-- user_course_comment (9 字段)
DROP TABLE IF EXISTS `user_course_comment`;
CREATE TABLE `user_course_comment` (
  `id` bigint NOT NULL,
  `gmt_create` datetime DEFAULT CURRENT_TIMESTAMP,
  `gmt_modified` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `status_id` tinyint DEFAULT NULL,
  `sort` int DEFAULT NULL,
  `user_id` bigint DEFAULT NULL,
  `course_id` bigint DEFAULT NULL,
  `comment_id` bigint DEFAULT NULL,
  `comment_text` text DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='user_course_comment';

-- user_study (11 字段)
DROP TABLE IF EXISTS `user_study`;
CREATE TABLE `user_study` (
  `id` bigint NOT NULL,
  `gmt_create` datetime DEFAULT CURRENT_TIMESTAMP,
  `gmt_modified` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `course_id` bigint DEFAULT NULL,
  `chapter_id` bigint DEFAULT NULL,
  `period_id` bigint DEFAULT NULL,
  `user_id` bigint DEFAULT NULL,
  `resource_type` tinyint DEFAULT NULL,
  `current_duration` int DEFAULT NULL,
  `current_page` int DEFAULT NULL,
  `progress` decimal(12,2) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='user_study';

-- users (18 字段)
DROP TABLE IF EXISTS `users`;
CREATE TABLE `users` (
  `id` bigint NOT NULL,
  `gmt_create` datetime DEFAULT CURRENT_TIMESTAMP,
  `gmt_modified` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `status_id` tinyint DEFAULT NULL,
  `mobile` varchar(255) DEFAULT NULL,
  `mobile_salt` varchar(255) DEFAULT NULL,
  `mobile_psw` varchar(255) DEFAULT NULL,
  `nickname` varchar(255) DEFAULT NULL,
  `user_sex` tinyint DEFAULT NULL,
  `user_age` date DEFAULT NULL,
  `user_head` varchar(255) DEFAULT NULL,
  `remark` text DEFAULT NULL,
  `union_id` varchar(255) DEFAULT NULL,
  `open_id` varchar(255) DEFAULT NULL,
  `city` varchar(255) DEFAULT NULL,
  `province` varchar(255) DEFAULT NULL,
  `country` varchar(255) DEFAULT NULL,
  `register_source` tinyint DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='users';

-- users_account (8 字段)
DROP TABLE IF EXISTS `users_account`;
CREATE TABLE `users_account` (
  `id` bigint NOT NULL,
  `gmt_create` datetime DEFAULT CURRENT_TIMESTAMP,
  `gmt_modified` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `status_id` tinyint DEFAULT NULL,
  `user_id` bigint DEFAULT NULL,
  `available_amount` decimal(12,2) DEFAULT NULL,
  `freeze_amount` decimal(12,2) DEFAULT NULL,
  `sign` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='users_account';

-- users_account_consume (8 字段)
DROP TABLE IF EXISTS `users_account_consume`;
CREATE TABLE `users_account_consume` (
  `id` bigint NOT NULL,
  `gmt_create` datetime DEFAULT CURRENT_TIMESTAMP,
  `user_id` bigint DEFAULT NULL,
  `consume_type` tinyint DEFAULT NULL,
  `consume_amount` decimal(12,2) DEFAULT NULL,
  `balance_amount` decimal(12,2) DEFAULT NULL,
  `order_no` bigint DEFAULT NULL,
  `remark` text DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='users_account_consume';

-- users_log (12 字段)
DROP TABLE IF EXISTS `users_log`;
CREATE TABLE `users_log` (
  `id` bigint NOT NULL,
  `gmt_create` datetime DEFAULT CURRENT_TIMESTAMP,
  `user_id` bigint DEFAULT NULL,
  `login_status` tinyint DEFAULT NULL,
  `login_client` tinyint DEFAULT NULL,
  `login_type` tinyint DEFAULT NULL,
  `login_ip` varchar(255) DEFAULT NULL,
  `country` varchar(255) DEFAULT NULL,
  `province` varchar(255) DEFAULT NULL,
  `city` varchar(255) DEFAULT NULL,
  `browser` varchar(255) DEFAULT NULL,
  `os` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='users_log';

-- website_app (11 字段)
DROP TABLE IF EXISTS `website_app`;
CREATE TABLE `website_app` (
  `id` bigint NOT NULL,
  `gmt_create` datetime DEFAULT CURRENT_TIMESTAMP,
  `gmt_modified` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `status_id` tinyint DEFAULT NULL,
  `sort` int DEFAULT NULL,
  `publish_time` datetime DEFAULT NULL,
  `app_type` tinyint DEFAULT NULL,
  `app_version` varchar(255) DEFAULT NULL,
  `update_force` tinyint DEFAULT NULL,
  `update_url` varchar(255) DEFAULT NULL,
  `update_tips` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='website_app';

-- website_carousel (11 字段)
DROP TABLE IF EXISTS `website_carousel`;
CREATE TABLE `website_carousel` (
  `id` bigint NOT NULL,
  `gmt_create` datetime DEFAULT CURRENT_TIMESTAMP,
  `gmt_modified` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `status_id` tinyint DEFAULT NULL,
  `sort` int DEFAULT NULL,
  `carousel_title` varchar(255) DEFAULT NULL,
  `carousel_img` varchar(255) DEFAULT NULL,
  `carousel_url` varchar(255) DEFAULT NULL,
  `carousel_target` tinyint DEFAULT NULL,
  `begin_time` datetime DEFAULT NULL,
  `end_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='website_carousel';

-- website_link (8 字段)
DROP TABLE IF EXISTS `website_link`;
CREATE TABLE `website_link` (
  `id` bigint NOT NULL,
  `gmt_create` datetime DEFAULT CURRENT_TIMESTAMP,
  `gmt_modified` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `status_id` tinyint DEFAULT NULL,
  `sort` int DEFAULT NULL,
  `link_name` varchar(255) DEFAULT NULL,
  `link_url` varchar(255) DEFAULT NULL,
  `link_target` tinyint DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='website_link';

-- website_nav (8 字段)
DROP TABLE IF EXISTS `website_nav`;
CREATE TABLE `website_nav` (
  `id` bigint NOT NULL,
  `gmt_create` datetime DEFAULT CURRENT_TIMESTAMP,
  `gmt_modified` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `status_id` tinyint DEFAULT NULL,
  `sort` int DEFAULT NULL,
  `nav_title` varchar(255) DEFAULT NULL,
  `nav_url` varchar(255) DEFAULT NULL,
  `nav_target` tinyint DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='website_nav';

-- zone (7 字段)
DROP TABLE IF EXISTS `zone`;
CREATE TABLE `zone` (
  `id` bigint NOT NULL,
  `gmt_create` datetime DEFAULT CURRENT_TIMESTAMP,
  `gmt_modified` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `status_id` tinyint DEFAULT NULL,
  `sort` int DEFAULT NULL,
  `zone_name` varchar(255) DEFAULT NULL,
  `zone_desc` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='zone';

-- zone_course (7 字段)
DROP TABLE IF EXISTS `zone_course`;
CREATE TABLE `zone_course` (
  `id` bigint NOT NULL,
  `gmt_create` datetime DEFAULT CURRENT_TIMESTAMP,
  `gmt_modified` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `status_id` tinyint DEFAULT NULL,
  `sort` int DEFAULT NULL,
  `zone_id` bigint DEFAULT NULL,
  `course_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='zone_course';

SET FOREIGN_KEY_CHECKS=1;
