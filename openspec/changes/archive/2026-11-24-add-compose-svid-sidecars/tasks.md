# Tasks

- [x] 1. 新增 `docker/svid-sidecar` 镜像与 `docker-compose.svid.yml` 覆盖层：十服务 sidecar + 命名卷 + env/依赖；verification: compose config 通过。
- [x] 2. `docker-deploy.sh` 默认启用 SVID：先 setup、叠加覆盖层、构建 sidecar；verification: 静态/脚本检查。
- [x] 3. 新增 `docker-svid-refresh.sh`：重建 sidecar 并滚动重启业务容器；verification: 语法与静态检查。
- [x] 4. 新增 12h `compose-svid.sh`：覆盖层、sidecar 镜像、单服务取证；verification: 本地 15 项通过。
- [x] 5. 更新 README、ROADMAP、docker/README、scripts/README、docs/27；verification: 文档一致。

## 未执行项

- 无（Compose 全量模式已默认 SVID；`IBOOT_SVID_ENABLED=false` 为回退开关）。

## Workflow follow-up

- [ ] 归档 OpenSpec 变更。
- [ ] 提交、推送、创建 PR；CI 与全栈回归通过后合并到 `master`。
