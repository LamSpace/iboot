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

### Requirement: SVID 刷新周期调度

环境 SHALL 在 SVID 到期前自动刷新，并允许运维巡检调度状态；刷新失败 MUST 留下可观测证据。

- 检查 MUST 覆盖十个服务的 SVID，任一缺失或剩余有效期低于阈值时判定需要刷新。
- 调度 MUST 默认每日执行一次，仅在需要时刷新，MUST NOT 无条件重启服务。
- 宿主机目标 MUST 使用 `spire-svid-sync.sh --refresh`；宿主机服务未运行时 MAY 只刷新文件。
- Compose SVID 栈 MUST 委托 `docker-svid-refresh.sh`，MUST NOT 回退到 mkcert 证书。
- 刷新结果 MUST 写入日志，并暴露最近成功时间、退出码与最小剩余有效期指标。
- 安装、卸载与状态查询 MUST 支持 systemd user timer 与 cron，且重复安装幂等。

#### Scenario: SVID 仍在有效期内

- **WHEN** 调度执行检查且所有 SVID 剩余有效期高于阈值
- **THEN** 不调用刷新脚本
- **AND** 日志记录检查通过，指标退出码为 0

#### Scenario: SVID 即将到期

- **WHEN** 任一 SVID 缺失或剩余有效期低于阈值
- **THEN** 调度按目标调用对应刷新入口
- **AND** 刷新后重新检查，成功才更新最近成功时间

#### Scenario: Compose SVID 栈自动识别

- **WHEN** 存在 `docker-svid-*` sidecar 且未显式指定目标
- **THEN** 调度选择 Compose 目标
- **AND** 通过 `docker-svid-refresh.sh` 刷新并重启，服务继续加载 SVID

#### Scenario: 刷新失败可观测

- **WHEN** 检查或刷新失败
- **THEN** 脚本以非零退出码结束并写入日志
- **AND** 指标退出码为 1，供 Prometheus 告警
