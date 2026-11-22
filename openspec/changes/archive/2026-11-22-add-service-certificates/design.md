# Design

## Context

`iboot-tls.yml` 为所有服务提供了一个名为 `iboot` 的 PEM SSL bundle，证书路径固定为 `${IBOOT_TLS_CERT_DIR}/portal.pem`；该 bundle 同时用于 `server.ssl` 与 Spring HTTP 客户端。门户、Nacos、Tempo 和其它组件也复用同一张证书。

## Goals / Non-Goals

**Goals:**

- 每个可部署服务的服务端证书独立，客户端可通过服务名校验证。
- 保持现有 CA、信任库、TLS 版本与端口检查不变。
- 证书缺失或过期时，`tls-init.sh` 自动补齐/重签。

**Non-Goals:**

- 不启用客户端证书认证；服务端仍不校验调用方身份。
- 不拆分 Nacos 客户端证书、Prometheus 探针证书或门户证书。
- 不引入 step-ca/SPIFFE 或自动轮换服务。

## Decisions

### 每个服务一张叶子证书

`tls-init.sh` 新增 `SERVICE_NAMES`，为每个服务调用 `mkcert -cert-file services/<svc>.pem -key-file services/<svc>-key.pem localhost 127.0.0.1 ::1 <svc>`。所有证书由同一个本地 CA 签发，现有 `truststore.p12` 不变。私钥在 `runtime` 副本中保持容器用户可读，宿主私钥 0600。

### 通过环境变量选择证书

`iboot-tls.yml` 改为：

```yaml
certificate: file:${IBOOT_TLS_CERT_FILE:${IBOOT_TLS_CERT_DIR:docker/certs}/portal.pem}
private-key: file:${IBOOT_TLS_KEY_FILE:${IBOOT_TLS_CERT_DIR:docker/certs}/portal-key.pem}
```

十个服务在 compose 中分别设置 `IBOOT_TLS_CERT_FILE=/certs/services/<svc>.pem` 与 `IBOOT_TLS_KEY_FILE=/certs/services/<svc>-key.pem`；`certs/runtime` 已整体挂载到 `/certs`，无需新增卷。宿主机模式由 `dev-init.sh` 在启动循环中注入同样两个变量。

### 共享证书的保留范围

门户 nginx、Nacos、Tempo、Prometheus/Grafana/Alertmanager/Thanos/Kibana 仍使用 `portal.pem`。它们不是“业务服务”，且 Nacos 的 keystore 与 Tempo 的配置在 `tls-init.sh` 中已经按共享证书生成；本变更只拆分十个业务端口。

### 回归校验服务名与唯一性

`internal-tls.sh` 在端口检查后：

- 用 `openssl s_client` 从每个端口取出证书，`-checkhost <svc>` 必须成功，`-checkhost localhost` 也必须成功。
- 计算十个证书的 SHA-256 指纹，必须互不相同。
- 现有明文拒绝、无 CA 失败与健康检查保持。

## Risks / Trade-offs

- **旧容器仍加载共享证书** → 重新签发后必须重建十个服务；滚动期间同 CA 信任，不会中断。
- **mkcert 未安装** → `tls-init.sh` 已有明确失败提示；CI 的安装步骤不变。
- **HTTP 客户端 bundle 同时使用服务证书** → 当前服务端不要求客户端证书，证书不会被发送；后续启用 mTLS 时可直接复用该 bundle。
- **证书探针仍只覆盖一个端点** → 十个证书在同一批次生成、到期时间一致；文档注明拆分后如引入不同有效期必须扩展探针。

## Migration Plan

1. 更新 `tls-init.sh` 生成十张服务证书，`--force` 重签。
2. 更新 `iboot-tls.yml`、compose 与 `dev-init.sh`，重建十个服务。
3. 回归：端口 TLS、SAN、唯一性、明文拒绝、契约快照与冒烟。
4. 回滚：恢复共享证书路径并重建服务；服务证书文件可保留但不被引用。

## Open Questions

无。
