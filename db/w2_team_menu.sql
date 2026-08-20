-- W2：班组管理的菜单与权限
--
-- roncoo 的菜单、路由、按钮权限全部是数据驱动的，代码写完还不够：
--   menu_type=2 的行 -> 前端动态路由（缺了就是登录后 404）
--   menu_type=3 的行 -> v-permission 指令（缺了按钮会被直接从 DOM 删掉，
--                       表现为「更多操作」点开是空的）
--   sys_menu_role     -> 没绑角色，上面两样都拿不到
-- 这三处之前都踩过，所以新功能一并补齐。
--
-- 执行：mysql -uroncoo -p roncoo_education < db/w2_team_menu.sql

-- 一、菜单：挂在「用户管理」(id=300) 下，排在讲师管理之后
--    id 沿用现有编号规律：3xx 为菜单，3xx0y 为该菜单下的权限
INSERT INTO sys_menu (id, status_id, sort, parent_id, is_show, menu_type, menu_name, menu_icon, path, component, permission, remark) VALUES
  (305, 1, 5, 300, 1, 2, '班组管理', NULL, '/users/team', '/users/team/index.vue', NULL, '班组字典维护，员工档案与按班组统计的分组依据')
ON DUPLICATE KEY UPDATE menu_name = VALUES(menu_name), path = VALUES(path),
                        component = VALUES(component), is_show = VALUES(is_show), sort = VALUES(sort);

-- 二、按钮权限
INSERT INTO sys_menu (id, status_id, sort, parent_id, is_show, menu_type, menu_name, permission) VALUES
  (30501, 1, 1, 305, 1, 3, '新增', 'team:save'),
  (30502, 1, 2, 305, 1, 3, '编辑', 'team:edit'),
  (30503, 1, 3, 305, 1, 3, '删除', 'team:delete')
ON DUPLICATE KEY UPDATE menu_name = VALUES(menu_name), permission = VALUES(permission);

-- 三、绑定到超级管理员角色
--    role_id 从现有绑定里取，不写死，避免不同环境角色ID不一致
INSERT INTO sys_menu_role (id, status_id, sort, menu_id, role_id)
SELECT 30500 + t.n, 1, t.n, t.menu_id, (SELECT role_id FROM sys_role_user ORDER BY id LIMIT 1)
FROM (
  SELECT 0 AS n, 305 AS menu_id
  UNION ALL SELECT 1, 30501
  UNION ALL SELECT 2, 30502
  UNION ALL SELECT 3, 30503
) t
ON DUPLICATE KEY UPDATE menu_id = VALUES(menu_id), role_id = VALUES(role_id);

-- 校验一：菜单与权限行是否就位
SELECT id, menu_type, menu_name, path, permission,
       CASE menu_type WHEN 2 THEN '菜单(驱动路由)' WHEN 3 THEN '权限(驱动按钮)' ELSE '其他' END AS 作用
FROM sys_menu WHERE id = 305 OR parent_id = 305 ORDER BY id;

-- 校验二：四行是否都绑到了角色。少一行，对应的路由或按钮就不会出现
SELECT COUNT(*) AS 已绑定行数, IF(COUNT(*) = 4, '完整', '缺失!!') AS 结论
FROM sys_menu_role WHERE menu_id IN (305, 30501, 30502, 30503);
