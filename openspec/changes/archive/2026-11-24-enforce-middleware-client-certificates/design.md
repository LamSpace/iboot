# Design

## Context

MySQL 与 Redis 已使用服务端 TLS 与每服务账号/ACL，但传输层不要求客户端证书。口令泄露后可被任意进程使用；
服务端日志无法从 TLS 层证明连接方身份。Silo 与 Nacos HTTP API 没有等价的客户端证书认证能力。

## Goals / Non-Goals

**Goals:**

- MySQL 八个服务账号要求客户端证书主体与服务身份一致。
- Redis 要求客户端证书，数据服务与 exporter 都能正常连接。
- 回归覆盖无证书、正确证书与错误服务证书。

**Non-Goals:**

- 不改变服务间 mTLS、Nacos gRPC 客户端证书或服务端证书。
- 不为 Silo 与 Nacos HTTP 伪造 mTLS；它们保持账号/令牌，并在文档中登记为例外。
- 不改变表级/键级权限矩阵。
- 不引入外部 PKI；仍使用本地 CA。

## Decisions

### 1. MySQL 使用每服务 PKCS12 + `REQUIRE SUBJECT`

`tls-init.sh` 在生成 PEM 证书后，为每个服务生成
`services/iboot-<service>.p12`（cert + key，口令 `changeit`）。`mysql-users-init.sh` 对八个服务账号执行：

```sql
ALTER USER 'iboot_<service>'@'%' REQUIRE SUBJECT '/CN=iboot-<service>';
```

Connector/J 通过 `MYSQL_SSL_PARAMS` 追加 `clientCertificateKeyStoreUrl=file:/certs/client.p12`、
`clientCertificateKeyStorePassword=changeit` 与 `clientCertificateKeyStoreType=PKCS12`。
Docker 把每服务 p12 挂载到 `/certs/client.p12`；宿主机由 `dev-init.sh` 指向实际文件。
迁移账号 `iboot_migrator` 与 `iboot_exporter` 不加 `REQUIRE`，避免初始化链路依赖业务证书。

### 2. Redis 开启客户端证书认证

Redis 命令改为 `--tls-auth-clients yes`。`iboot-tls.yml` 增加 `redis-client` PEM bundle：

```yaml
spring:
  data:
    redis:
      ssl:
        enabled: true
        bundle: redis-client
  ssl:
    bundle:
      pem:
        redis-client:
          keystore:
            certificate: ${IBOOT_TLS_CERT_FILE}
            private-key: ${IBOOT_TLS_KEY_FILE}
          truststore:
            certificate: ${IBOOT_TLS_CERT_DIR:docker/certs}/ca/rootCA.pem
```

服务已有的 `IBOOT_TLS_CERT_FILE` / `IBOOT_TLS_KEY_FILE` 就是自己的服务证书与私钥，且 EKU 同时包含
`serverAuth` 与 `clientAuth`，无需生成额外证书。Redis exporter 使用 `clients/redis-exporter.pem`
客户端证书（`tls-init.sh` 新增，EKU `clientAuth`）。

### 3. 回归命令显式带证书

`redis-cli` 与 `mysql` 的回归命令增加 `--cert/--key` 或 `--ssl-cert/--ssl-key`；无证书用例显式不传。
`exporter-mtls.sh` 的 Redis 抓取继续使用 exporter 客户端证书。

### 4. Silo 与 Nacos HTTP 例外

- Silo（MinIO 兼容）不提供服务端按客户端证书映射工作负载身份的稳定能力；继续使用服务端 TLS +
  桶级账号。文档与回归不得宣称 mTLS。
- Nacos HTTP API 使用登录令牌；gRPC 已有每服务客户端证书（见 `use-per-service-nacos-client-cert`），
  HTTP 面不重复实现 mTLS。
- 两项保留在 `docs/27` §29 的显式例外中，触发条件为上游提供能力或引入网格。

## Risks / Trade-offs

- [Redis exporter 需要客户端证书] → `tls-init.sh` 生成 clientAuth 证书并在 compose 注入。
- [MySQL 初始化链路不能依赖业务证书] → migrator/exporter 保持无客户端证书；服务账号才 `REQUIRE`。
- [Silo/Nacos HTTP 未闭环] → 明确登记为上游能力边界，不宣称零信任全覆盖。
- [证书轮换需要更新 keystore] → `tls-init.sh` 重新生成 p12；服务重建后生效。

## Migration Plan

1. 更新 `tls-init.sh` 生成 p12 与 redis-exporter 客户端证书。
2. 更新 `mysql-users-init.sh`、compose、`iboot-tls.yml`、`dev-init.sh`。
3. 重新执行 `tls-init.sh`、账号初始化与 Redis 重启，重建服务。
4. 运行新增回归与全量回归。
5. 回滚：MySQL 撤销 `REQUIRE SUBJECT`，Redis 改回 `--tls-auth-clients no`，移除客户端证书配置。

## Open Questions

无。
