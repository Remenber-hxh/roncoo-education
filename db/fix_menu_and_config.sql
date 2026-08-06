-- 修复两处 7 月种子数据的遗漏
--
-- 执行：mysql -uroncoo -p roncoo_education < db/fix_menu_and_config.sql

-- ============================================================
-- 一、把「用户记录」「学习记录」从侧边栏隐藏
--
-- 这两个是详情页，分别依赖 URL 参数 userId / courseId：
--   /users/record  <- 用户列表的「查看记录」按钮
--   /course/record <- 课程列表的「数据」按钮
-- 7 月建菜单种子数据时按 views 目录下的 index.vue 逐个建菜单，
-- 把这两个详情页也当成了独立菜单。直接点侧边栏进入时没有参数，
-- 前端会把字符串 "undefined" 发给后端，Long 转换失败抛异常，
-- 页面上连弹三个「服务繁忙，请重试」。
--
-- is_show=0 只影响侧边栏(menuList)，routerList 仍然包含它们，
-- 所以从列表页按钮跳转依旧正常。
-- 配套改动：SysUserCommonBiz.filters() 增加 is_show 过滤。
-- ============================================================
UPDATE sys_menu SET is_show = 0 WHERE path IN ('/users/record', '/course/record');

-- ============================================================
-- 二、补齐「参数配置」页依赖的平台选择项
--
-- AdminSysConfigBiz.list() 在 configType 为 3(点播/直播)、4(存储)、5(短信) 时，
-- 会读取下列配置键并调用 XxxPlatformEnum.byCode(值).getTag()。
-- 这些键缺失时 getByConfigKey() 返回 null，直接空指针，
-- 表现为进入这些标签页报「服务繁忙，请重试」。
--
-- 取值必须是对应枚举里存在的 code，否则 byCode() 返回 null 同样会空指针：
--   vodPlatform     1=私有云 2=保利威 3=百家云 4=获得场景
--   livePlatform    2=保利威 3=百家云 4=获得场景
--   storagePlatform 2=MinIO 3=阿里云 4=腾讯云 5=华为云
--   smsPlatform     1=私有云 2=阿里云 3=腾讯云 4=华为云
-- 平台均未接入，这里只是让配置页能正常打开，实际接入时在页面上改。
-- ============================================================
INSERT INTO sys_config (id, config_type, config_name, config_key, config_value, sort) VALUES
  (20, 3, '点播平台', 'vodPlatform',     '2', 1),
  (21, 3, '直播平台', 'livePlatform',    '2', 2),
  (22, 4, '存储平台', 'storagePlatform', '2', 1),
  (23, 5, '短信平台', 'smsPlatform',     '2', 1)
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value);

-- 校验
SELECT '侧边栏应隐藏的菜单' AS 检查项, GROUP_CONCAT(menu_name) AS 结果
FROM sys_menu WHERE is_show = 0 AND menu_type = 2
UNION ALL
SELECT '平台配置键', GROUP_CONCAT(CONCAT(config_key, '=', config_value))
FROM sys_config WHERE config_key IN ('vodPlatform', 'livePlatform', 'storagePlatform', 'smsPlatform');
