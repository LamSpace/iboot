# Proposal

## Why

B5.3 把 RocketMQ 应用入口收敛为只接受 TLS，但 Broker 与 Proxy 没有启用认证或授权：任何能连到
`rocketmq-tls:8081` 的进程都可以向任意 topic 生产或消费。传输加密不能替代调用方身份，通知事件链路
仍缺少“谁在发布、谁在消费”的约束。

## What Changes

- Broker 启用 ACL 2.0：`aclEnable`、认证/授权开关、Local 元数据 Provider 与 RocksDB 持久化目录。
- Proxy 启用 ACL 2.0：通过 Broker 元数据校验调用方，配置内部客户端凭据，并打开集群模式 ACL RPC Hook。
- 新增 ACL 账号：`iboot_workflow` 只允许 PUB `iboot-notification`；`iboot_notify` 只允许 SUB 该 topic、
  其消费组与重试 topic；运维账号只用于初始化与排障。
- `iboot-workflow` 与 `iboot-notify` 的 gRPC 客户端注入 access key / secret key。
- topic 初始化、ACL 初始化与 `mqadmin` 使用独立生成的 `tools.yml`，不再依赖镜像内置默认凭据。
- 回归新增：匿名/错误凭据被拒绝、workflow 可发布、notify 可消费、notify 不能发布、跨 topic 被拒绝。
- 客户端 5.2.2 不校验服务端证书的边界保持不变，继续登记为显式例外。

## Capabilities

### New Capabilities

无。

### Modified Capabilities

- `transport-security`：RocketMQ 从“只接受 TLS”升级为“TLS + 客户端认证 + topic 级授权”。

## Impact

- 编排：`docker/docker-compose.yml` 的 broker、proxy、topic 初始化与新增 ACL 初始化任务。
- 配置：`docker/rocketmq/*`、`.env`/`.env.example`、CI、`scripts/dev-init.sh`。
- 代码：`iboot-workflow`、`iboot-notify` 的 RocketMQ 客户端配置。
- 回归：新增 `scripts/regress/rocketmq-acl.sh`，接入 `regress.sh`。
- 文档：`docs/27` §19、`docs/08`、`docs/23`、`docker/README.md`、`ROADMAP.md`。
