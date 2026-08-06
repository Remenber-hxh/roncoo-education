# 内部培训平台 - 后端服务

公司内部员工培训学习平台的后端，基于 Spring Cloud Alibaba 的微服务架构。

### 技术栈

Spring Boot 3.5 + Spring Cloud Alibaba 2025 + MySQL 8 + Redis + MyBatis + Druid，JDK 17。

### 服务与端口

| 模块 | 端口 | 说明 |
| --- | --- | --- |
| roncoo-education-gateway | 7700 | 网关，统一入口、鉴权、接口级权限拦截 |
| roncoo-education-service-system | 7710 | 系统服务：登录、菜单、角色、系统配置 |
| roncoo-education-service-user | 7720 | 用户服务：员工账号、学习记录 |
| roncoo-education-service-course | 7730 | 课程服务：分类、课程、章节，以及考试模块 |

前端另有两个仓库：管理后台（Vue3，9528）、员工门户（Nuxt3，3000）。

### 本地运行

见 [LOCAL_RUN.md](LOCAL_RUN.md)。本地已绕过 Nacos / Seata，配置直接落在 `application-dev.properties`。

### 数据库

`db/` 目录下：

| 文件 | 说明 |
| --- | --- |
| schema_rebuild.sql | 全部建表语句（由 `gen_schema.js` 从 Mapper XML 反向生成） |
| seed_admin.sql | 管理员账号与 RSA 登录密钥 |
| seed_config.sql | 站点配置 |
| seed_menu.sql / seed_menu_exam.sql | 后台菜单与权限 |
| seed_demo.sql / seed_homepage.sql | 演示课程与门户首页数据 |
| exam_schema.sql | 考试模块 5 张表 |
| update_admin_account.sql | 管理员账号变更 |

### 考试模块

课程服务下的 `exam` 包，为二开新增：题库、试卷与组卷规则、课程指派、随机抽题、自动评分、补考。设计文档见项目根目录的《考试补考模块设计（二开）》。

### 开源声明

本项目基于开源项目 roncoo-education 二次开发，遵循 AGPL v3 许可证。
