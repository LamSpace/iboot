# 任务

## 1. 调度脚本

- [x] 1.1 新增 `scripts/tls-rotate-schedule.sh`：参数解析、`--print`、`--status`、`--install`、`--uninstall`、`--run-now`。
- [x] 1.2 实现 systemd user timer 安装/卸载与 cron 标记块回退，保证幂等且只管理本项目条目。
- [x] 1.3 实现目录锁、日志与“先检查、失败才重签”的运行语义；默认 `--restart none`。

## 2. 回归

- [x] 2.1 新增 `scripts/regress/tls-rotation-schedule.sh`：用临时 HOME、假 crontab/systemctl/tls-rotate 覆盖安装、幂等、卸载、check 通过/失败与锁。
- [x] 2.2 在 `scripts/regress/regress.sh` 接入新的调度检查。

## 3. 文档

- [x] 3.1 更新 `docs/27-TLS 与零信任操作指南.md` §23.2/§23.3，写入安装、状态、卸载与边界。
- [x] 3.2 更新 `docker/README.md` 与 `ROADMAP.md` 的完成状态。

## 4. 验证

- [x] 4.1 `bash scripts/regress/tls-rotation-schedule.sh` 通过。
- [x] 4.2 `scripts/tls-rotate.sh --check` 仍通过，`dev-init.sh` 启动路径未改变。
- [x] 4.3 运行完整本地回归 `bash scripts/regress/regress.sh`；仅剩 Docker 备份关闭导致的已知冒烟用例失败，与本变更无关。
