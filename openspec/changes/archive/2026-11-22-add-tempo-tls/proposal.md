# Proposal

## Why

链路追踪是最后一条仍在应用配置里使用明文的内部链路：九个服务通过 `http://tempo:4318` 上报 span，Grafana 通过 `http://tempo:3200` 查询。追踪数据可能包含请求参数与业务标识，且这两个地址已被 `plaintext-allowlist.txt` 登记为待处理项，阻碍把内部可观测面描述为全链路 TLS。

## What Changes

- Tempo 的查询 HTTP 端口（3200）启用 TLS，证书复用本地叶子证书。
- OTLP/HTTP（4318）与 OTLP/gRPC（4317）接收器启用 TLS，明文上报不再被接受。
- 编排中的 `TEMPO_OTLP` 与九个服务 `application.yml` 的默认地址改为 `https://`。
- Grafana 的 Tempo 数据源改为 `https://tempo:3200`，并使用本地 CA 校验证书。
- 追踪查询脚本（`trace-chain.sh`、`apm/verify.sh`）改用 HTTPS 与 `--cacert`。
- 从明文允许清单移除 Tempo 两条，内部 TLS 回归增加 Tempo 端口与服务端证书检查。
- **不改动**：Tempo 内部 gRPC 服务端口 9095 仍为明文（不发布、仅进程内使用）；Grafana/Prometheus/Thanos 的 UI 与数据源 HTTP 边界不在本批次；不启用客户端证书（mTLS 属于 B5）。

## Capabilities

### New Capabilities

无。

### Modified Capabilities

- `transport-security`: 新增“Tempo 接收与查询链路使用 TLS”的要求，并收窄明文允许清单。

## Impact

- 编排与配置：`docker/docker-compose.yml`、`docker/tempo/tempo.yaml`、九个服务的 `application.yml`、Grafana 数据源 provisioning。
- 脚本：`scripts/regress/trace-chain.sh`、`scripts/apm/verify.sh`、`scripts/regress/internal-tls.sh`、`scripts/regress/plaintext-allowlist.txt`。
- 文档：`docs/02`、`docs/11`、`docs/23`、`docs/27`、`docker/README.md`。
- 运行时：需重建 Tempo/Grafana 容器并重启宿主机服务进程；宿主机进程继续使用现有 PKCS12 信任库，无需新增证书。
