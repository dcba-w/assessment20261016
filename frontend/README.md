# Simple Boot Web

Simple Boot 管理端（Vue3 + Element Plus），配合 `simple-boot-new` 后端使用。

UPSTREAM-COPYRIGHT: 前端源自 ruoyi-vue-pro 配套管理端（MIT），详见后端 NOTICE。

## 环境要求

- Node.js >= 20.19.0（与 `package.json` 的 `engines` 一致）
- pnpm >= 8.6.0

## 快速开始

```bash
pnpm i
pnpm dev
```

本地默认读取 `.env` / `.env.local`。登录账号与密码不要配置到前端环境变量或页面中。

| 命令 | 说明 |
|------|------|
| `pnpm dev` | 本地开发（`env.local`） |
| `pnpm build:local` | 本地构建 |
| `pnpm ts:check` | TypeScript 检查 |

## 技术栈

Vue 3、Vite、Element Plus、TypeScript、Pinia、Vue Router、UnoCSS。
