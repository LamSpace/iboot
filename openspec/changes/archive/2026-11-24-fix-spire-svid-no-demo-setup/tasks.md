# Tasks

- [x] 1. `spire-up.sh` 支持 `--no-demo`，只启动 Server/Agent；verification: 本地 `--no-demo` 成功。
- [x] 2. `spire-svid-sync.sh --setup` 使用 `--no-demo` 并保留失败输出；verification: 本地 setup 成功。
- [x] 3. 本地 `spire-svid-services`/`internal-tls` 通过；verification: 61/147 项。
- [ ] 4. GitHub 全栈回归通过；verification: CI run 成功。

## Workflow follow-up

- [ ] 归档 OpenSpec 变更。
- [ ] 提交、推送、创建 PR；CI 与全栈回归通过后合并到 `master`。
