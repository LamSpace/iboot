# transport-security Specification

## Purpose

为本地开发与全量容器部署提供一套可重复的传输安全基线：统一的本地 CA 与证书、只接受 TLS 的公开入口、各客户端一致的信任链，以及可验证的证书过期门禁和显式明文回退。

## Requirements

### Requirement: 本地 CA 与证书可重复生成

本地开发 CA 与叶子证书 SHALL 由一个可重复执行的命令生成，不依赖手工 `openssl` 步骤；重复执行 MUST 幂等，证书私钥 MUST NOT 纳入版本管理。

- 叶子证书 MUST 覆盖宿主机开发与容器部署所需的全部主机名，至少包含 `localhost`、`127.0.0.1`、`::1` 与编排网络中的服务名。
- 脚本 MUST 生成 Java 可用的 PKCS12 信任库与 nginx、Node、curl 可用的 PEM CA。

#### Scenario: 首次生成

- **WHEN** 在干净检出上执行证书生成命令
- **THEN** 生成 CA、叶子证书与 PKCS12 信任库
- **AND** 叶子证书的 SAN 覆盖 `localhost` 与全部登记的服务名

#### Scenario: 重复执行不破坏现有证书

- **WHEN** 在证书仍然有效时再次执行生成命令
- **THEN** 现有证书与信任库保持可用
- **AND** 不产生需要重启全部服务的非预期变更

#### Scenario: 私钥不入仓

- **WHEN** 检查版本管理中的文件列表
- **THEN** 不包含 CA 私钥、叶子私钥或信任库文件

### Requirement: 公开入口仅接受 TLS

浏览器与 CLI 使用的公开入口 MUST 只通过 HTTPS 提供页面与 API；明文请求 MUST NOT 获得业务响应。

- 公开入口的 HTTP 监听 MUST 以 301 重定向到对应的 HTTPS 地址。
- HTTPS MUST 协商 TLS 1.2 或 1.3；TLS 1.0 / 1.1 MUST 被拒绝。
- 认证端点与令牌端点 MUST NOT 在明文入口上可用。
- HSTS MUST 在本地开发环境启用短时长策略，避免长期锁死本地浏览器。

#### Scenario: HTTP 被重定向

- **WHEN** 客户端访问 `http://` 公开入口
- **THEN** 返回 301 到同一主机的 HTTPS 地址
- **AND** 不返回任何业务数据

#### Scenario: 旧版 TLS 被拒绝

- **WHEN** 客户端仅提供 TLS 1.0 或 1.1
- **THEN** 握手失败
- **AND** TLS 1.2 / 1.3 客户端仍可正常连接

#### Scenario: 明文认证不可用

- **WHEN** 客户端通过明文入口请求授权或令牌端点
- **THEN** 请求被重定向或拒绝
- **AND** 不签发任何令牌

### Requirement: 客户端信任链统一分发

所有需要访问 HTTPS 公开入口的客户端 SHALL 使用同一份本地 CA 作为信任来源。

- Java 客户端 MUST 使用生成的 PKCS12 信任库；Node 客户端 MUST 使用生成的 PEM CA。
- 命令行与回归脚本中的 `curl` MUST 使用 `--cacert`；MUST NOT 使用 `-k` 或关闭校验来绕过失败。
- 健康检查 MUST 使用同一份 CA；MUST NOT 以跳过证书校验作为通过条件。

#### Scenario: Java 客户端信任本地 CA

- **WHEN** CLI 或服务端 Java 客户端带信任库访问 HTTPS 入口
- **THEN** 请求成功
- **AND** 去掉信任库后请求失败

#### Scenario: 回归脚本不使用 -k

- **WHEN** 检查回归脚本与健康检查命令
- **THEN** 不存在 `curl -k`、`--insecure` 或等价的证书校验关闭
- **AND** 所有 HTTPS 请求都显式使用本地 CA

### Requirement: 证书过期有自动门禁

证书生命周期 SHALL 被回归检查覆盖，避免本地环境在证书过期后突然整体不可用。

- 回归检查 MUST 计算叶子证书的剩余有效期。
- 剩余有效期小于 30 天时，检查 MUST 失败并指出证书路径与到期时间。
- 检查失败 MUST 阻止把环境判定为可验收。

#### Scenario: 证书即将过期时失败

- **WHEN** 叶子证书剩余有效期小于 30 天
- **THEN** 回归检查失败并给出生效日期
- **AND** 提示执行重签命令

#### Scenario: 证书有效时通过

- **WHEN** 叶子证书剩余有效期不少于 30 天
- **THEN** 检查通过

### Requirement: 明文回退必须显式开启

系统 SHALL 保留一个显式的明文回退开关，仅用于排障，不得成为默认路径。

- 默认值 MUST 为启用 TLS。
- 关闭 TLS 时 MUST 有明确的启动日志或命令输出，说明当前处于明文模式。
- 验收与发布检查 MUST 在启用 TLS 的状态下执行；明文模式 MUST NOT 被用于通过验收。

#### Scenario: 默认启用 TLS

- **WHEN** 未设置回退开关启动开发环境
- **THEN** 公开入口只提供 HTTPS
- **AND** 日志中不出现明文模式提示

#### Scenario: 显式关闭 TLS

- **WHEN** 显式设置回退开关为关闭
- **THEN** 启动输出明确提示当前为明文模式
- **AND** 该状态不会被回归脚本当作通过 TLS 验收

### Requirement: 公开来源在客户端之间保持一致

同一运行模式下，浏览器、门户、CLI、认证服务与回归脚本 SHALL 使用同一个 HTTPS 公开来源。

- 宿主机开发模式的公开来源 MUST 是门户开发服务器地址；Docker 全量部署模式的公开来源 MUST 是门户 nginx 的 443 地址。
- 认证服务的 issuer、门户回调、登出回调、门户 OIDC 配置、CLI 的 base URL 与 issuer、回归的认证地址 MUST 全部等于该来源的对应地址。
- 混合来源 MUST 被检查发现并以失败结束，不得只告警。

#### Scenario: 宿主机开发来源一致

- **WHEN** 在宿主机开发模式执行登录与回归
- **THEN** 认证发现文档的 issuer、门户回调与 CLI issuer 都指向同一个 HTTPS 开发服务器地址

#### Scenario: Docker 来源一致

- **WHEN** 在 Docker 全量部署模式执行登录与回归
- **THEN** 认证发现文档的 issuer、门户回调与 CLI issuer 都指向 `https://localhost`

#### Scenario: 混合来源被拒绝

- **WHEN** 某个客户端仍指向旧的 `http://localhost:8088`
- **THEN** 回归检查失败并指出该客户端与期望来源

### Requirement: 内部服务端口仅接受 TLS

网关、九个业务服务与运维控制台 SHALL 在各自端口上只提供 HTTPS，明文请求 MUST NOT 获得业务响应。服务端口 SHALL 要求客户端证书，未出示有效客户端证书的 TLS 连接 MUST 失败。

- 每个可部署服务 MUST 加载同一套本地证书并启用 `server.ssl`。
- 服务端口 MUST 要求客户端证书（`client-auth: need`）；客户端证书 MUST 由本地 CA 签发。
- 服务端口 MUST 协商 TLS 1.2 或 1.3；TLS 1.0 / 1.1 MUST 被拒绝。
- 同一端口 MUST NOT 同时接受明文与 TLS。
- 服务健康检查 MUST 通过 HTTPS 或 TCP 进行，MUST NOT 依赖明文 HTTP。

#### Scenario: 服务端口以 HTTPS 响应

- **WHEN** 使用本地 CA 与有效客户端证书访问任一服务端口的 Actuator 健康检查
- **THEN** 请求成功
- **AND** 协商结果为 TLS 1.2 或 1.3

#### Scenario: 明文请求失败

- **WHEN** 使用 `http://` 访问任一服务端口
- **THEN** 请求被拒绝或无法完成 HTTP 响应
- **AND** 不返回业务数据

#### Scenario: 旧版 TLS 被拒绝

- **WHEN** 客户端仅提供 TLS 1.0 或 1.1
- **THEN** 握手失败

#### Scenario: 缺少客户端证书时失败

- **WHEN** 客户端仅使用本地 CA 校验证书、未出示客户端证书访问服务端口
- **THEN** TLS 握手被拒绝或连接立即关闭
- **AND** 不返回任何业务响应

### Requirement: 内部调用使用统一信任链

门户到网关、服务之间以及服务拉取 JWKS 的调用 SHALL 使用本地 CA 校验服务端证书，并由调用方出示自己的客户端证书。

- Java 客户端 MUST 使用本地 PKCS12 信任库；负载均衡与普通 `RestClient` MUST 复用同一份 SSL bundle。
- 出站客户端 MUST 使用调用服务自己的证书作为客户端身份；MUST NOT 借用其它服务的证书。
- 客户端 MUST NOT 使用 `SimpleClientHttpRequestFactory` 等不感知 SSL bundle 的默认工厂绕过校验。
- 去掉信任库或客户端证书后调用 MUST 失败；MUST NOT 回退到明文。
- JWKS 地址 MUST 为 HTTPS，资源服务启动与验签不得依赖明文公钥拉取。

#### Scenario: 网关调用服务

- **WHEN** 网关经 HTTPS 转发请求到任一业务服务
- **THEN** 请求成功
- **AND** 网关出示 `iboot-gateway` 证书，服务端证书通过本地 CA 校验

#### Scenario: 服务间调用

- **WHEN** 审批服务调用系统或通知服务、AI 调用系统或调度服务
- **THEN** 调用通过 HTTPS 完成
- **AND** 调用方出示自己的服务证书

#### Scenario: JWKS 拉取

- **WHEN** 资源服务首次拉取认证服务公钥集
- **THEN** 请求使用 HTTPS 并通过本地 CA 校验
- **AND** 调用方出示自己的服务证书

#### Scenario: 去掉客户端证书后失败

- **WHEN** 调用方保留本地 CA 但不提供客户端证书
- **THEN** 服务端拒绝 TLS 连接
- **AND** 调用失败

### Requirement: MySQL 连接使用 TLS

服务与迁移执行者连接 MySQL SHALL 使用 TLS，数据库服务端 MUST 拒绝明文连接。

- MySQL 服务端 MUST 启用 TLS 并设置 `require_secure_transport=ON`。
- 客户端 MUST 使用 `VERIFY_IDENTITY` 或等价的主机名校验，并使用本地 CA。
- Druid 连接池初始化 MUST 在 TLS 下成功；MUST NOT 使用 `useSSL=false` 或关闭证书校验。

#### Scenario: 加密连接建立

- **WHEN** 服务通过 Druid 建立数据库连接
- **THEN** `SHOW STATUS LIKE 'Ssl_cipher'` 返回非空值
- **AND** 连接使用本地 CA 校验

#### Scenario: 明文连接被拒绝

- **WHEN** 客户端以 `sslMode=DISABLED` 连接 MySQL
- **THEN** 服务端拒绝连接
- **AND** 返回明确的“禁止不安全传输”错误

### Requirement: Redis 连接使用 TLS

服务连接 Redis SHALL 使用 TLS，Redis 服务端 MUST NOT 提供明文端口。

- Redis MUST 只监听 TLS 端口，或将其余端口关闭。
- 客户端 MUST 使用 `rediss` 语义并通过本地 CA 校验。
- 健康检查与运维脚本 MUST 使用 TLS；MUST NOT 用明文 `redis-cli` 判定健康。

#### Scenario: TLS 连接成功

- **WHEN** 服务通过 Lettuce 连接 Redis
- **THEN** `PING` 返回 `PONG`
- **AND** 连接使用本地 CA 校验

#### Scenario: 明文连接失败

- **WHEN** 客户端以明文连接 Redis 端口
- **THEN** 连接被关闭或拒绝

### Requirement: Silo 访问使用 TLS

服务与浏览器访问 Silo SHALL 使用 HTTPS，明文请求 MUST NOT 返回对象内容。

- Silo 服务端 MUST 加载本地证书，内部与公开 endpoint MUST 使用 `https://`。
- 预签名 URL MUST 以 HTTPS 公开地址签发，浏览器能够用本地 CA 校验。
- 健康检查与初始化脚本 MUST 使用本地 CA；MUST NOT 跳过证书校验。

#### Scenario: 内部读写使用 HTTPS

- **WHEN** 服务上传或读取对象
- **THEN** 请求发往 `https://` 内部 endpoint
- **AND** 服务端证书通过本地 CA 校验

#### Scenario: 预签名 URL 可打开

- **WHEN** 浏览器打开 HTTPS 预签名下载 URL
- **THEN** 证书受信并返回对象内容

#### Scenario: 明文请求被拒绝

- **WHEN** 客户端以 `http://` 访问 Silo
- **THEN** Silo 拒绝请求或不返回对象内容

### Requirement: Neo4j 连接使用 TLS

启用知识图谱时，服务连接 Neo4j SHALL 使用加密的 Bolt 或 Neo4j 协议。

- 连接 URI MUST 使用 `bolt+s://` 或 `neo4j+s://`。
- 驱动 MUST 使用本地 CA 校验服务端证书。
- 明文 `bolt://` 连接 MUST 被拒绝。

#### Scenario: 加密 Bolt 连接

- **WHEN** 知识图谱同步以 `bolt+s://` 连接 Neo4j
- **THEN** 连接建立并完成一次幂等同步

#### Scenario: 明文 Bolt 被拒绝

- **WHEN** 客户端以 `bolt://` 连接 Neo4j 的加密端口
- **THEN** 连接失败

### Requirement: 内部 TLS 有负向门禁

回归 SHALL 对内部服务端口与数据面执行 TLS 负向检查，明文或缺少 CA 必须导致失败。

- 检查 MUST 覆盖网关、全部业务服务端口、JWKS 与四个数据面组件。
- 去掉 CA、使用错误 CA 或改用明文时，对应用例 MUST 失败。
- 内部 TLS 检查 MUST 接入全量回归与 CI，不得只作为手工步骤。

#### Scenario: 明文端口被检出

- **WHEN** 任一服务端口仍接受明文
- **THEN** 内部 TLS 检查失败并指出该端口

#### Scenario: 缺少 CA 被检出

- **WHEN** 内部调用未携带本地 CA 或携带错误 CA
- **THEN** 检查失败并指出证书校验错误

#### Scenario: 内部 TLS 全部通过

- **WHEN** 在启用内部 TLS 的全栈环境执行回归
- **THEN** 正向链路与负向用例全部通过

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

### Requirement: Nacos 注册与配置链路使用 TLS

Nacos 的 HTTP API、控制台与 gRPC 注册/配置通道 MUST 只接受 TLS；gRPC MUST 要求客户端证书，明文或无证书请求 MUST NOT 获得业务响应。

- Nacos MUST 为 8848 与控制台端口加载 PKCS12 keystore，并为 9848 启用 gRPC TLS。
- 客户端 MUST 使用本地 CA 校验证书与主机名，并提供客户端证书；MUST NOT 使用 `trustAll=true`。
- 健康检查与运维脚本 MUST 使用 `--cacert`；MUST NOT 使用 `-k` 或跳过校验。
- Nacos 证书路径 MUST 带 `file:` 前缀；客户端 TLS 属性 MUST 在 JVM 启动前注入。

#### Scenario: Nacos HTTPS API 可用

- **WHEN** 使用本地 CA 访问 Nacos 的 8848 或控制台端口
- **THEN** 返回 200
- **AND** 证书通过本地 CA 校验

#### Scenario: Nacos 明文请求失败

- **WHEN** 客户端以 `http://` 访问 Nacos 的 8848 或控制台端口
- **THEN** 请求失败且不返回业务数据

#### Scenario: gRPC TLS 握手成功

- **WHEN** 使用本地 CA 对 9848 执行 TLS 握手
- **THEN** 握手成功且 `Verify return code` 为 0

#### Scenario: 客户端缺少证书时失败

- **WHEN** Nacos 客户端未配置 `trustCollectionChainPath`、客户端证书或使用错误 CA
- **THEN** 注册与配置拉取失败

### Requirement: 编排发布的内部端口仅绑定回环地址

Docker 编排发布到宿主机的内部端口 MUST 只绑定回环地址，MUST NOT 对宿主机其它网络接口提供监听；宿主机开发模式下由开发者直接启动的进程不在本条范围内。

- 业务服务、运维控制台、MySQL、Redis、Silo、Elasticsearch、Nacos、Seata、Kafka、RocketMQ Proxy、Neo4j 与可观测 UI 的容器发布端口 MUST 绑定 `127.0.0.1`。
- 通过宿主机非回环地址发起连接 MUST 失败，即使目标端口在回环地址上可用。
- 公开入口 MUST 只有门户的 80 与 443；新增任何全接口绑定 MUST 被视为安全回归。
- 宿主机开发 MUST 使用 `localhost` 或 `127.0.0.1` 访问内部端口，MUST NOT 使用机器 LAN 地址。

#### Scenario: 非回环地址无法访问内部端口

- **WHEN** 使用宿主机的非回环 IP 连接 MySQL、Redis、Elasticsearch 或 Nacos 的已发布端口
- **THEN** 连接失败
- **AND** 使用 `localhost` 的同一连接成功

#### Scenario: 内部端口不监听全部网卡

- **WHEN** 检查编排的容器端口映射
- **THEN** 内部端口只出现在回环地址上
- **AND** 只有门户 80/443 监听 `0.0.0.0`

#### Scenario: 宿主机开发不依赖 LAN 地址

- **WHEN** 按文档以宿主机进程方式启动并连接中间件
- **THEN** 所有地址均为 `localhost` 或 `127.0.0.1`
- **AND** 不出现指向本机 LAN IP 的配置

### Requirement: 公开入口唯一性有回归门禁

回归 SHALL 校验「公开入口唯一」这一边界；任何内部端口的额外全接口绑定 MUST 导致验收失败。

- 检查 MUST 枚举编排发布到宿主机的端口及其绑定地址。
- 出现门户 80/443 之外的 `0.0.0.0` 绑定 MUST 失败，并指出端口与服务。
- 检查 MUST 与 TLS 回归一起执行，MUST NOT 以关闭校验或跳过检查的方式通过。

#### Scenario: 额外的公开端口被检出

- **WHEN** 任一内部端口被误配置为绑定 `0.0.0.0`
- **THEN** 回归失败并指出该端口
- **AND** 环境不得被判定为零信任边界验收通过

#### Scenario: 暴露面符合预期

- **WHEN** 只有门户 80/443 绑定 `0.0.0.0`
- **THEN** 暴露面检查通过
- **AND** 服务与数据面仍可通过回环地址完成宿主机开发与回归

### Requirement: 证书过期必须被持续监控并告警

叶子证书的剩余有效期 SHALL 持续暴露为指标，并在临近过期时产生可投递的告警，而不只依赖启动或单次回归检查。

- 剩余有效期 MUST 以 Prometheus 指标形式持续采集，覆盖共享叶子证书与公开入口。
- 剩余有效期小于 30 天时 MUST 触发 warning 级告警，小于 7 天时 MUST 触发 critical 级告警。
- 证书探针自身失败时 MUST 单独告警，MUST NOT 让“证书已过期”与“探针未运行”都表现为无数据。
- 后续引入的独立证书 MUST 纳入同一套探针与告警，MUST NOT 只登记在文档中。

#### Scenario: 证书进入 30 天窗口

- **WHEN** 叶子证书剩余有效期少于 30 天
- **THEN** 产生包含证书名称与到期时间的 warning 告警
- **AND** 告警可通过既有 Alertmanager 通道投递

#### Scenario: 证书进入 7 天窗口

- **WHEN** 叶子证书剩余有效期少于 7 天
- **THEN** 告警级别升级为 critical

#### Scenario: 探针失效可被发现

- **WHEN** 证书探针目标不可达或探针未运行
- **THEN** 产生探针失败告警
- **AND** 该状态不会被误判为证书正常

### Requirement: 内部依赖的默认地址不得回退明文

应用配置与数据源配置中的内部依赖默认地址 SHALL 使用加密协议；回归 MUST 扫描并阻止新增明文默认地址。

- 服务到中间件、服务到服务、数据源到后端的默认地址 MUST 使用 `https://` 或对应的加密 scheme。
- 回归 MUST 扫描应用配置与数据源配置，发现未登记的 `http://` 默认地址时失败。
- 允许清单 MUST 为每条例外登记用途、不可加密原因与后续处理去向；清单只允许缩小，新增例外 MUST 经评审。
- 扫描 MUST 与现有 TLS 负向检查同时执行，MUST NOT 用静态扫描替代实际连接验证。

#### Scenario: 新增明文默认地址被阻止

- **WHEN** 某个服务的配置把内部依赖默认地址写成 `http://`
- **THEN** 回归失败并指出文件、键与地址
- **AND** 该变更不能通过验收

#### Scenario: 已登记边界被显式放行

- **WHEN** 扫描命中允许清单中登记的回环回调或已知边界
- **THEN** 回归通过但在输出中列出该例外
- **AND** 例外条目仍可在允许清单中追溯到原因

#### Scenario: 加密默认地址通过

- **WHEN** 配置中的内部依赖默认地址为 `https://` 或其它加密 scheme
- **THEN** 扫描通过
- **AND** 实际连接仍由 TLS 回归验证

### Requirement: Tempo 接收与查询链路使用 TLS

Tempo 的 OTLP/HTTP、OTLP/gRPC 接收端口与 HTTP 查询端口 SHALL 只接受 TLS；明文上报或查询 MUST NOT 获得成功响应。

- 应用上报地址与 Grafana 数据源 MUST 使用 `https://`，并通过本地 CA 校验服务端证书。
- 客户端缺少 CA、使用错误 CA 或跳过证书校验时 MUST 失败，MUST NOT 回退到明文。
- Tempo 内部 gRPC 服务端口 MUST NOT 发布到宿主机，其明文边界 MUST 在文档中登记。

#### Scenario: 加密上报与查询成功

- **WHEN** 服务通过 `https://tempo:4318/v1/traces` 上报 span，且 Grafana 通过 `https://tempo:3200` 查询
- **THEN** 两条请求均成功
- **AND** 服务端证书通过本地 CA 校验

#### Scenario: 明文上报被拒绝

- **WHEN** 客户端以 `http://` 访问 4318 或 3200
- **THEN** 请求不获得成功响应

#### Scenario: 缺少 CA 时握手失败

- **WHEN** 客户端使用空信任库访问 Tempo 的 TLS 端口
- **THEN** TLS 证书校验失败
- **AND** 回归判定为失败

### Requirement: 可观测查询与控制台只提供 TLS

Grafana、Prometheus、Alertmanager、Thanos Query 与 Kibana 的 HTTP 入口 SHALL 只提供 TLS；明文请求 MUST NOT 获得页面或查询结果。

- 各组件 MUST 加载本地叶子证书，并通过本地 CA 校验上游连接。
- Grafana 数据源、Thanos sidecar 到 Prometheus、Prometheus 到 Alertmanager MUST 使用 `https://`。
- 数据库中登记的控制台地址 MUST 使用 `https://`。
- Sentinel Dashboard 的明文回环入口 MUST 作为已登记边界，不得计入 TLS 覆盖。

#### Scenario: 控制台 HTTPS 可用

- **WHEN** 使用本地 CA 访问五个控制台的 HTTPS 地址
- **THEN** 页面或健康端点返回成功
- **AND** 证书通过本地 CA 校验

#### Scenario: 明文请求被拒绝

- **WHEN** 客户端以 `http://` 访问任一控制台端口
- **THEN** 请求失败或重定向到 HTTPS
- **AND** 不返回页面或查询数据

#### Scenario: 数据源与告警链路加密

- **WHEN** Grafana 查询 Prometheus/Thanos，或 Prometheus 向 Alertmanager 投递告警
- **THEN** 连接使用 HTTPS
- **AND** 服务端证书通过本地 CA 校验

#### Scenario: 明文允许清单收尾

- **WHEN** 执行明文默认地址审计
- **THEN** 仅剩 CLI 回环回调这一条例外
- **AND** 新增控制台明文地址会导致审计失败

### Requirement: 每个可部署服务使用独立服务端证书

十个可部署服务 SHALL 各自加载独立的证书；客户端 MUST 能用服务名校验证正在连接的服务，服务端也能把该证书当作该服务的客户端身份。

- 每个服务证书的 SAN MUST 包含该服务名，并包含 `localhost`、`127.0.0.1`、`::1` 与 `host.docker.internal` 以支持宿主机调试与探针。
- 服务证书 MUST 由同一套本地 CA 签发，使现有信任链无需变更。
- 服务证书 MUST 同时具备 `serverAuth` 与 `clientAuth` 用途；缺失 `clientAuth` 时 MUST 重新签发。
- 服务端 MUST NOT 继续使用共享门户证书提供业务端口。
- 客户端证书认证 MUST 在本变更中强制启用，旧证书 MUST 被重新签发后再上线。

#### Scenario: 客户端按服务名校验证成功

- **WHEN** 客户端使用本地 CA 连接 `https://iboot-system:8080` 并校验主机名
- **THEN** 证书校验通过
- **AND** 返回的证书 SAN 包含 `iboot-system`

#### Scenario: 服务证书不可互换

- **WHEN** 比较任意两个服务的证书指纹
- **THEN** 两者不相同

#### Scenario: 服务证书可作为客户端身份

- **WHEN** 检查任一服务证书的用途
- **THEN** `SSL server` 与 `SSL client` 均为可用
- **AND** 该证书可用于访问其它服务端口

#### Scenario: 端口仍接受 TLS 且拒绝明文

- **WHEN** 回归检查 8080–8089 的端口
- **THEN** 带客户端证书的 TLS 1.2 握手与健康检查通过
- **AND** 明文请求不获得业务响应

### Requirement: 每服务工作负载身份由客户端证书证明

每个调用方 SHALL 在访问业务服务时出示与自身身份对应的证书；服务端 SHALL 只接受由本地 CA 签发的客户端证书。

- 十个业务服务 MUST 使用自己的服务证书作为出站客户端证书。
- 门户 nginx 与宿主机 Vite 代理 MUST 使用门户证书作为客户端身份。
- Prometheus 与 blackbox MUST 使用各自的探针客户端证书抓取服务端口与执行证书探针。
- 服务端 MUST 通过本地 CA 校验证书链；本变更 MUST NOT 宣称已按调用方做授权或最小权限控制。

#### Scenario: 服务间调用携带各自身份

- **WHEN** 任一服务调用另一个服务
- **THEN** 调用方出示自己的服务证书
- **AND** 服务端信任链校验通过

#### Scenario: 门户链路携带门户身份

- **WHEN** 浏览器经门户访问网关、系统服务或运维控制台
- **THEN** 门户向上游出示门户证书
- **AND** 上游接受该证书并返回业务响应

#### Scenario: 探针携带探针身份

- **WHEN** Prometheus 抓取服务指标或 blackbox 执行证书探针
- **THEN** 请求携带对应的探针客户端证书
- **AND** 抓取与探针成功

#### Scenario: 未做按调用方授权

- **WHEN** 使用任一由本地 CA 签发的服务证书访问另一个服务端口
- **THEN** TLS 层接受该证书
- **AND** 该结果被登记为“已认证但未按调用方授权”的边界

### Requirement: 服务 mTLS 有负向门禁

回归 SHALL 对服务端口、网关与门户链路执行双向 TLS 负向检查；缺少客户端证书或错误 CA MUST 导致失败。

- 检查 MUST 覆盖十个服务端口、网关 8088 与 JWKS。
- 无客户端证书、错误客户端证书或去掉 CA 时，对应用例 MUST 失败。
- 检查 MUST 验证每个服务证书具备 `clientAuth` 用途且指纹唯一。
- 检查 MUST 接入全量回归与 CI，不得只作为手工步骤。
- 任一检查失败 MUST 阻止把环境判定为可验收。

#### Scenario: 无客户端证书被检出

- **WHEN** 任一服务端口仍接受不带客户端证书的 TLS 连接
- **THEN** 内部 TLS 检查失败并指出该端口

#### Scenario: 缺少 `clientAuth` 被检出

- **WHEN** 任一服务证书不含 `clientAuth` 用途
- **THEN** 证书检查失败并指出证书路径

#### Scenario: 双向 TLS 全部通过

- **WHEN** 在启用服务 mTLS 的全栈环境执行回归
- **THEN** 正向链路、门户链路与负向用例全部通过
