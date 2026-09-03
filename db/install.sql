-- 内部培训平台 · 全新安装（按顺序执行全部脚本）
--
-- 用法：
--   mysql -uroncoo -p roncoo_education < db/install.sql
--
-- 顺序不能乱：建表 -> 结构修正 -> 种子数据 -> 数据修正。
-- 各脚本本身可重复执行（幂等），但首次安装请一次性跑完。
--
-- 注意：执行完后必须到「系统管理 → 参数配置」里把下列值改成生产环境的：
--   localStoragePath    默认是 D:/视频培训/uploads，Linux 上应为 /data/uploads
--   localStorageDomain  默认是 http://localhost:7700/system/images
--                       应为 https://你的域名/gateway/system/images
--   websiteDomain       网站域名
--   websiteName         网站名称

-- ============ 一、表结构 ============
SOURCE schema_rebuild.sql;
SOURCE exam_schema.sql;
SOURCE w1_profile_and_study.sql;
SOURCE w2_project_group.sql;
SOURCE w3_question_chapter.sql;
SOURCE fix_comment_parent_id.sql;
-- 按实际学习进度归正课程指派的完成状态。全新安装时无数据可修，
-- 但保留在流程里，从旧库升级时才不会漏掉
SOURCE w3_fix_assign_finish_status.sql;
SOURCE w2_team_seed.sql;

-- 补齐反向重建时丢失的字段默认值。
-- 不执行会导致后台新建的课程在员工端完全看不到，且没有任何报错。
SOURCE fix_column_defaults.sql;

-- 删除商品/售卖模块（订单、支付、资金账户、课程定价）
SOURCE remove_commerce.sql;

-- ============ 二、账号与配置 ============
SOURCE seed_admin.sql;
SOURCE update_admin_account.sql;
SOURCE seed_config.sql;
SOURCE fix_website_config.sql;
SOURCE seed_local_storage.sql;

-- ============ 三、菜单与权限 ============
SOURCE seed_menu.sql;
SOURCE seed_menu_exam.sql;
SOURCE seed_permission.sql;
SOURCE fix_dashboard_permission.sql;
SOURCE fix_menu_and_config.sql;
SOURCE fix_missing_routes.sql;
SOURCE w2_team_menu.sql;
SOURCE w2_project_group_menu.sql;
SOURCE w3_user_delete_permission.sql;
SOURCE w3_stat_menu.sql;

-- 站内消息（逾期催办）+ 催办按钮权限
SOURCE w4_notice.sql;

-- 补齐菜单的接口权限。不执行的话，除 admin(userId=1) 外任何账号
-- 登录后都是 403，一个接口都调不了——必须放在所有菜单脚本之后。
SOURCE w4_menu_apis.sql;

-- 下线友情链接（内部系统不需要对外友链）
SOURCE w4_remove_friend_link.sql;

-- 按入职天数自动排课 + 顺序解锁（闯关）
SOURCE w5_auto_assign.sql;
SOURCE w5_schedule_menu.sql;

-- ============ 三点五、业务基础数据 ============
-- 六大培训模块的课程分类。课程、题库都要归到这些模块下，属于必装项。
SOURCE w3_course_category.sql;

-- ============ 四、演示数据（生产环境可跳过）============
-- SOURCE seed_demo.sql;
-- SOURCE seed_homepage.sql;
-- SOURCE seed_portal_user.sql;
-- 隐私政策正文：上线前需 HR / 法务过目，故默认不装
-- SOURCE seed_privacy_policy.sql;

-- ============ 校验 ============
SELECT '表数量' AS 检查项, COUNT(*) AS 值 FROM information_schema.tables WHERE table_schema = DATABASE()
UNION ALL SELECT '后台菜单', COUNT(*) FROM sys_menu WHERE menu_type IN (1,2)
UNION ALL SELECT '按钮权限', COUNT(*) FROM sys_menu WHERE menu_type = 3
UNION ALL SELECT '班组字典', COUNT(*) FROM team
UNION ALL SELECT '一级课程分类', COUNT(*) FROM category WHERE category_type = 1 AND parent_id = 0
UNION ALL SELECT '证书类型', COUNT(*) FROM certificate_type
UNION ALL SELECT '管理员账号', COUNT(*) FROM sys_user;

SELECT config_key, config_value FROM sys_config
 WHERE config_key IN ('localStoragePath','localStorageDomain','websiteDomain','websiteName');
