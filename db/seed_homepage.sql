-- 门户首页演示数据：专区+专区课程+轮播+导航，课程封面
INSERT INTO zone (id, status_id, sort, zone_name, zone_desc) VALUES
 (1, 1, 1, '入职必修', '新员工入职必修课程')
ON DUPLICATE KEY UPDATE zone_name=VALUES(zone_name);

INSERT INTO zone_course (id, status_id, sort, zone_id, course_id) VALUES
 (1, 1, 1, 1, 1)
ON DUPLICATE KEY UPDATE zone_id=VALUES(zone_id);

INSERT INTO website_carousel (id, status_id, sort, carousel_title, carousel_img, carousel_url, carousel_target, begin_time, end_time) VALUES
 (1, 1, 1, '内部培训学习平台', '/banner1.svg', '', 1, '2026-01-01 00:00:00', '2030-12-31 23:59:59')
ON DUPLICATE KEY UPDATE carousel_img=VALUES(carousel_img);

INSERT INTO website_nav (id, status_id, sort, nav_title, nav_url, nav_target) VALUES
 (1, 1, 1, '首页', '/', 1),
 (2, 1, 2, '全部课程', '/course/list', 1)
ON DUPLICATE KEY UPDATE nav_title=VALUES(nav_title);

UPDATE course SET course_logo='/course-logo.svg' WHERE id=1;
