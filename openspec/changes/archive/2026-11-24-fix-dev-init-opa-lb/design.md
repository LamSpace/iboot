# Design

## Context

OPA 双副本与 `opa-lb` 由 compose 定义；`dev-init.sh` 显式列举需要启动的中间件服务，`opa-lb` 不是 `opa` 的依赖，
因此不会因 `opa` 被拉起而自动启动。CI 每次都从空环境开始，所以该遗漏只在 CI 暴露；本地此前手动启动过 LB，
掩盖了问题。

## Decisions

- 把 `opa-lb` 加入 `dev-init.sh` 的 `infra` 列表，与 `opa` 同批 `docker compose up -d`。
- 失败日志步骤不再调用需要完整环境变量的 `docker compose ps/logs`，改为 `docker ps` 与
  `docker logs --tail 100`，并对每个后端日志文件只输出末尾 20KB，避免单个超大行导致日志不可读。
- 不改变 OPA 的失败关闭、重试或缓存语义；本修复只保证 LB 在干净启动路径中存在。

## Risks / Trade-offs

- [额外启动一个 nginx 容器] → 资源开销很小；这是 OPA 双副本方案的必要组件。
- [失败日志仍可能不完整] → 限制为末尾 20KB，优先保证可读与可上传；完整日志仍可从 runner 临时目录获取。

## Migration Plan

1. 合并修复后重新触发 `full-regress`，确认启动阶段不再超时。
2. 回滚：从 `infra` 列表移除 `opa-lb` 会恢复原先的单点 LB 缺失，不建议。

## Open Questions

无。
