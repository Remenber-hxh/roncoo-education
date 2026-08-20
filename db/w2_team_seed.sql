-- W2：班组字典补充
--
-- 依据《璟邑科技人员信息登记表》42 人的岗位职务分布确定。
-- W1 建表时按领导需求文件先放了 6 个组，比对实际人员后补 3 个：
--   技工组：5 人岗位只写「技工」，不属于强电/弱电/维修任一工种，单独成组
--   IT组  ：IT专员、IT主管 2 人，单开而不并入弱电组
--   总部  ：总经理/运营负责人/行政主管/财务专员/文员/兼职/仓管 共 7 人，
--           职能岗也要参加培训，故同样给班组
--
-- 客房维保组、消控值班当前无人对应，按要求保留备用，不删。
--
-- 归属对照（42 人）：
--   维修组 11 | 强电组 6 | 弱电组 6 | 总部 7 | 项目管理 5 | 技工组 5 | IT组 2
--
-- 执行：mysql -uroncoo -p roncoo_education < db/w2_team_seed.sql

INSERT INTO team (id, status_id, sort, team_name, remark) VALUES
  (7, 1, 7, '技工组', '岗位职务仅标注「技工」、未细分工种的人员'),
  (8, 1, 8, 'IT组',   'IT专员、IT主管'),
  (9, 1, 9, '总部',   '总经理、运营负责人、行政主管、财务专员、文员、仓管等职能岗')
ON DUPLICATE KEY UPDATE team_name = VALUES(team_name), remark = VALUES(remark),
                        sort = VALUES(sort), status_id = VALUES(status_id);

-- 校验
SELECT id, team_name, remark, status_id FROM team ORDER BY sort;
SELECT COUNT(*) AS 班组总数, IF(COUNT(*) = 9, '符合预期', '与预期9个不符') AS 结论 FROM team;
