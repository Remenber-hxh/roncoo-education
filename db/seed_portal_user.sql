-- 门户（员工端）测试账号
--
-- 注意：users 表的密码是 **小写** sha1，与后台 sys_user 的 **大写** 不同！
--   门户：mobile_psw = LOWER(SHA1(CONCAT(mobile_salt, 明文)))
--   后台：mobile_psw = UPPER(SHA1(CONCAT(mobile_salt, 明文)))
-- 接口只做非空校验、不校验手机号格式，所以 mobile 字段可以填任意字符串。
--
-- 执行：mysql -uroncoo -p roncoo_education < db/seed_portal_user.sql

-- 新增一个好记的员工账号：staff / 88775560（与后台 admin 同密码）
INSERT INTO users (id, status_id, mobile, mobile_salt, mobile_psw, nickname, register_source)
VALUES (2, 1, 'staff', 'roncoo', LOWER(SHA1(CONCAT('roncoo', '88775560'))), '测试员工', 1)
ON DUPLICATE KEY UPDATE
    mobile     = VALUES(mobile),
    mobile_psw = VALUES(mobile_psw),
    status_id  = 1;

-- 顺带把原有的 13900139000 密码也统一成 88775560，避免记两套
UPDATE users SET mobile_psw = LOWER(SHA1(CONCAT(mobile_salt, '88775560'))), status_id = 1
WHERE mobile = '13900139000';

-- 校验：两个账号的密码都应能用 88775560 通过
SELECT id, mobile, nickname, status_id,
       mobile_psw = LOWER(SHA1(CONCAT(mobile_salt, '88775560'))) AS 密码是88775560
FROM users;
