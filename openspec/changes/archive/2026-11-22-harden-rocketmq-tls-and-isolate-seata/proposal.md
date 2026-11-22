# Proposal

## Why

B5.2 已为业务服务端口启用双向 TLS，但 RocketMQ 仍在一个端口同时接受明文与 TLS，Seata 又完全没有原生传输层 TLS/认证且所有 `data` 网络容器都能访问 8091。两条链路仍是“加密/认证未闭环”的明显缺口：RocketMQ 的明文入口可绕过 TLS，Seata 控制面可被同网络容器直接连接。

## What Changes

- **BREAKING**：RocketMQ 应用入口改为只接受 TLS 的前置代理 `rocketmq-tls`；直接连接 `rocketmq-proxy:8081` 或使用明文的客户端将失败。
- RocketMQ NameServer、Broker、Proxy 与初始化任务迁到专用 `mq` 网络；只有 `rocketmq-tls` 同时加入 `app` 网络，应用统一使用 `rocketmq-tls:8081`；宿主机不再发布 Proxy 的明文端口。
- Seata Server 迁到专用 `tx` 网络，只有 `iboot-workflow` 与 `iboot-system` 加入；其余服务无法解析或连接 `seata-server:8091`。
- Seata 2.6.0 无原生 TLS/认证，本批不手搓 TLS 终止器，登记为**显式例外**：专用网络隔离 + 回环发布 + 业务侧 mTLS/内部令牌补偿，不纳入 mTLS/零信任宣称。
- 回归新增：RocketMQ 明文拒绝、应用无法直连 Proxy、非 Seata 客户端无法访问 8091；`log-search-tls.sh` 更新 RocketMQ 边界描述。
- 更新 `docs/27`、`docs/08`、`docs/23`、`docker/README.md` 与 `ROADMAP.md`。

## Capabilities

### New Capabilities

无。

### Modified Capabilities

- `transport-security`：RocketMQ 从“同端口加密通道”升级为“入口只接受 TLS”，并保留“客户端不校验服务端、无 ACL”的边界；新增 Seata 显式例外与网络隔离要求。
- `container-deployment`：RocketMQ 编排改为 `mq` 网络 + TLS 前置代理，移除 Proxy 宿主机明文端口；Seata 编排改为 `tx` 网络，仅 workflow/system 可访问。

## Impact

- 编排：`docker/docker-compose.yml` 新增 `mq`、`tx` 网络与 `rocketmq-tls` 服务；RocketMQ、Seata、system/workflow 网络调整。
- 配置：新增 `docker/rocketmq/tls-proxy.conf`；`IBOOT_ROCKETMQ_ENDPOINTS` 改为 `rocketmq-tls:8081`。
- 回归：`scripts/regress/log-search-tls.sh`、`scripts/regress/exposure.sh`、`scripts/regress/seata.sh`/`seata-tcc.sh` 的前置说明。
- 兼容性：明文 RocketMQ 客户端、直连 `rocketmq-proxy` 的脚本/容器、以及 `data` 网络上访问 Seata 的容器都会失败。
- 不包含：RocketMQ ACL、客户端服务端证书校验（官方 5.2.2 客户端硬编码 `InsecureTrustManager`）、Seata 原生 TLS/认证。
