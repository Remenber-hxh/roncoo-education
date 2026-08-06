-- 启用本地磁盘存储（二开新增）
--
-- 背景：roncoo 只提供 MinIO 和阿里云 OSS 两种上传实现，
--       StoragePlatformEnum 里的 LOCAL(1) 是被注释掉的。
--       内网自建不想再多维护一个对象存储服务，因此：
--         1. 启用枚举 LOCAL(1, "本地存储", "local%", "local")
--         2. 新增 LocalUploadImpl（@Component("local")）落盘到本地目录
--         3. 新增 LocalFileController 提供 /system/images/** 读取（网关对 /images 免鉴权）
--
-- 执行：mysql -uroncoo -p roncoo_education < db/seed_local_storage.sql

-- 切到本地存储
UPDATE sys_config SET config_value = '1' WHERE config_key = 'storagePlatform';

-- 本地存储的两个参数
-- localStoragePath   落盘根目录，下面会自动建 public/ 和 private/ 子目录
-- localStorageDomain 对外访问前缀，走网关 7700 转发到 system 服务
INSERT INTO sys_config (id, config_type, config_name, config_key, config_value, sort) VALUES
  (24, 4, '本地存储目录',   'localStoragePath',   'D:/视频培训/uploads',                 2),
  (25, 4, '本地存储访问地址', 'localStorageDomain', 'http://localhost:7700/system/images', 3)
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value);

-- 清理上一次 MinIO 失败上传留下的空 URL 记录（就是后台里那张裂图）
DELETE FROM resource WHERE resource_url IS NULL OR resource_url = '';

-- 校验
SELECT config_key, config_value FROM sys_config
WHERE config_key IN ('storagePlatform', 'localStoragePath', 'localStorageDomain');
SELECT COUNT(*) AS 剩余资源数 FROM resource;
