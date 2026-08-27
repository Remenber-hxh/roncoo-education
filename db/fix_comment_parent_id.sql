-- 修正评论的父级ID：顶级评论应为 0，而不是 NULL
--
-- 新增评论的接口早期没有给 comment_id 赋值，落库就是 NULL。
-- 查询侧 CourseBiz.filter() 按 comment_id 逐层挑出回复，约定 0 表示顶级，
-- 遇到 NULL 时 item.getCommentId().equals(...) 直接抛空指针，
-- 整个评论列表接口返回「服务繁忙」——一条评论都看不到。
--
-- 代码两侧都已修：新增时补 0，查询时把 NULL 当 0 处理。
-- 这个脚本把存量数据一并归正，让数据本身也符合约定。
--
-- 执行：mysql -uroncoo -p roncoo_education < db/fix_comment_parent_id.sql

UPDATE user_course_comment SET comment_id = 0 WHERE comment_id IS NULL;

-- 校验：不应再有 NULL
SELECT COUNT(*) AS 仍为NULL的条数, IF(COUNT(*) = 0, '已归正', '仍有残留!!') AS 结论
FROM user_course_comment WHERE comment_id IS NULL;

SELECT id, course_id, comment_id, LEFT(comment_text, 20) AS 内容 FROM user_course_comment;
