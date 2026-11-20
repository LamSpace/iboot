# Spec Delta

## Purpose

为本地开发与全量容器部署提供一套可重复的传输安全基线：统一的本地 CA 与证书、只接受 TLS 的公开入口、各客户端一致的信任链，以及可验证的证书过期门禁和显式明文回退。

## ADDED Requirements

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
