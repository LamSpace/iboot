# workload-identity Specification

## Purpose
为 Docker Compose 开发与演示环境提供可验证的 SPIFFE 工作负载身份控制面：SPIRE Server/Agent
通过 Workload API 签发 X.509-SVID，身份使用统一信任域 `iboot.local`。

## Requirements

### Requirement: SPIRE Workload API 可签发工作负载身份

环境 SHALL 提供可启动的 SPIRE Server/Agent 与 Workload API，用于签发和轮换 X.509-SVID。

- SPIRE Server 与 Agent MUST 使用独立数据目录与 join token 完成节点证明。
- 工作负载注册 MUST 使用明确的 selector，MUST NOT 使用通配 selector 向所有容器签发身份。
- Agent MUST 通过 Unix socket 提供 Workload API；demo 工作负载 MUST 能获取 SVID。
- SVID MUST 在 URI SAN 中包含 `spiffe://iboot.local/...` 身份；信任域 MUST 与现有证书解析器一致。
- SPIRE MUST 作为 opt-in profile 部署，MUST NOT 改变默认 compose 启动路径或现有 mTLS 信任根。
- 回归 MUST 验证 Server/Agent 健康、SVID 获取、SPIFFE ID 与未注册工作负载无法获取身份。

#### Scenario: Agent 完成节点证明

- **WHEN** 以 join token 启动 SPIRE Agent
- **THEN** Agent 连接 Server 并通过节点证明
- **AND** Workload API socket 可被 demo 工作负载访问

#### Scenario: Demo 工作负载获取 SVID

- **WHEN** 已注册的 demo 工作负载调用 Workload API
- **THEN** 获得 X.509-SVID
- **AND** 证书 URI SAN 为 `spiffe://iboot.local/service/iboot-demo`

#### Scenario: 未注册工作负载被拒绝

- **WHEN** 没有匹配 selector 的工作负载请求 SVID
- **THEN** Workload API 不签发身份

#### Scenario: 现有 mTLS 不受影响

- **WHEN** 未启用 `spire` profile
- **THEN** 默认 compose 与现有证书链保持原状
- **AND** 回归不依赖 SPIRE
