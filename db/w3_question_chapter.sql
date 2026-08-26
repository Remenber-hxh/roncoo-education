-- W3：题库支持绑定到课程章节
--
-- 现状：exam_question 只能绑到 course_id 和 category_id，
-- 做不到「第三章配套第三章的题」。要做章节测验、按章节抽题，
-- 必须有 chapter_id。
--
-- 为什么绑章(course_chapter)而不是绑课时(course_chapter_period)：
--   课时是最小播放单元，一章往往有好几节，题目通常是覆盖整章知识点的，
--   绑到课时会让出题人被迫把题拆得过细。绑章更贴合实际出题习惯。
--   将来若确实需要按课时出题，再加 period_id 不影响现有数据。
--
-- chapter_id 允许为空：不属于任何章节的题（比如通用安全常识）
-- 仍然可以只挂在课程或分类下。
--
-- 执行：mysql -uroncoo -p roncoo_education < db/w3_question_chapter.sql

SET @exists := (SELECT COUNT(*) FROM information_schema.columns
                WHERE table_schema = DATABASE() AND table_name = 'exam_question' AND column_name = 'chapter_id');
SET @sql := IF(@exists = 0,
  'ALTER TABLE exam_question ADD COLUMN chapter_id BIGINT COMMENT ''章节ID，关联 course_chapter，为空表示不限章节''',
  'SELECT ''chapter_id 已存在，跳过''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 按课程+章节抽题是组卷的高频查询，加联合索引
SET @idx := (SELECT COUNT(*) FROM information_schema.statistics
             WHERE table_schema = DATABASE() AND table_name = 'exam_question' AND index_name = 'idx_course_chapter');
SET @sql2 := IF(@idx = 0,
  'ALTER TABLE exam_question ADD KEY idx_course_chapter (course_id, chapter_id)',
  'SELECT ''idx_course_chapter 已存在，跳过''');
PREPARE stmt2 FROM @sql2; EXECUTE stmt2; DEALLOCATE PREPARE stmt2;

-- 组卷规则也要能按章节限定，否则「第三章测验」没法自动组卷
SET @e3 := (SELECT COUNT(*) FROM information_schema.columns
            WHERE table_schema = DATABASE() AND table_name = 'exam_paper_rule' AND column_name = 'chapter_id');
SET @sql3 := IF(@e3 = 0,
  'ALTER TABLE exam_paper_rule ADD COLUMN chapter_id BIGINT COMMENT ''限定章节，为空表示不限''',
  'SELECT ''exam_paper_rule.chapter_id 已存在，跳过''');
PREPARE stmt3 FROM @sql3; EXECUTE stmt3; DEALLOCATE PREPARE stmt3;

-- 校验
SELECT COLUMN_NAME, COLUMN_TYPE, COLUMN_COMMENT FROM information_schema.columns
WHERE table_schema = DATABASE() AND table_name IN ('exam_question','exam_paper_rule') AND column_name = 'chapter_id';
