# Design

## Context

`x-app-common` 通过 `env_file: ../.env` 把根 `.env` 注入容器。CI 不创建 `.env`，凭据全部由 step env 提供；
`docker compose up` 只启动中间件时不解析应用服务的 `env_file`，但 `docker compose config` 会解析全部服务并因文件缺失失败。
`middleware-auth.sh` 用 `docker compose config --format json` 做静态凭据断言，于 CI 得到空输出。

## Decisions

- 使用 Compose 的对象式 `env_file`：
  ```yaml
  env_file:
    - path: ../.env
      required: false
  ```
  `.env` 存在时行为不变；缺失时不再阻止 `config`，凭据仍由环境变量提供，`${VAR:?}` 守卫继续保证缺失时失败。
- 不在 CI 写入 `.env`：避免把整份 runner 环境写入工作区，也避免凭据落盘。

## Risks / Trade-offs

- [旧版 Compose 不支持 `required`] → 项目使用 Docker Compose v2；CI 与本地均为新版本，`docker compose config` 已验证可用。
- [隐藏 `.env` 缺失] → 关键凭据仍由 `${VAR:?}` 守卫；应用服务的非关键默认值有代码内默认值。

## Migration Plan

1. 合并后重新触发 `full-regress`，确认静态凭据检查通过。
2. 回滚：恢复 `env_file: ../.env`，同时需在 CI 创建 `.env`。

## Open Questions

无。
