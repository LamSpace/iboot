# Tasks

- [x] 1. `spire-svid-sync.sh` 注册十个服务条目、取回 SVID、合并 SPIRE CA 并同步信任库；verification: 十张 SVID 可验证。
- [x] 2. `dev-init.sh` 默认启用 SVID，宿主机发现地址使用 localhost，服务加载 SVID；verification: 十个服务健康。
- [x] 3. SPIRE `ca_ttl` 与 SVID TTL 设为 30 天，Agent 重启清理缓存；verification: SVID 有效期 30 天。
- [x] 4. 新增 12g 回归（SAN/有效期/mTLS），并修正 CA 轮换回归的单 CA 起点；verification: 本地全栈回归通过。
- [x] 5. 更新 README、ROADMAP、docker/README、scripts/README、docs/23、docs/27；verification: 文档与脚本一致。

## 未执行项

- [ ] Compose 全量部署切换 SVID：需要每服务 sidecar/agent 与滚动重启，保留为独立部署变更；宿主机开发已默认切换。

## Workflow follow-up

- [ ] 归档 OpenSpec 变更。
- [ ] 提交、推送、创建 PR；本地与 GitHub 全栈回归通过后合并到 `master`。
