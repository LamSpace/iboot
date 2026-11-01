# iBoot 演进路线图

> 本文只保留**尚未完成**的路线与**仍然生效**的约束；已完成能力见 [§2 能力索引](#2-能力索引)，
> 用户视角的功能清单与启动方式见 [README](README.md)，每次交付的完整过程见 `openspec/changes/archive/`。
> 项目性质：技术栈练习项目，在可运行的管理系统之上逐步长出**微服务**、**云原生**、**智能 Agent** 三块能力。
> 每个条目都带验收判据，判据不成立的条目不进路线图。最近更新：2026-11-01

## 目录

1. [目标与完成判据](#1-目标与完成判据)
2. [能力索引](#2-能力索引)
3. [未完成路线](#3-未完成路线)
4. [版本矩阵](#4-版本矩阵)
5. [风险登记](#5-风险登记)
6. [本地开发环境与成本红线](#6-本地开发环境与成本红线)
7. [决策记录](#7-决策记录)

---

## 1. 目标与完成判据

| 能力 | 本项目的定义 | 完成判据 |
|---|---|---|
| **微服务** | 现有单体按 DDD 限界上下文**彻底拆分**为多个独立可部署服务，Spring Cloud Alibaba 技术栈真实承载（非摆设） | 任一服务可独立启停与发布；单服务故障不雪崩；跨服务强一致写具备可回滚的分布式事务 |
| **云原生** | 所有服务容器化，Helm 化，运行在阿里云 ACK，具备弹性、可观测、声明式发布 | `helmfile sync` 一条命令完成发布；滚动升级零 5xx；压测触发 HPA；删 Pod 自愈 |
| **智能 Agent** | 以百炼 Qwen 为对话底座的多能力 AI 工作台：对话、RAG、智能体编排、知识图谱 | Agent 能安全地调用既有管理端 API 完成任务；RAG 检索结果继承用户数据权限 |

约束与原则：

1. **验收判据优先**：无判据的条目不写。写代码前先定义「什么算完成」。
2. **YAGNI**：与主线弱相关、或职责与已选方案重叠的条目，显式剔除而非长期挂账。
3. **复用既有基础设施优先**：已有 ES、MinIO、Redis、Quartz、SSE、CloudEvents、二级缓存、可观测栈——先榨干存量，再引入新中间件。
4. **不破坏 DDD 边界**：新增能力必须待在既有四层结构内，否则后续拆分成本被推高。
5. **前端契约稳定**：portal 只访问网关 `/api/**`，接口归属变化只改路由表。
6. **不为练手而降级主线**：工程已站在 Boot 4.0.3 上，不为了迁就旧版生态而退回已 EOL 的 Boot 3.5。
7. **成本可见**：任何消耗云账单或本机内存的能力，必须自带用量观测。

---

## 2. 能力索引

已完成能力，一项一句话。实现细节与操作方式见「文档」列；用户视角的功能清单见 [README](README.md)。

| 能力 | 一句话 | 归属服务 | 文档 |
|---|---|---|---|
| 认证与授权 | OAuth2 / OIDC 授权码 + PKCE、JWK 验签、RBAC 与五级数据权限 | `iboot-auth`、`iboot-kernel`、`iboot-system` | [doc/23](<./doc/23-微服务底座操作指南.md>) |
| 系统与组织 | 用户、角色、菜单、字典、参数，以及部门树、岗位与组织架构图 | `iboot-system` | — |
| 消息与推送 | 站内消息、公告、模板、多渠道投递，SSE 单播/组播/广播与集群广播 | `iboot-notify` | [doc/20](<./doc/20-多渠道通知操作指南.md>)、[doc/15](<./doc/15-SSE 推送集成.md>) |
| 文件管理 | 上传、下载、预览、分类，MinIO / 本地 / OSS 存储抽象 | `iboot-file` | — |
| 定时任务 | Quartz 集群任务 CRUD、启停与执行日志 | `iboot-job` | — |
| 数据备份 | 全量逻辑备份、AES-256-GCM 加密、独立演练库恢复校验 | `iboot-job` | [doc/21](<./doc/21-数据备份与恢复操作指南.md>) |
| 审批工作流 | Flowable 8 请假流程、审批中心与通知登记 | `iboot-workflow` | — |
| 日志与监控 | 操作/登录/运行日志、在线会话、服务与中间件资源检查 | `iboot-monitor` | [doc/11](<./doc/11-可观测性设计.md>) |
| 统计分析 | 仪表板、趋势统计与 Excel 导出 | `iboot-system` | — |
| AI 对话 | 多轮流式对话、Redis 会话记忆、每日配额与 token 观测 | `iboot-ai` | [doc/22](<./doc/22-AI 工作台操作指南.md>) |
| 知识库 RAG | PDF/DOCX/TXT 解析、分块、Embedding、BM25 + kNN + RRF、引用溯源 | `iboot-ai` | [doc/22](<./doc/22-AI 工作台操作指南.md>) |
| 智能体编排 | 受控工具调用、写操作二次确认、运行审计与确定性回放 | `iboot-ai` | [doc/22](<./doc/22-AI 工作台操作指南.md>) |
| 知识图谱 | 组织与 RBAC 入图、多跳查询与名称解析 | `iboot-ai` | [doc/22](<./doc/22-AI 工作台操作指南.md>) |
| 国际化 | 前端 vue-i18n + 后端 MessageSource 中英文语言包 | `iboot-portal` + 各服务 | [doc/17](<./doc/17-国际化设计.md>) |
| 可观测性 | Actuator + Micrometer + Prometheus/Grafana + ELK + Tempo 链路追踪 | 全部服务 | [doc/11](<./doc/11-可观测性设计.md>) |

---

## 3. 未完成路线

```
阶段 2 收尾  -. 按需，不阻塞 .->  阶段 3 云原生  -->  阶段 4 深水区
```

### 阶段 2 收尾

阶段 2 的五项验收判据中四项已达标（TraceID 贯穿、故障隔离、Nacos 配置热更新、前端零改动），唯一未达标的是 Seata 分布式事务。它并非拆分的前置条件，而是「出现真实的跨服务强一致写」时才需要的能力。

| 项 | 现状 | 复核方式 |
|---|---|---|
| Seata 分布式事务 | 未引入。当前跨服务写入都是旁路（审计、通知），失败不影响主流程 | `grep -ri seata --include=pom.xml` 无命中 |
| Sentinel 业务接入 | 仅在 2.0 门禁的 Spike 中验证过，业务服务与网关尚未接入规则 | 各服务 pom 无 sentinel 依赖 |
| RocketMQ | 未引入。审批 → 通知走内部 HTTP 端点并退避重试 | `grep -ri rocketmq` 无命中 |
| `iboot_admin` schema 改名 | 保留历史名称。改名需要停机窗口，且 `flyway_schema_history` 必须随数据迁移 | `iboot-system` / `iboot-auth` 数据源仍指向 `iboot_admin` |
| 遗留表清理 | `iboot_admin` 中仍有前几次拆分遗留的 25 张表 | 见 [doc/23](<./doc/23-微服务底座操作指南.md>) 各 `remove-*` 变更的记录 |

### 阶段 3：云原生（ACK + Helm）

| 编号 | 内容 | 技术栈 | 约束 | 验收判据 |
|---|---|---|---|---|
| 3.1 | 镜像工程 | 每服务 Dockerfile + 非 root 运行 + 推 **ACR** | 当前仅 `iboot-system` 与 `iboot-portal` 有 Dockerfile；基础镜像需与 GraalVM/native 路线兼容 | 每服务镜像可构建、可启动 |
| 3.2 | Helm 组织 | **Helmfile + 一个通用 library chart** | 避免 9 份重复 chart 样板 | `helmfile sync` 幂等 |
| 3.3 | K8s 清单 | Deployment / Service / ConfigMap / Secret / HPA / PDB / NetworkPolicy / Ingress(ALB) / 命名空间(dev, prod) | Secret 走 KMS 或 ACK Secret 管理，不入仓 | 滚动升级零 5xx；删 Pod 自愈 |
| 3.4 | 中间件分层混合 | 本地 Kind 全自建（MySQL/Redis/ES/Nacos/RocketMQ）练手；云上数据层用 RDS MySQL / Tair / 阿里云 ES / **OSS（替 MinIO）** / MSE Nacos / 云 RocketMQ | 云上跑有状态中间件的学习收益 < 运维成本；MinIO→OSS 依赖 `FileStorageStrategy` 抽象 | 本地与云上同一份镜像、不同 values 可跑 |
| 3.5 | 可观测上云 | 指标：ARMS 托管 Prometheus 或 kube-prometheus-stack；日志：Logtail→SLS（本地 ELK 保留）；追踪：Tempo / ARMS | SLS 按量计费需设采集白名单；Thanos 在云上可被托管方案替代 | 云上 Grafana 看到同一套业务面板 |
| 3.6 | CI/CD | GitHub Actions 或云效 Flow：构建 → 镜像推 ACR → `helmfile sync` → dev 晋升 prod → `helm rollback` | 密钥走 CI Secrets，不入仓 | 一次提交自动完成 dev 发布与回滚演练 |
| 3.7 | 弹性与成本 | request/limit 调优、HPA 按 CPU + 自定义 QPS 指标、突发负载用 ECI/ASK | 常驻资源是主要账单 | 压测触发 HPA 扩容且账单可观测 |

### 阶段 4：深水区选做

| 条目 | 内容 | 约束 / 前置 |
|---|---|---|
| **服务网格零信任** | ACK ASM 或 Istio，mTLS 全链路 + 服务间授权策略 | 改变运维模型，必须在服务稳定后；sidecar 开销与虚拟线程 / native 路线需一并评估 |
| **GraalVM Native** | 先对 `iboot-gateway`（WebFlux、无 MyBatis、反射面最小）做可行性验证；真实动机是缓解 N×JVM 内存与冷启动 | Boot 4 native 要求 GraalVM **25** 工具链；Druid / MyBatis / ES / MinIO / Quartz 的反射与代理风险清单依然成立 |
| **分库分表** | ShardingSphere-JDBC | 独立 schema 与拆分后的服务边界是其前置；× Boot 4 兼容需验证 |
| **Dubbo RPC** | 内网热点调用替换 | 等待 Boot 4 兼容版本（见 §5 R2） |
| **Spring Batch** | 批处理（报表、备份、对账） | 与 Quartz 职责划清 |
| **多租户 SaaS 化** | `tenant_id` + MyBatis 插件 + Nacos 命名空间隔离 | 横切改造，会让每张表都背租户改动、污染拆分边界；拆分完成后再施加更合理 |
| **Spring LDAP** | 企业目录接入 | 需真实 LDAP 环境 |
| **日志/行为流 → Flink** | Kafka + Flink 一条线 | 仅在有真实流分析场景时启动 |

---

## 4. 版本矩阵

锁定的事实基线，除非明确另起变更，否则不升级、不混搭。

| 组件 | 目标版本 | 约束来源 |
|---|---|---|
| Java | 25 | 现状 |
| Spring Boot | **4.0.x（锁死，不升 4.1）** | SCA 对 Boot 4.1 适配仍在进行（#4339 / #4358） |
| Spring Cloud / SCA | 2025.1.x / **2025.1.0.0** | SCA 首个支持 Boot 4.0.x 的版本线 |
| Nacos | 3.1.1+（client/server） | bootstrap 方式已移除，必须用 `spring.config.import` |
| Sentinel / Seata | 1.8.9 / 2.x | 随 SCA，勿自行混搭 |
| Spring AI | **2.0.1（GA）** | Boot 4 必需；1.1.x 仅到 Boot 3.5 |
| AgentScope Java | 2.0.1（GA） | JDK 25 已 Spike 通过；starter 构建于 Boot 4.0.3 |
| Spring AI Alibaba | **暂不引入** | 仅 M 版预发布 |
| 对话模型 | **百炼 `qwen3.8-flash`**（备用 `qwen3.7-flash`） | thinking 默认开启，须显式关闭 |
| DeepSeek（备用） | `deepseek-flash`（OpenAI 兼容端点） | 仅用于故障切换，不默认启用 |
| Embedding | 百炼 `text-embedding-v4` 或本地 BGE-M3 | 第二供应商必然存在 |
| OCR 模型 | 百炼 `qwen3.5-ocr`（可回退 `qwen-vl-ocr-2025-11-20`） | 默认关闭 |
| MySQL / Redis / ES / MinIO | 8.0.45 / 7.0.14 / 9.2.3 / RELEASE.2025-09；ES 客户端 **9.2.5**（Boot 托管） | 客户端与服务端同属 9.2.x |
| Flowable / Neo4j | **8.0.0** / **5.26.29-community**（驱动 6.0.2） | Flowable 8 是唯一支持 Boot 4 的版本线；Neo4j 5.26 为 5.x LTS |
| Apache Dubbo | **阻塞** | 无 Boot 4 支持 |
| GraalVM（若做 native） | 工具链必须 **25** | Boot 4 native 构建要求 |

数据边界：每个服务拥有独立 schema 的**写入所有权**；跨 schema 只读是**长期接受**模式，不是待还的债。消费清单与写入例外见 [doc/23](<./doc/23-微服务底座操作指南.md>) §11.12。

---

## 5. 风险登记

只保留尚未消除的风险。

| ID | 风险 | 影响阶段 | 处置 |
|---|---|---|---|
| R2 | Dubbo 不支持 Boot 4 | 3 / 4 | 服务间 HTTP 先行；延后至出现 Boot 4 兼容版本后重评 |
| R4 | 本机内存：N×JVM + 21 个中间件容器 | 全阶段 | 按需 profiles 只跑改动服务；Neo4j 置于 `graph` profile 且堆/页缓存显式限制；mock 依赖服务；native 列为阶段 4 缓解手段 |
| R6 | LLM token 与云账单失控 | 全阶段 | 配额 + 用量看板 + Alertmanager 告警 |
| R12 | 存量文档与实现漂移 | 全程 | 新增或修改能力时文档与代码同 PR |
| R13 | 对话与 Embedding 同属百炼，单一供应商故障 | 3 / 4 | base-url 与模型配置化；对话可切 DeepSeek，Embedding 暂无等价兜底 |

---

## 6. 本地开发环境与成本红线

| 项 | 红线 | 观测手段 |
|---|---|---|
| LLM token | 必须有日配额与告警 | AI 工作台的用量看板 |
| 云资源 | ACK 常驻节点 + LB + SLS 为大头；数据层走托管按量 | 阶段 3.7 |
| 本机内存 | 拆分后至少 16GB 才舒适；优先按服务裁剪 | 按需启动 |
| 云 API 密钥 | 不入仓，仅 env / Nacos / KMS | `.env.example` 只留占位 |

本地开发策略：

- 中间件：`docker/docker-compose.yml`，开发默认只起 mysql / redis / minio / es / nacos / tempo；阶段 3 起本地增加 Kind 集群。
- 应用：`scripts/dev-init.sh` 一键拉起九个后端服务并按依赖顺序等待健康；也可单独 `mvn spring-boot:run` 调试。
- 密钥：统一放根目录 `.env`（已在 `.gitignore` 中），模板见 `.env.example`。

---

## 7. 决策记录

### 7.1 GraalVM Native 计划的沉淀与勘误

原 `graalvm-native-image-plan.md` 以 iboot-admin 全量 native 化为目标。其**风险判断依然成立**，其**构建方法已过期**。

仍然有效的部分：Druid（反射 + 动态代理）、MyBatis（XML + 反射）、ES Client、MinIO（OkHttp 代理）、Quartz、jjwt、Logstash Encoder 属于高风险依赖；`RedisConfig`、`SecurityConfig`、`JwtAuthenticationFilter`、`TwoLevelCacheManager`、`LogAspect`、`DataScopeInterceptor` 是代码层风险点；构建期内存大、编译慢 5–10 倍、跨平台分别编译、调试困难；回滚原则是保留 JVM 部署路径。

勘误（照原样执行会失败）：

| 原文写法 | 现状 |
|---|---|
| 添加依赖 `spring-boot-starter-native` | **该 artifact 不存在**；native 支持由 Boot 自身 + `native-maven-plugin` 承担 |
| `gu install native-image` / `--report-unsupported-elements-at-runtime` | 均已废弃或移除 |
| 手写 `reflect-config.json` 为主路径 | 首选 `RuntimeHints` / `@ImportRuntimeHints` / `@RegisterReflectionForBinding` |
| 未指定构建期 JDK | Boot 4 native 构建**要求 GraalVM 25 工具链** |
| 目标：iboot-admin 全量 | **改判**：从 `iboot-gateway` 这类无 ORM、反射面最小的服务起步 |

**否决项**：不采纳「以 iboot-admin 全量 native 化为近期目标」。它不消解任何当前功能缺口，收益（冷启动、内存）在单体阶段没有消费者；等到 N×JVM 内存压力与弹性冷启动需求出现后，它才有明确的服务对象。

### 7.2 AI 底座：为什么不是 langchain4j / Spring AI Alibaba / Python 版 AgentScope

- **Spring AI Alibaba**：Boot 4 上只有预发布 M 版且 pin 在非 GA 的 Spring AI，不满足「稳定线」要求。
- **LangChain4j**：框架成熟且 provider 无关，但与 Spring AI 抽象层职责重叠；同时引入两套 AI 抽象会挤掉真正要学的东西（模型接入不是难点，权限与检索质量才是）。
- **AgentScope（Python 版）**：会在 Java 微服务旁引入一个 Python 服务，偏离练 Java 技术栈的主线；而 **AgentScope Java 2.0.1 已 GA**，同一生态、同一种语言，Agent Card 可注册进 Nacos。
- **模型全走百炼**：一个 Key、一本账单、一个兼容端点；代价是单一供应商故障半径重叠，已用配置化 base-url 与 DeepSeek 备用端点缓解。

### 7.3 阶段顺序：为什么 AI 在微服务之前

AI 是**新增部署单元**而非改造存量，不依赖拆分底座；先补全功能再拆分，能让每个限界上下文在拆出时就是完整可测的业务体。唯一附加的硬性条件在实施时已落实：AI 必须是独立模块，不能是单体内的包。

被推迟的选项：多租户（阶段 0 → 阶段 4），横切改造会污染拆分边界；服务网格（→ 阶段 4），会改变运维模型，应在服务数量稳定后引入。

### 7.4 短信渠道为何默认关闭

阿里云自 2025-06-06 起不再接受个人实名账号申请国内短信自用资质，签名必须完成运营商实名报备。本项目当前只有个人账号，因此保留阿里云 Dysmsapi 适配器，但 `iboot.notification.sms.enabled=false`，不创建短信投递记录；站内、邮件、钉钉三个渠道承担验收。待有企业或个体工商户资质后，只需配置签名与模板 Code 即可启用。

### 7.5 认证服务不签发刷新令牌

Spring Authorization Server 对「公共客户端 + 授权码」不签发刷新令牌，因此**没有静默续期**。会话语义由共享 Redis 会话的滑动续期承担：资源服务不因令牌自身过期而拒绝请求，访问令牌有效期与会话超时（30 分钟）对齐。前端仍保留「有刷新令牌才续期」的分支。

### 7.6 系统服务是唯一「停掉会连带」的拆分

系统服务宕机时，其余服务的数据权限查询在缓存过期后失败关闭，通知与审批的用户读取不可用；但**认证登录仍然可用**（认证服务直连数据库），已登录用户访问不带数据范围的接口也不受影响（JWT 自包含 + 共享 Redis 会话）。因此系统服务的故障隔离判据是「**失败有界**」，而不是「其它域仍可用」；这也是跨 schema 只读被接受为长期模式的原因之一：改成远程调用会让可用性倒退。

---

## 附：外部事实来源

- Spring AI 2.0.0 GA：https://spring.io/blog/2026-06-12/spring-ai-2-0-0-GA-available-now
- Spring AI 2.0.1：https://spring.io/blog/2026-08-21/spring-ai-2-0-1-available-now
- Spring AI × Boot 4 兼容 EPIC：https://github.com/spring-projects/spring-ai/issues/3379
- Spring AI Alibaba 版本说明：https://java2ai.com/docs/versions
- Spring AI Alibaba Releases：https://github.com/alibaba/spring-ai-alibaba/releases
- AgentScope Java 发布说明（2.0.1）：https://java.agentscope.io/v2/en/docs/others/release-notes.html
- Spring Cloud Alibaba：https://github.com/alibaba/spring-cloud-alibaba
- SCA 2025.x 版本说明：https://sca.aliyun.com/docs/2025.x/overview/version-explain/
- SCA Nacos bootstrap 移除：https://github.com/alibaba/spring-cloud-alibaba/issues/4253
- SCA 对 Boot 4.1 的升级中：https://github.com/alibaba/spring-cloud-alibaba/issues/4339
- Dubbo 对 Boot 4 支持缺失：https://github.com/apache/dubbo/issues/15996
- Spring Boot × GraalVM：https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-with-GraalVM
- Boot 4 native 需 GraalVM 25 工具链：https://noregressions.dev/guides/migration/cards/graalvm-25.html
- Spring Boot 版本与支持窗口：https://endoflife.date/spring-boot
