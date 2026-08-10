-- 补齐三个「只能从按钮进入」的页面的路由
--
-- 现象：点「添加课程」「章节」「专区管理」没反应。
--
-- 原因：前端动态路由完全来自登录接口返回的 routerList，而 routerList
--       只包含 sys_menu 里 menu_type=2 的记录。7 月建菜单种子数据时，
--       这三个页面因为不在侧边栏出现就没有登记，于是根本没有对应路由，
--       router.push 找不到匹配，静默失败（控制台有 No match found 警告）。
--
-- 修法：登记为 menu_type=2 拿到路由，同时 is_show=0 不进侧边栏
--       （SysUserCommonBiz.filters() 会过滤 is_show，见 fix_menu_and_config.sql）。
--       这三个页面都依赖 URL 参数(courseId / zoneId)，本来也不该出现在侧边栏。
--
-- 执行：mysql -uroncoo -p roncoo_education < db/fix_missing_routes.sql

INSERT INTO sys_menu (id, status_id, sort, parent_id, is_show, menu_type, menu_name, path, component) VALUES
  (205, 1, 5, 200, 0, 2, '课程编辑', '/course/update',       '/course/update/index.vue'),
  (206, 1, 6, 200, 0, 2, '章节管理', '/course/chapter',      '/course/chapter/index.vue'),
  (406, 1, 6, 400, 0, 2, '专区课程', '/common/zone/course',  '/common/zone/course/index.vue')
ON DUPLICATE KEY UPDATE path = VALUES(path), component = VALUES(component), is_show = VALUES(is_show);

-- 绑定到超级管理员角色，否则登录时取不到这些菜单
INSERT INTO sys_menu_role (id, status_id, role_id, menu_id) VALUES
  (2051, 1, 1, 205),
  (2061, 1, 1, 206),
  (4061, 1, 1, 406)
ON DUPLICATE KEY UPDATE menu_id = VALUES(menu_id);

-- 校验：这三条应为 is_show=0，且都已绑定超管
SELECT m.id, m.menu_name, m.path, m.is_show, IF(r.id IS NULL, '未绑定', '已绑定') AS 角色绑定
FROM sys_menu m
LEFT JOIN sys_menu_role r ON r.menu_id = m.id AND r.role_id = 1
WHERE m.id IN (205, 206, 406);
