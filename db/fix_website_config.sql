-- 修复网站配置在后台不可见、不可编辑，并补齐缺失的配置项
--
-- 问题一：现有 5 行网站配置的 content_type 为 NULL。
--   后台参数配置页(List.vue / ConfigForm.vue)完全依赖该字段决定渲染哪种控件：
--     1=文本框  2=富文本  3=图片选择器  4=开关
--   为 NULL 时列表里值显示空白、编辑弹窗里没有任何输入框，等于改不了。
--
-- 问题二：7 个网站配置项在库里根本不存在（roncoo 未开源初始化数据），
--   其中 websiteLogo 导致门户页头 <img> 没有 src，渲染出来是空白/裂图。
--   而参数配置页只有「编辑」权限、没有「新增」，缺失的行无法从界面创建，
--   只能靠 SQL 补。
--   注意：ConfigForm.vue 里已经为 websiteLogo / websiteIcon 写好了专用的
--   图片选择器（分别建议 187x45 和 64x64），代码是全的，缺的只是数据行。
--
-- 执行：mysql -uroncoo -p roncoo_education < db/fix_website_config.sql

-- 一、给现有行补上控件类型
UPDATE sys_config SET content_type = 1, config_show = 1, sort = 1  WHERE config_key = 'websiteName';
UPDATE sys_config SET content_type = 1, config_show = 1, sort = 2  WHERE config_key = 'websiteDomain';
UPDATE sys_config SET content_type = 1, config_show = 1, sort = 20 WHERE config_key = 'websiteIcp';
UPDATE sys_config SET content_type = 1, config_show = 1, sort = 21 WHERE config_key = 'websitePrn';
UPDATE sys_config SET content_type = 1, config_show = 1, sort = 30 WHERE config_key = 'websiteCopyright';

-- 二、补齐缺失的配置项（值留空，由管理员在后台填）
INSERT INTO sys_config (id, config_type, content_type, config_show, config_name, config_key, config_value, sort) VALUES
  (30, 2, 3, 1, '网站Logo',   'websiteLogo',          '', 3),
  (31, 2, 3, 1, '网站图标',   'websiteIcon',          '', 4),
  (32, 2, 1, 1, '网站描述',   'websiteDesc',          '', 5),
  (33, 2, 1, 1, '公安备案编号', 'websitePrnNo',        '', 22),
  (34, 2, 2, 0, '用户协议',   'websiteUserAgreement', '', 40),
  (35, 2, 2, 0, '隐私政策',   'websitePrivacyPolicy', '', 41),
  (36, 2, 2, 0, '关于我们',   'websiteAboutUs',       '', 42)
ON DUPLICATE KEY UPDATE content_type = VALUES(content_type), config_show = VALUES(config_show),
                        config_name = VALUES(config_name), sort = VALUES(sort);

-- 校验：网站类配置应有 12 项，且 content_type 均不为 NULL
SELECT COUNT(*) AS 网站配置项数, SUM(content_type IS NULL) AS 控件类型仍为NULL的
FROM sys_config WHERE config_type = 2;

SELECT config_key, config_name, content_type,
       IF(config_value = '' OR config_value IS NULL, '(待填写)', '已填') AS 值
FROM sys_config WHERE config_type = 2 ORDER BY sort;
