# Spec Delta

## ADDED Requirements

### Requirement: Kafka 客户端使用 SASL 身份与 Topic 授权

Kafka 的客户端监听 SHALL 使用 SASL_SSL；未通过认证的连接 MUST NOT 生产或消费消息。

- 业务日志生产、Logstash 消费、运维工具与节点间通信 MUST 使用不同账号。
- 服务端 MUST 启用授权并默认拒绝：生产账号只能写日志 topic，消费账号只能读该 topic 与自己的消费组。
- 凭据 MUST 从环境或密钥注入，MUST NOT 写入镜像层或提交到仓库。

#### Scenario: 正确账号可生产与消费

- **WHEN** `iboot_log` 生产日志、`iboot_logstash` 消费同一 topic
- **THEN** 生产与消费均成功
- **AND** 客户端与服务端握手使用 TLS

#### Scenario: 无凭据连接被拒绝

- **WHEN** 客户端只配置 SSL 信任库、不提供 SASL 凭据
- **THEN** 连接失败
- **AND** 不产生或读取任何消息

#### Scenario: 越权 Topic 被拒绝

- **WHEN** `iboot_log` 尝试消费日志 topic 或访问其它 topic
- **THEN** 服务端返回授权失败
- **AND** 合法写入仍可继续

#### Scenario: ACL 初始化可重复执行

- **WHEN** 初始化任务重复创建 topic 与 ACL
- **THEN** 命令成功且不产生重复副作用
- **AND** 已有 topic 配置保持不变

### Requirement: Redis 禁用默认用户并使用命名 ACL 账号

Redis MUST 关闭 `default` 用户，并按用途使用命名 ACL 账号；默认用户登录或未认证命令 MUST 被拒绝。

- 业务服务 MUST 使用 `iboot_app`，指标导出 MUST 使用受限命令集的 `iboot_exporter`。
- 口令 MUST 从环境或密钥注入；健康检查与回归脚本 MUST 使用命名账号。
- 认证 MUST 与 TLS 同时生效，MUST NOT 以关闭认证换取连通性。

#### Scenario: 命名账号通过 TLS 认证

- **WHEN** 服务使用 `iboot_app` 与正确口令通过 TLS 连接 Redis
- **THEN** `PING` 返回 `PONG`
- **AND** 业务读写成功

#### Scenario: 默认用户被禁用

- **WHEN** 客户端使用 `default` 用户与任意口令认证
- **THEN** 认证失败
- **AND** 不返回业务数据

#### Scenario: 导出账号权限受限

- **WHEN** `iboot_exporter` 读取 `INFO` 等监控命令
- **THEN** 请求成功
- **AND** 写入或删除键被拒绝
