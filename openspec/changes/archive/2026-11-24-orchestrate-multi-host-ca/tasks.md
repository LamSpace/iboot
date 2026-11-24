# Tasks

- [x] 1. 新增 `tls-ca-orchestrate.sh`：checklist、调度安装、巡检、阶段执行、dry-run 与失败停止；verification: 命令输出与退出码符合预期。
- [x] 2. 新增 `scripts/regress/tls-ca-orchestrate.sh` 并纳入 `regress.sh` 12k；verification: 21 项断言通过。
- [x] 3. 更新 `docs/27` §23.3/§23.4/§28.4、`docs/28`、README、docs/README、ROADMAP、`scripts/README.md`；verification: docs/28 行动项清零。
- [x] 4. 运行本地全量回归；verification: `scripts/regress/regress.sh` 通过。
- [x] 5. 归档 OpenSpec 变更并同步 `transport-security` 主规格。
- [x] 6. 新建分支，提交、推送、创建 PR；本地与 GitHub 回归通过后合并到 `master`。
