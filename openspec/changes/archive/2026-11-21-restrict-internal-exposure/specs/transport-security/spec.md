# Spec Delta

## ADDED Requirements

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
