# Design

## Context

Silo 是 MinIO 兼容的对象存储，当前文件/AI 服务使用长期 access key。Silo 支持 STS
`AssumeRoleWithCertificate`：客户端通过 TLS 出示证书，服务端按证书 CN 找到同名策略并签发
默认 1 小时的临时凭据。证书由本地 CA 签发，服务端通过 `SSL_CERT_FILE` 信任该 CA。

## Decisions

- 使用 `MINIO_IDENTITY_TLS_ENABLE=on`；由于现有证书同时带 SPIFFE URI SAN，设置
  `MINIO_IDENTITY_TLS_URI_SAN_SCHEME=disabled` 让服务端回退到 CN 映射，避免新建
  `service/iboot-file` 形式的策略。
- 文件/AI 的桶策略名改为 `iboot-file`、`iboot-ai`，与证书 CN 一致；长期 access key 继续存在，
  作为回退与运维手段。
- Java 客户端用每服务 PKCS12（`/certs/client.p12`）构建 `KeyManagerFactory`，用 JVM 信任库
  （`/certs/truststore.p12`）构建 `X509TrustManager`，再交给 `CertificateIdentityProvider`。
- 预签名客户端使用对外 endpoint，但 STS 固定走内网 endpoint，避免容器内解析 `localhost`。
- 开关默认关闭；compose 为文件/AI 显式打开，宿主机开发可按需注入 keystore 路径。

## Risks / Trade-offs

- [STS 临时凭据过期] → provider 自动刷新；默认 1 小时。
- [证书 CN 与策略名不一致] → `silo-init.sh` 统一命名，回归检查策略存在。
- [对外 endpoint 解析] → STS 固定内网 endpoint；预签名仍用对外地址。
- [回退] → 关闭 `client-cert-enabled` 即回到 access key，无需改服务端。

## Migration Plan

1. 合并后重建 Silo 与 silo-init，创建同名策略。
2. 重建文件/AI 服务，启用客户端证书；回归验证上传与 STS。
3. 回滚：关闭开关并重启文件/AI；策略与 access key 保留。

## Open Questions

无。
