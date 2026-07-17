-- 考试管理菜单（二开 Phase A 前端页面）
INSERT INTO sys_menu (id, status_id, sort, parent_id, is_show, menu_type, menu_name, menu_icon, path, component, apis, permission, remark) VALUES
 (600, 1, 3, 0,   1, 1, '考试管理', '课程', NULL, NULL, NULL, NULL, NULL),
 (601, 1, 1, 600, 1, 2, '题库管理', NULL, '/exam/question', '/exam/question/index.vue', NULL, NULL, NULL),
 (602, 1, 2, 600, 1, 2, '试卷管理', NULL, '/exam/paper', '/exam/paper/index.vue', NULL, NULL, NULL),
 (603, 1, 3, 600, 1, 2, '课程指派', NULL, '/exam/assign', '/exam/assign/index.vue', NULL, NULL, NULL),
 (604, 1, 4, 600, 1, 2, '考试记录', NULL, '/exam/record', '/exam/record/index.vue', NULL, NULL, NULL)
ON DUPLICATE KEY UPDATE menu_name=VALUES(menu_name), path=VALUES(path), component=VALUES(component);

INSERT INTO sys_menu_role (id, status_id, sort, role_id, menu_id)
SELECT id, 1, sort, 1, id FROM sys_menu WHERE id BETWEEN 600 AND 604
ON DUPLICATE KEY UPDATE menu_id=VALUES(menu_id);
