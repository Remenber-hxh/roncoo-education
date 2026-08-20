-- W2：项目组管理的菜单与权限
--
-- 同班组：menu_type=2 驱动前端路由，menu_type=3 驱动 v-permission，
-- 两者都要绑到角色，缺一样就是登录后 404 或按钮从 DOM 里消失。
--
-- 执行：mysql -uroncoo -p roncoo_education < db/w2_project_group_menu.sql

INSERT INTO sys_menu (id, status_id, sort, parent_id, is_show, menu_type, menu_name, menu_icon, path, component, permission, remark) VALUES
  (306, 1, 6, 300, 1, 2, '项目组管理', NULL, '/users/projectGroup', '/users/projectGroup/index.vue', NULL, '项目组字典维护，与班组是两个正交维度')
ON DUPLICATE KEY UPDATE menu_name = VALUES(menu_name), path = VALUES(path),
                        component = VALUES(component), is_show = VALUES(is_show), sort = VALUES(sort);

INSERT INTO sys_menu (id, status_id, sort, parent_id, is_show, menu_type, menu_name, permission) VALUES
  (30601, 1, 1, 306, 1, 3, '新增', 'projectGroup:save'),
  (30602, 1, 2, 306, 1, 3, '编辑', 'projectGroup:edit'),
  (30603, 1, 3, 306, 1, 3, '删除', 'projectGroup:delete')
ON DUPLICATE KEY UPDATE menu_name = VALUES(menu_name), permission = VALUES(permission);

INSERT INTO sys_menu_role (id, status_id, sort, menu_id, role_id)
SELECT 30600 + t.n, 1, t.n, t.menu_id, (SELECT role_id FROM sys_role_user ORDER BY id LIMIT 1)
FROM (
  SELECT 0 AS n, 306 AS menu_id
  UNION ALL SELECT 1, 30601
  UNION ALL SELECT 2, 30602
  UNION ALL SELECT 3, 30603
) t
ON DUPLICATE KEY UPDATE menu_id = VALUES(menu_id), role_id = VALUES(role_id);

-- 校验
SELECT id, menu_type, menu_name, path, permission FROM sys_menu WHERE id = 306 OR parent_id = 306 ORDER BY id;
SELECT COUNT(*) AS 已绑定行数, IF(COUNT(*) = 4, '完整', '缺失!!') AS 结论
FROM sys_menu_role WHERE menu_id IN (306, 30601, 30602, 30603);
