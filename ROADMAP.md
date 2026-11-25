# iBoot 演进路线图

> 本文只保留**尚未完成**的路线与**仍然生效**的约束；已完成能力见五条 Native 线的「已完成」与 [§3 业务能力基线](#3-业务能力基线)，
> 用户视角的功能清单与启动方式见 [README](README.md)，每次交付的完整过程见 `openspec/changes/archive/`。
> 项目性质：技术栈练习项目，在可运行的管理系统之上把 **Agent / Cloud / Security / Micro Service / SRE Native** 五种能力做成系统默认属性。
> 每个条目都带验收判据，判据不成立的条目不进路线图。最近更新：2026-11-25

## 目录

1. [目标与完成判据](#1-目标与完成判据)
2. [五条 Native 总览](#2-五条-native-总览)
3. [业务能力基线](#3-业务能力基线)
4. [Agent Native](#agent-native)
5. [Cloud Native](#cloud-native)
6. [Security Native](#security-native)
7. [Micro Service Native](#micro-service-native)
8. [SRE Native](#sre-native)
9. [版本矩阵](#9-版本矩阵)
10. [风险登记](#10-风险登记)
11. [本地开发环境与成本红线](#11-本地开发环境与成本红线)
12. [决策记录](#12-决策记录)
13. [外部事实来源](#13-外部事实来源)

---

## 1. 目标与完成判据

| 能力线 | 本项目的定义 | 完成判据 |
|---|---|---|
| **Agent Native** | AI 是系统的一等执行单元，不是附加功能：对话、RAG、智能体编排、知识图谱，以及技能与外部工具接入 | Agent 能安全调用既有 API；工具治理、权限、审计、回放闭环；技能可复用；MCP 接入不绕过权限 |
| **Cloud Native** | 所有服务容器化、Helm 化，运行在阿里云 ACK，具备弹性、可观测、声明式发布 | `helmfile sync` 一条命令发布；滚动升级零 5xx；压测触发 HPA；删 Pod 自愈；GitOps 漂移可检测 |
| **Security Native** | 每次调用都被验证，不因来自内网而信任 | OAuth2/OIDC + 下游独立验签；mTLS/SPIFFE；路径级授权；每服务最小权限；Token Exchange；密钥不入仓 |
| **Micro Service Native** | 服务边界由领域模型驱动，并且边界质量可验证 | 业务服务独立启停；表所有权清晰；Seata/outbox 闭环；契约测试；EventStorming 复盘；无分布式单体信号 |
| **SRE Native** | 告警和运维动作由 SLO/错误预算驱动 | SLI/SLO/错误预算；燃烧率告警；每条告警有 Runbook；Trace-Log 关联；成本可见 |

约束与原则：

1. **验收判据优先**：无判据的条目不写。写代码前先定义「什么算完成」。
2. **YAGNI**：与主线弱相关、或职责与已选方案重叠的条目，显式剔除而非长期挂账。
3. **复用既有基础设施优先**：已有 ES、Silo、Redis、Quartz、SSE、CloudEvents、二级缓存、可观测栈——先榨干存量，再引入新中间件。
4. **不破坏 DDD 边界**：新增能力必须待在既有四层结构内，否则后续拆分成本被推高。
5. **前端契约稳定**：portal 只访问网关 `/api/**`，接口归属变化只改路由表。
6. **不为练手而降级主线**：工程已站在 Boot 4.0.3 上，不为了迁就旧版生态而退回已 EOL 的 Boot 3.5。
7. **成本可见**：任何消耗云账单或本机内存的能力，必须自带用量观测。
8. **五条线共享同一套服务与基础设施**：新增能力必须说明落在哪条线、依赖哪条线，不另起一套平行栈。

---

## 2. 五条 Native 总览

| 线 | 定位 | 当前成熟度 |
|---|---|---|
| **Agent Native** | AI 是系统的一等执行单元 | 对话、RAG、编排、图谱已落地；技能库、MCP、图编排未开始 |
| **Cloud Native** | 运行环境由声明式配置定义，可复现、可审计 | 本地 Compose 已就位；K8s/Helm/CI/CD 未开始 |
| **Security Native** | 每次调用都被验证 | B0–B6 已完成；Token Exchange、多客户端会话未开始 |
| **Micro Service Native** | 边界由领域模型驱动且可验证 | 拆分与治理已完成；契约测试、边界复盘未开始 |
| **SRE Native** | 告警和运维动作由 SLO/错误预算驱动 | 可观测栈已完成；SLO/燃烧率未开始 |

跨线依赖：

- Cloud Native 是 SRE 上云与 Security 网格化的运行底座。
- Security Native 的 Token Exchange、多客户端会话可先于 Cloud Native 落地。
- Agent Native 的跨服务 Agent 化依赖 Micro Service Native 的边界与 Security Native 的授权。
- Micro Service Native 的契约测试不依赖任何基础设施，可立即开始。

历史阶段对照（保留已完成记录，不丢失旧编号的上下文）：

| 历史阶段 | 主要落点 |
|---|---|
| 阶段 1 AI 工作台 | Agent Native |
| 阶段 2 微服务拆分 | Micro Service Native + Security Native + SRE Native |
| 阶段 2 收尾 TLS / 零信任 | Security Native |
| 阶段 3 云原生 | Cloud Native + SRE Native |
| 阶段 4 深水区 | 按条目分散到五条线 |

---

<a id="2-能力索引"></a>
## 3. 业务能力基线

已完成业务功能，一项一句话。工程能力（认证、可观测性、服务治理、分布式事务、TLS 等）不在此表重复，
分别见五条 Native 线的「已完成」。

| 能力 | 一句话 | 归属服务 | 工程归属 | 文档 |
|---|---|---|---|---|
| 系统与组织 | 用户、角色、菜单、字典、参数，以及部门树、岗位与组织架构图 | `iboot-system` | Micro Service Native（业务负载） | — |
| 消息与推送 | 站内消息、公告、模板、多渠道投递，SSE 单播/组播/广播与集群广播 | `iboot-notify` | Micro Service Native + SRE Native | [docs/20](<./docs/20-多渠道通知操作指南.md>)、[docs/15](<./docs/15-SSE 推送集成.md>) |
| 文件管理 | 上传、下载、预览、分类，Silo / 本地 / OSS 存储抽象 | `iboot-file` | Micro Service Native | — |
| 定时任务 | Quartz 集群任务 CRUD、启停与执行日志 | `iboot-job` | Micro Service Native | — |
| 数据备份 | 全量逻辑备份、AES-256-GCM 加密、独立演练库恢复校验 | `iboot-job` | SRE Native | [docs/21](<./docs/21-数据备份与恢复操作指南.md>) |
| 审批工作流 | Flowable 8 请假流程、审批中心与通知登记 | `iboot-workflow` | Micro Service Native | — |
| 统计分析 | 仪表板、趋势统计与 Excel 导出 | `iboot-system` | Micro Service Native | — |
| 国际化 | 前端 vue-i18n + 后端 MessageSource 中英文语言包 | `iboot-portal` + 各服务 | 跨线基线 | [docs/17](<./docs/17-国际化设计.md>) |

---

<a id="agent-native"></a>
## 4. Agent Native

### 4.1 定位与完成判据

让 AI 从「一个服务」变成「贯穿系统的执行单元」：Agent 能调用既有 API、使用可复用技能、接入外部工具，
同时工具权限、数据权限、审计与回放保持闭环。

- Agent 能安全调用既有管理端 API，权限与数据范围继承调用者。
- 工具调用有白名单、写操作二次确认、审计与确定性回放。
- 技能可复用；技能只扩提示词，不扩工具集，不执行代码。
- 外部 MCP 工具不能绕过 `AgentTool` 权限模型。
- RAG 检索结果继承用户数据权限。

### 4.2 已完成

| 能力 | 一句话 | 归属服务 | 文档 |
|---|---|---|---|
| AI 对话 | 多轮流式对话、Redis 会话记忆、每日配额与 token 观测 | `iboot-ai` | [docs/22](<./docs/22-AI 工作台操作指南.md>) |
| 知识库 RAG | PDF/DOCX/TXT 解析、分块、Embedding、BM25 + kNN + RRF、引用溯源 | `iboot-ai` | [docs/22](<./docs/22-AI 工作台操作指南.md>) |
| 智能体编排 | 受控工具调用、写操作二次确认、运行审计与确定性回放 | `iboot-ai` | [docs/22](<./docs/22-AI 工作台操作指南.md>) |
| 知识图谱 | 组织与 RBAC 入图、多跳查询与名称解析 | `iboot-ai` | [docs/22](<./docs/22-AI 工作台操作指南.md>) |

### 4.3 未完成路线

A1/A2 来自 2026-10-30 的 SKILL/MCP 可行性分析：拆分与 OAuth2 已完成，前置已解除；技能库先于 MCP，不交叉。

| ID | 内容 | 技术 / 约束 | 时机 | 验收判据 |
|---|---|---|---|---|
| A1 | 技能库（SKILL） | 内置技能留 classpath，不灌 DB；`ai_skill` 表存用户内容（GLOBAL/PRIVATE + owner + 状态）；v1 只收单文件 SKILL.md 纯文本；单步状态机 DRAFT → PENDING → APPROVED/REJECTED → PUBLISHED → SUSPENDED，不用 Flowable；四眼原则服务端强制；权限码 `ai:skill:use/create/publish/review/manage`；私有技能数量上限 | 下一步 | 提交者用自己的 token 调 `/skills/{id}/review` 被拒且状态不变；无 `ai:skill:review` 返回 403；第二账号审核通过 → PUBLISHED 且审计记录审核人；原作者编辑回到 PENDING 并立即从他人技能集消失；管理员下架立即消失 |
| A2 | MCP 客户端 | `McpAgentToolAdapter implements AgentTool`；MCP 服务自报 readOnlyHint 不可信，默认按写操作，由管理员显式放行；连接池 + 健康检查；动态 provider；工具名加 `mcp__<server>__<tool>` 前缀；只开 HTTP/SSE，禁用 stdio；地址白名单防 SSRF | 下一步（A1 后） | MCP 工具不能绕过 AgentTool 权限与二次确认；MCP 服务宕机不影响普通对话；工具名不冲突；依赖树只有一个 MCP SDK 版本；工具 schema 计入 token 配额 |
| A3 | Agent Graph 编排 | 先 spike 选运行时：AgentScope Java 2.0.1 或轻量图执行器；不引入 Spring AI Alibaba；状态持久化；可重试 | 后续 | 多步流程可暂停/恢复；状态持久化；确定性回放仍成立；失败节点可重试 |
| A4 | 跨服务 Agent 化 | 服务暴露受控 REST，`iboot-ai` 包成 `AgentTool`；不预留 MCP 端点；身份与权限校验留在各服务内部；工具白名单只在 `iboot-ai` 收口；工具端点只在集群内可达，不进网关外部路由 | 后续 | 审批/任务可声明式调用 Agent；身份与权限在拥有方服务内校验；网关外部路由不暴露工具端点 |
| A5 | Agent Runtime 契约 | 仅在出现第二种运行时后抽取；不做单实现的过度抽象 | 触发条件 | 契约测试覆盖 execute/cancel/status；两种运行时行为一致 |

补充约束：

- 单人环境下的四眼原则：只有一名管理员时，全局技能提交按钮提示「需要至少一名其他审核人」，规则不降级、默认不可用；内置技能与私有技能不受影响。
- 技能标识与快照：skillId 带来源前缀（`builtin.*` / `global.*` / `user.<ownerId>.*`），不同来源不互相遮蔽；运行开始固定技能快照，中途编辑不影响进行中的运行。
- 评测：上线前用现有云端评测基线跑「带 / 不带用户技能」两份对照，避免工具选择准确率被用户技能扰动。
- MCP 并发：开工前验证 `McpClientWrapper` 在虚拟线程 + 每请求新 Toolkit 下的并发安全性。
- 授权刷新：授权后需重新登录，会话中的权限才会刷新（docs/22 §12.2）。

### 4.4 明确不做 / 触发条件

- **Spring AI Alibaba**：Boot 4 上只有预发布 M 版，不满足稳定线要求。
- **SkillBox 代码执行 / `setCodeExecutionEnable`**：Web 服务里对终端用户开放等于远程代码执行，永久关闭。
- **MCP `stdioTransport`**：允许 Java 进程 spawn 任意子进程，服务端禁用。
- **Agent Runtime 契约**：出现第二种运行时前不抽取（A5）。
- **技能资源包 / zip**：v1 只收单文件 SKILL.md。

---

<a id="cloud-native"></a>
<a id="阶段-3云原生ack--helm"></a>
## 5. Cloud Native

### 5.1 定位与完成判据

运行环境完全由声明式配置定义，可复现、可审计；本地 Kind 与云上 ACK 使用同一份镜像、不同 values。

- `helmfile sync` 一条命令完成发布。
- 滚动升级零 5xx；删 Pod 自愈。
- 压测触发 HPA 扩容，账单可观测。
- 本地与云上同一镜像、不同 values 可跑。
- GitOps 漂移可检测（C7 后）。

### 5.2 已完成

| 能力 | 一句话 | 归属服务 | 文档 |
|---|---|---|---|
| 本地容器化 | 八个业务服务、网关、`iboot-ops` 与前端均有 Dockerfile；Compose 全量模式可跑 | 全部服务 | [docker/README.md](docker/README.md) |
| 本地基础设施 | Compose 管理 MySQL/Redis/ES/Nacos/RocketMQ/Kafka/Seata/OPA 等；`dev-init.sh` 一键拉起 | 本地开发 | [scripts/README.md](scripts/README.md) |

### 5.3 未完成路线

| ID | 内容 | 技术 / 约束 | 时机 | 验收判据 |
|---|---|---|---|---|
| C1 | 镜像工程 | 每服务 Dockerfile + 非 root 运行 + 推 ACR；基础镜像需与 GraalVM/native 路线兼容 | 下一步 | 每服务镜像可构建、可启动 |
| C2 | Helm 组织 | Helmfile + 一个通用 library chart，避免 9 份重复样板 | 下一步 | `helmfile sync` 幂等 |
| C3 | K8s 清单 | Deployment/Service/ConfigMap/Secret/HPA/PDB/NetworkPolicy/Ingress(ALB)/命名空间(dev, prod)；Secret 走 KMS 或 ACK Secret，不入仓 | 下一步 | 滚动升级零 5xx；删 Pod 自愈 |
| C4 | 中间件分层混合 | 本地 Kind 全自建；云上数据层用 RDS MySQL / Tair / 阿里云 ES / OSS（替 Silo）/ MSE Nacos / 云 RocketMQ；Silo→OSS 依赖 `FileStorageStrategy` 抽象 | 下一步 | 本地与云上同一份镜像、不同 values 可跑 |
| C5 | CI/CD | GitHub Actions 或云效 Flow：构建 → 镜像推 ACR → `helmfile sync` → dev 晋升 prod → `helm rollback`；密钥走 CI Secrets | 下一步 | 一次提交自动完成 dev 发布与回滚演练 |
| C6 | 弹性 | request/limit 调优、HPA 按 CPU + 自定义 QPS 指标、突发负载用 ECI/ASK；常驻资源是主要账单 | 后续 | 压测触发 HPA 扩容 |
| C7 | GitOps（Argo CD） | Git 为期望状态源，Argo CD 同步与漂移检测；在 C5 之后 | 后续 | 手工改集群被检测并回滚；回滚由 Git revert 完成 |
| C8 | GraalVM Native | 先对 `iboot-gateway` 做可行性验证；真实动机是缓解 N×JVM 内存与冷启动；Boot 4 native 要求 GraalVM 25 工具链 | 后续 | 网关 native 镜像可启动；内存/冷启动与 JVM 对比有数据 |
| C9 | Spring Cloud Kubernetes | 只读 ConfigMap/Secret；不替代 Nacos 注册发现 | 触发条件 | 一个服务从 K8s 读取配置；Nacos 发现保持不变 |

### 5.4 明确不做 / 触发条件

- **Spring Cloud Config / Consul**：与 Nacos 配置中心、服务发现重叠。
- **Spring Cloud Bus**：Nacos 配置刷新已覆盖；再引入消息广播只增加故障面。
- **Spring Cloud Task / Data Flow**：与 Quartz / Spring Batch / Flink 职责重叠。
- **Spring Cloud Function**：当前不走 Spring Cloud Stream binder，无 Serverless/FaaS 需求；出现需求再评估。
- **Spring Cloud Kubernetes 替代 Nacos**：只读配置，不替代注册发现。
- **Vault**：KMS/ACK Secret 足够；需要动态凭据时再评估（S7）。

---

<a id="security-native"></a>
## 6. Security Native

### 6.1 定位与完成判据

不因为请求来自内网就信任它：入口、服务间、数据面、工作负载身份与密钥都按最小权限验证。

- OAuth2/OIDC + PKCE；下游服务独立验签，不信任网关传来的身份头。
- mTLS/SPIFFE；路径级授权；每服务最小权限。
- Token Exchange 收窄 scope/audience。
- 密钥不入仓；证书轮换自动化。
- 多客户端会话可共存，管理员强制下线覆盖所有类型。

### 6.2 已完成

| 能力 | 一句话 | 归属服务 | 文档 |
|---|---|---|---|
| 认证与授权 | OAuth2 / OIDC 授权码 + PKCE、JWK 验签、RBAC 与五级数据权限 | `iboot-auth`、`iboot-kernel`、`iboot-system` | [docs/23](<./docs/23-微服务底座操作指南.md>) |
| 本地 TLS 与零信任基线 | 全链路 HTTPS/mTLS、每服务证书与 SPIFFE 身份、路径级授权、证书轮换、表/键级最小权限、OPA 与 SPIRE 演示 | 全部服务与中间件 | [docs/27](<./docs/27-TLS 与零信任操作指南.md>)、[docs/23 §19](<./docs/23-微服务底座操作指南.md>) |
| 开发者 CLI 与 PAT | 登录、只读/写命令、PAT、scope、`authType`、审计认证方式 | `iboot-cli`、`iboot-auth` | [docs/25](<./docs/25-开发者 CLI 操作指南.md>)、[docs/26](<./docs/26-CI 与个人访问令牌.md>) |
| 审计与在线会话 | 操作/登录/运行日志、在线会话 | `iboot-monitor` | [docs/11](<./docs/11-可观测性设计.md>) |

#### 6.2.1 开发者入口（CLI）

独立模块 `iboot-cli`，只经网关调用既有接口，复用现有权限、数据范围与审计；不进入 Docker/Helm 部署清单。

v0–v4 已完成：登录与只读查询（v1）、运维写命令（v2，含任务执行、备份触发/等待、在线强退与 context 增删）、
个人访问令牌与 CI 自动化（v3，含 PAT、GitHub Actions 示例、本地 SKILL.md）、PAT 最小权限与审计认证方式
（v4，含 scope、`authType`、`--all` 翻页与 `job run --wait`）。判据「无浏览器环境完成一次备份触发」已由
`token create` + `--token-stdin` 满足。

**后续（不设版本号，按需启动）**：CLI 功能面到此为止，后续由真实使用驱动。

- 体验打磨（低风险，不单独立项）：shell completion、预构建 jar/安装脚本、`iboot api -d @file`、`token list --expiring-in`、`job logs --follow`。
- 属于其他能力域：门户 PAT 管理页、管理员查看/撤销他人 PAT、scope 预设、PAT 权限实时同步。
- 明确不做：管理类 CRUD、`backup restore` 等破坏性操作、消息/审批/AI 命令、MCP 通道、`add-multi-client-sessions`。

**非目标**：不做门户替代；不做 Web 智能体的执行通道；不覆盖全部 206 个接口；不做离线缓存；不引入密码授权。

**过渡期限制（已知限制，暂不改造）**：CLI 会话与 PAT 都不写入在线名单，管理员按用户强制下线不覆盖它们。
PAT 通过 `token revoke` 或用户停用/删除撤销。当前阶段保留该限制，不启动 `add-multi-client-sessions`；
需要管理员强制下线覆盖 CLI/PAT，或需要多客户端会话管理时再单独立项。契约见
`openspec/specs/auth-service/spec.md` 与 `openspec/specs/personal-access-token/spec.md`，变更记录见
`openspec/changes/archive/2026-11-18-add-developer-cli/`、
`openspec/changes/archive/2026-11-18-add-personal-access-token/` 与
`openspec/changes/archive/2026-11-19-harden-pat-and-extend-cli/`。

#### 6.2.2 本地 TLS 与零信任基线（B0–B6）

本地传输层加密按批次推进，不新开阶段；操作指南与证书细节只维护在
[docs/27](<./docs/27-TLS 与零信任操作指南.md>)，B0 实测结论见
[docs/spike/tls-zero-trust-spike-report.md](./docs/spike/tls-zero-trust-spike-report.md)。

| 批次 | 内容 | 状态 / 判据 |
|---|---|---|
| B0 | 信任链、Vite HTTPS、MySQL/Redis/Silo、Nacos/RocketMQ/Seata 可行性 | 已完成；见 B0 报告 |
| B1 | 边缘 TLS：本地 CA、门户 443、Vite HTTPS、OAuth2/CLI/回归 HTTPS | 已完成：`openspec/changes/archive/2026-11-20-add-edge-tls/` |
| B2 | 门户→网关、服务间、JWKS、MySQL/Redis/Silo/Neo4j | 已完成：`openspec/changes/archive/2026-11-20-add-internal-tls/`；Nacos/ES/Kafka/RocketMQ/Seata 不在本批 |
| B3 | ES/Kafka/Logstash 日志与检索面；RocketMQ 仅作为加密通道 | 已完成：`openspec/changes/archive/2026-11-21-add-log-search-tls/` |
| B4 | 入口收敛、中间件认证、网络分段、每服务 DB 账号 | 已完成：Nacos TLS（`openspec/changes/archive/2026-11-21-add-nacos-tls/`）、入口收敛、中间件认证、网络分段与每服务账号 |
| B5 | 服务间 mTLS、工作负载身份、中间件身份收尾 | 已完成：B5.1–B5.14：每服务证书、服务间 mTLS、RocketMQ TLS-only 与 ACL、路径级授权、中间件每服务身份、exporter mTLS、证书轮换与 SPIFFE 身份、证书轮换自动调度、宿主机进程回环绑定、Sentinel Dashboard TLS 与登录、RocketMQ 服务端证书校验、表级/键级最小权限、OPA 集中决策与 SPIRE Workload API；Kubernetes 服务网格不在本项目范围，Compose 以 SPIFFE+OPA+mTLS 为 L3 平面；残余边界清单见 docs/27 §29 |
| B6 | 凭据与决策面收尾 | 已完成：MySQL 迁移/管理账号边界、MySQL/Redis/Nacos 组件独立凭据、Nacos 每服务工作负载客户端证书与配置命名空间隔离、MySQL/Redis/Silo 客户端证书 mTLS、宿主机开发与 Compose 全量模式十个服务切换 SPIRE SVID（含 sidecar、刷新脚本与到期调度）、OPA 有界重试/可选短 TTL 缓存/双副本 nginx LB、三阶段 CA 轮换与 Docker 滚动重启、容器化轮换调度；Nacos HTTP 保留例外；详见 docs/27 §13/§18/§23/§28/§29 与 docs/28 基线 |

边界：B1 只加密公开入口；B2 加密门户→网关、服务间与核心数据面，网关 `8088` 改为 HTTPS 但仍是内部地址；
Nacos 的 TLS 在 B4 完成。不在本地手搓服务网格，L3 留给 Cloud Native 之后的 ASM/Istio。

### 6.3 未完成路线

S3/S4 来自 2026-11-13 的微信小程序可行性分析：只做员工轻量入口（登录、消息、审批、个人中心），
不做管理后台移动版；真正的门槛是备案、认证与公网 HTTPS。

| ID | 内容 | 技术 / 约束 | 时机 | 验收判据 |
|---|---|---|---|---|
| S1 | Token Exchange | RFC 8693；服务代表用户调用时签发收窄 scope/audience 的派生令牌；Spring Security 7 支持情况先 spike；原始 JWT 不转发 | 下一步 | workflow 以用户身份调 file 时使用派生令牌；原始 JWT 不转发；audience 不匹配被拒 |
| S2 | Secret 管理 | K8s Secret 走 KMS/ACK Secret；CI Secrets 不入仓；与 C3 同步 | 下一步（C3 同步） | 仓库无明文密钥；轮换流程可执行 |
| S3 | 微信小程序身份接入 | `/api/auth/wechat/token` 与 `/bind`；`auth_wechat_binding` 表；wx.login → jscode2session；未绑定返回 5 分钟一次性 bindTicket；绑定必须验证既有凭据并复用失败计数/锁定；appsecret 只在服务端；session_key 不下发；openid 按 PII 脱敏；网关 auth 白名单加新端点；登录端点限流；审计写 `browser=WeChat MiniProgram`、`os=WeChat` | 后续（C3/C4 提供公网 HTTPS 后） | 首次进入返回未绑定且不签发业务令牌；正确凭据绑定成功并静默登录；错误密码计入锁定；未绑定 openid 调业务接口 401；A 用户只见自己的数据；令牌过期静默重登一次不死循环；审批通过/驳回审计有操作人；客户端与日志无 appsecret/session_key；生产域名 HTTPS + 443 + 备案 + 白名单；v3 下载不暴露 MinIO 内网地址；v3 订阅消息仅授权后发送 |
| S4 | 多客户端会话模型 | 会话键改为 `userId + clientType`；驱逐只在同类型内发生；在线列表显示客户端类型；涉及 `online_token` 键设计与在线列表 | 下一步（S3 前置） | 门户登录不驱逐小程序会话；管理员强制下线覆盖所有类型；在线列表可区分客户端类型 |
| S5 | 服务网格零信任 | ACK ASM 或 Istio；mTLS 全链路 + 服务间授权策略；改变运维模型，必须在服务稳定后 | 后续（Cloud Native 后） | 未授权服务间调用被拒；sidecar 开销与虚拟线程/native 有数据 |
| S6 | Spring LDAP | 企业目录接入 | 触发条件 | 有真实 LDAP 环境后再定判据 |
| S7 | Vault | 动态凭据/租约 | 触发条件 | 当前 KMS/ACK Secret 不足时再评估 |

补充约束：

- 实时推送：小程序没有 `EventSource`，v1 轮询未读数；v3 用订阅消息；需要真实实时再评估 WebSocket。
- 文件：下载统一走 `/api/file/download/{id}`，不暴露 MinIO 内网地址；域名白名单只保留网关一个。
- 微信平台门槛：企业主体认证、备案、类目审核、隐私指引、订阅消息授权、主包 2MB / 总包 20MB；本地开发者工具可跳过域名与证书校验，真机与体验版受限。
- 服务端 `access_token` 全局缓存（约 2 小时），不能每请求获取；刷新失败需告警。
- 前端建议 Taro + Vue 3，独立目录 `iboot-miniapp/`，不并入 Maven reactor 或 portal 构建；Element Plus 不可复用。

### 6.4 明确不做 / 触发条件

- **密码授权**：不引入。
- **Vault / Spring LDAP**：触发条件，见 S6/S7。
- **微信小程序管理后台移动版**：不做；表单密集、权限复杂、审核风险高。
- **CLI 管理类 CRUD、`backup restore`、消息/审批/AI 命令、MCP 通道**：不做。

---

<a id="micro-service-native"></a>
## 7. Micro Service Native

### 7.1 定位与完成判据

服务边界由领域模型驱动，并且边界质量可以被持续审视；拆开只是起点，可独立演进才是目标。

- 业务服务独立启停与发布；单服务故障不雪崩。
- 表所有权清晰；同库表级只读是长期接受模式，不是待还的债。
- 跨服务强一致写有可回滚的分布式事务；旁路事件走 outbox。
- 关键服务边有消费者驱动契约测试。
- 1–2 条核心链路完成 EventStorming 复盘；无未处置的分布式单体信号。

### 7.2 已完成

| 能力 | 一句话 | 归属服务 | 文档 |
|---|---|---|---|
| 微服务拆分 | 八个业务服务 + 网关 + 内核；独立启停与发布 | 全部服务 | [docs/23](<./docs/23-微服务底座操作指南.md>) |
| 服务治理 | 网关路由级限流、跨服务熔断降级、Nacos 规则源、Sentinel Dashboard 与门户「微服务治理」控制台入口 | `iboot-gateway`、`iboot-workflow`、`iboot-notify` | [docs/23 §13](<./docs/23-微服务底座操作指南.md>) |
| 分布式事务 | Seata AT：审批通过与请假额度扣减的跨服务原子提交与回滚；Seata TCC：管理员额度划转的冻结/确认/取消 | `iboot-workflow`、`iboot-system` | [docs/23 §16/§17](<./docs/23-微服务底座操作指南.md>) |
| 可靠事件投递 | 审批待办事件经本地 outbox 与 RocketMQ 可靠投递，消费幂等、失败重试与死信可查 | `iboot-workflow`、`iboot-notify` | [docs/23 §14](<./docs/23-微服务底座操作指南.md>) |
| 服务发现与契约 | Nacos 注册发现；CloudEvents 响应信封；API 版本头 | 全部服务 | [docs/13](<./docs/13-CloudEvents 集成.md>)、[docs/23](<./docs/23-微服务底座操作指南.md>) |

### 7.3 未完成路线

| ID | 内容 | 技术 / 约束 | 时机 | 验收判据 |
|---|---|---|---|---|
| M1 | 契约测试 | Spring Cloud Contract 或 Pact 二选一；先覆盖 gateway→auth/system、workflow→notify 两条边；先验证 Boot 4 兼容性 | 下一步 | 消费者契约生成；提供者构建在破坏性变更时失败；CI 有门禁 |
| M2 | EventStorming 边界复盘 | 选 1–2 条核心链路，输出事件/命令/聚合与跨服务调用清单；发现的边界问题有后续变更单 | 下一步 | 复盘报告落地；边界问题有后续变更单；无未处置的分布式单体信号 |
| M3 | 分库分表 | ShardingSphere-JDBC；先 `sys_operate_log`；服务边界与表级所有权是前置；Boot 4 兼容需验证 | 后续 | 分片路由正确；现有查询/测试通过；无跨分片 join |
| M4 | Spring Batch | 批处理（报表、备份、对账）；与 Quartz 职责划清 | 后续 | 一个批处理作业可重启/重试；不与 Quartz 重叠 |
| M5 | 多租户 SaaS 化 | `tenant_id` + MyBatis 插件 + Nacos 命名空间隔离；横切改造，会让每张表都背租户改动 | 触发条件 | 租户隔离测试通过；无跨租户数据 |
| M6 | Dubbo RPC | 内网热点调用替换；等待 Boot 4 兼容版本（见 R2） | 触发条件 | 出现 Boot 4 兼容版本后重评 |
| M7 | RocketMQ 顺序消息 | 仅在出现真实顺序语义时评估；不影响 outbox 链路；事务消息仍不采纳 | 触发条件 | 顺序消费验证通过；outbox 链路不受影响 |

### 7.4 明确不做 / 触发条件

- **RocketMQ 事务消息**：§12.9 的 outbox 已取代；半消息要求 broker 在线，违背「broker 停机时审批不受影响」。
- **Spring Statemachine**：Flowable 已覆盖流程编排；任务生命周期用枚举/状态字段足够。
- **Spring Cloud Config / Consul / Bus**：与 Nacos 重叠。
- **Spring Cloud Task / Data Flow**：与 Quartz / Spring Batch / Flink 重叠。
- **多租户 / Dubbo / 顺序消息**：触发条件，见 M5–M7。

---

<a id="sre-native"></a>
## 8. SRE Native

### 8.1 定位与完成判据

分水岭不在采集了多少指标，而在告警和运维动作是否由 SLO 与错误预算驱动。

- gateway + system 有 SLI/SLO/错误预算。
- 燃烧率告警；每条告警有 Runbook。
- Trace-Log 关联；从告警一键跳到 Trace 与日志。
- 云上 Grafana 看到同一套业务面板；成本可见。
- 备份恢复演练可重复执行。

### 8.2 已完成

| 能力 | 一句话 | 归属服务 | 文档 |
|---|---|---|---|
| 可观测性 | Actuator + Micrometer + Prometheus/Grafana + ELK + Tempo 链路追踪 | 全部服务 | [docs/11](<./docs/11-可观测性设计.md>) |
| 可靠日志管道 | 服务日志经 Kafka 缓冲（24h）后由 Logstash 写入 ES，索引与字段不变 | 全部服务 | [docs/11 §11.11.1](<./docs/11-可观测性设计.md>)、[docs/23 §15](<./docs/23-微服务底座操作指南.md>) |
| 运维控制台 | 独立 SBA 控制台，经 Nacos 发现全部实例，支持单实例巡检与运行期日志级别调整 | `iboot-ops` | [docs/11](<./docs/11-可观测性设计.md>) |
| 日志与监控 | 操作/登录/运行日志、在线会话、服务与中间件资源检查 | `iboot-monitor` | [docs/11](<./docs/11-可观测性设计.md>) |
| SkyWalking 对比实验 | OAP/Horizon/BanyanDB + agent，作为可选、可整体摘除的对比栈，不转正 | 辅助实验 | [docs/24](<./docs/24-SkyWalking 与 Tempo 对比实验.md>) |

业务侧的数据备份入口见 §3；SRE Native 只负责备份成功与恢复演练的可观测性。

### 8.3 未完成路线

| ID | 内容 | 技术 / 约束 | 时机 | 验收判据 |
|---|---|---|---|---|
| R1 | SLI/SLO 基线 | 先 gateway + system：可用性、p99、28 天 SLO 99.9%；错误预算；低流量下 SLI 噪声需标注 | 下一步 | 看板展示 SLI/SLO/错误预算；数据来源可追溯 |
| R2 | 燃烧率告警 + Runbook | 多窗口（1h/6h/3d）燃烧率；Alertmanager 路由；每条告警有 Runbook | 下一步（R1 后） | 模拟错误尖峰触发燃烧率告警；Runbook 可执行；恢复后告警关闭 |
| R3 | Trace-Log 关联 | 结构化日志注入 trace_id/span_id；Grafana/ELK 跳转；不新增中间件 | 下一步 | 从告警一次跳到 Trace，再跳到关联日志 |
| R4 | 可观测上云 | 指标 ARMS 托管 Prometheus 或 kube-prometheus-stack；日志 Logtail→SLS（本地 ELK 保留）；追踪 Tempo/ARMS；SLS 需采集白名单 | 后续（C3 后） | 云上 Grafana 看到同一套业务面板；采集白名单与成本可见 |
| R5 | 成本与弹性观测 | HPA、节点/容器成本、账单看板；与 C6 联动 | 后续（C6/R4 后） | 压测扩容与账单变化同屏可见 |
| R6 | 混沌工程 | Chaos Mesh：Pod kill、网络延迟；仅在 SLO 基线稳定后 | 触发条件 | 故障注入后 SLO 告警正确；自愈验证 |
| R7 | Flink 日志/行为流 | Flink 消费 Kafka 日志 topic；仅在有真实流分析场景时启动 | 触发条件 | 有真实流分析场景再启动 |

### 8.4 明确不做 / 触发条件

- **SkyWalking 转正**：保持可选、可整体摘除；不替代 Tempo。
- **Flink / Chaos Mesh**：触发条件，见 R6/R7。
- **多集群 Thanos**：云上可被托管方案替代，不单独建设。

---

<a id="4-版本矩阵"></a>
## 9. 版本矩阵

锁定的事实基线，除非明确另起变更，否则不升级、不混搭。

| 组件 | 目标版本 | 约束来源 |
|---|---|---|
| Java | 25 | 现状 |
| Spring Boot | **4.0.x（锁死，不升 4.1）** | SCA 对 Boot 4.1 适配仍在进行（#4339 / #4358） |
| Spring Cloud / SCA | 2025.1.x / **2025.1.0.0** | SCA 首个支持 Boot 4.0.x 的版本线 |
| Nacos | **3.3.0（服务端）/ 3.1.1（客户端，随 SCA BOM）** | bootstrap 方式已移除，必须用 `spring.config.import`；控制台鉴权独立于客户端/管理端鉴权 |
| Sentinel / Seata | Sentinel Dashboard **1.8.10**（客户端 1.8.9）；Seata **2.5.0 客户端 / 2.6.0 TC**（`org.apache.seata`） | 客户端随 SCA 2025.1.0.0 BOM；Dashboard 与 TC 使用独立镜像 |
| Kafka | **4.4.0 服务端（KRaft，无 ZK）** | Boot 4.0.3 托管 kafka-clients 4.1.1 / spring-kafka 4.0.3；4.0 起移除 ZooKeeper |
| RocketMQ | **5.5.0（服务端）/ 5.2.2（`rocketmq-client-java`）** | 阶段 2 收尾：审批通知 outbox；Spring Boot Starter 仍停留在 Boot 2.7，不使用 |
| Spring AI | **2.0.1（GA）** | Boot 4 必需；1.1.x 仅到 Boot 3.5 |
| AgentScope Java | 2.0.1（GA） | JDK 25 已 Spike 通过；starter 构建于 Boot 4.0.3 |
| Spring AI Alibaba | **暂不引入** | 仅 M 版预发布 |
| Spring Boot Admin | **4.0.2** | 运维控制台；4.x 对应 Boot 4，勿与 Boot 3 的 3.x 混搭 |
| SkyWalking | OAP **11.1.0** + Horizon UI **1.1.0** + BanyanDB **0.11.2** + Java Agent **9.8.0** | 阶段 2 辅助对比实验（已完成，不转正）；OAP 与 BanyanDB 的 server API 精确锁定 0.11；ES 9.x 不在官方支持矩阵 |
| 对话模型 | **百炼 `qwen3.8-flash`**（备用 `qwen3.7-flash`） | thinking 默认开启，须显式关闭 |
| DeepSeek（备用） | `deepseek-flash`（OpenAI 兼容端点） | 仅用于故障切换，不默认启用 |
| Embedding | 百炼 `text-embedding-v4` 或本地 BGE-M3 | 第二供应商必然存在 |
| OCR 模型 | 百炼 `qwen3.5-ocr`（可回退 `qwen-vl-ocr-2025-11-20`） | 默认关闭 |
| MySQL / Redis / ES / Silo | 8.4.13 / 8.8.4 / 9.5.5 / RELEASE.2026-11-12；ES 客户端 **9.2.5**（Boot 托管） | 客户端可连接 9.x 服务端；以 Compose 镜像为准 |
| Flowable / Neo4j | **8.0.0** / **5.26.33-community**（驱动 6.0.2） | Flowable 8 是唯一支持 Boot 4 的版本线；Neo4j 5.26 为 5.x LTS |
| OPA / SPIRE | **1.8.0** / **1.12.4** | 服务间授权集中决策；SPIRE 由 `spire` profile 提供，`dev-init.sh` / `docker-deploy.sh` 默认启用并为十个服务签发 SVID，`IBOOT_SVID_ENABLED=false` 回退 mkcert |
| Apache Dubbo | **阻塞** | 无 Boot 4 支持 |
| GraalVM（若做 native） | 工具链必须 **25** | Boot 4 native 构建要求 |

数据边界：所有服务共用数据库 `iboot`；每张表只有一个所有者，其余服务只读登记的表与列。该只读关系是**长期接受**模式，不是待还的债；消费清单与写入例外见 [docs/23](<./docs/23-微服务底座操作指南.md>) §11.12。

新增项在开工前先验证版本，未验证前不写入上表：

- Spring Cloud Contract / Pact：Boot 4 兼容性待验证。
- Argo CD：进入 C7 时定版本。
- Spring Cloud Kubernetes：跟随 Spring Cloud 2025.1.x，待验证。
- Spring Security 7 Token Exchange：待 spike。
- AgentScope Java 2.0.1 Graph：待 spike。
- MCP SDK：`dependency:tree` 确认单一版本。

---

<a id="5-风险登记"></a>
## 10. 风险登记

只保留尚未消除的风险。

| ID | 风险 | 影响范围 | 处置 |
|---|---|---|---|
| R2 | Dubbo 不支持 Boot 4 | Micro Service Native | 服务间 HTTP 先行；延后至出现 Boot 4 兼容版本后重评 |
| R4 | 本机内存：N×JVM + 28 个常驻中间件容器（含 Sentinel Dashboard、RocketMQ 三组件、Kafka、Seata Server 与 OPA；SkyWalking 实验再 +3 共 31 个），且单服务挂 Agent 后 RSS +约 470MB | 全线 | 按需 profiles 只跑改动服务；`IBOOT_OPS_ENABLED=false` 可跳过运维控制台；Sentinel Dashboard 堆上限 256MB 且不参与业务启动依赖；RocketMQ Broker 堆 512MB、Proxy/NameServer 256MB，且不参与业务启动依赖；Seata Server 堆上限 512MB、直接内存 256MB；SkyWalking 实验按需启停（OAP 堆上限 1GB），实验数据见 docs/24；Neo4j 置于 `graph` profile 且堆/页缓存显式限制；mock 依赖服务；native 列为 C8 缓解手段 |
| R6 | LLM token 与云账单失控 | Agent Native / Cloud Native / SRE Native | 配额 + 用量看板 + Alertmanager 告警 |
| R12 | 存量文档与实现漂移 | 全线 | 新增或修改能力时文档与代码同 PR |
| R13 | 对话与 Embedding 同属百炼，单一供应商故障 | Agent Native | base-url 与模型配置化；对话可切 DeepSeek，Embedding 暂无等价兜底 |
| R14 | Seata TC 成为审批路径的强依赖；TC 不可用时审批 fail-closed | Micro Service Native | 保持 fail-closed，不提供静默降级；seata-server 常驻并设健康检查；审批 5xx 与 TC 存活告警 |
| R15 | AT 与 TCC 在同一代理数据源混用会产生双分支与双倍补偿 | Micro Service Native | 现有 TCC 使用非代理数据源（`TccDruidDataSource` 按类名排除）并独占全局事务；新增 TCC 动作沿用该约束，见 docs/23 §16.7/§17 |
| R16 | 数据库监控（Druid）在多模块拆分后只能展示单个服务的连接池：iframe 直连 `iboot-system` 的 `:8080/druid`，开发环境跨源被 `X-Frame-Options` 拦截（新窗口不受限），且 Druid 没有跨模块聚合视图 | SRE Native / Micro Service Native | 菜单暂保留；后续用统一代理或指标聚合方案替代，见 docs/23「已知限制」 |
| R17 | Swagger UI 同样只聚合单模块接口：多模块拆分后无法在一个页面看到全部服务的 API | SRE Native / Micro Service Native | 菜单暂保留；后续用 springdoc 聚合（gateway 或独立文档服务）解决，见 docs/23「已知限制」 |
| R18 | MCP 出站带来 SSRF/数据外泄，工具 schema 挤占 token | Agent Native / Security Native | 只开 HTTP/SSE、地址白名单、默认按写操作、工具 schema 预算、并入 R6 配额 |
| R19 | 微信小程序备案/认证/审核周期不可控 | Security Native | 代码与材料并行准备；C3/C4 提供公网 HTTPS 后再进 v1 |
| R20 | 低流量下 SLI 噪声导致 SLO 误判 | SRE Native | 先选高流量服务；标注数据可信度；不把 SLO 当唯一告警源 |
| R21 | Argo CD、契约测试增加维护成本 | Cloud Native / Micro Service Native | 先做单集群、两条边；判据不成立就回退到 Helmfile/CI |
| R22 | 五条线并行分散时间与本机内存 | 全线 | 按线排优先级；同时最多推进两条线；沿用按需 profiles |

---

<a id="6-本地开发环境与成本红线"></a>
## 11. 本地开发环境与成本红线

| 项 | 红线 | 观测手段 |
|---|---|---|
| LLM token | 必须有日配额与告警 | AI 工作台的用量看板 |
| 云资源 | ACK 常驻节点 + LB + SLS 为大头；数据层走托管按量 | Cloud Native C6 / SRE R5 |
| 本机内存 | 拆分后至少 16GB 才舒适；优先按服务裁剪 | 按需启动 |
| 云 API 密钥 | 不入仓，仅 env / Nacos / KMS | `.env.example` 只留占位 |

本地开发策略：

- 中间件：`docker/docker-compose.yml`，`dev-init.sh` 默认启动 mysql / mysql-users-init / mysql-grants-init / redis / silo / es / nacos / tempo / sentinel-dashboard / seata-server / OPA、RocketMQ（NameServer / Broker / Proxy / TLS 入口）、集中日志链路（kafka / kafka-init / logstash）以及 observability profile（Prometheus / Grafana / Kibana / Alertmanager / Thanos 与 exporter）；资源紧张时用 `IBOOT_OBSERVABILITY_ENABLED=false`、`IBOOT_LOGGING_KAFKA_ENABLED=false` 裁剪；Cloud Native C3 起本地增加 Kind 集群。
- 分布式事务：审批通过依赖 `seata-server`（8091），`dev-init.sh` 等待其健康后再拉起 workflow/system；内部令牌由 `.env` 的 `IBOOT_SYSTEM_INTERNAL_TOKEN` 注入，缺省使用本地开发值。
- 应用：`scripts/dev-init.sh` 一键拉起八个业务服务、网关与运维控制台并按依赖顺序等待健康；也可单独 `mvn spring-boot:run` 调试。
- 运维控制台：默认随开发环境启动，`IBOOT_OPS_ENABLED=false` 可跳过；生产环境不经公网网关暴露。
- 密钥：统一放根目录 `.env`（已在 `.gitignore` 中），模板见 `.env.example`。

---

<a id="7-决策记录"></a>
## 12. 决策记录

### 12.1 GraalVM Native 计划的沉淀与勘误

原 `graalvm-native-image-plan.md` 以 iboot-admin 全量 native 化为目标。其**风险判断依然成立**，其**构建方法已过期**。

仍然有效的部分：Druid（反射 + 动态代理）、MyBatis（XML + 反射）、ES Client、MinIO 客户端（OkHttp 代理）、Quartz、jjwt、Logstash Encoder 属于高风险依赖；`RedisConfig`、`SecurityConfig`、`JwtAuthenticationFilter`、`TwoLevelCacheManager`、`LogAspect`、`DataScopeInterceptor` 是代码层风险点；构建期内存大、编译慢 5–10 倍、跨平台分别编译、调试困难；回滚原则是保留 JVM 部署路径。

勘误（照原样执行会失败）：

| 原文写法 | 现状 |
|---|---|
| 添加依赖 `spring-boot-starter-native` | **该 artifact 不存在**；native 支持由 Boot 自身 + `native-maven-plugin` 承担 |
| `gu install native-image` / `--report-unsupported-elements-at-runtime` | 均已废弃或移除 |
| 手写 `reflect-config.json` 为主路径 | 首选 `RuntimeHints` / `@ImportRuntimeHints` / `@RegisterReflectionForBinding` |
| 未指定构建期 JDK | Boot 4 native 构建**要求 GraalVM 25 工具链** |
| 目标：iboot-admin 全量 | **改判**：从 `iboot-gateway` 这类无 ORM、反射面最小的服务起步 |

**否决项**：不采纳「以 iboot-admin 全量 native 化为近期目标」。它不消解任何当前功能缺口，收益（冷启动、内存）在单体阶段没有消费者；等到 N×JVM 内存压力与弹性冷启动需求出现后，它才有明确的服务对象。

### 12.2 AI 底座：为什么不是 langchain4j / Spring AI Alibaba / Python 版 AgentScope

- **Spring AI Alibaba**：Boot 4 上只有预发布 M 版且 pin 在非 GA 的 Spring AI，不满足「稳定线」要求。
- **LangChain4j**：框架成熟且 provider 无关，但与 Spring AI 抽象层职责重叠；同时引入两套 AI 抽象会挤掉真正要学的东西（模型接入不是难点，权限与检索质量才是）。
- **AgentScope（Python 版）**：会在 Java 微服务旁引入一个 Python 服务，偏离练 Java 技术栈的主线；而 **AgentScope Java 2.0.1 已 GA**，同一生态、同一种语言，Agent Card 可注册进 Nacos。
- **模型全走百炼**：一个 Key、一本账单、一个兼容端点；代价是单一供应商故障半径重叠，已用配置化 base-url 与 DeepSeek 备用端点缓解。

### 12.3 阶段顺序：为什么 AI 在微服务之前

AI 是**新增部署单元**而非改造存量，不依赖拆分底座；先补全功能再拆分，能让每个限界上下文在拆出时就是完整可测的业务体。唯一附加的硬性条件在实施时已落实：AI 必须是独立模块，不能是单体内的包。

被推迟的选项：多租户（阶段 0 → 阶段 4），横切改造会污染拆分边界；服务网格（→ 阶段 4），会改变运维模型，应在服务数量稳定后引入。

### 12.4 短信渠道为何默认关闭

阿里云自 2025-06-06 起不再接受个人实名账号申请国内短信自用资质，签名必须完成运营商实名报备。本项目当前只有个人账号，因此保留阿里云 Dysmsapi 适配器，但 `iboot.notification.sms.enabled=false`，不创建短信投递记录；站内、邮件、钉钉三个渠道承担验收。待有企业或个体工商户资质后，只需配置签名与模板 Code 即可启用。

### 12.5 认证服务不向公共客户端签发刷新令牌

Spring Authorization Server 对「公共客户端 + 授权码」不签发刷新令牌，因此**没有静默续期**。会话语义由共享 Redis 会话的滑动续期承担：资源服务不因令牌自身过期而拒绝请求，访问令牌有效期与会话超时（30 分钟）对齐。前端与开发者 CLI 都保留「有刷新令牌才续期」的分支；当前公共客户端拿不到刷新令牌，401 表示会话已失效，需要重新登录。刷新令牌的授权记录在内存，认证服务重启后需要重新登录；已签发访问令牌在资源服务上的验签不受影响。

### 12.6 系统服务是唯一「停掉会连带」的拆分

系统服务宕机时，其余服务的数据权限查询在缓存过期后失败关闭，通知与审批的用户读取不可用；但**认证登录仍然可用**（认证服务直连数据库），已登录用户访问不带数据范围的接口也不受影响（JWT 自包含 + 共享 Redis 会话）。因此系统服务的故障隔离判据是「**失败有界**」，而不是「其它域仍可用」；这也是同库表级只读被接受为长期模式的原因之一：改成远程调用会让可用性倒退。

<a id="77-为什么允许一个与-tempo-职责重叠的-skywalking"></a>
### 12.7 为什么允许一个与 Tempo 职责重叠的 SkyWalking

SkyWalking 与既有 Tempo 都做链路追踪，按 §1 原则 2 本应剔除。保留它，是因为练习项目的目标包含技术栈的横向对比，而「装过」不产生认知：agent 字节码增强的零侵入自动埋点与现有 SDK 埋点，在启动开销、常驻内存、虚拟线程下的表现、拓扑发现能力上的差异，只有在同一批服务、同一批流量上并行观测才能看清。因此它被定义为**阶段 2 收尾的辅助对比实验**：有独立验收标准，不替代 Seata / Kafka 任何一项判据，也不替换 Tempo。实验已完成，结论是 **Tempo 保持主链路，SkyWalking 保留为可选、可整体摘除的对比栈，不转正**，数据与边界见 [docs/24](<./docs/24-SkyWalking 与 Tempo 对比实验.md>)。11.x 还能直接接收 OTLP 并把 OAP 作为 Grafana Tempo 的数据源，这为后续「agent vs OTLP」的第二种对比留了口子，但不作为本次实验的前置。

### 12.8 为什么日志走 Kafka，业务事件仍走 RocketMQ

两条链路都可靠，但语义不同：日志是高频、可丢尾批、无顺序与事务要求、且需要被流处理消费的数据；业务事件要求与本地事务绑定、可按事件重放、有死信可查。日志若混入 RocketMQ，会让日志流量进入业务 broker 的故障半径，并通过 outbox 与消费重试机制制造两套语义；反过来，业务事件若走 Kafka，则要自行补 outbox、死信与重试分类。因此保留两条链路：Kafka 只做日志缓冲（24 小时、无长期存储职责），业务事件仍由 outbox + RocketMQ 承载。

服务端日志投递由自研的 `KafkaAppender` 完成，而不是 Filebeat 或第三方 appender：`logstash-logback-encoder` 9.0 不提供 Kafka appender；第三方实现停留在 `kafka-clients` 2.x；Filebeat 在本地需要给十个容器挂共享日志卷并新增常驻进程，到 Cloud Native C3 用采集器读 stdout 才自然。自研实现只补缺失的积木，环形缓冲、丢弃与停机排空仍由 encoder 的基类提供。

### 12.9 为什么是生产方 outbox，而不是事务消息或提交后直发

阶段 2 收尾要把「审批 → 通知」从进程内 HTTP 重试换成可靠投递。提交后直发在 broker 不可用时直接丢事件；RocketMQ 事务消息虽然把半消息与本地事务绑定，但半消息发送本身要求 broker 在线，无法满足「broker 停机时审批不受影响」。最终选择生产方 outbox：审批事务内写 `biz_notification_outbox`，中继独立重试发布；代价是一张表与一个后台中继，换来的是与 broker 可用性彻底解耦。消费侧继续用 `sys_notification_delivery` 作为投递 outbox，两级 outbox 分别负责「登记不丢」与「渠道重试」。客户端使用官方 gRPC 客户端而非 Starter，是因为后者仍停留在 Boot 2.7 基线；为它回退 Boot 4 违反原则 6。

### 12.10 为什么在单库上引入 Seata

单库并不等于可以跨服务混写表：表所有权仍按服务划分，审批与额度分属两个服务。如果为了省掉分布式事务而让一个服务直接写另一方的表，就破坏了拆分边界，也让后续拆库成本更高。审批通过与额度扣减是**真实的不变量**：批了不扣或扣了没批都不可接受，因此用 Seata AT 把两侧放进同一个可回滚的全局事务。

代价被明确接受：TC 成为审批路径的强依赖，不可用时 fail-closed；全局事务内只允许纯 DB 写，通知继续走 outbox。可替代方案是把额度表放进 `iboot-workflow` 退回本地事务，但那会掩盖跨服务一致性问题，也违背「额度随用户、由系统域拥有」的边界。选择 Seata 是为了在保留边界的条件下获得原子性，而不是为了给中间件找用途。

### 12.11 AT、TCC 与 outbox 的分工

| 场景 | 机制 | 理由 |
|---|---|---|
| 短事务、纯 DB、必须原子 | Seata AT | undo_log 直接覆盖，代码侵入最小 |
| 需要资源预占或长时间持有 | 业务 TCC / 预占 + 确认 | Try/Confirm/Cancel 的语义比 AT 更贴合 |
| 旁路通知、审计、事件 | 本地 outbox + MQ | 最终一致即可，失败不应阻断主流程 |

**AT 与 TCC 不能共享同一个被代理的数据源**：Seata 2.5.0 的 `ConnectionProxy.register()` 只在连接上有 undo log（DML）或 lock key 时登记 AT 分支；TCC 的 Try 如果走同一代理数据源并写库，就会同时产生 AT 分支，回滚时 TCC Cancel 与 AT undo 可能双倍补偿。因此额度划转使用 `TccDruidDataSource`（按类名排除 AT 自动代理）并独占全局事务，TM 不写本地库；这也是把 TCC 放到独立变更、不叠在审批路径上的原因。实现与验收见 [docs/23 §17](<./docs/23-微服务底座操作指南.md>)。

### 12.12 为什么按五条 Native 重组

原有「阶段 3 云原生 → 阶段 4 深水区」是时间轴，无法回答「每条能力线现在成熟到什么程度、下一步做什么」。
五条 Native 是架构姿态：每条线有自己的完成判据，避免把「装过组件」当成完成；业务功能留在 §3，不硬塞进五条线；
历史阶段用 §2 的对照表保留，不丢失已完成记录。被否决的建议集中在 12.13，避免以后重复讨论。

### 12.13 DeepSeek 建议的取舍

采纳：

| 来源 | 建议 | 落点 | 理由 |
|---|---|---|---|
| DeepSeek | 契约测试（Spring Cloud Contract / Pact） | M1 | 拆分已完成，接口兼容性需要工程门禁；与 Testcontainers 互补 |
| DeepSeek | SLO/SLI + 燃烧率 + Runbook | R1/R2 | 可观测栈已存在，缺的是「按 SLO 决策」；不新增中间件 |
| DeepSeek | GitOps（Argo CD） | C7 | 在 Helm 化之后增加漂移检测与 Git 回滚 |
| DeepSeek | Token Exchange（RFC 8693） | S1 | 服务代表用户调用时收窄权限，补零信任的授权粒度 |
| DeepSeek | Agent Graph / 跨服务 Agent 化 | A3/A4 | 与既有 Agent 编排衔接；不引入 Spring AI Alibaba |
| DeepSeek | Spring Cloud Kubernetes（只读配置） | C9 | K8s 落地后的低成本补充；不替代 Nacos |
| 桌面文档 | 技能库 → MCP | A1/A2 | 报告已论证，前置（拆分/OAuth2）已完成 |
| 桌面文档 | 微信小程序 | S3/S4 | 后端改动小；平台流程是主门槛，先做身份与会话 |

否决 / 触发条件：

| 建议 | 结论 | 理由 |
|---|---|---|
| RocketMQ 事务消息 | 否决 | 12.9 已用生产方 outbox 取代；半消息要求 broker 在线 |
| Spring Cloud Config | 否决 | 与 Nacos 配置中心重叠 |
| Spring Cloud Bus | 否决 | Nacos 配置刷新已覆盖 |
| Spring Cloud Consul | 否决 | 与 Nacos 服务发现重叠 |
| Spring Cloud Task | 否决 | 与 Quartz / Spring Batch 职责重叠 |
| Spring Cloud Data Flow | 否决 | 过重；流处理需求由 Flink 触发条件承接 |
| Spring Statemachine | 否决 | Flowable 已覆盖流程编排；任务生命周期用枚举/状态字段足够 |
| Spring AI Alibaba | 否决 | 仅 M 版预发布，违反稳定线要求 |
| Spring Cloud Function | 触发条件 | 当前不走 Spring Cloud Stream binder；无 Serverless/FaaS 需求 |
| Spring Cloud Vault | 触发条件 | KMS/ACK Secret 足够；需要动态凭据时再评估 |
| Spring LDAP | 触发条件 | 需真实企业目录环境 |
| 小程序管理后台移动版 | 否决 | 表单密集、权限复杂、审核风险高；只做员工轻量入口 |

---

## 13. 外部事实来源

- Spring AI 2.0.0 GA：https://spring.io/blog/2026-06-12/spring-ai-2-0-0-GA-available-now
- Spring AI 2.0.1：https://spring.io/blog/2026-08-21/spring-ai-2-0-1-available-now
- Spring AI × Boot 4 兼容 EPIC：https://github.com/spring-projects/spring-ai/issues/3379
- Spring AI Alibaba 版本说明：https://java2ai.com/docs/versions
- Spring AI Alibaba Releases：https://github.com/alibaba/spring-ai-alibaba/releases
- AgentScope Java 发布说明（2.0.1）：https://java.agentscope.io/v2/en/docs/others/release-notes.html
- Spring Cloud Alibaba：https://github.com/alibaba/spring-cloud-alibaba
- Apache Seata 2.5.0：https://github.com/apache/incubator-seata/releases/tag/v2.5.0
- Seata AT 模式：https://seata.apache.org/docs/user/transaction-mode/at
- SCA 2025.x 版本说明：https://sca.aliyun.com/docs/2025.x/overview/version-explain/
- SCA Nacos bootstrap 移除：https://github.com/alibaba/spring-cloud-alibaba/issues/4253
- SCA 对 Boot 4.1 的升级中：https://github.com/alibaba/spring-cloud-alibaba/issues/4339
- SkyWalking 11.1.0：https://github.com/apache/skywalking/releases/tag/v11.1.0
- SkyWalking Java Agent 9.8.0：https://github.com/apache/skywalking-java/releases/tag/v9.8.0
- Dubbo 对 Boot 4 支持缺失：https://github.com/apache/dubbo/issues/15996
- Spring Boot × GraalVM：https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-with-GraalVM
- Boot 4 native 需 GraalVM 25 工具链：https://noregressions.dev/guides/migration/cards/graalvm-25.html
- Spring Boot 版本与支持窗口：https://endoflife.date/spring-boot
- DeepSeek 分享对话（2026-11-25，讨论输入而非版本事实）：https://chat.deepseek.com/share/rslplogwwjdwhky5r7
- 桌面可行性分析（2026-10-30 SKILL/MCP；2026-11-13 微信小程序），内容已折叠进 §4 与 §6。
