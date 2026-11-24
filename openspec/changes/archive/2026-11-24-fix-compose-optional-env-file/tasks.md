# Tasks

- [x] 1. `docker/docker-compose.yml` 的 `env_file` 改为 `path`/`required: false`；verification: 无 `.env` 时 `docker compose config --format json` 返回 0 且输出完整 JSON。
- [ ] 2. 重新触发 GitHub `full-regress`；verification: `middleware-auth.sh` 静态凭据检查通过，启动与全栈回归通过。

## Workflow follow-up

- [ ] 归档 OpenSpec 变更。
- [ ] 新建 `fix/compose-optional-env-file` 分支，提交、推送、创建 PR；CI 通过后合并到 `master`。
