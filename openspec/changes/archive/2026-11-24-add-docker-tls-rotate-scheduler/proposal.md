# Proposal

## Why

`scripts/tls-rotate-schedule.sh` 已支持 systemd/cron 安装，但实际安装会写宿主机持久化任务。
当前会话的自动模式权限分类器将这类操作判定为未授权持久化并拒绝；容器化的本地开发环境也需要
一个不依赖宿主机 cron/systemd 的调度选项。

## What Changes

- `scripts/tls-rotate.sh` 新增 `--restart api`：通过 Docker Engine API 仅重启使用证书的业务容器与 exporter。
- 新增 `tls-rotate` compose profile 与 `docker/tls-rotate/entrypoint.sh`：容器内按日检查，失败才重签；
  支持 `TLS_ROTATE_RUN_ONCE=1` 一次性验证。
- `scripts/tls-rotate-schedule.sh` 支持 `--scheduler docker` 的 install/uninstall/status/print。
- 新增 `scripts/regress/tls-rotate-scheduler.sh`：只运行一次性容器检查，不启动常驻调度。
- 更新 README、`docker/README.md`、`scripts/README.md`、`docs/27`。

## Capabilities

### New Capabilities

无。

### Modified Capabilities

无。调度属于运维入口，不改变服务身份或授权语义。

## Impact

- 脚本：`tls-rotate.sh`、`tls-rotate-schedule.sh`。
- 编排：`docker/docker-compose.yml` 的 `tls-rotate` profile。
- 回归：`scripts/regress/regress.sh` 新增 12c。
- 文档：README、docker/README、scripts/README、docs/27。

## 说明

常驻调度安装未在本会话执行：自动模式权限分类器拒绝启动宿主机或容器常驻任务。仓库侧能力
已实现并可用一次性模式回归，安装动作需维护者显式批准。
