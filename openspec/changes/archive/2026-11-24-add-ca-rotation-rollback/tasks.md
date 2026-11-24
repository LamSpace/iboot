# Tasks

- [x] 1. `tls-ca-rotate.sh` 增加 `--rollback` 与 `rollback=available|unavailable` 状态；verification: prepared/activated 回滚成功，finalized 拒绝。
- [x] 2. 扩展 `scripts/regress/ca-rotation.sh` 覆盖两种回滚路径；verification: 25 项断言通过。
- [x] 3. 更新 `docs/27` §23.4、`docs/28`、`scripts/README.md`；verification: 文档命令与状态字段一致。
- [x] 4. 运行本地全量回归；verification: `scripts/regress/regress.sh` 通过。
- [x] 5. 归档 OpenSpec 变更并同步 `transport-security` 主规格。
- [x] 6. 新建分支，提交、推送、创建 PR；本地与 GitHub 回归通过后合并到 `master`。
