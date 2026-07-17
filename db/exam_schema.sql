-- 考试与补考模块（二开新增，Phase A）
-- 设计文档：D:\视频培训\考试补考模块设计（二开）.md

-- 1) 题库
CREATE TABLE IF NOT EXISTS `exam_question` (
  `id` bigint NOT NULL,
  `gmt_create` datetime DEFAULT CURRENT_TIMESTAMP,
  `gmt_modified` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `status_id` tinyint DEFAULT 1,
  `sort` int DEFAULT 0,
  `category_id` bigint DEFAULT NULL COMMENT '所属课程分类/模块',
  `course_id` bigint DEFAULT NULL COMMENT '关联课程(可空=模块通用题)',
  `question_type` tinyint DEFAULT 1 COMMENT '1单选 2多选 3判断',
  `question_title` text COMMENT '题干',
  `options_json` text COMMENT '选项JSON [{"key":"A","value":"..."},...]',
  `correct_answer` varchar(32) DEFAULT NULL COMMENT '正确答案 如 A 或 A,C',
  `analysis` text COMMENT '错题解析',
  `difficulty` tinyint DEFAULT 1 COMMENT '1易 2中 3难',
  PRIMARY KEY (`id`),
  KEY `idx_category` (`category_id`), KEY `idx_course` (`course_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考试题库';

-- 2) 试卷定义
CREATE TABLE IF NOT EXISTS `exam_paper` (
  `id` bigint NOT NULL,
  `gmt_create` datetime DEFAULT CURRENT_TIMESTAMP,
  `gmt_modified` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `status_id` tinyint DEFAULT 1,
  `sort` int DEFAULT 0,
  `course_id` bigint DEFAULT NULL COMMENT '关联课程',
  `paper_name` varchar(255) DEFAULT NULL,
  `total_score` int DEFAULT 100,
  `pass_score` int DEFAULT 80,
  `duration_minutes` int DEFAULT 60,
  `retake_limit` int DEFAULT 1 COMMENT '允许补考次数',
  `need_finish_course` tinyint(1) DEFAULT 0 COMMENT '是否要求学完课程(Phase A 默认不校验)',
  PRIMARY KEY (`id`), KEY `idx_course` (`course_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='试卷定义';

-- 3) 组卷规则
CREATE TABLE IF NOT EXISTS `exam_paper_rule` (
  `id` bigint NOT NULL,
  `gmt_create` datetime DEFAULT CURRENT_TIMESTAMP,
  `gmt_modified` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `status_id` tinyint DEFAULT 1,
  `sort` int DEFAULT 0,
  `paper_id` bigint NOT NULL,
  `category_id` bigint DEFAULT NULL COMMENT '从哪个模块抽(空=全库)',
  `question_type` tinyint DEFAULT NULL COMMENT '抽哪种题型(空=不限)',
  `question_count` int DEFAULT 10,
  `score_per_question` int DEFAULT 4,
  PRIMARY KEY (`id`), KEY `idx_paper` (`paper_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='组卷规则';

-- 4) 考试记录
CREATE TABLE IF NOT EXISTS `exam_record` (
  `id` bigint NOT NULL,
  `gmt_create` datetime DEFAULT CURRENT_TIMESTAMP,
  `gmt_modified` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `status_id` tinyint DEFAULT 1,
  `user_id` bigint NOT NULL,
  `paper_id` bigint NOT NULL,
  `course_id` bigint DEFAULT NULL,
  `attempt_no` int DEFAULT 1 COMMENT '第几次: 1正考 2补考...',
  `start_time` datetime DEFAULT NULL,
  `submit_time` datetime DEFAULT NULL,
  `score` int DEFAULT NULL,
  `is_pass` tinyint(1) DEFAULT NULL,
  `exam_status` tinyint DEFAULT 1 COMMENT '1进行中 2已交卷 3超时交卷',
  `questions_json` longtext COMMENT '题目快照(含正确答案,不下发)',
  `answers_json` longtext COMMENT '作答快照',
  PRIMARY KEY (`id`),
  KEY `idx_user_paper` (`user_id`,`paper_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考试记录';

-- 5) 课程指派
CREATE TABLE IF NOT EXISTS `user_course_assign` (
  `id` bigint NOT NULL,
  `gmt_create` datetime DEFAULT CURRENT_TIMESTAMP,
  `gmt_modified` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `status_id` tinyint DEFAULT 1,
  `user_id` bigint NOT NULL,
  `course_id` bigint NOT NULL,
  `assign_type` tinyint DEFAULT 1 COMMENT '1必修 2选修',
  `deadline` date DEFAULT NULL,
  `finish_status` tinyint DEFAULT 0 COMMENT '0未开始 1学习中 2已学完 3已通过考试',
  `finish_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_course` (`user_id`,`course_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程指派';
