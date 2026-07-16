-- 网站基础配置种子（修复 /system/api/sys/config/website 因 websitePrn 为 null 导致的 NPE，
-- 并向前端提供 rsaLoginPublicKey 所在的 website 配置集）
INSERT INTO sys_config (id, config_type, config_name, config_key, config_value) VALUES
 (10, 2, '网站名称', 'websiteName', '内部培训平台'),
 (11, 2, '网站域名', 'websiteDomain', 'localhost'),
 (12, 2, '公安备案号', 'websitePrn', ''),
 (13, 2, '网站ICP', 'websiteIcp', ''),
 (14, 2, '网站版权', 'websiteCopyright', '内部培训平台')
ON DUPLICATE KEY UPDATE config_value=VALUES(config_value);
