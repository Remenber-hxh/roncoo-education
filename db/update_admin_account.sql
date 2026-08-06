-- 管理员登录账号变更：13800138000 -> admin，密码改为 88775560
--
-- 说明：
--   sys_user.mobile 字段虽名为 mobile，但后端登录接口（AdminSysUserLoginReq.mobile）
--   只做 @NotBlank 校验，不校验手机号格式，字段语义就是"登录账号"，可以填任意字符串。
--   密码算法：mobile_psw = UPPER(SHA1(CONCAT(mobile_salt, 明文)))，见 Sha1Util.getSign。
--
-- 执行：
--   mysql -uroncoo -p roncoo_education < db/update_admin_account.sql

UPDATE sys_user
SET mobile     = 'admin',
    mobile_psw = UPPER(SHA1(CONCAT(mobile_salt, '88775560')))
WHERE id = 1;

-- 校验：应返回 mobile=admin，且 psw_ok=1
SELECT id,
       mobile,
       real_name,
       mobile_psw = UPPER(SHA1(CONCAT(mobile_salt, '88775560'))) AS psw_ok
FROM sys_user
WHERE id = 1;
