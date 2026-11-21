# Spec Delta

## ADDED Requirements

### Requirement: 宿主机端口按暴露等级绑定

部署 SHALL 只把门户的 HTTP/HTTPS 端口绑定到全部网卡，其余发布到宿主机的端口 MUST 只绑定回环地址；不需要宿主机访问的端口 MUST NOT 发布。

- 门户的 80 与 443 MUST 绑定 `0.0.0.0`，作为唯一跨网卡入口。
- 业务服务、中间件、控制台与可观测 UI 的宿主端口 MUST 绑定 `127.0.0.1`。
- Logstash、SkyWalking Banyandb、Thanos 组件、各 exporter 与 SkyWalking OAP 的内部端口 MUST NOT 发布到宿主机。
- 宿主机开发 MUST 继续通过 `localhost` 访问其所需的中间件与服务端口；MUST NOT 依赖机器 LAN 地址。

#### Scenario: 只有门户对其它主机可见

- **WHEN** 从同一网段的另一台主机访问本机端口
- **THEN** 只有 80 与 443 可连接
- **AND** MySQL、Redis、Elasticsearch、Nacos、Kafka 与服务端口均不可连接

#### Scenario: 宿主机开发仍可访问中间件

- **WHEN** 宿主机进程通过 `localhost` 连接 MySQL、Redis、Silo、Elasticsearch、Nacos、Seata、Kafka 或 RocketMQ Proxy
- **THEN** 连接成功
- **AND** 无需修改现有地址或新增启动参数

#### Scenario: 内部端口不再发布

- **WHEN** 检查编排容器的端口映射
- **THEN** Logstash、SkyWalking Banyandb、Thanos 组件与 exporter 没有宿主机映射
- **AND** Prometheus 在编排网络内完成抓取

### Requirement: 容器网络按角色分段

编排 SHALL 把容器划分到 `edge`、`app`、`data` 与 `obs` 网络，每个服务 MUST 只加入其职责所需的最少网络。

- 门户 MUST 只加入 `edge`，并 MUST NOT 加入承载数据面的网络。
- 网关 MUST 加入 `edge` 与 `app`；需要注册中心或配置中心的组件 MUST 同时加入 `data`。
- 业务服务 MUST 加入 `app`，并按实际依赖加入 `data`。
- MySQL、Redis、Silo、Elasticsearch、Nacos、Seata、RocketMQ、Kafka 与 Tempo MUST 只加入 `data`。
- 可观测组件 MUST 按抓取或存储关系加入 `obs`，需要访问数据面时再加入 `data`。

#### Scenario: 门户不能直达数据面

- **WHEN** 从门户容器尝试连接 MySQL、Redis、Elasticsearch 或 Nacos
- **THEN** 连接失败
- **AND** 门户仍能通过 `edge` 访问网关、系统服务与运维控制台

#### Scenario: 业务服务可访问其依赖

- **WHEN** 业务服务连接 MySQL、Redis、Nacos、Silo 或其调用的其它服务
- **THEN** 连接成功
- **AND** 不因网络分段出现解析或超时错误

#### Scenario: 可观测组件仍能抓取与存储

- **WHEN** Prometheus 抓取业务服务与 exporter，Grafana 查询 Elasticsearch 与 Prometheus
- **THEN** 抓取与查询成功
- **AND** Thanos 与 Tempo 仍可读写 Silo

### Requirement: 暴露面回归门禁

回归 SHALL 检查宿主机端口绑定、非回环地址可达性与容器网络连通矩阵，任一不符合 MUST 导致失败。

- 检查 MUST 断言只有门户 80/443 绑定 `0.0.0.0`，其余已发布端口绑定回环地址。
- 检查 MUST 断言通过宿主机非回环地址访问内部端口失败。
- 检查 MUST 在容器运行时执行门户到数据面的负向连通性，以及业务服务到依赖的正向连通性。
- 容器不可用时 MUST 明确跳过容器矩阵，但仍执行端口绑定检查。
- 检查 MUST 接入全量回归与 CI，不得只作为手工步骤。

#### Scenario: 内部端口意外公开

- **WHEN** 任一内部端口绑定到 `0.0.0.0`
- **THEN** 暴露面检查失败并指出该端口
- **AND** 环境不得被判定为可验收

#### Scenario: 网络分段被破坏

- **WHEN** 门户容器能够连接 MySQL、Redis、Elasticsearch 或 Nacos
- **THEN** 网络矩阵检查失败并指出可达的目标
- **AND** 不会只告警后继续

#### Scenario: 容器未运行

- **WHEN** 宿主机开发模式只运行了部分容器
- **THEN** 端口绑定检查仍然执行
- **AND** 容器网络矩阵被明确标记为跳过
