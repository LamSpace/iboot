# Tasks

## 1. 证书 SPIFFE 身份

- [x] 1.1 `tls-init.sh` 为服务证书写入 `spiffe://iboot.local/service/<name>`，为探针证书写入 `.../client/<name>`；verification: `openssl x509 -text` 显示 URI SAN。
- [x] 1.2 `--check` 校验 URI SAN 与 CN 一致；verification: 手工删除 URI 后检查失败。

## 2. 授权身份解析

- [x] 2.1 `ClientCertificateIdentity` 优先解析 URI SAN，校验信任域并回退 CN；更新单元测试；verification: `mvn -pl iboot-service-authz test` 通过。
- [x] 2.2 保持过滤器/策略签名不变，确认服务端与网关加载新解析器；verification: `service-authz.sh` 正向用例通过。

## 3. 回归与文档

- [x] 3.1 `internal-tls.sh` 断言每张叶子证书的 SPIFFE ID；verification: 本地全栈通过。
- [x] 3.2 `service-authz.sh` 新增 CN/URI 不一致与旧证书回退用例；verification: 本地全栈通过。
- [x] 3.3 更新 `docs/27` §18/§20、`docs/10` §10.26.3、`ROADMAP.md`；verification: 文档写明 SPIFFE 格式与信任域。
- [x] 3.4 运行本地全量回归；verification: 通过（备份用例按既有边界跳过）。

## Workflow follow-up

- 归档 OpenSpec 变更并同步 `service-authorization` 与 `transport-security`。
- 触发 GitHub Actions `full-regress` 并确认通过。
- 新建 `feat/adopt-spiffe-identities` 分支，提交、推送、创建 PR 并合并到 `master`。
