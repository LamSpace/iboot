# Design

## Context

SVID 由 SPIRE 以 30 天 TTL 签发，本地开发没有热加载，刷新写入文件或命名卷后仍需重启进程。
现有两个手动入口分别覆盖宿主机开发（`spire-svid-sync.sh --refresh`）和 Compose 全量模式
（`docker-svid-refresh.sh`），但没有任何周期任务触发它们。

## Goals / Non-Goals

**Goals:**

- 每日检查十个服务 SVID 的最小剩余有效期，低于阈值（默认 7 天）才刷新，避免无谓重启。
- 调度能自动区分宿主机与 Compose SVID 栈，并复用既有刷新脚本。
- 失败可观测：日志、非零退出码与 Prometheus 文本指标。
- 安装/卸载/状态与既有 `tls-rotate-schedule.sh` 保持同一套操作习惯。

**Non-Goals:**

- 不实现 SVID 热加载或 SPIRE Agent 常驻 sidecar 网格。
- 不引入中心化跨主机调度；每台主机分别安装（A5 的编排只覆盖阶段顺序）。
- 不改变 SVID TTL、信任域或注册 selector。

## Decisions

### 1. 检查与刷新分离

`spire-svid-sync.sh --check` 只读证书并计算最小剩余秒数，退出码 0/1 供调度判断；
`svid-refresh-schedule.sh --run-now` 先检查，必要时才调用既有刷新入口。刷新后再次检查，
避免“脚本成功但证书缺失”被记为成功。

### 2. 目标自动识别

显式 `--target=host|compose` 优先；`auto` 下检测 `docker-svid-*-1` 容器（`docker ps -a`，
sidecar 退出后仍存在）。宿主机目标根据是否存在 `iboot-*-1.0.0.jar` 进程决定是否
`--restart host`；未运行时只刷新文件，下次启动生效。

### 3. 指标

指标写入 `${IBOOT_TLS_CERT_DIR}/runtime/svid-refresh.prom`，与现有运行期证书副本同目录，
由 A4 的 node-exporter 文本文件采集读取。字段：

- `iboot_svid_refresh_last_success_timestamp_seconds`
- `iboot_svid_refresh_last_exit_code`
- `iboot_svid_cert_min_remaining_seconds`

`last_success` 单独持久化到 state 目录，失败时保留上次成功时间，便于告警判断“长时间未成功”。

### 4. 调度器

复用 `tls-rotate-schedule.sh` 的 systemd/cron 模板与锁策略：默认每日 04:00、`Persistent=true`、
目录锁防止并发。默认不安装，首次部署显式 `--install`。
