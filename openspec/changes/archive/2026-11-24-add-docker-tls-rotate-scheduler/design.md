# Design

## Context

证书轮换脚本本身完整，缺口是“谁来定时触发”。宿主机 systemd/cron 安装会写用户级持久化任务；
容器化开发环境又不保证宿主机有 systemd user manager。需要一种不写宿主机持久化的可选调度方式。

## Decisions

- 使用 compose 的 `tls-rotate` profile，而不是默认启动：只有显式 `--scheduler docker --install`
  才启动常驻容器，避免影响默认栈。
- 容器镜像复用本地已有的 `maven:3.10.0-eclipse-temurin-25`（含 bash、openssl、curl），
  不引入新的镜像构建与 Docker Hub 拉取依赖。
- 容器内通过 Docker Engine API（`curl --unix-socket`）重启服务，而不是 `docker compose` CLI；
  只匹配 `iboot-<service>` 与 `*-exporter`，避免重启数据库等基础设施。
- `TLS_ROTATE_RUN_ONCE=1` 只执行一次检查后退出，用于回归；回归不启动常驻容器。
- 常驻容器需要挂载 Docker socket 与仓库根目录，权限较高；文档标注为本地/容器化开发专用，
  安装需维护者显式批准。

## Risks / Trade-offs

- [Docker socket 权限] → 仅 opt-in profile，不进入默认启动；文档明确边界。
- [重启范围过大] → 通过 `TLS_ROTATE_RESTART_FILTER` 限定容器名正则；默认只覆盖业务服务与 exporter。
- [容器内缺少依赖] → 复用已缓存镜像；一次性回归覆盖。
- [常驻安装被权限策略拒绝] → 不绕过；仓库能力保留，安装动作交由维护者批准。

## Migration Plan

1. 合并后默认行为不变；不启动 `tls-rotate` profile。
2. 需要时执行 `scripts/tls-rotate-schedule.sh --install --scheduler docker --restart=api`。
3. 回滚：`--uninstall --scheduler docker`，或直接 `docker compose --profile tls-rotate rm -sf tls-rotate`。

## Open Questions

无。
