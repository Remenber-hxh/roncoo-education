-- 归正课程指派的完成状态
--
-- user_course_assign.finish_status 是冗余状态，原本只有「考试通过」一条路会写它
-- （AuthExamBiz 判卷时置 3），员工把课学完并不更新，于是后台「课程指派」列表里
-- 出现了进度 100% 却显示「未开始」的记录。
--
-- 代码侧已修：新增 AssignStatusBiz，在各处上报学习进度后重算这个状态。
-- 本脚本把存量数据一并按实际进度归正。
--
-- 判定与 AssignStatusBiz.refresh 保持一致：
--   已通过考试(3) 是终态，不动；
--   课程的已发布课时全部完成 -> 2 已学完；
--   有学习记录但没学完   -> 1 学习中；
--   没有任何学习记录     -> 0 未开始。
-- 一门课还没发布任何课时时不算学完——「所有课时都完成了」在逻辑上成立
-- 但毫无意义，会让完成率虚高。
--
-- 执行：mysql -uroncoo -p roncoo_education < db/w3_fix_assign_finish_status.sql

UPDATE user_course_assign a
SET a.finish_status = (
        SELECT CASE
                   WHEN p.cnt > 0 AND IFNULL(s.done, 0) >= p.cnt THEN 2
                   WHEN IFNULL(s.touched, 0) > 0                 THEN 1
                   ELSE 0
               END
        FROM (SELECT COUNT(*) AS cnt FROM course_chapter_period
               WHERE course_id = a.course_id AND status_id = 1) p
        LEFT JOIN (SELECT SUM(CASE WHEN progress >= 100 THEN 1 ELSE 0 END) AS done,
                          COUNT(*) AS touched
                     FROM user_study
                    WHERE user_id = a.user_id AND course_id = a.course_id) s ON 1 = 1
    ),
    a.finish_time = CASE WHEN a.finish_time IS NULL THEN NOW() ELSE a.finish_time END
WHERE IFNULL(a.finish_status, 0) <> 3;

-- 校验：指派表里的状态应与实际进度一致
SELECT a.user_id, a.course_id, a.finish_status AS 状态,
       p.cnt AS 课时数, IFNULL(s.done, 0) AS 已完成课时,
       CASE
           WHEN a.finish_status = 3 THEN '已通过考试(终态)'
           WHEN p.cnt > 0 AND IFNULL(s.done, 0) >= p.cnt AND a.finish_status = 2 THEN '一致'
           WHEN IFNULL(s.touched, 0) > 0 AND NOT (p.cnt > 0 AND IFNULL(s.done, 0) >= p.cnt)
                AND a.finish_status = 1 THEN '一致'
           WHEN IFNULL(s.touched, 0) = 0 AND a.finish_status = 0 THEN '一致'
           ELSE '不一致!!'
       END AS 结论
FROM user_course_assign a
LEFT JOIN (SELECT course_id, COUNT(*) AS cnt FROM course_chapter_period
            WHERE status_id = 1 GROUP BY course_id) p ON p.course_id = a.course_id
LEFT JOIN (SELECT user_id, course_id,
                  SUM(CASE WHEN progress >= 100 THEN 1 ELSE 0 END) AS done,
                  COUNT(*) AS touched
             FROM user_study GROUP BY user_id, course_id) s
       ON s.user_id = a.user_id AND s.course_id = a.course_id;
