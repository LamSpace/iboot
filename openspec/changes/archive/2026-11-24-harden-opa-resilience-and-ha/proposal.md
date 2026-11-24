# Proposal

## Why

OPA 已成为服务间授权的集中决策点，但当前只有一个实例、客户端只有超时没有重试或缓存，且每次内部请求都同步
调用 OPA。单实例故障会让全部服务间请求失败关闭；生产高可用与故障演练也尚未实现（`docs/27` §27.3、§29 #6）。

## What Changes

- OPA Java 客户端增加有界重试（仅连接失败/超时）与可选短 TTL、容量受限的决策缓存；任何最终失败仍失败关闭。
- Docker Compose 运行两个 OPA 副本，并用 nginx `opa-lb` 做健康检查与故障切换；服务统一连接 `opa-lb:8181`。
- 回归新增：瞬时失败重试、缓存命中、单副本故障仍可用、全部副本故障返回 403。
- 同步更新 `docs/27` §27/§29、`docs/23`、`docker/README.md` 与 `ROADMAP.md`。

## Capabilities

### New Capabilities

无。

### Modified Capabilities

- `service-authorization`：新增 OPA 客户端韧性与多副本高可用要求，保持失败关闭语义不变。

## Impact

- 代码：`iboot-service-authz` 的 `ServiceAuthorizationProperties`、`OpaAuthorizationClient` 及测试。
- 配置：`docker/docker-compose.yml`、新增 `docker/opa/nginx.conf`、`x-app-env` 的 OPA 地址、`dev-init.sh`。
- 回归：`scripts/regress/opa-authz.sh`、`regress.sh`。
- 文档：`docs/27`、`docs/23`、`docker/README.md`、`ROADMAP.md`。
