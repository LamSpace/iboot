# Proposal

## Why

`dev-init.sh` 的中间件启动列表包含 `opa` 但没有 `opa-lb`。在干净环境（CI）中 nginx 负载均衡器从未启动，
服务连接 `http://localhost:8181` 失败，OPA 失败关闭导致 `iboot-system` 健康检查返回 403，全栈回归在启动阶段失败。
该问题不影响已手动启动过 `opa-lb` 的本地环境，因此 PR CI 与本地回归未能暴露。

## What Changes

- `scripts/dev-init.sh` 的 `infra` 列表加入 `opa-lb`，保证干净启动时同时拉起两个 OPA 副本与 nginx LB。
- `.github/workflows/regress.yml` 的失败日志步骤改用 `docker ps`/`docker logs`，避免缺少 compose 插值变量时
  再次失败，并限制每个日志文件输出 20KB。
- 无规格行为变化；OPA 失败的既有语义保持不变。

## Capabilities

### New Capabilities

无。

### Modified Capabilities

无。该修复只纠正启动编排与 CI 诊断，不改变对外行为。

## Impact

- 脚本：`scripts/dev-init.sh`。
- CI：`.github/workflows/regress.yml` 的失败日志步骤。
- 运行时：干净环境启动时的 `opa-lb` 容器。
