-- 「排课配置」菜单
--
-- 需求文件《线上入职培训系统—培训模块清单》的「培训模块总览」有 42 门课，
-- 每门都标了「入职第 N 天推送 / 给哪个班组 / 要不要闯关」。
-- 这些规则散在 42 个课程编辑页里既改不动，也看不出哪些课漏配了、
-- 哪个班组一门课都没分到。集中成一张表来维护。
--
-- 放在「课程管理」下面：它配的是课程的属性，不是考试或统计。
--
-- 执行：mysql -uroncoo -p roncoo_education < db/w5_schedule_menu.sql

INSERT INTO sys_menu (id, status_id, sort, parent_id, is_show, menu_type, menu_name, menu_icon, path, component, permission, remark) VALUES
  (207, 1, 7, 200, 1, 2, '排课配置', NULL, '/course/schedule', '/course/schedule/index.vue', NULL,
   '集中维护每门课的推送时机、推送范围、完成期限与顺序解锁'),
  (20701, 1, 1, 207, 0, 3, '保存', NULL, NULL, NULL, 'schedule:save', '保存排课配置')
ON DUPLICATE KEY UPDATE menu_name = VALUES(menu_name), path = VALUES(path),
                        component = VALUES(component), parent_id = VALUES(parent_id),
                        menu_type = VALUES(menu_type), is_show = VALUES(is_show),
                        permission = VALUES(permission);

-- 接口权限。网关对非超管账号按 apis 逐接口校验，
-- 不填这一列的话，除 admin 外任何账号打开这个页面都是 403
UPDATE sys_menu SET apis = '/course/admin/schedule/list,/course/admin/schedule/save,/user/admin/team/list'
WHERE id = 207;

-- 绑到超管角色
INSERT INTO sys_menu_role (id, status_id, sort, menu_id, role_id)
SELECT 20700, 1, 0, 207, (SELECT role_id FROM sys_role_user ORDER BY id LIMIT 1)
ON DUPLICATE KEY UPDATE menu_id = VALUES(menu_id), role_id = VALUES(role_id);

INSERT INTO sys_menu_role (id, status_id, sort, menu_id, role_id)
SELECT 20701, 1, 0, 20701, (SELECT role_id FROM sys_role_user ORDER BY id LIMIT 1)
ON DUPLICATE KEY UPDATE menu_id = VALUES(menu_id), role_id = VALUES(role_id);

-- 培训管理员角色若已存在，一并授权——这张表本来就是给他用的
INSERT INTO sys_menu_role (id, status_id, sort, menu_id, role_id)
SELECT 20702, 1, 0, 207, r.id FROM sys_role r WHERE r.role_name = '培训管理员'
ON DUPLICATE KEY UPDATE menu_id = VALUES(menu_id), role_id = VALUES(role_id);

INSERT INTO sys_menu_role (id, status_id, sort, menu_id, role_id)
SELECT 20703, 1, 0, 20701, r.id FROM sys_role r WHERE r.role_name = '培训管理员'
ON DUPLICATE KEY UPDATE menu_id = VALUES(menu_id), role_id = VALUES(role_id);

-- 校验
SELECT id, menu_name, path, component, LEFT(apis, 60) AS apis片段 FROM sys_menu WHERE id IN (207, 20701);
SELECT r.role_name AS 角色, COUNT(*) AS 已授权
FROM sys_menu_role mr JOIN sys_role r ON r.id = mr.role_id
WHERE mr.menu_id IN (207, 20701) GROUP BY r.role_name;
