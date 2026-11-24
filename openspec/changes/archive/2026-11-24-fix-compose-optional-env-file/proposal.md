# Proposal

## Why

CI 的 `.env` 不存在，但 `x-app-common` 的 `env_file: ../.env` 仍是必填项。`docker compose config` 因此直接失败，
`middleware-auth.sh` 的静态凭据检查拿到空输出并报“初始化容器凭据注入不符合最小权限”。启动阶段只拉起中间件服务，
不解析应用服务的 `env_file`，所以该问题直到回归静态检查才暴露。

## What Changes

- `docker/docker-compose.yml` 的 `env_file` 改为对象形式并设置 `required: false`；`.env` 存在时仍完整加载，
  不存在时由环境变量注入，`docker compose config` 不再失败。
- 无规格行为变化；部署仍要求所有 `${VAR:?}` 守卫变量由 `.env` 或环境变量提供。

## Capabilities

### New Capabilities

无。

### Modified Capabilities

无。该修复只调整 compose 的环境文件可选性，不改变对外行为。

## Impact

- 配置：`docker/docker-compose.yml`。
- CI：`middleware-auth.sh` 的静态检查在无 `.env` 环境下恢复可用。
