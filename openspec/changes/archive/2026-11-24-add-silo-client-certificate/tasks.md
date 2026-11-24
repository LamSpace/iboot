# Tasks

- [x] 1. Silo 启用 `MINIO_IDENTITY_TLS_ENABLE=on`，证书 CN 映射同名桶策略；verification: STS 证书换取凭据成功。
- [x] 2. 文件/AI 服务用 PKCS12 构建 `CertificateIdentityProvider`，支持开关与 keystore 配置；verification: 本地文件服务启动并完成上传。
- [x] 3. `silo-init.sh` 将文件/AI 策略名改为与证书 CN 一致；verification: `mcli admin policy info` 存在。
- [x] 4. 新增 `scripts/regress/silo-client-cert.sh` 并接入回归 12e；verification: 本地 8 项通过。
- [x] 5. 更新 README、ROADMAP、docker/README、docs/23、docs/27；verification: 文档与配置一致。

## 未执行项

- [ ] Nacos HTTP API 仍不是客户端证书 mTLS：上游 API 仅支持令牌，已在 docs/27 §29 登记。

## Workflow follow-up

- [ ] 归档 OpenSpec 变更。
- [ ] 新建功能分支，提交、推送、创建 PR；CI 通过后合并到 `master`。
