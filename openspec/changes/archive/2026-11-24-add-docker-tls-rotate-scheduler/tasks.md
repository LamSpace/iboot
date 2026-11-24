# Tasks

- [x] 1. 新增 `scripts/tls-rotate.sh --restart api`，通过 Docker API 仅重启使用证书的服务容器；verification: `bash -n` 与回归脚本通过。
- [x] 2. 新增 `tls-rotate` compose profile 与容器入口，支持每日检查与 `TLS_ROTATE_RUN_ONCE=1` 一次性验证；verification: 一次性容器输出「检查通过」。
- [x] 3. `tls-rotate-schedule.sh` 支持 `--scheduler docker` 的 install/uninstall/status/print；verification: `--print --scheduler docker` 输出配置。
- [x] 4. 新增 `scripts/regress/tls-rotate-scheduler.sh` 并接入 `regress.sh`；verification: 本地 3 项通过。
- [x] 5. 更新 README、docker/README、scripts/README、docs/27；verification: 文档与脚本行为一致。

## 未执行项

- [ ] 常驻调度安装（`--install` / `--scheduler docker` 启动）：当前会话的自动模式权限分类器将宿主机或容器常驻调度判定为未授权持久化并拒绝执行。仓库侧能力已就绪，需维护者显式批准后安装。

## Workflow follow-up

- [ ] 归档 OpenSpec 变更。
- [ ] 新建功能分支，提交、推送、创建 PR；CI 通过后合并到 `master`。
