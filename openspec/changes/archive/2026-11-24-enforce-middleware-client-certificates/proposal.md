# Proposal

## Why

MySQL 与 Redis 目前只做服务端 TLS + 账号/ACL 认证，服务端无法从传输层确认连接方是哪个工作负载；
持有口令的任意进程都可以冒充服务。`docs/27` §29 #7 将“中间件不是客户端证书 mTLS”登记为残余边界。

## What Changes

- MySQL：为八个数据服务生成 PKCS12 客户端 keystore；服务账号 `REQUIRE SUBJECT '/CN=iboot-<service>'`；
  Connector/J 使用每服务 keystore；迁移与 exporter 账号保持无客户端证书。
- Redis：开启 `--tls-auth-clients yes`；Spring SSL bundle 使用每服务证书/私钥；redis-exporter 与回归命令
  使用客户端证书。
- **边界**：Silo（MinIO 兼容）与 Nacos HTTP API 不支持按工作负载的客户端证书认证；保持“服务端 TLS +
  账号/令牌”并登记为显式例外，不宣称这两条链路是 mTLS。
- 回归新增：MySQL/Redis 不带客户端证书被拒绝，正确证书可连接；错误服务证书不可冒充。
- 同步更新 `docs/27` §13/§14/§29、`docs/08`、`docker/README.md` 与 `ROADMAP.md`。

## Capabilities

### New Capabilities

无。

### Modified Capabilities

- `transport-security`：新增“MySQL 与 Redis 客户端证书 mTLS”要求，并把 Silo/Nacos HTTP 登记为显式例外。

## Impact

- 证书：`scripts/tls-init.sh` 生成每服务 PKCS12 keystore。
- 配置：`iboot-tls.yml`（三份）、`docker/docker-compose.yml`、`scripts/dev-init.sh`、Redis/exporter 证书。
- 账号：`scripts/mysql-users-init.sh` 为八个服务账号追加 `REQUIRE SUBJECT`。
- 回归：`internal-tls.sh`、`middleware-auth.sh`、`middleware-least-privilege.sh`、`exporter-mtls.sh`。
- 文档：`docs/27`、`docs/08`、`docker/README.md`、`ROADMAP.md`。
