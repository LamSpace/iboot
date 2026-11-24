# Proposal

## Why

B6 的残余边界中，Silo 仍使用长期 access key，文件/AI 服务无法按工作负载归因。Silo（MinIO 兼容）
支持 `MINIO_IDENTITY_TLS_ENABLE=on` 与 STS `AssumeRoleWithCertificate`：客户端证书的 CN 映射到同名
桶策略，换取短时临时凭据。该能力已在本地容器验证可行。

## What Changes

- Silo 启用 `MINIO_IDENTITY_TLS_ENABLE=on`，并设置 `MINIO_IDENTITY_TLS_URI_SAN_SCHEME=disabled`
  使证书 CN 映射到现有桶策略。
- 文件/AI 服务新增 `client-cert-enabled` 开关与 PKCS12 keystore 配置；启用时使用
  `CertificateIdentityProvider` 调用 STS，否则回退到 access key。
- `silo-init.sh` 把文件/AI 策略名改为与证书 CN 一致（`iboot-file`、`iboot-ai`），access key 仍可回退。
- 新增 `scripts/regress/silo-client-cert.sh`：证书换取凭据、无证书拒绝、策略存在、服务开关。
- 更新 README、ROADMAP、docker/README、docs/23、docs/27。

## Capabilities

### New Capabilities

无。

### Modified Capabilities

无。Silo 身份认证方式变化，不改变桶级授权语义。

## Impact

- 代码：`SiloProperties`、`SiloFileStorageStrategy`、`RagProperties`、`SiloKnowledgeStorage`。
- 配置：文件/AI `application.yml`、compose Silo 与 file/ai 服务、`silo-init.sh`。
- 回归：`scripts/regress/regress.sh` 新增 12e。
- 文档：README、ROADMAP、docker/README、docs/23、docs/27。

## 说明

Nacos HTTP API 仍只支持令牌，保留为残余边界；Silo 已从该边界移除。
