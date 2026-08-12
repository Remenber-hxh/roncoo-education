-- 删除商品/售卖模块
--
-- 内部培训平台不售卖课程，roncoo 骨架自带的电商能力（订单、支付、
-- 用户资金账户、课程定价、试看）全部移除。
--
-- 保留说明：
--   user_course 是**报名记录**，学习进度、学习统计都依赖它，必须保留；
--   只是语义从「购买记录」变为「报名记录」，buy_type 字段一并删除。
--
-- 执行：mysql -uroncoo -p roncoo_education < db/remove_commerce.sql

-- ============================================================
-- 一、删除商业表
-- ============================================================
DROP TABLE IF EXISTS order_pay;
DROP TABLE IF EXISTS order_info;
DROP TABLE IF EXISTS users_account_consume;
DROP TABLE IF EXISTS users_account;

-- ============================================================
-- 二、课程去掉价格与售卖字段
-- ============================================================
ALTER TABLE course
  DROP COLUMN is_free,
  DROP COLUMN ruling_price,
  DROP COLUMN course_price,
  DROP COLUMN count_buy;

-- 章节 / 课时的「试看」标记（只在付费课场景下有意义）
ALTER TABLE course_chapter DROP COLUMN is_free;
ALTER TABLE course_chapter_period DROP COLUMN is_free;

-- 报名记录去掉购买方式
ALTER TABLE user_course DROP COLUMN buy_type;

-- ============================================================
-- 三、删除订单管理菜单及其权限
-- ============================================================
DELETE r FROM sys_menu_role r
  JOIN sys_menu m ON m.id = r.menu_id
 WHERE m.path = '/common/order' OR m.permission LIKE 'order:%';
DELETE FROM sys_menu WHERE path = '/common/order' OR permission LIKE 'order:%';

-- ============================================================
-- 四、删除支付相关配置项（config_type=6 为支付设置）
-- ============================================================
DELETE FROM sys_config
 WHERE config_type = 6
    OR config_key LIKE 'wxPay%'
    OR config_key LIKE 'aliPay%'
    OR config_key IN ('subSellerId', 'subAppId', 'subMchId');

-- ============================================================
-- 校验
-- ============================================================
SELECT '残留商业表' AS 检查项,
       IFNULL(GROUP_CONCAT(table_name), '无 ✓') AS 结果
  FROM information_schema.tables
 WHERE table_schema = 'roncoo_education'
   AND table_name IN ('order_info', 'order_pay', 'users_account', 'users_account_consume')
UNION ALL
SELECT '残留价格字段',
       IFNULL(GROUP_CONCAT(CONCAT(table_name, '.', column_name)), '无 ✓')
  FROM information_schema.columns
 WHERE table_schema = 'roncoo_education'
   AND column_name IN ('is_free', 'course_price', 'ruling_price', 'count_buy', 'buy_type')
UNION ALL
SELECT '残留订单菜单/权限',
       IFNULL(GROUP_CONCAT(menu_name), '无 ✓')
  FROM sys_menu
 WHERE path = '/common/order' OR permission LIKE 'order:%'
UNION ALL
SELECT '残留支付配置',
       IFNULL(GROUP_CONCAT(config_key), '无 ✓')
  FROM sys_config
 WHERE config_type = 6 OR config_key LIKE 'wxPay%' OR config_key LIKE 'aliPay%';
