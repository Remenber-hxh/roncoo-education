-- 图文课时（二开新增）
--
-- 背景：入职引导模块的 4 项内容（企业文化、员工手册、薪酬制度、入职资料填写）
--       在《培训模块清单》里标注的形式是「文本」和「文本+跳转链接」，
--       但平台的课时只支持「资源(10)」和「直播(20)」两种类型，
--       资源又只能是上传的文件。也就是说：无法直接在平台里写一篇图文内容。
--
--       另一条路是上传 Word/PDF 走文档预览，但本地存储没有预览服务
--       （getPreviewConfig 返回空，门户 JSON.parse 会直接抛异常），
--       且 Word 在手机上体验差、拿不到阅读进度、改内容要重新传文件。
--
-- 方案：新增课时类型「图文(30)」，内容用富文本直接存在课时上。
--
-- 执行：mysql -uroncoo -p roncoo_education < db/article_period.sql

-- 图文内容（富文本 HTML）。资源型和直播型课时该字段为空。
ALTER TABLE course_chapter_period
  ADD COLUMN content LONGTEXT COMMENT '图文课时的正文内容（富文本HTML），period_type=30 时使用';

-- 是否需要阅读后签署确认。员工手册与规章制度要求「含签署确认」。
ALTER TABLE course_chapter_period
  ADD COLUMN need_sign TINYINT DEFAULT 0 COMMENT '是否需要签署确认：0否 1是';

-- 阅读达标的最短停留秒数。防止秒滑到底就算学完。
ALTER TABLE course_chapter_period
  ADD COLUMN read_seconds INT DEFAULT 0 COMMENT '阅读达标所需最短停留秒数，0表示不限制';

-- 校验
SELECT COLUMN_NAME AS 新增字段, COLUMN_TYPE AS 类型, COLUMN_COMMENT AS 说明
  FROM information_schema.columns
 WHERE table_schema = DATABASE()
   AND table_name = 'course_chapter_period'
   AND COLUMN_NAME IN ('content', 'need_sign', 'read_seconds');
