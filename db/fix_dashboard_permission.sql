-- 补齐概况页的三个统计权限
--
-- 现象：登录后「概况」页整页空白。
--
-- 原因：上一版权限种子数据（db/seed_permission.sql）只扫描了
--       v-permission="'xxx'" 这种**指令**写法，漏掉了
--       hasPermission('xxx') 这种**函数调用**写法。
--       概况页恰好三个统计组件全用的是函数写法：
--         <el-col v-if="hasPermission('stat:data')">  <stat-data />
--         <div   v-if="hasPermission('stat:login')">  <stat-login />
--         <div   v-if="hasPermission('stat:vod')">    <stat-vod />
--       权限不存在 -> hasPermission 全返回 false -> 三个组件都不渲染 -> 空白页。
--
-- 已重新用「指令 + 函数」两种写法完整扫描，全项目共 50 个权限标识，
-- 此前入库 47 个，本脚本补齐剩余 3 个。
--
-- 执行：mysql -uroncoo -p roncoo_education < db/fix_dashboard_permission.sql

INSERT INTO sys_menu (id, status_id, sort, parent_id, is_show, menu_type, menu_name, permission) VALUES
  (10001, 1, 1, 100, 0, 3, '数据统计', 'stat:data'),
  (10002, 1, 2, 100, 0, 3, '登录统计', 'stat:login'),
  (10003, 1, 3, 100, 0, 3, '点播统计', 'stat:vod')
ON DUPLICATE KEY UPDATE menu_name = VALUES(menu_name), permission = VALUES(permission);

INSERT INTO sys_menu_role (id, status_id, role_id, menu_id) VALUES
  (100011, 1, 1, 10001),
  (100021, 1, 1, 10002),
  (100031, 1, 1, 10003)
ON DUPLICATE KEY UPDATE menu_id = VALUES(menu_id);

-- 校验：权限总数应为 50，且三个 stat 权限均已绑定超管
SELECT COUNT(*) AS 权限总数 FROM sys_menu WHERE menu_type = 3;

SELECT m.permission, IF(r.id IS NULL, '未绑定', '已绑定') AS 超管
FROM sys_menu m
LEFT JOIN sys_menu_role r ON r.menu_id = m.id AND r.role_id = 1
WHERE m.permission LIKE 'stat:%';
