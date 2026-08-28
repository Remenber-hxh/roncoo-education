-- 课程分类体系：六大培训模块
--
-- 来自领导需求文件里划定的六个板块。原来库里只有一条「入职培训」，
-- 而「按模块统计学习情况」「样板课程归到哪个模块」都依赖这套分类，
-- 缺了它课程只能堆在一起。
--
-- category_type=1 表示课程分类（2 是资源分类，两者共用一张表）；
-- parent_id=0 为顶级。这里只建一级，二级等实际课程铺开后再按需细分——
-- 现在就拍二级容易拍错，改起来还要动已归类的课程。
--
-- 保留原有的「入职培训」并归入「入职引导」之下，不直接删：
-- 已经有 4 道题挂在它上面（exam_question.category_id=1），
-- 删了那些题就会失去分类，冒烟测试卷也跟着抽不到题。
--
-- 执行：mysql -uroncoo -p roncoo_education < db/w3_course_category.sql

INSERT INTO category (id, status_id, sort, parent_id, category_type, category_name, remark) VALUES
  (101, 1, 1, 0, 1, '入职引导',     '新员工入职必修：公司介绍、规章制度、安全须知、保密协议'),
  (102, 1, 2, 0, 1, '作业安全',     '各工种作业安全规范与事故案例'),
  (103, 1, 3, 0, 1, '形象礼仪',     '仪容仪表、服务礼仪、沟通规范'),
  (104, 1, 4, 0, 1, '工作软件应用', '企业微信、工单系统等日常工具的使用'),
  (105, 1, 5, 0, 1, '班组技能SOP',  '各班组的标准作业流程与专业技能'),
  (106, 1, 6, 0, 1, '考证管理',     '低压电工证、消控证等证照的培训与复审')
ON DUPLICATE KEY UPDATE category_name = VALUES(category_name), remark = VALUES(remark),
                        sort = VALUES(sort), parent_id = VALUES(parent_id),
                        category_type = VALUES(category_type), status_id = VALUES(status_id);

-- 原有的「入职培训」挪到「入职引导」下面当二级，保住挂在它上面的题目
UPDATE category SET parent_id = 101, category_type = 1, sort = 1 WHERE id = 1;

-- 校验
SELECT id, parent_id, sort, category_name,
       CASE WHEN parent_id = 0 THEN '一级' ELSE '二级' END AS 层级
FROM category WHERE category_type = 1 ORDER BY IF(parent_id=0, id, parent_id), sort;

SELECT COUNT(*) AS 一级模块数, IF(COUNT(*) = 6, '符合预期', '与预期6个不符') AS 结论
FROM category WHERE category_type = 1 AND parent_id = 0;

-- 确认题目没有失去分类
SELECT COUNT(*) AS 仍挂在入职培训下的题 FROM exam_question WHERE category_id = 1;
