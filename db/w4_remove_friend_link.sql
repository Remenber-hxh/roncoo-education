-- 下线「友情链接」
--
-- 友情链接是对外门户网站的概念：公司内部培训平台不会往外链，
-- 而它偏偏占着员工端页脚唯一的栏目，页面看着像个没做完的半成品。
-- 员工端已移除该组件（pages/index.vue），后台菜单一并隐藏，
-- 否则管理员还能进去维护一批永远不会显示的数据。
--
-- 用 is_show=0 而不是删除：表和接口都还在，
-- 万一将来要恢复，改回 1 即可，不用重新建菜单和权限。
--
-- 执行：mysql -uroncoo -p roncoo_education < db/w4_remove_friend_link.sql

UPDATE sys_menu SET is_show = 0 WHERE id = 403;

-- 校验
SELECT id, menu_name, is_show,
       IF(is_show = 0, '已隐藏', '仍显示!!') AS 结论
FROM sys_menu WHERE id = 403;
