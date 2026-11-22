# Design

## Context

`observability-tls.sh` 覆盖 Grafana/Prometheus/Alertmanager/Thanos/Kibana 的 HTTPS，但 Prometheus 到四个
exporter 仍用明文 HTTP；exporter 端口只在 Docker 网络内可达，没有客户端身份。证书轮换依赖人工执行
`tls-init.sh --force`，重启范围靠记忆。

## Goals / Non-Goals

**Goals:**

- exporter 端点只接受 TLS + 受管客户端证书；Prometheus 抓取成功，其它抓取失败。
- 轮换脚本可重复、可检查、可显式重签，并能列出需要重启的组件。
- 保持 exporter 不发布到宿主机。

**Non-Goals:**

- 不引入 mTLS 到 Grafana/Thanos/Kibana 的内网调用；它们已有 HTTPS。
- 不做自动定时轮换与证书吊销；脚本提供 cron 入口，调度由部署方负责。
- 不改 exporter 采集目标（MySQL/Redis/ES）的认证方式。

## Decisions

### 1. 用 exporter-toolkit web config，Redis 用原生 TLS

mysqld/node/elasticsearch exporter 均支持 `--web.config.file`，统一用同一份结构：
`tls_server_config` + `client_auth_type: RequireAndVerifyClientCert` + `client_ca_file`。
redis_exporter 使用 `--tls-server-cert-file/--tls-server-key-file/--tls-server-ca-cert-file`。

替代方案是在 exporter 前加 nginx TLS 终止；放弃，因为增加一跳与额外容器，且 exporter 原生支持。

### 2. 每个 exporter 独立服务端证书

`tls-init.sh` 增加 `EXPORTER_NAMES`，签发给 `mysql-exporter` 等四个名字，`serverAuth` only，
SAN 包含服务名与 localhost。Prometheus 的客户端证书沿用 `clients/prometheus.pem`。

### 3. Prometheus 抓取使用 HTTPS + 客户端证书

两份 Prometheus 配置的 exporter job 增加 `scheme: https` 与 `tls_config`，CA、证书、私钥来自
`/certs` 只读挂载，`server_name` 指向目标服务名。

### 4. 轮换脚本复用 `tls-init.sh`

`tls-rotate.sh` 三种模式：
- `--check`：只执行 `tls-init.sh --check`，输出剩余天数与需要重启的组件。
- `--rotate`：执行 `--force` 重签，再按 `--restart docker|host|none` 重启。
- `--restart docker`：重建挂载证书的容器；`host`：停止并由 `dev-init.sh` 重启宿主机进程。

替代方案是每次启动自动重签；放弃，因为会让证书指纹不断变化，破坏审批与审计。

## Risks / Trade-offs

- [exporter 证书 SAN 缺失导致 Prometheus 校验失败] → 签发时显式写入服务名；回归覆盖四个 job。
- [Node exporter 只加入 obs 网络，app 容器无法直接测试] → 回归使用 `curlimages/curl` 在 obs 网络内取证。
- [重启范围过大] → 脚本按挂载关系列出组件，默认只重签不重启；重启需显式参数。
- [轮换期间短暂不可用] → 本地开发接受；生产由滚动发布承担。

## Migration Plan

1. 重新执行 `tls-init.sh --force` 生成 exporter 证书与运行期副本。
2. 重建四个 exporter 与 Prometheus；确认目标健康。
3. 运行 `exporter-mtls.sh` 与全量回归。
4. 回滚：移除 `--web.config.file`/Redis TLS 参数并恢复 Prometheus HTTP 抓取。

## Open Questions

无。
