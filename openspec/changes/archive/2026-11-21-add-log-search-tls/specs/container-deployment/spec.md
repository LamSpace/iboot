# Spec Delta

## ADDED Requirements

### Requirement: Kafka 日志缓冲栈只提供 SSL 监听

容器编排中的 Kafka SHALL 在容器内、宿主机与 controller 监听上只接受 SSL，并继续作为非启动依赖运行。

- broker MUST 挂载包含私钥的 keystore 与本地 CA 信任库，MUST NOT 以 PLAINTEXT 提供服务监听。
- 容器内服务、`kafka-init`、健康检查与宿主机运维工具 MUST 使用带 truststore 的 SSL 客户端配置。
- Kafka 数据 MUST 继续使用命名卷持久化；普通停止或重启 MUST NOT 删除。
- Kafka 不可用 MUST NOT 阻止任何业务服务启动；集中日志投递失败 MUST 只降级为本地日志。

#### Scenario: 干净环境以 SSL 启动

- **WHEN** 从干净环境启动核心编排
- **THEN** Kafka 的三个监听只接受 SSL 并达到健康状态
- **AND** 日志 topic 初始化完成

#### Scenario: 健康检查使用 SSL

- **WHEN** 编排器执行 Kafka 健康检查
- **THEN** 检查使用 truststore 与本地 CA 完成 SSL 连接
- **AND** 检查命令中没有 PLAINTEXT 回退

#### Scenario: 明文客户端无法初始化

- **WHEN** `kafka-init` 或运维工具以 PLAINTEXT 连接 broker
- **THEN** 初始化或命令失败并指出协议错误

### Requirement: 日志与检索面证书编排

编排 SHALL 为 Elasticsearch 及其全部日志与检索客户端挂载同一份本地证书，并让健康检查通过 CA 与凭据执行。

- Elasticsearch MUST 加载 HTTP 与 transport 证书，MUST 启用 security，MUST NOT 以匿名明文模式启动。
- Logstash、Kibana、Grafana 与 elasticsearch-exporter MUST 挂载本地 CA，并在连接 Elasticsearch 时启用校验。
- ES 凭据 MUST 从 `.env` 或编排环境注入，MUST NOT 出现在 compose 文件、镜像或版本管理中。
- 健康检查 MUST 使用本地 CA 与有效凭据；MUST NOT 使用 `-k`、`--insecure` 或等价跳过校验参数。
- 证书缺失、凭据缺失或信任库不匹配时，容器 MUST 启动失败或健康检查失败，MUST NOT 回退为明文。

#### Scenario: 服务与日志栈都加载证书

- **WHEN** 启动全量容器部署
- **THEN** Elasticsearch 与全部日志客户端容器都加载本地证书
- **AND** 客户端到 Elasticsearch 的连接通过本地 CA 校验

#### Scenario: 证书缺失时失败

- **WHEN** 证书未生成或被移除
- **THEN** Elasticsearch 或其日志客户端启动或健康检查失败
- **AND** 不会以明文模式继续

#### Scenario: 凭据错误时健康检查失败

- **WHEN** 编排注入的 ES 凭据与集群不一致
- **THEN** 依赖 Elasticsearch 的客户端健康检查失败
- **AND** 失败信息指向认证错误

### Requirement: RocketMQ Proxy 的加密通道编排

编排 MAY 为 RocketMQ Proxy 挂载本地证书并开启 TLS，但 MUST 保持其非启动依赖语义与本变更登记的加密边界。

- Proxy MUST 加载本地证书；配置 MUST 明确 TLS 为可选加密，而不是强制入口。
- 业务服务 MUST NOT 依赖 Proxy 启动；Proxy 不可用时审批提交与通知事件持久化 MUST 仍然成功。
- 编排与文档 MUST NOT 将 Proxy 的 TLS 描述为客户端认证、服务端身份校验或零信任链路。
- 强制 TLS MUST 由后续变更或前置 TLS 终止代理引入，MUST NOT 在本变更中伪造该能力。

#### Scenario: Proxy 以 TLS 启动

- **WHEN** 启动核心编排
- **THEN** Proxy 加载本地证书并可完成 TLS 握手
- **AND** 业务服务仍不依赖 Proxy 健康状态启动

#### Scenario: Proxy 不可用不阻塞审批

- **WHEN** Proxy 停止运行
- **THEN** 审批提交成功
- **AND** 通知事件保留在 outbox 中等待补投
