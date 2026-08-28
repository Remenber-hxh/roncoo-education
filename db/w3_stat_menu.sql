-- W3：学习统计看板的菜单
--
-- menu_type=2 驱动前端路由，缺了就是登录后访问 /stat 直接 404。
-- 这个页面只有查看、没有增删改，所以不需要 menu_type=3 的按钮权限。
--
-- sort 与「概况」同为 1，靠 id 排在它后面——看板是给主管和领导看的，
-- 放在侧边栏第二位，不要埋进二级菜单里。
--
-- 执行：mysql -uroncoo -p roncoo_education < db/w3_stat_menu.sql

INSERT INTO sys_menu (id, status_id, sort, parent_id, is_show, menu_type, menu_name, menu_icon, path, component, permission, remark) VALUES
  (700, 1, 1, 0, 1, 2, '学习统计', NULL, '/stat', '/stat/index.vue', NULL, '学习统计看板：必修完成率、班组对比、逾期名单')
ON DUPLICATE KEY UPDATE menu_name = VALUES(menu_name), path = VALUES(path),
                        component = VALUES(component), is_show = VALUES(is_show),
                        sort = VALUES(sort), parent_id = VALUES(parent_id);

-- 绑到角色。不绑的话菜单存在但取不到，侧边栏依然没有这一项
INSERT INTO sys_menu_role (id, status_id, sort, menu_id, role_id)
SELECT 70000, 1, 0, 700, (SELECT role_id FROM sys_role_user ORDER BY id LIMIT 1)
ON DUPLICATE KEY UPDATE menu_id = VALUES(menu_id), role_id = VALUES(role_id);

-- 校验
SELECT id, sort, menu_type, menu_name, path, component FROM sys_menu WHERE id = 700;
SELECT COUNT(*) AS 已绑定, IF(COUNT(*) = 1, '完整', '缺失!!') AS 结论
FROM sys_menu_role WHERE menu_id = 700;
