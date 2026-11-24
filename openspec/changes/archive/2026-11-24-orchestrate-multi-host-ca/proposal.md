# Proposal

## Why

CA 三阶段轮换要求“先在全部主机 prepare，再 activate，最后 finalize”，但仓库只提供单机脚本；
跨主机时运维需要手工逐台 SSH，容易把阶段顺序做错。调度安装与巡检同样缺少跨主机入口；
docs/28 将该项登记为 A5。

## What Changes

- 新增 `scripts/tls-ca-orchestrate.sh`：
  - `--checklist` 输出完整 runbook：预检、调度安装、prepare/activate/finalize、收尾巡检与回滚提示。
  - `--install-schedules` 逐台安装 `tls-rotate-schedule.sh` 与 `svid-refresh-schedule.sh`。
  - `--status` 逐台汇总轮换调度、SVID 调度与 CA 阶段状态。
  - `--stage prepare|activate|finalize` 在全部主机按顺序执行；任一主机失败即停止。
  - 默认 dry-run，仅 `--execute` 才通过 SSH 执行；支持 `--remote-root`、`--ssh-opts`、`--restart`。
- 新增回归 `scripts/regress/tls-ca-orchestrate.sh`（12k），用假 ssh 验证顺序、dry-run、失败停止与巡检。
- 更新 `docs/27` §23.3/§23.4/§28.4、`docs/28`、README、docs/README、ROADMAP、`scripts/README.md`；
  docs/28 移除 A5，行动项清零。

## Capabilities

### New Capabilities

无。

### Modified Capabilities

- `transport-security`：跨主机 CA 轮换必须按阶段分组执行，并提供调度巡检入口。

## Impact

- 脚本：新增 `scripts/tls-ca-orchestrate.sh`、`scripts/regress/tls-ca-orchestrate.sh`、`scripts/regress/regress.sh`。
- 运维：跨主机 SSH 编排；不改变单机轮换逻辑。
- 文档：`docs/27`、`docs/28`、`README.md`、`docs/README.md`、`ROADMAP.md`、`scripts/README.md`。
