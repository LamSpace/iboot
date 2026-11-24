# Tasks

- [x] 1. `tls-rotate.sh` 增加 SVID 栈检测与 `docker|rolling` 委托；verification: 假 docker 下委托路径与基础路径均正确。
- [x] 2. 新增 `scripts/regress/tls-rotate-svid.sh` 并纳入 `regress.sh` 12j；verification: 8 项断言通过。
- [x] 3. 更新 `docs/27` §23.2/§23.4/§28.3、`docs/28`、`scripts/README.md`；verification: 文档描述与脚本一致。
- [x] 4. 运行本地全量回归；verification: `scripts/regress/regress.sh` 通过。
- [x] 5. 归档 OpenSpec 变更并同步 `transport-security` 主规格。
- [x] 6. 新建分支，提交、推送、创建 PR；本地与 GitHub 回归通过后合并到 `master`。
