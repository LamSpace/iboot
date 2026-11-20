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
