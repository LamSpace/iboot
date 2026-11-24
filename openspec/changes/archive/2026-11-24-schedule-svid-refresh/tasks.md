# Tasks

## 1. 检查入口

- [x] 1.1 `spire-svid-sync.sh` 增加 `--check`/`--threshold-days`，`--status` 输出 `refresh_needed` 与剩余秒数；verification: 有效期正常时退出 0，缺失/临期时退出 1。
- [x] 1.2 保持既有 `--setup`/`--refresh`/`--status` 行为不变；verification: 现有 12g/12h 回归通过。

## 2. 调度脚本

- [x] 2.1 新增 `svid-refresh-schedule.sh`：检查、目标识别、刷新、日志、锁与指标；verification: 假同步脚本下检查/刷新/失败路径正确。
- [x] 2.2 支持 systemd user timer 与 cron 的安装、幂等、卸载与 `--status`；verification: 临时 HOME 下安装/重复安装/卸载断言通过。

## 3. 回归

- [x] 3.1 新增 `scripts/regress/svid-refresh-schedule.sh` 并纳入 `regress.sh` 12i；verification: 脚本逐项 PASS。
- [x] 3.2 `spire-svid-services.sh` 增加真实 `--check` 断言；verification: 12g 通过。
- [x] 3.3 运行本地全量回归；verification: `scripts/regress/regress.sh` 通过。

## 4. 文档

- [x] 4.1 更新 `docs/27` §28.2/§28.3/§28.4、`docs/28`、README、docs/README、ROADMAP、`scripts/README.md`；verification: 文档命令与新脚本一致。
- [x] 4.2 docs/28 移除 A1 并更新行动项数量；verification: 文档不再把 A1 计为未完成。

## 5. Workflow follow-up

- [x] 归档 OpenSpec 变更并同步 `workload-identity` 主规格。
- [x] 新建分支，提交、推送、创建 PR；本地与 GitHub 回归通过后合并到 `master`。
