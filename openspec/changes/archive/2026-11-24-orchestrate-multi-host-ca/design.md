# Design

## Context

单机已有 `tls-rotate-schedule.sh`、`svid-refresh-schedule.sh` 与 `tls-ca-rotate.sh`，三者都提供
`--status` 与显式操作入口。跨主机缺少的是“按主机分组、按阶段推进”的编排，而不是另一套证书逻辑。

## Goals / Non-Goals

**Goals:**

- 用一条命令打印/执行跨主机 runbook，保证 prepare 全部完成后才 activate。
- 调度安装与三类状态巡检可批量执行，失败可见。
- 默认不执行，避免误操作生产主机。

**Non-Goals:**

- 不实现中心化调度服务、配置下发或主机发现。
- 不替代单机 `tls-ca-rotate.sh` 的状态机与回滚。
- 不并行执行阶段；同阶段内逐台串行，任一失败即停止。

## Decisions

### 1. 默认 dry-run

所有动作默认只打印 `ssh` 命令；`--execute` 才真正执行。这样两主机 dry-run/checklist 可直接用于
变更评审，也避免脚本被误用于未授权主机。

### 2. 阶段分组而非一键全流程

`--stage` 一次只推进一个阶段，强制运维在 prepare 全部成功后显式执行 activate。
`--checklist` 输出完整三个阶段、调度安装与回滚提示，作为固化 runbook。

### 3. 远端命令复用单机脚本

SSH 命令只执行远端仓库中的既有脚本，不复制参数逻辑；`--remote-root` 处理不同主机路径差异，
`--ssh-opts` 允许关闭 BatchMode 或加跳板参数。
