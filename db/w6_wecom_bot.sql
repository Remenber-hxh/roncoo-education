-- 企业微信群机器人
--
-- 公司没有正规域名和服务器，走不了企业微信自建应用那条路（自建应用要配可信域名，
-- 而可信域名必须是企微能访问到的地址）。群机器人是单向外发的：
-- 在群里添加机器人拿到一个 Webhook 地址，我们往腾讯的地址 POST 消息即可，
-- 不需要 CorpID、Secret，也不需要对外暴露任何地址。
--
-- 用途：把逾期催办推到群里并 @ 到人。站内消息要员工主动登录才看得见，
-- 而群通知会直接弹到手机上——这是目前唯一能主动触达员工的通道
-- （短信平台也没配）。
--
-- 获取方式：企业微信群 → 右上角 → 添加群机器人 → 新建 → 复制 Webhook 地址。
--
-- 执行：mysql -uroncoo -p roncoo_education < db/w6_wecom_bot.sql

INSERT INTO sys_config (id, config_type, content_type, config_show, sort, config_name, config_key, config_value, remark) VALUES
  (900011, 6, 1, 1, 11, '企微机器人开关', 'wecomBotEnable', '0',
   '1开启 0关闭。开启后催办会同时推送到企业微信群'),
  (900012, 6, 1, 1, 12, '企微机器人Webhook', 'wecomBotWebhook', '',
   '企业微信群 → 添加群机器人 → 复制 Webhook 地址。形如 https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=xxx')
ON DUPLICATE KEY UPDATE config_name = VALUES(config_name), remark = VALUES(remark),
                        config_type = VALUES(config_type), config_show = VALUES(config_show);

-- 校验
SELECT config_key, IF(config_value = '' OR config_value IS NULL, '(未配置)', LEFT(config_value, 30)) AS 值, remark
FROM sys_config WHERE config_key LIKE 'wecomBot%';
