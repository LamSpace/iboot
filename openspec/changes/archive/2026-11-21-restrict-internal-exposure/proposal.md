# Proposal

## Why

Docker 全量部署当前向宿主机发布了 48 个端口，且全部绑定 `0.0.0.0`。Docker 的端口发布绕过主机防火墙，MySQL、Redis、Elasticsearch、Nacos 等中间件因此对同网段直接可达。门户已经是唯一公开入口，但网络层尚未落实这一边界；只有 TLS 加密，没有「谁能连到哪里」的控制。B4 收尾需要收敛暴露面并按角色分段，为 B5 的工作负载身份提供可判定的网络边界。

## What Changes

- **端口绑定策略**：仅门户 `80` / `443` 绑定 `0.0.0.0`；其余宿主端口全部改为 `127.0.0.1:<port>:<port>`。宿主机开发仍通过 `localhost` 使用 MySQL、Redis、Silo、Elasticsearch、Nacos、Seata、Kafka、RocketMQ Proxy、各服务端口与可观测 UI。
- **移除不必要的宿主机映射**：Logstash `5044`、SkyWalking Banyandb `17913`、SkyWalking OAP `17128`、Thanos 四个组件端口、四个 exporter 端口。Prometheus 改为在编排网络内抓取。
- **Compose 网络分段**：新增 `edge` / `app` / `data` / `obs` 四个网络。门户只加入 `edge`，不得直连数据面；业务服务加入 `app` 并按需加入 `data`；可观测组件按抓取与存储关系加入 `obs`、`data`。
- **暴露面回归门禁**：新增 `scripts/regress/exposure.sh`，断言宿主机端口绑定、非回环地址不可达、容器网络正负向连通性，并接入 `regress.sh` 与 CI 全量回归。
- **文档同步**：更新部署指南、架构设计与 TLS 操作指南中的端口表、网络边界与验收命令。

**BREAKING**：依赖从其它主机访问中间件端口的脚本或外部工具将失效；本地开发必须使用 `localhost` / `127.0.0.1`，不得使用机器 LAN 地址。

## Capabilities

### New Capabilities

无。本变更不引入新的系统能力，只收紧既有部署与传输边界。

### Modified Capabilities

- `container-deployment`：新增宿主机端口暴露策略、Docker 网络分段与暴露面回归门禁要求。
- `transport-security`：新增最小暴露面要求——公开入口唯一、内部端口仅回环、非回环地址不可达。

## Impact

- `docker/docker-compose.yml`：全部 `ports` 绑定地址、服务 `networks` 归属、网络定义与注释。
- `docker/docker-compose.prod.yml`：如生产覆盖需要保留额外入口，按同一策略调整。
- `scripts/regress/regress.sh`、新增 `scripts/regress/exposure.sh`；CI 不新增独立步骤，随全量回归执行。
- `docs/08-部署指南.md`、`docs/27-TLS 与零信任操作指南.md`、`docs/03-架构设计.md`、`docker/README.md`。
- 宿主机开发脚本的调用地址不变，仍为 `localhost`；Docker 全量部署的外部入口仍只有 `https://localhost`。
