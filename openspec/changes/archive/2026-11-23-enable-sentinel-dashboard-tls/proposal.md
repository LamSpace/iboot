# Sentinel Dashboard TLS 与登录

## Why

Sentinel Dashboard 是最后一个仍以明文 HTTP 暴露的可观测控制台，且本地开发默认关闭登录。
它与门户、Grafana、Kibana 等控制台的安全基线不一致，属于已登记边界。Dashboard 的规则入口可以
修改限流阈值，匿名可写比只读看板风险更高。

## What Changes

- 为 Sentinel Dashboard 生成 `sentinel-keystore.p12`（复用本地门户证书，补充 `sentinel-dashboard` SAN），
  通过 `server.ssl.*` 只提供 HTTPS。
- 本地与生产都开启登录：`auth.enabled=true`，账号口令来自 `SENTINEL_DASHBOARD_USER/PASSWORD`；
  基础 compose 提供本地默认值，生产覆盖文件继续要求显式非空值。
- Docker 内服务通过 `https://sentinel-dashboard:8858` 心跳；宿主机开发通过 `https://localhost:8858`，
  复用 JVM 本地 CA 信任库。
- 门户登记地址改为 HTTPS，并关闭跨源 iframe 内嵌（登录会话不能跨源 iframe 可靠复用），改为新窗口打开。
- 健康检查、`wait_http`、`observability-tls.sh` 增加 HTTPS 与登录检查。
- 更新 `docs/27`、`docs/23`、`docker/README.md`、`ROADMAP.md`。

## Capabilities

### New Capabilities

无。

### Modified Capabilities

- `observability-security`：新增“Sentinel Dashboard 只提供 HTTPS 并要求登录”要求。

## Impact

- 修改：`scripts/tls-init.sh`、`scripts/dev-init.sh`、`docker/docker-compose.yml`、`docker/docker-compose.prod.yml`、
  `scripts/regress/observability-tls.sh`、`docs/27`、`docs/23`、`docker/README.md`、`ROADMAP.md`。
- 新增：`iboot-db-init/src/main/resources/db/migration/V17__sentinel_dashboard_https.sql`。
- 不改变 Sentinel 规则存储（仍在 Nacos）与客户端命令端口 8719。
- 非目标：命令端口的绑定地址收敛；它由 Sentinel 自身实现决定，继续作为残余边界。
