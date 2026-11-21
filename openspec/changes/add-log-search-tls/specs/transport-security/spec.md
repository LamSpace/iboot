# Spec Delta

## ADDED Requirements

### Requirement: Elasticsearch 访问使用 TLS 与认证

Elasticsearch MUST 只通过 HTTPS 提供检索服务；匿名明文请求与未通过本地 CA 校验的请求 MUST NOT 获得数据。

- Elasticsearch MUST 同时为 HTTP 与 transport 监听加载本地证书。
- Spring 客户端 MUST 使用共享 SSL bundle 与本地 CA，并通过环境注入的凭据认证。
- Logstash、Kibana、Grafana 与 elasticsearch-exporter MUST 使用 HTTPS、本地 CA 与凭据；MUST NOT 关闭证书校验。
- 凭据 MUST 通过环境或编排密钥注入，MUST NOT 写入镜像层或提交到仓库。

#### Scenario: Spring 客户端通过 HTTPS 查询

- **WHEN** 监控、AI 或通知服务查询 Elasticsearch
- **THEN** 请求发往 `https://` 地址并通过本地 CA 校验
- **AND** 请求携带有效凭据

#### Scenario: 明文请求被拒绝

- **WHEN** 客户端以 `http://` 访问 Elasticsearch 的 9200 端口
- **THEN** 请求失败且不返回索引数据

#### Scenario: 缺少 CA 时失败

- **WHEN** 客户端不使用本地 CA 访问 Elasticsearch
- **THEN** 证书校验失败且不返回数据

#### Scenario: 凭据错误时拒绝

- **WHEN** 客户端携带错误凭据访问 Elasticsearch
- **THEN** 返回认证失败且不返回索引数据

### Requirement: Kafka 连接使用 TLS

Kafka 的容器内监听、宿主机监听与 controller 监听 MUST 只接受 SSL；明文生产与消费 MUST 被拒绝。

- broker MUST 加载包含私钥的 keystore 与本地 CA 信任库。
- 服务、初始化任务、健康检查与运维脚本 MUST 使用 SSL 客户端配置与本地 CA。
- 客户端 MUST 校验证书与主机名；MUST NOT 使用跳过校验的配置。
- 日志投递失败 MUST 继续降级为本地输出，不得阻塞业务请求。

#### Scenario: 服务经 SSL 投递日志

- **WHEN** 服务向 Kafka 投递结构化日志
- **THEN** 连接使用 SSL 并通过本地 CA 校验
- **AND** 日志可在 Logstash 与 Elasticsearch 中检索到

#### Scenario: 明文客户端被拒绝

- **WHEN** 客户端以 PLAINTEXT 连接任一 Kafka 监听
- **THEN** 连接失败或不产生可消费的消息

#### Scenario: 缺少信任库时失败

- **WHEN** Kafka 客户端未配置 truststore
- **THEN** SSL 握手失败且不发送消息

#### Scenario: broker 不可用时业务不受影响

- **WHEN** Kafka 停止运行
- **THEN** 业务请求仍然成功
- **AND** 控制台与文件日志保持可用

### Requirement: 日志管道全链路使用 TLS

服务到 Kafka、Kafka 到 Logstash、Logstash 到 Elasticsearch 的日志管道 MUST 全程加密，链路上的每一跳都 MUST 使用本地 CA 校验证书。

- Logstash 的 Kafka 输入 MUST 使用 SSL truststore；Elasticsearch 输出 MUST 使用 HTTPS、CA 与凭据。
- Logstash MUST NOT 以关闭证书校验的方式连接 Kafka 或 Elasticsearch。
- 日志探针 MUST 能从服务经 Kafka、Logstash 到达 Elasticsearch，并在 HTTPS 检索中命中。
- 任一中间组件不可用时，服务 MUST 仍可启动并处理请求；恢复后日志 MUST 自动续传。

#### Scenario: 探针贯穿全链路

- **WHEN** 向日志 topic 写入带唯一 traceId 的探针
- **THEN** 该探针经 Logstash 写入 Elasticsearch
- **AND** 通过 HTTPS 检索可以命中该 traceId

#### Scenario: 停 Kafka 后恢复

- **WHEN** Kafka 停止一段时间后恢复
- **THEN** 业务请求成功率不受影响
- **AND** 恢复后的日志继续进入 Elasticsearch

#### Scenario: 停 Logstash 后补齐

- **WHEN** Logstash 停止期间服务继续产生日志，随后 Logstash 恢复
- **THEN** 停止期间的日志从 Kafka 补齐到 Elasticsearch

### Requirement: RocketMQ 只提供加密通道

RocketMQ 链路 MAY 启用 TLS 以防止被动窃听，但 MUST NOT 被描述为具备服务端身份认证或强制加密能力。

- Proxy MUST 能加载本地证书并提供 TLS 握手；客户端 MAY 通过 `enableSsl` 使用加密连接。
- 同一端口接受明文连接与客户端不校验服务端证书 MUST 被登记为已知边界，不得作为验收失败或安全能力宣称。
- 文档与配置命名 MUST 明确其为“加密通道”，MUST NOT 将其纳入零信任身份或 mTLS 范围。
- 强制 TLS、客户端证书认证与服务端身份校验 MUST 留待后续变更或前置 TLS 终止代理解决。

#### Scenario: TLS 握手成功

- **WHEN** 使用本地 CA 对 RocketMQ Proxy 的 gRPC 端口执行 TLS 握手
- **THEN** 握手成功且证书链可被本地 CA 验证

#### Scenario: 加密客户端可投递通知

- **WHEN** workflow 以 `enableSsl=true` 发布通知事件
- **THEN** 事件投递成功且消费端可处理

#### Scenario: 明文仍被接受作为边界记录

- **WHEN** 客户端以明文连接同一 Proxy 端口
- **THEN** 连接仍可能成功
- **AND** 该结果被记录为已知限制，而不是零信任能力

### Requirement: 日志与检索面 TLS 有负向门禁

回归 SHALL 对 Elasticsearch、Kafka 与日志管道执行 TLS 负向检查，明文、缺少 CA 或错误凭据 MUST 导致失败。

- 检查 MUST 覆盖 ES 的明文、无 CA、错误凭据，Kafka 的明文与无 truststore，以及 Logstash 两端连接的证书校验。
- RocketMQ 的“明文仍可连接”MUST 作为边界信息输出，MUST NOT 被计为 TLS 通过。
- 检查 MUST 接入全量回归与 CI，不得只作为手工步骤。
- 任一检查失败 MUST 阻止把环境判定为可验收。

#### Scenario: 明文 ES 被检出

- **WHEN** Elasticsearch 仍接受明文请求
- **THEN** 日志与检索面 TLS 检查失败并指出该端口

#### Scenario: 明文 Kafka 被检出

- **WHEN** Kafka 任一监听仍接受 PLAINTEXT
- **THEN** 检查失败并指出该监听

#### Scenario: 全部 TLS 检查通过

- **WHEN** 在启用日志与检索面 TLS 的全栈环境执行回归
- **THEN** 正向链路与负向用例全部通过
