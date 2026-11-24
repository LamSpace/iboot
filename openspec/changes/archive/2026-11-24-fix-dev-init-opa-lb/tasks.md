# Tasks

- [x] 1. `scripts/dev-init.sh` 的中间件启动列表加入 `opa-lb`；verification: 干净环境启动后 `curl http://localhost:8181/health` 返回 200。
- [x] 2. 修正 `.github/workflows/regress.yml` 的失败日志步骤：使用 `docker ps`/`docker logs`，后端日志限制 20KB；verification: 失败步骤不再因缺少 `ES_*` 变量而中断。
- [ ] 3. 重新触发 GitHub `full-regress`；verification: 启动阶段通过且全栈回归通过。

## Workflow follow-up

- [ ] 归档 OpenSpec 变更。
- [ ] 新建 `fix/dev-init-opa-lb` 分支，提交、推送、创建 PR；CI 通过后合并到 `master`。
