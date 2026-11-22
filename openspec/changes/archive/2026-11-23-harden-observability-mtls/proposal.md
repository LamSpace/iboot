# Proposal

## Why

Prometheus 抓取 MySQL、Redis、Node 与 Elasticsearch exporter 时使用容器网络内的明文 HTTP，且 exporter
端口没有客户端身份校验。能进入 `data`/`obs` 网络的容器都可以读取这些指标；证书到期仍依赖人工执行
`tls-init.sh --force` 并逐个重启组件，缺少可重复的轮换入口。

## What Changes

- 四个 exporter 启用 HTTPS 与客户端证书校验：exporter-toolkit `web.config.file` 或 Redis 原生 TLS 参数。
- 为 exporter 签发独立服务端证书；Prometheus 使用自己的客户端证书与本地 CA 抓取。
- 新增 `scripts/tls-rotate.sh`：检查剩余有效期、按需重签、按 Docker/宿主机模式重启依赖组件。
- 回归新增 exporter mTLS 正向/负向用例，并校验轮换脚本的检查模式。
- 更新 Prometheus 两份抓取配置、编排、证书脚本与文档。

## Capabilities

### New Capabilities

- `observability-security`：监控采集链路的传输加密与客户端身份。

### Modified Capabilities

- `transport-security`：证书过期门禁从“人工重签”升级为“可重复轮换脚本”。

## Impact

- 证书：`scripts/tls-init.sh` 增加 exporter 服务端证书。
- 编排：四个 exporter、Prometheus 的挂载与启动参数。
- 配置：`docker/scripts/exporters/*`、`docker/scripts/prometheus/*`。
- 脚本：新增 `scripts/tls-rotate.sh`、`scripts/regress/exporter-mtls.sh`，接入 `regress.sh`。
- 文档：`docs/27`、`docs/08`、`docker/README.md`、`ROADMAP.md`。
