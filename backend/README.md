# Simple Boot

基于 Spring Boot 的后端基础脚手架。

包名 `cn.simple.boot`，Maven 模块名 `simple-*`。开源来源见 [NOTICE](NOTICE)。

UPSTREAM-COPYRIGHT: 基于 ruoyi-vue-pro（MIT）精简，详见 NOTICE 与 LICENSE。

## 模块

| 模块 | 说明 |
|------|------|
| `simple-dependencies` | 依赖 BOM |
| `simple-framework` | 框架组件 |
| `simple-server` | 启动入口 |
| `simple-module-system` | 用户、角色、菜单、租户等 |
| `simple-module-infra` | 文件、配置、定时任务、代码生成等 |
| `simple-nl2sql` | 自然语言转只读 SQL（安全闸 + 表白名单） |

## 本地启动

1. 准备 JDK 17+、MySQL、Redis。
2. 一键初始化数据库（会建库并导入 schema、Quartz、菜单精简、种子清洗）：

```powershell
cd sql\mysql
.\init-all.ps1 -Database simple-project
```

手动导入时执行 `sql/mysql/init.sql` 即可。

3. 使用 `simple-server/src/main/resources/application-dev.yaml`（`application.yaml` 已激活 `dev`）。默认库 `simple-project`，账号 `root` / `root`。本机不同时用环境变量 `MYSQL_HOST`、`MYSQL_PORT`、`MYSQL_DATABASE`、`MYSQL_USERNAME`、`MYSQL_PASSWORD` 覆盖。
4. 打包并启动：

```bash
mvn -pl simple-server -am clean package -DskipTests
java -jar simple-server/target/simple-server.jar --spring.profiles.active=dev
```

默认登录：租户 `Simple`，账号 `admin` / `admin123`。文件上传默认走本地目录 `./data/file`。

## AI 调用

`simple.ai.openai` 为 OpenAI 兼容协议的最小封装。Key 使用环境变量 `OPENAI_API_KEY`，不要写进仓库。未配置时 `OpenAiCompatibleClient.chat()` 返回空字符串。

NL2SQL 接口：`POST /admin-api/nl2sql/query`（需登录）。配置项 `nl2sql.*`，Key 优先 `NL2SQL_API_KEY`，未设时回落 `OPENAI_API_KEY`。详见 [simple-nl2sql/README.md](simple-nl2sql/README.md)。

## 相关工程

- 管理端：`../simple-boot-web`
- App：`../simple-boot-uniapp`
- 总说明：`../README.md`
