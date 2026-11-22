# Spec Delta

## MODIFIED Requirements

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

## ADDED Requirements

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
