# Design

## Context

See `proposal.md` - Why. 当前 broker 与 proxy 使用镜像默认配置启动：broker 只有 `conf/broker.conf` 的基础项，
proxy 只配置 TLS 证书；两者都没有认证元数据。RocketMQ 5.5 的 ACL 2.0 由 `rocketmq-auth` 模块提供，
Broker 与 Proxy 各自持有认证/授权配置，Proxy 通过 RPC Hook 向 Broker 校验调用方。

## Goals / Non-Goals

**Goals:**

- 启用 ACL 2.0 后，未认证客户端无法生产或消费；workflow/notify 各自只有最小 topic 权限。
- 账号与授权在容器重建后仍存在，初始化幂等。
- 保持 B5.3 的 TLS-only 入口与端到端 TLS，不引入新的明文跳。

**Non-Goals:**

- 不解决客户端服务端证书校验：`rocketmq-client-java` 5.2.2 硬编码 `InsecureTrustManagerFactory`。
- 不对 NameServer 启用认证；NameServer 只服务 Broker/Proxy 的路由查询，不在应用入口路径上。
- 不引入 RocketMQ 的 OIDC/外部认证 Provider。

## Decisions

### 1. 使用 ACL 2.0 + Local 元数据 Provider

Broker 配置 `aclEnable=true`、`authenticationEnabled=true`、`authorizationEnabled=true`，
Provider 使用 `DefaultAuthenticationProvider` / `LocalAuthenticationMetadataProvider` 与对应授权类；
元数据持久化到 `/home/rocketmq/auth` 的独立卷。

替代方案是 v1 `plain_acl.yml` + `migrateAuthFromV1Enabled`；放弃，因为 5.5 的 gRPC 路径以 ACL 2.0
为准，v1 迁移只适合过渡，且无法覆盖 Proxy 的元数据查询。

### 2. Proxy 使用 Broker 元数据，并打开集群模式 ACL RPC Hook

Proxy 配置与 Broker 相同的 Provider 名称，但元数据 Provider 使用
`ProxyAuthenticationMetadataProvider` / `ProxyAuthorizationMetadataProvider`；内部客户端凭据
必须与 Broker 的运维账号一致。`enableAclRpcHookForClusterMode=true` 是 gRPC 客户端在集群模式下
收到 `40100 Unauthorized` 的已知修复项。

### 3. 账号与授权由一次性任务创建

新增 `rocketmq-acl-init`：等待 Broker 健康后，用运维凭据生成 `tools.yml`，按“删除用户 → 创建用户 →
创建 ACL”的顺序幂等执行。topic 初始化任务也生成同一份 `tools.yml`，因此不再依赖镜像默认凭据。

ACL 规则：
- `iboot_workflow`：`Topic:iboot-notification`，`PUB`。
- `iboot_notify`：`Topic:iboot-notification`、`Topic:%RETRY%iboot-notify-consumer`，`SUB`；
  `Group:iboot-notify-consumer`，`SUB`。
- 运维账号：`UserType=SUPER`，只用于初始化与排障。

### 4. 客户端凭据通过 ClientConfiguration 注入

`RocketMqProperties` 增加 `accessKey` / `secretKey`；两个客户端在
`ClientConfiguration.newBuilder()` 上追加 `setCredentialProvider(new StaticSessionCredentialsProvider(...))`。
凭据为空时保持无凭据行为，便于单测与本地无 ACL 排障；受管环境由 compose/dev-init 注入。

### 5. 运行时配置由入口脚本生成

Broker 与 Proxy 的配置文件包含口令，不能入库。入口脚本从环境变量生成 `/tmp/broker.conf` 与
`/tmp/rmq-proxy.json`，再 `exec` 启动；`tools.yml` 同样由环境生成。仓库只保留模板/脚本。

## Risks / Trade-offs

- [ACL 配置错误会阻断通知链路] → 初始化任务与回归同时覆盖 workflow 发布、notify 消费与负向拒绝；
  `rocketmq-acl.sh` 在 CI 全量回归中执行。
- [Proxy 与 Broker 内部凭据不一致导致 40100] → 两者读取同一组 `ROCKETMQ_ADMIN_*` 环境变量。
- [用户/ACL 每次初始化删除重建造成瞬时拒绝] → 初始化任务在业务服务启动前完成；业务客户端有重试。
- [RocksDB 卷丢失导致账号消失] → 卷保留；初始化任务每次启动重新写入，不依赖卷内容。
- [客户端仍不校验服务端证书] → 文档与 `ROADMAP` 继续登记为显式例外，不宣称 mTLS。

## Migration Plan

1. 在 `.env` 增加运维与两个应用账号口令；启动前校验缺失即失败。
2. 重建 broker/proxy，执行 topic 与 ACL 初始化；业务服务随后启动。
3. 运行 `rocketmq-acl.sh` 与全量回归；确认通知链路与负向用例通过。
4. 回滚：移除 ACL 配置与初始化任务，恢复无认证 Broker/Proxy；客户端凭据字段可保留为空。

## Open Questions

无。
