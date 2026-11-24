# Proposal

## Why

宿主机与 Compose 全量模式都已切换到 SPIRE SVID，但 SVID 只有 30 天有效期，刷新一直是显式手工动作
（`spire-svid-sync.sh --refresh` 或 `docker-svid-refresh.sh`）。忘记刷新会让服务在证书到期后互相拒绝，
与“到期前自动 refresh”的零信任运维目标不一致；docs/28 将该项登记为 A1。

## What Changes

- `spire-svid-sync.sh` 新增 `--check`（按阈值判断是否需要刷新，输出 `refresh_needed` 与剩余秒数）
  和 `--threshold-days`；`--status` 增加同样字段。
- 新增 `scripts/svid-refresh-schedule.sh`：每日检查，低于阈值或 SVID 缺失时按目标刷新。
  - 宿主机目标：`spire-svid-sync.sh --refresh --restart host`；服务未运行时只刷新文件。
  - Compose 目标：委托 `docker-svid-refresh.sh`，沿用 SVID 覆盖层与滚动重启。
  - 支持 systemd user timer 与 cron 安装、幂等、卸载、`--status` 巡检。
- 结果写 `logs/svid-refresh.log`，并写 Prometheus 文本文件指标
  `docker/certs/runtime/svid-refresh.prom`（供 A4 采集与告警）。
- 新增回归 `scripts/regress/svid-refresh-schedule.sh`，纳入 `regress.sh` 12i；
  `spire-svid-services.sh`（12g）增加真实 `--check` 断言。
- 同步更新 `docs/27` §28、`docs/28`、README、docs/README、ROADMAP 与 `scripts/README.md`；
  docs/28 移除 A1。

## Capabilities

### New Capabilities

无。

### Modified Capabilities

- `workload-identity`：SVID 必须由周期调度在到期前刷新，并暴露可巡检状态与指标。

## Impact

- 脚本：`scripts/spire-svid-sync.sh`、新增 `scripts/svid-refresh-schedule.sh`、
  `scripts/regress/svid-refresh-schedule.sh`、`scripts/regress/spire-svid-services.sh`、
  `scripts/regress/regress.sh`。
- 运行期：宿主机开发与 Compose SVID 栈的定时刷新；新增日志与指标文件。
- 文档：`docs/27`、`docs/28`、`README.md`、`docs/README.md`、`ROADMAP.md`、`scripts/README.md`。
