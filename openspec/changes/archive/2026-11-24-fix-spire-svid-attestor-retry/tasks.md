# Tasks

- [x] 1. 先重启 Agent 清缓存、再注册条目，给 Docker attestor 稳定时间；verification: 本地 setup 成功。
- [x] 2. 取回 SVID 重试 10 次，并在最后一次输出容器标签、条目与 Agent 日志；verification: 本地 setup 成功且诊断可读。
- [x] 3. 本地 `spire-svid-services.sh` 与全栈回归通过；verification: 51 项与 regress 通过。
- [ ] 4. GitHub 全栈回归通过；verification: CI run 成功。

## Workflow follow-up

- [ ] 归档 OpenSpec 变更。
- [ ] 提交、推送、创建 PR；CI 与全栈回归通过后合并到 `master`。
