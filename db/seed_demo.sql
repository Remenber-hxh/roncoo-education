-- 演示数据（可在后台删除）：课程分类 + 讲师 + 一门演示课程
INSERT INTO category (id, status_id, sort, parent_id, category_type, category_name, remark) VALUES
 (1, 1, 1, 0, 1, '入职培训', '演示分类')
ON DUPLICATE KEY UPDATE category_name=VALUES(category_name);

INSERT INTO lecturer (id, status_id, sort, user_id, lecturer_name, lecturer_position, introduce) VALUES
 (1, 1, 1, 1, '培训讲师', '内部讲师', '演示讲师')
ON DUPLICATE KEY UPDATE lecturer_name=VALUES(lecturer_name);

INSERT INTO course (id, status_id, sort, lecturer_id, category_id, course_name, is_free, ruling_price, course_price, is_putaway, course_sort, count_buy, count_study, speed_double, speed_drag, introduce) VALUES
 (1, 1, 1, 1, 1, '新员工入职第一课（演示）', 1, 0, 0, 1, 1, 0, 0, 1, 1, '<p>这是一门演示课程，用于验证平台前台展示。</p>')
ON DUPLICATE KEY UPDATE course_name=VALUES(course_name);

-- 门户员工测试账号（13900139000 / 123456，密码=sha1小写(salt+明文)）
INSERT INTO users (id, status_id, mobile, mobile_salt, mobile_psw, nickname, register_source) VALUES
 (1, 1, '13900139000', 'roncoo', '71464777cdd2f351329a37f1d31635dd0bd8cbfe', '测试员工', 1)
ON DUPLICATE KEY UPDATE mobile_psw=VALUES(mobile_psw);
