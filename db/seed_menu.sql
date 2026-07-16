-- 菜单/角色初始化数据（roncoo 未开源，按前端 roncoo-education-admin 现有页面重建）
-- 结构：sys_role(超管) + sys_role_user(管理员1↔角色1) + sys_menu(菜单树) + sys_menu_role(角色↔全部菜单)
-- menu_type: 1=目录 2=菜单 3=权限；is_show: 1=显示；status_id: 1=正常

-- 角色
INSERT INTO sys_role (id, status_id, sort, role_name, remark) VALUES
 (1, 1, 1, '超级管理员', '系统内置超管角色')
ON DUPLICATE KEY UPDATE role_name=VALUES(role_name);

-- 管理员(sys_user id=1) 绑定角色
INSERT INTO sys_role_user (id, status_id, sort, role_id, user_id) VALUES
 (1, 1, 1, 1, 1)
ON DUPLICATE KEY UPDATE role_id=VALUES(role_id), user_id=VALUES(user_id);

-- 菜单树
INSERT INTO sys_menu (id, status_id, sort, parent_id, is_show, menu_type, menu_name, menu_icon, path, component, apis, permission, remark) VALUES
 -- 概况
 (100, 1, 1, 0, 1, 2, '概况', '概况', '/dashboard', '/dashboard/index.vue', NULL, NULL, NULL),
 -- 课程管理
 (200, 1, 2, 0,   1, 1, '课程管理', '课程', NULL, NULL, NULL, NULL, NULL),
 (201, 1, 1, 200, 1, 2, '课程分类', NULL, '/course/category', '/course/category/index.vue', NULL, NULL, NULL),
 (202, 1, 2, 200, 1, 2, '课程列表', NULL, '/course/list', '/course/list/index.vue', NULL, NULL, NULL),
 (203, 1, 3, 200, 1, 2, '学习记录', NULL, '/course/record', '/course/record/index.vue', NULL, NULL, NULL),
 (204, 1, 4, 200, 1, 2, '资源管理', NULL, '/course/resource', '/course/resource/index.vue', NULL, NULL, NULL),
 -- 用户管理
 (300, 1, 3, 0,   1, 1, '用户管理', '用户', NULL, NULL, NULL, NULL, NULL),
 (301, 1, 1, 300, 1, 2, '用户列表', NULL, '/users/list', '/users/list/index.vue', NULL, NULL, NULL),
 (302, 1, 2, 300, 1, 2, '讲师管理', NULL, '/users/lecturer', '/users/lecturer/index.vue', NULL, NULL, NULL),
 (303, 1, 3, 300, 1, 2, '用户记录', NULL, '/users/record', '/users/record/index.vue', NULL, NULL, NULL),
 (304, 1, 4, 300, 1, 2, '用户日志', NULL, '/users/log', '/users/log/index.vue', NULL, NULL, NULL),
 -- 网站管理
 (400, 1, 4, 0,   1, 1, '网站管理', '装修', NULL, NULL, NULL, NULL, NULL),
 (401, 1, 1, 400, 1, 2, '轮播管理', NULL, '/common/carousel', '/common/carousel/index.vue', NULL, NULL, NULL),
 (402, 1, 2, 400, 1, 2, '导航管理', NULL, '/common/navigation', '/common/navigation/index.vue', NULL, NULL, NULL),
 (403, 1, 3, 400, 1, 2, '友链管理', NULL, '/common/link', '/common/link/index.vue', NULL, NULL, NULL),
 (404, 1, 4, 400, 1, 2, '专区管理', NULL, '/common/zone', '/common/zone/index.vue', NULL, NULL, NULL),
 (405, 1, 5, 400, 1, 2, '订单管理', NULL, '/common/order', '/common/order/index.vue', NULL, NULL, NULL),
 -- 系统管理
 (500, 1, 5, 0,   1, 1, '系统管理', '系统', NULL, NULL, NULL, NULL, NULL),
 (501, 1, 1, 500, 1, 2, '菜单管理', NULL, '/system/menu', '/system/menu/index.vue', NULL, NULL, NULL),
 (502, 1, 2, 500, 1, 2, '角色管理', NULL, '/system/role', '/system/role/index.vue', NULL, NULL, NULL),
 (503, 1, 3, 500, 1, 2, '系统用户', NULL, '/system/user', '/system/user/index.vue', NULL, NULL, NULL),
 (504, 1, 4, 500, 1, 2, '参数配置', NULL, '/system/config', '/system/config/index.vue', NULL, NULL, NULL),
 (505, 1, 5, 500, 1, 2, '应用管理', NULL, '/system/app', '/system/app/index.vue', NULL, NULL, NULL),
 (506, 1, 6, 500, 1, 2, '操作日志', NULL, '/system/log', '/system/log/index.vue', NULL, NULL, NULL)
ON DUPLICATE KEY UPDATE menu_name=VALUES(menu_name), path=VALUES(path), component=VALUES(component);

-- 角色1 绑定全部菜单
INSERT INTO sys_menu_role (id, status_id, sort, role_id, menu_id)
SELECT id, 1, sort, 1, id FROM sys_menu
ON DUPLICATE KEY UPDATE menu_id=VALUES(menu_id);
