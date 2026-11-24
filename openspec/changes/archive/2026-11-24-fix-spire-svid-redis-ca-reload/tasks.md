# Tasks

- [x] 1. SVID setup 合并 SPIRE CA 后重启 Redis，等待 healthy；verification: 全新栈十个服务健康。
- [x] 2. 保留 Prometheus/blackbox 重启；verification: `internal-tls` 147 项、`spire-svid-services` 61 项通过。
- [ ] 3. GitHub 全栈回归通过；verification: CI run 成功。

## Workflow follow-up

- [ ] 归档 OpenSpec 变更。
- [ ] 提交、推送、创建 PR；CI 与全栈回归通过后合并到 `master`。
