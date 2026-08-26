-- 用户列表增加「删除」按钮权限
--
-- 后端 /user/admin/users/delete 接口本来就有，缺的只是权限行——
-- sys_menu 里没有 menu_type=3 的 user:delete，v-permission 会把按钮
-- 直接从 DOM 里删掉，所以界面上一直看不到。
--
-- 注意：该接口是硬删除，只删 users 一行，不动关联数据。
-- 库里有 14 张表挂着 user_id，其中这几张是培训合规凭证：
--   exam_record          考试成绩
--   user_study_daily     每日学习时长
--   user_agreement_sign  协议签署记录（含签署时间与IP）
-- 删了用户，这些记录会变成查不到人的孤儿数据。
-- 因此界面上把「禁用」作为推荐操作，删除的二次确认里写明了后果。
--
-- 执行：mysql -uroncoo -p roncoo_education < db/w3_user_delete_permission.sql

INSERT INTO sys_menu (id, status_id, sort, parent_id, is_show, menu_type, menu_name, permission) VALUES
  (30103, 1, 3, 301, 1, 3, '删除', 'user:delete')
ON DUPLICATE KEY UPDATE menu_name = VALUES(menu_name), permission = VALUES(permission);

-- 绑定到超级管理员角色，role_id 不写死
INSERT INTO sys_menu_role (id, status_id, sort, menu_id, role_id)
SELECT 30103, 1, 3, 30103, (SELECT role_id FROM sys_role_user ORDER BY id LIMIT 1)
ON DUPLICATE KEY UPDATE menu_id = VALUES(menu_id), role_id = VALUES(role_id);

-- 校验
SELECT id, menu_name, permission FROM sys_menu WHERE parent_id = 301 ORDER BY sort;
SELECT COUNT(*) AS 已绑定, IF(COUNT(*) = 1, '完整', '缺失!!') AS 结论
FROM sys_menu_role WHERE menu_id = 30103;
