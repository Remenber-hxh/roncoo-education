-- 补齐菜单的接口权限（apis 字段）
--
-- 网关对非超管账号逐接口校验：把菜单的 apis 拼成一个 JSON 数组存进 Redis，
-- 再用 tk.contains(请求路径) 判断。而库里 91 条菜单的 apis 全是空的，
-- 于是除了 userId=1 走硬编码直通，任何账号登录后都是 403、一个接口都调不了。
-- 实测：新建账号绑「超级管理员」角色，登录直接跳 403。
--
-- 本脚本按「菜单 -> 该页面实际会调用的接口」补齐 apis。
-- 注意是子串匹配，必须存完整路径，存前缀不生效。
-- 共 211 个后台接口，由 scratchpad/scan_api.js 从 38 个控制器扫描得出。
--
-- 角色分配不在这里做：谁能看哪些菜单，在「系统管理 -> 角色管理」里配。
-- 本脚本只解决「配了也用不了」的问题。
--
-- 执行：mysql -uroncoo -p roncoo_education < db/w4_menu_apis.sql

-- apis 原本是 varchar(255)，只够存两三个路径。
-- 一个页面实际会调十几个接口（学习记录 583 字符、章节管理 528 字符），
-- 不扩容就会 Data too long 直接失败。改成 text。
ALTER TABLE sys_menu MODIFY COLUMN apis text COMMENT '该菜单授权的接口路径，逗号分隔';

-- 概况（3 个接口）
UPDATE sys_menu SET apis = '/system/admin/stat/vod,/user/admin/stat/data,/user/admin/stat/login' WHERE id = 100;

-- 课程分类（7 个接口）
UPDATE sys_menu SET apis = '/course/admin/category/delete,/course/admin/category/edit,/course/admin/category/list,/course/admin/category/page,/course/admin/category/save,/course/admin/category/sort,/course/admin/category/view' WHERE id = 201;

-- 课程列表（4 个接口）
UPDATE sys_menu SET apis = '/course/admin/course/delete,/course/admin/course/page,/course/admin/course/sort,/course/admin/course/view' WHERE id = 202;

-- 学习记录（6 个接口）
UPDATE sys_menu SET apis = '/course/admin/user/study/delete,/course/admin/user/study/edit,/course/admin/user/study/page,/course/admin/user/study/save,/course/admin/user/study/stat,/course/admin/user/study/view' WHERE id = 203;

-- 资源管理（10 个接口）
UPDATE sys_menu SET apis = '/course/admin/resource/batch/delete,/course/admin/resource/batch/edit,/course/admin/resource/delete,/course/admin/resource/edit,/course/admin/resource/page,/course/admin/resource/preview,/course/admin/resource/save,/course/admin/resource/sort,/course/admin/resource/view,/course/admin/resource/vod/config' WHERE id = 204;

-- 课程编辑（11 个接口）
UPDATE sys_menu SET apis = '/course/admin/category/list,/course/admin/course/edit,/course/admin/course/save,/course/admin/course/view,/system/admin/ai/write/completions,/system/admin/ai/write/create,/system/admin/upload/app,/system/admin/upload/doc,/system/admin/upload/pic,/system/admin/upload/video,/user/admin/lecturer/page' WHERE id = 205;

-- 章节管理（14 个接口）
UPDATE sys_menu SET apis = '/course/admin/course/chapter/delete,/course/admin/course/chapter/edit,/course/admin/course/chapter/list,/course/admin/course/chapter/page,/course/admin/course/chapter/period/delete,/course/admin/course/chapter/period/edit,/course/admin/course/chapter/period/list,/course/admin/course/chapter/period/page,/course/admin/course/chapter/period/save,/course/admin/course/chapter/period/sort,/course/admin/course/chapter/period/view,/course/admin/course/chapter/save,/course/admin/course/chapter/sort,/course/admin/course/chapter/view' WHERE id = 206;

-- 用户列表（16 个接口）
UPDATE sys_menu SET apis = '/user/admin/project/group/list,/user/admin/team/list,/user/admin/users/delete,/user/admin/users/edit,/user/admin/users/import,/user/admin/users/import/template,/user/admin/users/log/delete,/user/admin/users/log/edit,/user/admin/users/log/page,/user/admin/users/log/save,/user/admin/users/log/view,/user/admin/users/page,/user/admin/users/profile/edit,/user/admin/users/psw/reset,/user/admin/users/save,/user/admin/users/view' WHERE id = 301;

-- 讲师管理（7 个接口）
UPDATE sys_menu SET apis = '/system/admin/upload/pic,/user/admin/lecturer/delete,/user/admin/lecturer/edit,/user/admin/lecturer/page,/user/admin/lecturer/save,/user/admin/lecturer/sort,/user/admin/lecturer/view' WHERE id = 302;

-- 用户记录（16 个接口）
UPDATE sys_menu SET apis = '/course/admin/user/course/collect/delete,/course/admin/user/course/collect/edit,/course/admin/user/course/collect/page,/course/admin/user/course/collect/save,/course/admin/user/course/collect/view,/course/admin/user/course/comment/delete,/course/admin/user/course/comment/edit,/course/admin/user/course/comment/page,/course/admin/user/course/comment/save,/course/admin/user/course/comment/view,/course/admin/user/course/delete,/course/admin/user/course/edit,/course/admin/user/course/page,/course/admin/user/course/record,/course/admin/user/course/save,/course/admin/user/course/view' WHERE id = 303;

-- 用户日志（6 个接口）
UPDATE sys_menu SET apis = '/system/admin/sys/log/page,/user/admin/users/log/delete,/user/admin/users/log/edit,/user/admin/users/log/page,/user/admin/users/log/save,/user/admin/users/log/view' WHERE id = 304;

-- 班组管理（6 个接口）
UPDATE sys_menu SET apis = '/user/admin/team/delete,/user/admin/team/edit,/user/admin/team/list,/user/admin/team/page,/user/admin/team/save,/user/admin/team/view' WHERE id = 305;

-- 项目组管理（6 个接口）
UPDATE sys_menu SET apis = '/user/admin/project/group/delete,/user/admin/project/group/edit,/user/admin/project/group/list,/user/admin/project/group/page,/user/admin/project/group/save,/user/admin/project/group/view' WHERE id = 306;

-- 轮播管理（7 个接口）
UPDATE sys_menu SET apis = '/system/admin/upload/pic,/system/admin/website/carousel/delete,/system/admin/website/carousel/edit,/system/admin/website/carousel/page,/system/admin/website/carousel/save,/system/admin/website/carousel/sort,/system/admin/website/carousel/view' WHERE id = 401;

-- 导航管理（6 个接口）
UPDATE sys_menu SET apis = '/system/admin/website/nav/delete,/system/admin/website/nav/edit,/system/admin/website/nav/page,/system/admin/website/nav/save,/system/admin/website/nav/sort,/system/admin/website/nav/view' WHERE id = 402;

-- 友链管理（6 个接口）
UPDATE sys_menu SET apis = '/system/admin/website/link/delete,/system/admin/website/link/edit,/system/admin/website/link/page,/system/admin/website/link/save,/system/admin/website/link/sort,/system/admin/website/link/view' WHERE id = 403;

-- 专区管理（6 个接口）
UPDATE sys_menu SET apis = '/course/admin/zone/delete,/course/admin/zone/edit,/course/admin/zone/page,/course/admin/zone/save,/course/admin/zone/sort,/course/admin/zone/view' WHERE id = 404;

-- 专区课程（7 个接口）
UPDATE sys_menu SET apis = '/course/admin/course/page,/course/admin/zone/course/delete,/course/admin/zone/course/edit,/course/admin/zone/course/page,/course/admin/zone/course/save,/course/admin/zone/course/sort,/course/admin/zone/course/view' WHERE id = 406;

-- 菜单管理（8 个接口）
UPDATE sys_menu SET apis = '/system/admin/sys/menu/delete,/system/admin/sys/menu/edit,/system/admin/sys/menu/list,/system/admin/sys/menu/role/list,/system/admin/sys/menu/role/save,/system/admin/sys/menu/save,/system/admin/sys/menu/sort,/system/admin/sys/menu/view' WHERE id = 501;

-- 角色管理（9 个接口）
UPDATE sys_menu SET apis = '/system/admin/sys/menu/list,/system/admin/sys/role/delete,/system/admin/sys/role/edit,/system/admin/sys/role/page,/system/admin/sys/role/save,/system/admin/sys/role/sort,/system/admin/sys/role/user/list,/system/admin/sys/role/user/save,/system/admin/sys/role/view' WHERE id = 502;

-- 系统用户（9 个接口）
UPDATE sys_menu SET apis = '/system/admin/sys/role/page,/system/admin/sys/user/current,/system/admin/sys/user/delete,/system/admin/sys/user/edit,/system/admin/sys/user/page,/system/admin/sys/user/password,/system/admin/sys/user/save,/system/admin/sys/user/sort,/system/admin/sys/user/view' WHERE id = 503;

-- 参数配置（8 个接口）
UPDATE sys_menu SET apis = '/system/admin/sys/config/delete,/system/admin/sys/config/edit,/system/admin/sys/config/list,/system/admin/sys/config/page,/system/admin/sys/config/save,/system/admin/sys/config/video/config,/system/admin/sys/config/video/init,/system/admin/sys/config/view' WHERE id = 504;

-- 应用管理（7 个接口）
UPDATE sys_menu SET apis = '/system/admin/upload/app,/system/admin/website/app/delete,/system/admin/website/app/edit,/system/admin/website/app/page,/system/admin/website/app/save,/system/admin/website/app/sort,/system/admin/website/app/view' WHERE id = 505;

-- 操作日志（1 个接口）
UPDATE sys_menu SET apis = '/system/admin/sys/log/page' WHERE id = 506;

-- 题库管理（12 个接口）
UPDATE sys_menu SET apis = '/course/admin/category/list,/course/admin/course/chapter/list,/course/admin/course/page,/course/admin/exam/question/count/chapter,/course/admin/exam/question/delete,/course/admin/exam/question/export,/course/admin/exam/question/import,/course/admin/exam/question/import/template,/course/admin/exam/question/page,/course/admin/exam/question/save,/course/admin/exam/question/update,/course/admin/exam/question/view' WHERE id = 601;

-- 试卷管理（9 个接口）
UPDATE sys_menu SET apis = '/course/admin/category/list,/course/admin/course/chapter/list,/course/admin/course/page,/course/admin/exam/paper/delete,/course/admin/exam/paper/page,/course/admin/exam/paper/save,/course/admin/exam/paper/update,/course/admin/exam/paper/view,/course/admin/exam/question/count/chapter' WHERE id = 602;

-- 课程指派（7 个接口）
UPDATE sys_menu SET apis = '/course/admin/course/page,/course/admin/exam/assign/batch,/course/admin/exam/assign/delete,/course/admin/exam/assign/page,/user/admin/project/group/list,/user/admin/team/list,/user/admin/users/page' WHERE id = 603;

-- 考试记录（1 个接口）
UPDATE sys_menu SET apis = '/course/admin/exam/record/page' WHERE id = 604;

-- 学习统计（2 个接口）
UPDATE sys_menu SET apis = '/course/admin/stat/overview,/course/admin/stat/remind' WHERE id = 700;

-- 校验：所有可见菜单都应有 apis
SELECT menu_type AS 类型, COUNT(*) AS 总数, SUM(apis IS NULL OR apis = '') AS 仍为空
FROM sys_menu WHERE menu_type IN (1, 2) GROUP BY menu_type;

SELECT id, menu_name, LENGTH(apis) AS 字符数,
       ROUND((LENGTH(apis) - LENGTH(REPLACE(apis, ',', ''))) + 1) AS 接口数
FROM sys_menu WHERE menu_type = 2 ORDER BY id;
