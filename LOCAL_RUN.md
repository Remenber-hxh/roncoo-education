# 本地运行说明（二开）

> 本文件记录 roncoo-education 在本机的二开本地运行方式。roncoo 开源仓库未提供 SQL 与 Nacos 配置，本项目通过**从代码反向重建 SQL + 本地化配置**使其可运行。

## 一、依赖环境（本机已就绪）
- JDK 17：`C:\Program Files\Microsoft\jdk-17.0.19.10-hotspot`
- Maven 3.9.9：`D:\apache-maven-3.9.9`（阿里云镜像 `~/.m2/settings.xml`）
- MySQL 8：`D:\mysql\mysql-8.0.42-winx64`，库 `roncoo_education`，账号 `roncoo/roncoo123456`
- Redis：`D:\redis\redis-server.exe`（端口 6379）
- Node 22（nvm 并存）：`C:\Users\33793\AppData\Local\nvm\v22.23.1`（前端用）

## 二、二开改了什么
1. **数据库**：`db/gen_schema.js` 从 34 个 Mapper XML 反向生成 `db/schema_rebuild.sql`（34 张表）。
2. **种子数据**：`db/seed_admin.sql`（管理员 + RSA 登录密钥），由 scratchpad 的 `gen_seed.js` 生成。
3. **配置本地化**（`common-core` 的 `application-dev.properties` + `bootstrap-common.properties`）：
   - 数据源/Redis/MyBatis 落本地；Nacos 配置导入改 `optional:` 且关闭 nacos 注册；
   - `seata.enabled=false`（本地无 Seata 服务器，否则启动崩溃）。
4. **网关路由**：`roncoo-education-gateway/.../common/RouteConfig.java`（原路由在 Nacos，此处按端口重建）。

## 三、启动顺序
```powershell
# 0) 环境变量（每个新终端）
$env:JAVA_HOME="C:\Program Files\Microsoft\jdk-17.0.19.10-hotspot"
$env:PATH="$env:JAVA_HOME\bin;D:\apache-maven-3.9.9\bin;$env:PATH"

# 1) Redis
D:\redis\redis-server.exe --port 6379

# 2) MySQL（作为服务已在运行）；首次需导入表结构与种子
#   mysql -u roncoo -proncoo123456 roncoo_education < db\schema_rebuild.sql
#   mysql -u roncoo -proncoo123456 roncoo_education < db\seed_admin.sql

# 3) 构建（首次或改代码后）
cd D:\视频培训\roncoo-education
mvn -DskipTests install

# 4) 后端服务（各开一个终端 / 后台）
java -jar roncoo-education-service\roncoo-education-service-system\target\*.jar   # 7710
java -jar roncoo-education-gateway\target\*.jar                                    # 7700
# 可选：user 7720、course 7730 同理

# 5) 前端（管理后台）
$env:PATH="C:\Users\33793\AppData\Local\nvm\v22.23.1;$env:PATH"
cd D:\视频培训\roncoo-education-admin
npm run dev    # http://localhost:9528
```

## 四、登录
- 地址：http://localhost:9528
- 账号：`13800138000`　密码：`123456`　验证码：按页面显示输入

## 五、已知限制（roncoo 未开源初始化数据导致）
- 登录成功，但**后台菜单/权限为空**（`sys_menu`/`sys_role` 无初始数据）。
- 网关对 admin 接口按 Redis 中的 API 权限校验，缺初始化数据时多数业务接口会被拦。
- 重建表的索引/字段长度/默认值为推断值，非 roncoo 原版。
- 如需"完整可用"，需重建菜单/角色/权限/系统配置等初始化数据（工作量较大）。
