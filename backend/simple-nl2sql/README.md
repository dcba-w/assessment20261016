# simple-nl2sql

backend 内的 NL2SQL 模块：自然语言转成只读 SQL，经安全闸后再查当前数据源。

## 能力边界

```
自然语言
  → OpenAI 兼容 Chat Completions（生成 SELECT）
  → JSQLParser 安全闸（只允许单条 SELECT、表白名单、强制 LIMIT）
  → 执行查询
  → 失败时按配置回喂模型修正一次（可关）
  → 再生成口语化回答
```

安全闸还会根据当前库表结构自动补条件：

- 表上有 `tenant_id`：注入当前租户（优先用登录态 `TenantContextHolder`）
- 表上有 `deleted`：注入 `deleted = 0`

模型看不到表白名单之外的表；表白名单里的表也必须真实存在于当前数据源。

## 配置项（`nl2sql.*`）

写在 `simple-server` 的 `application.yaml` / `application-dev.yaml`，或用环境变量覆盖。**不要把密钥提交到仓库。**

| 配置键 | 环境变量 | 说明 |
| --- | --- | --- |
| `nl2sql.base-url` | `NL2SQL_BASE_URL`（可回落到 `OPENAI_BASE_URL`） | OpenAI 兼容根地址，多数实现要带 `/v1` |
| `nl2sql.api-key` | `NL2SQL_API_KEY`（可回落到 `OPENAI_API_KEY`） | 必填。空值或占位 `sk-xxxx` 会拒绝查询 |
| `nl2sql.model` | `NL2SQL_MODEL`（可回落到 `OPENAI_MODEL`） | 模型 ID |
| `nl2sql.timeout-seconds` | `NL2SQL_TIMEOUT_SECONDS` | 调模型超时，默认 `60` |
| `nl2sql.max-rows` | `NL2SQL_MAX_ROWS` | 强制 LIMIT 上限，默认 `100` |
| `nl2sql.max-retries` | `NL2SQL_MAX_RETRIES` | SQL 失败后回喂模型的次数，默认 `1`；设 `0` 关闭自纠 |
| `nl2sql.query-timeout-seconds` | `NL2SQL_QUERY_TIMEOUT_SECONDS` | JDBC 查询超时，默认 `10` |
| `nl2sql.default-tenant-id` | `NL2SQL_DEFAULT_TENANT_ID` | 请求无租户上下文时兜底，默认 `1` |
| `nl2sql.allowed-tables` | 见下 | **表白名单**，支持 `*` 通配 |

表白名单 yaml：

```yaml
nl2sql:
  allowed-tables:
    - inspect_*      # 匹配 inspect_facility、inspect_record 等
    - system_users   # 精确表名仍然有效
    # - system_*     # 系统模块全部表（含 oauth、菜单等，按需打开）
    # - "*"          # 当前库全部表
```

数据源复用 `simple-server` 主库，不单独配置。

## 接口

登录后：`POST /admin-api/nl2sql/query`

```json
{ "question": "最近有多少条订单？" }
```

成功时 `code` 为 `0`，`data` 含 `answer`、`columns`、`rows`，不返回 SQL。
