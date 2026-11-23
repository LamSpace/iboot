# 自动调度证书轮换

## Why

B5.7 已经提供 `scripts/tls-rotate.sh` 的检查、显式重签与重启入口，但调度仍留给部署方手工配置 cron。
本地与演示环境因此仍依赖人记住证书到期时间；一旦遗忘，30 天门禁只会在回归时被动失败，
运行中的服务也不会在重签后自动切换证书。零信任边界中的“证书轮换自动调度”仍未闭环。

## What Changes

- 新增 `scripts/tls-rotate-schedule.sh`，提供 `--install`、`--uninstall`、`--status`、`--run-now`、`--print`。
- 调度器优先安装 systemd user timer（每日检查），不可用时回退到当前用户 crontab；安装幂等，只管理自己的单元/标记块。
- 定时任务先执行 `tls-rotate.sh --check`：检查通过则不修改任何文件；检查失败才执行 `tls-rotate.sh --rotate --restart <scope>`。
- 重启范围在安装时显式选择：`none`、`docker`、`host`；默认 `none`，避免无人值守时意外重建整栈。
- 运行日志写入 `logs/tls-rotate.log`；同一时刻只允许一个实例（`flock`），失败以非零退出并保留日志。
- 回归脚本用临时 `HOME`、假 `crontab` 与 `--print` 覆盖安装、幂等、卸载和调度内容，不触碰开发者真实定时任务。
- 更新 `docs/27`、`docker/README.md`、`ROADMAP.md`，并把调度检查加入 `scripts/regress/regress.sh`。

## Capabilities

### New Capabilities

无。

### Modified Capabilities

- `transport-security`：新增“证书轮换可周期调度”要求，把“可执行轮换入口”扩展为“可安装、可卸载、可观测的周期调度”，同时保留“普通启动不重签”。

## Impact

- 新增脚本：`scripts/tls-rotate-schedule.sh`。
- 新增回归：`scripts/regress/tls-rotation-schedule.sh`。
- 修改：`scripts/regress/regress.sh`、`docs/27-TLS 与零信任操作指南.md`、`docker/README.md`、`ROADMAP.md`。
- 不修改证书生成逻辑、CA、服务 TLS 配置或运行中的容器编排。
- 非目标：CA 轮换、跨主机调度、Kubernetes CronJob；本轮只覆盖当前主机上的叶子证书生命周期。
