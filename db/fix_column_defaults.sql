-- 补齐反向重建表结构时丢失的字段默认值
--
-- 背景：roncoo 未开源 SQL，本项目的建表语句是从 34 个 Mapper XML 的 resultMap
--       反向推导的（db/gen_schema.js）。resultMap 只有字段名和类型，**推不出
--       DEFAULT 值**，于是所有字段都成了默认 NULL。
--
-- 后果：后端多处依赖数据库默认值而不显式赋值，例如 AdminCourseBiz.save() 从不
--       设置 status_id。而门户端 ApiCourseBiz 按 status_id = 1 过滤课程、章节、
--       课时。结果就是：后台新建的课程在员工端**完全看不到**，且没有任何报错。
--       同样的问题波及章节、课时、资源、报名记录等。
--
-- 默认值取值依据见 gen_defaults_sql.js 里的 RULES 注释。
--
-- 执行：mysql -uroncoo -p roncoo_education < db/fix_column_defaults.sql

-- 一、给字段补上默认值（共 56 个字段）
ALTER TABLE `category` MODIFY COLUMN `sort` int DEFAULT 0;
ALTER TABLE `category` MODIFY COLUMN `status_id` tinyint DEFAULT 1;
ALTER TABLE `course` MODIFY COLUMN `count_buy` int DEFAULT 0;
ALTER TABLE `course` MODIFY COLUMN `count_study` int DEFAULT 0;
ALTER TABLE `course` MODIFY COLUMN `is_free` tinyint DEFAULT 0;
ALTER TABLE `course` MODIFY COLUMN `is_putaway` tinyint DEFAULT 0;
ALTER TABLE `course` MODIFY COLUMN `sort` int DEFAULT 0;
ALTER TABLE `course` MODIFY COLUMN `status_id` tinyint DEFAULT 1;
ALTER TABLE `course_chapter` MODIFY COLUMN `is_free` tinyint DEFAULT 0;
ALTER TABLE `course_chapter` MODIFY COLUMN `sort` int DEFAULT 0;
ALTER TABLE `course_chapter` MODIFY COLUMN `status_id` tinyint DEFAULT 1;
ALTER TABLE `course_chapter_period` MODIFY COLUMN `is_free` tinyint DEFAULT 0;
ALTER TABLE `course_chapter_period` MODIFY COLUMN `sort` int DEFAULT 0;
ALTER TABLE `course_chapter_period` MODIFY COLUMN `status_id` tinyint DEFAULT 1;
ALTER TABLE `lecturer` MODIFY COLUMN `sort` int DEFAULT 0;
ALTER TABLE `lecturer` MODIFY COLUMN `status_id` tinyint DEFAULT 1;
ALTER TABLE `live` MODIFY COLUMN `sort` int DEFAULT 0;
ALTER TABLE `live` MODIFY COLUMN `status_id` tinyint DEFAULT 1;
ALTER TABLE `msg` MODIFY COLUMN `sort` int DEFAULT 0;
ALTER TABLE `msg` MODIFY COLUMN `status_id` tinyint DEFAULT 1;
ALTER TABLE `msg_user` MODIFY COLUMN `sort` int DEFAULT 0;
ALTER TABLE `msg_user` MODIFY COLUMN `status_id` tinyint DEFAULT 1;
ALTER TABLE `resource` MODIFY COLUMN `sort` int DEFAULT 0;
ALTER TABLE `resource` MODIFY COLUMN `status_id` tinyint DEFAULT 1;
ALTER TABLE `sys_config` MODIFY COLUMN `sort` int DEFAULT 0;
ALTER TABLE `sys_menu` MODIFY COLUMN `is_show` tinyint(1) DEFAULT 1;
ALTER TABLE `sys_menu` MODIFY COLUMN `sort` int DEFAULT 0;
ALTER TABLE `sys_menu` MODIFY COLUMN `status_id` tinyint DEFAULT 1;
ALTER TABLE `sys_menu_role` MODIFY COLUMN `sort` int DEFAULT 0;
ALTER TABLE `sys_menu_role` MODIFY COLUMN `status_id` tinyint DEFAULT 1;
ALTER TABLE `sys_role` MODIFY COLUMN `sort` int DEFAULT 0;
ALTER TABLE `sys_role` MODIFY COLUMN `status_id` tinyint DEFAULT 1;
ALTER TABLE `sys_role_user` MODIFY COLUMN `sort` int DEFAULT 0;
ALTER TABLE `sys_role_user` MODIFY COLUMN `status_id` tinyint DEFAULT 1;
ALTER TABLE `sys_user` MODIFY COLUMN `sort` int DEFAULT 0;
ALTER TABLE `sys_user` MODIFY COLUMN `status_id` tinyint DEFAULT 1;
ALTER TABLE `user_course` MODIFY COLUMN `sort` int DEFAULT 0;
ALTER TABLE `user_course` MODIFY COLUMN `status_id` tinyint DEFAULT 1;
ALTER TABLE `user_course_collect` MODIFY COLUMN `sort` int DEFAULT 0;
ALTER TABLE `user_course_collect` MODIFY COLUMN `status_id` tinyint DEFAULT 1;
ALTER TABLE `user_course_comment` MODIFY COLUMN `sort` int DEFAULT 0;
ALTER TABLE `user_course_comment` MODIFY COLUMN `status_id` tinyint DEFAULT 1;
ALTER TABLE `users` MODIFY COLUMN `status_id` tinyint DEFAULT 1;
ALTER TABLE `users_account` MODIFY COLUMN `status_id` tinyint DEFAULT 1;
ALTER TABLE `website_app` MODIFY COLUMN `sort` int DEFAULT 0;
ALTER TABLE `website_app` MODIFY COLUMN `status_id` tinyint DEFAULT 1;
ALTER TABLE `website_carousel` MODIFY COLUMN `sort` int DEFAULT 0;
ALTER TABLE `website_carousel` MODIFY COLUMN `status_id` tinyint DEFAULT 1;
ALTER TABLE `website_link` MODIFY COLUMN `sort` int DEFAULT 0;
ALTER TABLE `website_link` MODIFY COLUMN `status_id` tinyint DEFAULT 1;
ALTER TABLE `website_nav` MODIFY COLUMN `sort` int DEFAULT 0;
ALTER TABLE `website_nav` MODIFY COLUMN `status_id` tinyint DEFAULT 1;
ALTER TABLE `zone` MODIFY COLUMN `sort` int DEFAULT 0;
ALTER TABLE `zone` MODIFY COLUMN `status_id` tinyint DEFAULT 1;
ALTER TABLE `zone_course` MODIFY COLUMN `sort` int DEFAULT 0;
ALTER TABLE `zone_course` MODIFY COLUMN `status_id` tinyint DEFAULT 1;

-- 二、回填已经产生的 NULL 数据
UPDATE `category` SET `sort` = 0 WHERE `sort` IS NULL;
UPDATE `category` SET `status_id` = 1 WHERE `status_id` IS NULL;
UPDATE `course` SET `count_buy` = 0 WHERE `count_buy` IS NULL;
UPDATE `course` SET `count_study` = 0 WHERE `count_study` IS NULL;
UPDATE `course` SET `is_free` = 0 WHERE `is_free` IS NULL;
UPDATE `course` SET `is_putaway` = 0 WHERE `is_putaway` IS NULL;
UPDATE `course` SET `sort` = 0 WHERE `sort` IS NULL;
UPDATE `course` SET `status_id` = 1 WHERE `status_id` IS NULL;
UPDATE `course_chapter` SET `is_free` = 0 WHERE `is_free` IS NULL;
UPDATE `course_chapter` SET `sort` = 0 WHERE `sort` IS NULL;
UPDATE `course_chapter` SET `status_id` = 1 WHERE `status_id` IS NULL;
UPDATE `course_chapter_period` SET `is_free` = 0 WHERE `is_free` IS NULL;
UPDATE `course_chapter_period` SET `sort` = 0 WHERE `sort` IS NULL;
UPDATE `course_chapter_period` SET `status_id` = 1 WHERE `status_id` IS NULL;
UPDATE `exam_paper` SET `sort` = 0 WHERE `sort` IS NULL;
UPDATE `exam_paper` SET `status_id` = 1 WHERE `status_id` IS NULL;
UPDATE `exam_paper_rule` SET `sort` = 0 WHERE `sort` IS NULL;
UPDATE `exam_paper_rule` SET `status_id` = 1 WHERE `status_id` IS NULL;
UPDATE `exam_question` SET `sort` = 0 WHERE `sort` IS NULL;
UPDATE `exam_question` SET `status_id` = 1 WHERE `status_id` IS NULL;
UPDATE `exam_record` SET `status_id` = 1 WHERE `status_id` IS NULL;
UPDATE `lecturer` SET `sort` = 0 WHERE `sort` IS NULL;
UPDATE `lecturer` SET `status_id` = 1 WHERE `status_id` IS NULL;
UPDATE `live` SET `sort` = 0 WHERE `sort` IS NULL;
UPDATE `live` SET `status_id` = 1 WHERE `status_id` IS NULL;
UPDATE `msg` SET `sort` = 0 WHERE `sort` IS NULL;
UPDATE `msg` SET `status_id` = 1 WHERE `status_id` IS NULL;
UPDATE `msg_user` SET `sort` = 0 WHERE `sort` IS NULL;
UPDATE `msg_user` SET `status_id` = 1 WHERE `status_id` IS NULL;
UPDATE `resource` SET `sort` = 0 WHERE `sort` IS NULL;
UPDATE `resource` SET `status_id` = 1 WHERE `status_id` IS NULL;
UPDATE `sys_config` SET `sort` = 0 WHERE `sort` IS NULL;
UPDATE `sys_menu` SET `is_show` = 1 WHERE `is_show` IS NULL;
UPDATE `sys_menu` SET `sort` = 0 WHERE `sort` IS NULL;
UPDATE `sys_menu` SET `status_id` = 1 WHERE `status_id` IS NULL;
UPDATE `sys_menu_role` SET `sort` = 0 WHERE `sort` IS NULL;
UPDATE `sys_menu_role` SET `status_id` = 1 WHERE `status_id` IS NULL;
UPDATE `sys_role` SET `sort` = 0 WHERE `sort` IS NULL;
UPDATE `sys_role` SET `status_id` = 1 WHERE `status_id` IS NULL;
UPDATE `sys_role_user` SET `sort` = 0 WHERE `sort` IS NULL;
UPDATE `sys_role_user` SET `status_id` = 1 WHERE `status_id` IS NULL;
UPDATE `sys_user` SET `sort` = 0 WHERE `sort` IS NULL;
UPDATE `sys_user` SET `status_id` = 1 WHERE `status_id` IS NULL;
UPDATE `user_course` SET `sort` = 0 WHERE `sort` IS NULL;
UPDATE `user_course` SET `status_id` = 1 WHERE `status_id` IS NULL;
UPDATE `user_course_assign` SET `status_id` = 1 WHERE `status_id` IS NULL;
UPDATE `user_course_collect` SET `sort` = 0 WHERE `sort` IS NULL;
UPDATE `user_course_collect` SET `status_id` = 1 WHERE `status_id` IS NULL;
UPDATE `user_course_comment` SET `sort` = 0 WHERE `sort` IS NULL;
UPDATE `user_course_comment` SET `status_id` = 1 WHERE `status_id` IS NULL;
UPDATE `users` SET `status_id` = 1 WHERE `status_id` IS NULL;
UPDATE `users_account` SET `status_id` = 1 WHERE `status_id` IS NULL;
UPDATE `website_app` SET `sort` = 0 WHERE `sort` IS NULL;
UPDATE `website_app` SET `status_id` = 1 WHERE `status_id` IS NULL;
UPDATE `website_carousel` SET `sort` = 0 WHERE `sort` IS NULL;
UPDATE `website_carousel` SET `status_id` = 1 WHERE `status_id` IS NULL;
UPDATE `website_link` SET `sort` = 0 WHERE `sort` IS NULL;
UPDATE `website_link` SET `status_id` = 1 WHERE `status_id` IS NULL;
UPDATE `website_nav` SET `sort` = 0 WHERE `sort` IS NULL;
UPDATE `website_nav` SET `status_id` = 1 WHERE `status_id` IS NULL;
UPDATE `zone` SET `sort` = 0 WHERE `sort` IS NULL;
UPDATE `zone` SET `status_id` = 1 WHERE `status_id` IS NULL;
UPDATE `zone_course` SET `sort` = 0 WHERE `sort` IS NULL;
UPDATE `zone_course` SET `status_id` = 1 WHERE `status_id` IS NULL;

-- 校验：应全部为 0
SELECT
  (SELECT COUNT(*) FROM course WHERE status_id IS NULL) AS course_null,
  (SELECT COUNT(*) FROM course_chapter WHERE status_id IS NULL) AS chapter_null,
  (SELECT COUNT(*) FROM course_chapter_period WHERE status_id IS NULL) AS period_null,
  (SELECT COUNT(*) FROM resource WHERE status_id IS NULL) AS resource_null;
