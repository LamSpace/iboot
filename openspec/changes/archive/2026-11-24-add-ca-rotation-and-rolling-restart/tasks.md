# Tasks

- [x] 1. `tls-init.sh` 拆分签名 CA 与信任 bundle，支持 `--sync-runtime` 与多 CA 信任库；verification: 临时目录初始化与同步通过。
- [x] 2. 新增 `scripts/tls-ca-rotate.sh` 三阶段 prepare/activate/finalize/status；verification: 临时目录三阶段均通过。
- [x] 3. `tls-rotate.sh` 新增 `--restart rolling` 与 `--restart-only`，逐个重建并等待 healthy/running；verification: `bash -n` 与 Docker 环境回归。
- [x] 4. 新增 `scripts/regress/ca-rotation.sh` 并接入回归 12d；verification: 本地 11 项通过。
- [x] 5. 更新 README、scripts/README、docs/27 §23.4 与残余边界说明；verification: 文档与脚本一致。

## 未执行项

- [ ] 跨主机中心化调度：仍需每台主机分别安装调度并协调三阶段顺序，本仓库不提供中心化编排。

## Workflow follow-up

- [ ] 归档 OpenSpec 变更。
- [ ] 新建功能分支，提交、推送、创建 PR；CI 通过后合并到 `master`。
