<div align="center">

# iBoot

**把 Agent / Cloud / Security / Micro Service / SRE Native 做成系统默认属性的技术实践项目**

不是又一个后台管理系统：在可运行的通用管理平台上，把微服务边界、零信任、AI 执行单元、可观测与云原生发布逐条做成带验收判据的工程能力。

[English](README.md) · [中文](README.zh-CN.md)

<p align="center">
  <a href="https://github.com/vuejs/core"><img src="https://img.shields.io/badge/Vue.js-3.4-brightgreen?style=flat-square&logo=vue.js&logoColor=white" alt="Vue.js"></a>&nbsp;
  <a href="https://github.com/spring-projects/spring-boot"><img src="https://img.shields.io/badge/Spring%20Boot-4.0.3-blue?style=flat-square&logo=spring-boot&logoColor=white" alt="Spring Boot"></a>&nbsp;
  <a href="https://github.com/alibaba/spring-cloud-alibaba"><img src="https://img.shields.io/badge/Spring%20Cloud%20Alibaba-2025.1.0.0-orange?style=flat-square" alt="Spring Cloud Alibaba"></a>&nbsp;
  <a href="https://www.java.com/"><img src="https://img.shields.io/badge/Java-25-red?style=flat-square&logo=openjdk&logoColor=white" alt="Java 25"></a>&nbsp;
  <a href="https://www.docker.com/"><img src="https://img.shields.io/badge/Docker-Compose-2496ED?style=flat-square&logo=docker&logoColor=white" alt="Docker"></a>&nbsp;
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-Apache%202.0-blue?style=flat-square" alt="License"></a>
</p>

<p align="center">
  <a href="#为什么值得看">为什么值得看</a> •
  <a href="#五条-native-现状">五条 Native</a> •
  <a href="#架构一览">架构</a> •
  <a href="#技术栈速览">技术栈</a> •
  <a href="#核心能力">能力</a> •
  <a href="#快速开始">快速开始</a> •
  <a href="#文档地图">文档</a>
</p>

</div>

## 为什么值得看

- **五条 Native 是验收标准，不是口号。** Agent、Cloud、Security、Micro Service、SRE 每条线都写清了「什么算完成」，未达成判据的能力不进入已完成清单，避免把「装过组件」当成「已经具备」。
- **微服务是真的拆开了。** 八个业务服务、一个网关、一个旁路运维控制台（共十个可部署服务）；DDD 四层、表所有权、Seata AT + TCC、生产方 outbox + RocketMQ 都在真实链路上承载，而不是停留在目录结构。
- **零信任是默认值。** OAuth2/OIDC + PKCE、服务间 mTLS、SPIFFE 工作负载身份、路径级授权、OPA 集中决策、每服务数据库账号、表/键级最小权限、CA 三阶段轮换与 SVID 自动刷新；本地开发默认开启，可用一个开关回退。
- **AI 是系统的一等执行单元。** 独立的 `iboot-ai` 服务承载流式对话、混合检索 RAG、受控工具编排与知识图谱；工具调用有白名单、写操作二次确认、运行审计与确定性回放，检索结果继承用户 RBAC 与数据权限。技能库与 MCP 是路线图的下一步。
- **工程闭环可验证。** `dev-init.sh` 一条命令拉起本地全栈；全栈回归覆盖契约快照、路径守恒、路由覆盖、TLS/授权、冒烟与权限矩阵；CLI + PAT 打通无浏览器自动化；所有能力契约沉淀在 `openspec/`。

> 这是一个面向技术实践的工程项目，不以生产级产品为交付目标。所有能力以可复现、可验证的工程判据为准，边界与取舍记录在 [ROADMAP](ROADMAP.md)。

## 五条 Native 现状

| 能力线 | 已经做到 | 下一步 |
|---|---|---|
| **Agent Native** | 流式对话、知识库 RAG（BM25 + kNN + RRF）、受控工具编排、组织/RBAC 知识图谱 | 技能库（SKILL）、MCP 客户端、Agent Graph、跨服务 Agent 化 |
| **Cloud Native** | Compose 全量本地环境、共享镜像模板、一键部署脚本 | Helmfile + ACK、HPA、GitOps（Argo CD）、GraalVM Native |
| **Security Native** | OAuth2/OIDC + PKCE、mTLS/SPIFFE、OPA、每服务凭据、证书轮换、CLI/PAT | Token Exchange（RFC 8693）、多客户端会话、微信小程序身份接入 |
| **Micro Service Native** | 业务服务拆分、Nacos 发现、Sentinel 治理、Seata AT/TCC、outbox + RocketMQ、契约/路由回归 | 消费者驱动契约测试、EventStorming 边界复盘、分库分表 |
| **SRE Native** | 指标、日志、链路追踪、告警、备份与恢复演练 | SLI/SLO、燃烧率告警、Trace-Log 联动、成本与弹性观测 |

完整路线、触发条件、版本矩阵与风险登记见 [ROADMAP](ROADMAP.md)。

## 架构一览

```text
Browser / CLI
   │  HTTPS · OAuth2/OIDC + PKCE
   ▼
iboot-portal ── /iboot/api ──▶ iboot-gateway :8088
                                      │ Nacos 服务发现 · 路由限流 · SSE 透传
                                      ▼
        ┌──────────────────────────────────────────────────┐
        │  八个独立业务服务                                  │
        │  system 8080   ai 8081        file 8082           │
        │  auth 8083     notify 8084    job 8085            │
        │  workflow 8086 monitor 8087                       │
        └──────────────────────────────────────────────────┘
                    │                          │
                    ▼                          ▼
   iboot-ops :8089（旁路运维控制台）    MySQL · Redis · Elasticsearch · Silo
                                        Nacos · RocketMQ · Kafka · Seata
                                        OPA · SPIRE · Tempo
                                        Prometheus · Grafana · ELK
```

- 前端只访问门户公开入口，由门户反代到网关；服务之间按 Nacos 服务名寻址，不经过网关。
- `iboot-ops`、Sentinel Dashboard 与 Nacos/Sentinel 控制台都是旁路：开发环境经门户同源代理，生产环境只允许内网访问。
- 所有服务共用一个数据库 `iboot`，但每张表只有一个所有者；只读共享是长期接受模式，清单见 [docs/23 §11.12](<./docs/23-微服务底座操作指南.md>)。
- 服务与端口的权威清单见 [docs/23 §8.1](<./docs/23-微服务底座操作指南.md>)；容器与网络边界见 [`docker/README.md`](docker/README.md)。

## 技术栈速览

| 层 | 选型 |
|---|---|
| 运行时 | Java 25（虚拟线程）、Spring Boot 4.0.3、Spring Framework 7、Spring Security 7 |
| 微服务 | Spring Cloud 2025.1.0、Spring Cloud Alibaba 2025.1.0.0、Nacos 3.3.0、Sentinel、Seata、RocketMQ、Kafka |
| 数据 | MySQL 8.4、Redis 8.8 + Caffeine、Elasticsearch 9.5、Silo（MinIO 兼容）、Neo4j 5.26、Flowable 8、Quartz |
| 安全 | OAuth2/OIDC + PKCE、mTLS、SPIFFE、OPA、SPIRE、每服务数据库账号、CA/证书轮换 |
| AI | Spring AI 2.0.1、AgentScope Java 2.0.1、百炼 Qwen、BM25 + kNN + RRF 混合检索、Neo4j 知识图谱 |
| 可观测 | Actuator + Micrometer、Prometheus / Grafana / Alertmanager / Thanos、ELK、Grafana Tempo |
| 前端 | Vue 3.4、TypeScript 5.4、Vite 5.2、Element Plus、Pinia、vue-i18n |

完整版本矩阵与选型理由见 [docs/02-技术栈](docs/02-技术栈.md)；版本锁定与升级约束见 [ROADMAP §9](ROADMAP.md#9-版本矩阵)。

## 核心能力

- **系统与组织**：用户、角色、菜单、部门树、岗位、字典、参数；RBAC 与五级数据权限；仪表板与 Excel 导出。
- **消息与通知**：站内消息、公告、模板、多渠道投递（站内/邮件/钉钉/短信）、SSE 单播/组播/广播与集群广播、全文检索。
- **文件与检索**：上传、下载、预览、分类；Silo / 本地 / OSS 三种存储实现；Elasticsearch 内容检索。
- **审批与任务**：Flowable 8 请假审批、审批中心、驳回归档；Quartz 集群任务；备份与恢复演练。
- **监控与运维**：登录/操作/运行日志、在线会话与强制下线、服务与中间件资源检查、旁路运维控制台。
- **AI 工作台**：多轮流式对话、知识库 RAG、受控智能体工具、组织与 RBAC 知识图谱、每日配额与 token 观测。
- **开发者 CLI**：授权码 + PKCE 登录、多 context、只读查询、运维写命令、个人访问令牌（PAT）与 `iboot api` 逃生口。
- **服务治理**：网关路由级限流、跨服务熔断降级、Nacos 规则源、Sentinel Dashboard、审批 → 通知可靠投递。

按模块展开的功能说明见 [docs/04-功能模块](docs/04-功能模块.md)；AI 工作台见 [docs/22](<./docs/22-AI 工作台操作指南.md>)；CLI 见 [docs/25](<./docs/25-开发者 CLI 操作指南.md>)。

## 快速开始

**前置条件**：JDK 25、Maven 3.9+、Docker 与 Docker Compose、Node.js 18+ 与 cnpm（仅前端需要）。

```bash
# 1. 准备本地配置与密钥
cp .env.example .env
#    至少填写百炼 API Key；其余按需

# 2. 生成本地 CA/证书，并把 CA 装入浏览器信任库（一次性）
bash scripts/tls-init.sh
CAROOT=docker/certs/ca mkcert -install

# 3. 拉起中间件、Sentinel、数据库迁移、八个业务服务、网关与运维控制台
bash scripts/dev-init.sh

# 4. 需要前端时（HTTPS 开发服务器）
bash scripts/dev-init.sh --frontend
```

- 前端：<https://localhost:5173>
- 默认管理员：`admin` / `admin123456`（首次登录后请立即修改）
- 网关健康检查：<https://localhost:8088/actuator/health>（内部地址，需要客户端证书）
- 常用参数：`--status` 查看状态，`--stop` / `--down` / `--purge` 停止或清理，`--build` 强制重建后端。

完整启动、手工启动、数据库初始化与排障见 [docs/05-快速开始](docs/05-快速开始.md)；证书与信任见 [docs/27](<./docs/27-TLS 与零信任操作指南.md>)。

## 构建、测试与回归

```bash
# 一键构建前后端
bash scripts/build.sh

# 后端全量构建与单模块测试
mvn clean package
mvn -pl iboot-system test

# 全栈回归：契约快照 → 路径守恒 → 路由覆盖 → TLS/授权 → 冒烟 → 权限矩阵
bash scripts/regress/regress.sh
bash scripts/regress/cli.sh
```

回归脚本要求八个业务服务、网关、运维控制台与中间件已启动；`--refresh` 仅在确认接口差异合理时用于重建基线。CI 配置见 `.github/workflows/regress.yml`。回归层次与脚本清单见 [docs/23 §1](<./docs/23-微服务底座操作指南.md>) 与 [`scripts/README.md`](scripts/README.md)。

## 部署形态

- **本地全量**：`scripts/docker-deploy.sh up` 构建镜像并启动 Compose 全栈；容器、端口、卷与排障见 [`docker/README.md`](docker/README.md)，环境与生产部署见 [docs/08-部署指南](docs/08-部署指南.md)。
- **云原生（规划中）**：Helmfile + ACK + HPA + Argo CD，本地 Kind 与云上 ACK 使用同一镜像、不同 values，见 [ROADMAP §5 Cloud Native](ROADMAP.md#cloud-native)。

## 文档地图

| 文档 | 内容 |
|---|---|
| [docs/README.md](docs/README.md) | 全部项目文档的入口与阅读路径 |
| [ROADMAP.md](ROADMAP.md) | 五条 Native 路线、版本矩阵、风险登记与决策记录 |
| [docs/03-架构设计](docs/03-架构设计.md) | 模块拓扑、DDD 分层、数据流、权限模型 |
| [docs/23-微服务底座操作指南](<./docs/23-微服务底座操作指南.md>) | 服务清单、Nacos、网关、链路追踪、Sentinel、Seata、最小权限 |
| [docs/27-TLS 与零信任操作指南](<./docs/27-TLS 与零信任操作指南.md>) | 本地 CA、mTLS、SPIFFE、OPA、轮换与排障 |
| [docs/11-可观测性设计](docs/11-可观测性设计.md) | 指标、日志、追踪、告警与 Grafana 面板 |
| [docs/22-AI 工作台操作指南](<./docs/22-AI 工作台操作指南.md>) | 模型配置、RAG、智能体与知识图谱 |
| [docs/25-开发者 CLI 操作指南](<./docs/25-开发者 CLI 操作指南.md>) | CLI 安装、登录、context、命令与安全边界 |
| [openspec/specs](openspec/specs) | 能力契约与变更记录 |

> 英文 [README.md](README.md) 是默认的项目概览；`docs/` 下的详细文档目前以中文维护。

## 项目结构

```text
iboot/
├── pom.xml                  # 父 POM，聚合 16 个 Maven 模块
├── bin/iboot                # 开发者 CLI 启动脚本
├── docker/                  # Compose 编排、镜像模板与中间件初始化
├── scripts/                 # 开发、部署与回归脚本
├── docs/                    # 项目文档，入口 docs/README.md
├── openspec/                # 能力契约与变更记录
├── iboot-kernel/            # 共享内核库（不独立部署）
├── iboot-service-authz/     # 服务间授权库（不独立部署）
├── iboot-logging/           # 日志共享库（不独立部署）
├── iboot-sentinel/          # 治理指标桥接库（不独立部署）
├── iboot-db-init/           # 建库与集中式迁移，运行后退出
├── iboot-gateway/           # 网关 :8088
├── iboot-system/            # 系统域 :8080
├── iboot-ai/                # AI 工作台 :8081
├── iboot-file/              # 文件服务 :8082
├── iboot-auth/              # 认证服务 :8083
├── iboot-notify/            # 消息与通知 :8084
├── iboot-job/               # 调度与备份 :8085
├── iboot-workflow/          # 审批工作流 :8086
├── iboot-monitor/           # 监控与日志 :8087
├── iboot-ops/               # 运维控制台 :8089
├── iboot-cli/               # 开发者命令行工具（不独立部署）
└── iboot-portal/            # 前端（Vue 3 + Vite）
```

业务服务内部遵循 DDD 四层：`interfaces` → `application` → `domain` → `infrastructure`；网关与运维控制台没有业务领域，不强行套用四层结构。

## 许可证

Licensed under the Apache License, Version 2.0. 详见 [LICENSE](LICENSE)。
