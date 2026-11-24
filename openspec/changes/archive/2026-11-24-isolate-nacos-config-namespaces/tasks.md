# Tasks

- [x] 1. `nacos-init.sh` 创建每服务配置命名空间与独立角色，`public` 只用于服务发现；verification: 服务账号只能读自己的命名空间。
- [x] 2. 九个服务配置 `spring.cloud.nacos.config.namespace` 指向 `iboot-<service>`；verification: 服务健康启动。
- [x] 3. 新增 `scripts/regress/nacos-namespace-isolation.sh` 并接入 12f；verification: 本地 8 项通过。
- [x] 4. 更新 README、ROADMAP、docs/23、docs/27；verification: 文档与权限矩阵一致。

## 未执行项

- [ ] Nacos HTTP API 客户端证书 mTLS：上游仍只支持令牌，保留为显式例外。

## Workflow follow-up

- [ ] 归档 OpenSpec 变更。
- [ ] 新建功能分支，提交、推送、创建 PR；CI 通过后合并到 `master`。
