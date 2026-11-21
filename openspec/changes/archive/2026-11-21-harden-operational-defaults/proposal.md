# Proposal

## Why

B4.3 把 MySQL、Redis、Silo 与 Nacos 切到了受管凭据，但同一次审计暴露的另外几处默认值和运维缺口仍在：Neo4j 编排里保留 `iboot123456` 兜底口令，Grafana 以匿名 Admin 运行并硬编码 `admin123`，业务服务仍用 `elastic` 超级用户读写 Elasticsearch；证书只剩启动时的一次性检查，没有持续告警，证书过期会让整套环境突然不可用。这些问题成本低、风险直接，适合在进入 B5 工作负载身份之前先收口。

## What Changes

- **Neo4j 凭据**：移除编排与 `.env.example` 中的可用默认口令，改为必填环境变量；缺失时编排在拉起容器前失败。
- **Grafana 控制台**：取消匿名 Admin，匿名角色最多为 Viewer；管理员口令改为必填环境变量，生产覆盖同样生效；端口继续只绑定回环地址。
- **Elasticsearch 最小权限**：新增幂等账号初始化，按用途创建 AI、通知、监控读、日志写、Grafana 与 exporter 账号；业务服务与 Grafana 不再使用 `elastic`，`elastic` 口令只保留在初始化与运维脚本中。
- **证书生命周期告警**：新增 TLS 证书探针与告警规则，在剩余有效期低于 30 天时告警、低于 7 天时升级；保留现有启动与回归检查。
- **明文默认地址审计**：新增回归扫描，应用配置与 Grafana 数据源中的内部依赖默认地址不得是 `http://`；已登记的已知边界（Tempo OTLP、CLI 回环回调）进入显式允许清单，清单只允许缩小。
- **不包含**：Tempo OTLP/HTTP 的 TLS 化、服务间 mTLS、RocketMQ 强制 TLS 与 Seata 认证；这些留待后续批次，其中 Tempo 明文链路在本次审计中被显式登记。

## Capabilities

### New Capabilities

无。

### Modified Capabilities

- `middleware-auth`: 新增 Elasticsearch 应用账号最小权限与 Neo4j 受管凭据要求，明确超级用户口令不得进入业务服务。
- `container-deployment`: 新增可观测控制台默认值要求——Grafana 不得匿名 Admin、管理员口令必须来自环境，Sentinel 非本地部署必须开启登录。
- `transport-security`: 新增证书过期持续告警与明文默认地址回归审计要求。

## Impact

- 编排与配置：`docker/docker-compose.yml`、`docker/docker-compose.prod.yml`、`.env.example`、各服务 `application*.yml`、Grafana provisioning、Prometheus 配置与告警规则。
- 脚本：新增 Elasticsearch 账号初始化脚本、证书探针配置与明文审计回归；更新 `scripts/regress/regress.sh`、`scripts/dev-init.sh`、`scripts/docker-deploy.sh`。
- 运行时：新增一次性 `es-users-init` 与常驻 `blackbox-exporter` 容器；Grafana 匿名权限收紧可能影响门户内嵌看板的可用权限，需要一并验证。
- 文档：`docs/08`、`docs/11`、`docs/23`、`docs/27`、`docker/README.md`。
- CI：回归工作流需要注入新增的 ES 账号口令与 Grafana 管理员口令。
