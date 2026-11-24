# 28-TLS 与零信任当前基线与例外

> 基线日期：2026-11-24　｜　复核日期：2027-02-28　｜　Owner：iBoot Team
>
> 本页是当前阶段 TLS/零信任范围的**唯一清单**。只有“行动项”算未完成；
> “例外登记”是已接受的风险、外部依赖或后续阶段，不再重复计入遗留任务。

## 1. 阶段范围（冻结）

当前阶段 = **本地宿主机开发 + Compose 全量模式**，不包含：

- Kubernetes/ACK 服务网格（阶段 4）；
- 上游组件改造（Seata、RocketMQ、Nacos HTTP、Tempo、Neo4j、Sentinel）；
- 跨主机中心化编排平台。

范围内已闭环：MySQL/Redis/Silo/Nacos gRPC 客户端证书、Nacos 配置命名空间隔离、
OPA 韧性/本地 HA、CA 三阶段轮换 + Docker 滚动重启、备份命令客户端证书、
宿主机与 Compose 全量模式 SPIRE SVID、轮换调度安装（本机 systemd user timer）、
SVID 到期检查与刷新调度（A1，`svid-refresh-schedule.sh`）、
`tls-rotate` 与 Compose SVID 重启路径对齐（A2）、
CA 轮换回滚入口（A3，`tls-ca-rotate.sh --rollback`）、
SPIRE/SVID 监控与告警（A4，`spire-svid-monitor.sh`）、
跨主机调度巡检与 CA 阶段编排（A5，`tls-ca-orchestrate.sh`）。

最终全栈回归：GitHub full-regress `45427875943`（success）；本地全量回归（含 7a、12c–12k）通过。

## 2. 行动项（0 项）

A1–A5 已全部闭环，当前没有未完成行动项。后续新增项必须按 §4 规则先登记。

| # | 行动项 | 验收标准 | 验证 |
|---|---|---|---|
| — | 无 | — | — |

## 3. 例外登记（不计入遗留任务）

| # | 项 | 类型 | 触发条件 / 复核 |
|---|---|---|---|
| E1 | Nacos HTTP API 仅令牌，无客户端证书 mTLS | 上游限制 | 上游支持按工作负载证书或引入网格；复核 2027-02-28 |
| E2 | Seata 无原生 TLS/认证 | 上游限制 | 上游提供原生 TLS/认证；当前靠 `tx` 网络 + 内部令牌 |
| E3 | RocketMQ 客户端证书 mTLS / NameServer 认证 / 兼容层 | 上游限制 | 客户端与 NameServer 能力支持后移除兼容层 |
| E4 | Tempo 9095 进程内明文 | 已接受 | 仅在容器内部回环；开 `grpc_tls_config` 会打断内部调用 |
| E5 | Neo4j 社区版单账号 | 已接受 | 迁移企业版或引入网格后再做每服务账号 |
| E6 | Sentinel 8719 命令端口默认绑定 | 上游限制 | 上游支持绑定地址，或前置回环代理 |
| E7 | SVID 无 IP SAN，宿主机发现用 `localhost` | 条件项 | 需要按 IP 发现/多主机时重新评估 |
| E8 | 多信任域/多有效期证书探针 | 条件项 | 出现多信任域或不同有效期时扩展 |
| E9 | OPA 生产级 Helm/ACK 高可用 | 阶段项 | 迁 K8s 后做超时预算、副本与故障演练 |
| E10 | SPIRE 生产级上游 CA/节点证明/HA/备份 | 阶段项 | 生产部署时接企业 PKI、平台 attestation 与备份演练 |
| E11 | Kubernetes sidecar 网格 | 阶段项 | 阶段 4 选做，当前不引入 |

## 4. 变更规则

1. 当前行动项为 0；E1–E11 不再作为遗留任务重复盘点。
2. 新增项必须先归入“行动项”或“例外登记”，并写明 Owner、触发条件、复核日期。
3. 行动项完成后从本表移除，并在 `docs/27` 对应章节记录验证结果。
4. 阶段范围变化（例如迁 K8s）时重新基线化本页。

## 5. 关联文档

- 操作与排障：[docs/27-TLS 与零信任操作指南](./27-TLS%20与零信任操作指南.md)
- 回归脚本：`scripts/regress/regress.sh` 7a、12c–12k
- OpenSpec 归档：`openspec/changes/archive/`
