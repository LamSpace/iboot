# Spec Delta

## ADDED Requirements

### Requirement: 内部服务端口仅接受 TLS

网关、九个业务服务与运维控制台 SHALL 在各自端口上只提供 HTTPS，明文请求 MUST NOT 获得业务响应。

- 每个可部署服务 MUST 加载同一套本地证书并启用 `server.ssl`。
- 服务端口 MUST 协商 TLS 1.2 或 1.3；TLS 1.0 / 1.1 MUST 被拒绝。
- 同一端口 MUST NOT 同时接受明文与 TLS。
- 服务健康检查 MUST 通过 HTTPS 或 TCP 进行，MUST NOT 依赖明文 HTTP。

#### Scenario: 服务端口以 HTTPS 响应

- **WHEN** 使用本地 CA 访问任一服务端口的 Actuator 健康检查
- **THEN** 请求成功
- **AND** 协商结果为 TLS 1.2 或 1.3

#### Scenario: 明文请求失败

- **WHEN** 使用 `http://` 访问任一服务端口
- **THEN** 请求被拒绝或无法完成 HTTP 响应
- **AND** 不返回业务数据

#### Scenario: 旧版 TLS 被拒绝

- **WHEN** 客户端仅提供 TLS 1.0 或 1.1
- **THEN** 握手失败

### Requirement: 内部调用使用统一信任链

门户到网关、服务之间以及服务拉取 JWKS 的调用 SHALL 使用本地 CA 校验服务端证书。

- Java 客户端 MUST 使用本地 PKCS12 信任库；负载均衡与普通 `RestClient` MUST 复用同一份 SSL bundle。
- 客户端 MUST NOT 使用 `SimpleClientHttpRequestFactory` 等不感知 SSL bundle 的默认工厂绕过校验。
- 去掉信任库后调用 MUST 失败；MUST NOT 回退到明文。
- JWKS 地址 MUST 为 HTTPS，资源服务启动与验签不得依赖明文公钥拉取。

#### Scenario: 网关调用服务

- **WHEN** 网关经 HTTPS 转发请求到任一业务服务
- **THEN** 请求成功且服务端证书通过本地 CA 校验

#### Scenario: 服务间调用

- **WHEN** 审批服务调用系统或通知服务、AI 调用系统或调度服务
- **THEN** 调用通过 HTTPS 完成
- **AND** 去掉调用方信任库后调用失败

#### Scenario: JWKS 拉取

- **WHEN** 资源服务首次拉取认证服务公钥集
- **THEN** 请求使用 HTTPS 并通过本地 CA 校验
- **AND** 去掉 CA 后验签初始化失败

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
