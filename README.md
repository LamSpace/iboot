<div align="center">

# iBoot

<p align="center">
  <a href="https://github.com/vuejs/core"><img src="https://img.shields.io/badge/Vue.js-3.x-brightgreen?style=flat-square&logo=vue.js&logoColor=white" alt="Vue.js"></a>&nbsp;
  <a href="https://github.com/spring-projects/spring-boot"><img src="https://img.shields.io/badge/Spring%20Boot-4.0.x-blue?style=flat-square&logo=spring-boot&logoColor=white" alt="Spring Boot"></a>&nbsp;
  <a href="https://www.typescriptlang.org/"><img src="https://img.shields.io/badge/TypeScript-5.x-blue?style=flat-square&logo=typescript&logoColor=white" alt="TypeScript"></a>&nbsp;
  <a href="https://nodejs.org/"><img src="https://img.shields.io/badge/Node.js-18%2B-green?style=flat-square&logo=node.js&logoColor=white" alt="Node.js"></a>&nbsp;
  <a href="https://www.mysql.com/"><img src="https://img.shields.io/badge/MySQL-8.0-red?style=flat-square&logo=mysql&logoColor=white" alt="MySQL"></a>&nbsp;
  <a href="https://redis.io/"><img src="https://img.shields.io/badge/Redis-7.0-red?style=flat-square&logo=redis&logoColor=white" alt="Redis"></a>
</p>

<p align="center">
  <strong>一个前后端分离的微服务管理系统：九个可独立部署的业务服务、独立运维控制台与统一网关，DDD 分层，带 AI 工作台。</strong><br>
  Vue 3 + TypeScript + Vite / Java 25 + Spring Boot 4.0.3 + Spring Cloud Alibaba
</p>

<p align="center">
  <a href="#项目定位">项目定位</a> •
  <a href="#架构总览">架构总览</a> •
  <a href="#技术栈">技术栈</a> •
  <a href="#功能清单">功能清单</a> •
  <a href="#快速开始">快速开始</a> •
  <a href="#目录结构">目录结构</a> •
  <a href="#构建测试与回归">构建与测试</a> •
  <a href="#镜像与部署现状">部署</a> •
  <a href="#文档导航">文档</a>
</p>

</div>

## 项目定位

iBoot 是一个面向技术实践的通用管理系统。它在 DDD 分层与 RBAC 权限模型之上，逐步长出三块能力：

- **微服务**：单体按限界上下文拆分为九个独立可部署的业务服务，并增加一个独立的运维控制台；注册中心、配置中心、网关、链路追踪与 Sentinel 治理真实承载。
- **云原生**：容器化与 Helm 化路线（尚未实施，见 [ROADMAP](ROADMAP.md)）。
- **智能 Agent**：以百炼 Qwen 为底座的对话、RAG、智能体编排与知识图谱。

已完成能力见 [ROADMAP 的能力索引](ROADMAP.md#2-能力索引)，未完成路线与仍然生效的技术约束也在同一份文件里。

## 架构总览

```
浏览器
  |  /iboot/api          （开发期由 Vite 代理，生产由 nginx 代理）
  v
[iboot-portal] --代理--> [iboot-gateway :8088] --路由--> iboot-system   :8080
                                                          iboot-ai       :8081
                                                          iboot-file     :8082
                                                          iboot-auth     :8083
                                                          iboot-notify   :8084
                                                          iboot-job      :8085
                                                          iboot-workflow :8086
                                                          iboot-monitor  :8087

[iboot-portal] --/sba 同源代理（开发/内网）--> [iboot-ops :8089] --Nacos 发现--> 上述业务服务

[iboot-gateway / iboot-workflow / iboot-notify] --规则与指标--> [sentinel-dashboard :8858]
```

前端业务接口只访问网关；服务之间的调用按 Nacos 服务名寻址，不经过网关。运维控制台是旁路，
开发环境经 Vite 代理以同源 iframe 打开，生产环境只允许内网访问，公网网关不提供 `/sba` 路由。
Sentinel Dashboard 同样是旁路：只用于治理观察与临时演练，规则以 Nacos 为唯一持久化来源。

**服务与端口**

| 服务 | 端口 | 职责 | 主要表组 |
|---|---|---|---|
| `iboot-gateway` | 8088 | 统一入口、路由、跨域、SSE 长连接透传 | — |
| `iboot-system` | 8080 | 用户、角色、菜单、部门、岗位、字典、参数；仪表板与统计；数据权限范围 | 系统与组织 |
| `iboot-ai` | 8081 | 对话、知识库 RAG、智能体编排、知识图谱 | `ai_*` |
| `iboot-file` | 8082 | 文件上传、下载、预览与分类 | `sys_file` |
| `iboot-auth` | 8083 | OAuth2 / OIDC 授权服务器、登录页、登出与注册 | 系统表（直连） |
| `iboot-notify` | 8084 | 消息中心、公告、模板、多渠道投递、SSE 推送、内容检索 | `sys_message*` / `sys_notice*` |
| `iboot-job` | 8085 | 定时任务调度、数据备份与恢复 | `sys_job*` / `QRTZ_*` / 备份记录 |
| `iboot-workflow` | 8086 | 请假审批与 Flowable 流程引擎 | `ACT_*` / `FLW_RU_*` / 审批表 |
| `iboot-monitor` | 8087 | 操作/登录/运行日志、在线会话、资源监控 | `sys_operate_log` / `sys_login_log` |
| `iboot-ops` | 8089 | 运维控制台：注册中心发现实例、单实例巡检、运行期日志级别调整 | — |
| `sentinel-dashboard` | 8858 | 治理控制台：单资源流量、阻塞与规则查看；本地必选，生产必须认证 | — |
| `iboot-kernel` | — | 共享内核库（统一响应、鉴权、审计写入），不独立部署 | — |
| `iboot-sentinel` | — | 治理指标桥接库（MetricExtension → Micrometer），不独立部署 | — |

**数据边界**：所有服务共用一个数据库 `iboot`；每张表只有一个所有者，其余服务只读登记的表与列。
结构由 `iboot-db-init` 统一迁移，服务不执行 DDL。消费清单与写入例外见
[docs/23](<./docs/23-微服务底座操作指南.md>) §11.12。

## 技术栈

**后端**

| 领域 | 选型 |
|---|---|
| 运行时 | Java 25（虚拟线程已启用） |
| 框架 | Spring Boot 4.0.3、Spring Framework 7、Spring Security 7 |
| 微服务 | Spring Cloud 2025.1.0、Spring Cloud Alibaba 2025.1.0.0（Nacos 3.1.1、Sentinel 1.8.9）、Spring Cloud Gateway |
| 持久层 | MyBatis 4.0.1 + PageHelper 2.1.1、Druid 1.2.28、Flyway、MySQL 8.0.45 |
| 缓存 | Redis 7.0.14 + Caffeine 3.1.8（二级缓存，Pub/Sub 失效广播） |
| 认证 | OAuth2 / OIDC 授权码 + PKCE（Spring Authorization Server）、JWT、RBAC 与五级数据权限 |
| 文件与检索 | MinIO 8.5.7（另有本地与 OSS 实现）、Elasticsearch 9.2.3（客户端 9.2.5） |
| 流程与调度 | Flowable 8.0.0、Quartz（集群模式） |
| AI | Spring AI 2.0.1、AgentScope Java 2.0.1、Neo4j 5.26.29（驱动 6.0.2）、PDFBox 3.0.5、Apache POI 5.4.1 |
| 其他 | MapStruct 1.6.3、FastExcel 0.19.0、CloudEvents 4.0.1、Springdoc 3.0.2、Testcontainers 2.0.5 |
| 可观测 | Actuator + Micrometer、Prometheus / Grafana / Alertmanager / Thanos、ELK、Tempo 链路追踪 |

**前端**

| 领域 | 选型 |
|---|---|
| 框架 | Vue 3.4、TypeScript 5.4、Vite 5.2 |
| UI 与状态 | Element Plus 2.6、Pinia 2.1、Vue Router 4.3 |
| 其他 | vue-i18n 9.14、axios 1.6、markdown-it、DOMPurify、Sass |

**中间件**：`docker/docker-compose.yml` 提供 26 个常驻容器，按 profile 分组启动（`graph` 为 Neo4j，`platform` 为 Nacos 与 Tempo）；Sentinel Dashboard 默认常驻；RocketMQ NameServer / Broker / Proxy 常驻，宿主机通过 `localhost:18081` 接入；Kafka（KRaft 单节点）作为日志缓冲层常驻，宿主机通过 `localhost:29092` 接入。

## 功能清单

**系统域 `iboot-system`**：用户、角色、菜单、部门树、岗位、字典、参数配置；数据权限范围；仪表板与趋势统计、Excel 导出。

**认证 `iboot-auth`**：OIDC 发现文档与 JWK 公钥集；浏览器授权码 + PKCE 登录；登出与自助注册；认证服务托管的登录页。

**文件 `iboot-file`**：上传、下载、预览、分类；MinIO / 本地 / OSS 三种存储实现按配置切换。

**消息与通知 `iboot-notify`**：站内消息、公告、消息模板、已读同步；站内 / 邮件 / 钉钉 / 短信四个投递渠道（短信默认关闭）；SSE 单播、组播、广播；公告与消息的全文检索。

**定时任务与备份 `iboot-job`**：在管理端维护定时任务、启停并查看执行日志；发起备份、查看备份记录、执行恢复演练，产物加密后落到 MinIO / 本地 / OSS。

**审批 `iboot-workflow`**：请假申请、审批中心、驳回与归档；审批待办事件经本地 outbox 与 RocketMQ 可靠投递。

**监控 `iboot-monitor`**：登录日志、操作日志、运行日志；在线会话与强制下线；服务资源、Redis、MinIO、SQL 监控入口。

**运维控制台 `iboot-ops`**：通过注册中心自动发现全部业务实例，提供健康、JVM、环境、配置、Bean、缓存、定时任务与线程巡检，并可在不重启的情况下调整日志级别。开发环境从门户的服务器监控页进入，`dev-init.sh` 默认关闭控制台认证以便 iframe 直接打开；生产环境保持认证开启且仅限内网访问。

**AI 工作台 `iboot-ai`**：多轮流式对话与 Redis 会话记忆；知识库解析、Embedding、BM25 + kNN + RRF 混合检索与引用溯源；受控智能体工具与写操作二次确认；组织与 RBAC 知识图谱。

**网关 `iboot-gateway`**：统一路由、跨域、SSE 长连接透传与服务降级。

**服务治理**：网关按路由限流（`429` 统一 JSON），`iboot-notify` 内部端点具备入口流控；规则存 Nacos 并热更新，Sentinel Dashboard（8858）为本地必选组件，治理指标进入 Prometheus。审批 → 通知已改为本地 outbox + RocketMQ，消费失败按重试进入死信队列。

## 快速开始

**前置条件**

- JDK 25、Maven 3.9+
- Docker 与 Docker Compose
- Node.js 18+ 与 cnpm（仅前端需要）

**一键启动（推荐）**

```bash
# 1. 准备本地配置与密钥
cp .env.example .env
#    至少填写百炼 API Key；其余按需

# 2. 拉起中间件（含 RocketMQ）、Sentinel Dashboard 与基线规则，构建后端并启动九个业务服务与运维控制台
bash scripts/dev-init.sh

# 3. 需要前端时
bash scripts/dev-init.sh --frontend
```

| 参数 | 作用 |
|---|---|
| `--build` | 强制重新构建后端 |
| `--frontend` | 安装前端依赖并启动开发服务器 |
| `--status` | 查看中间件、后端与前端状态 |
| `--stop` / `--down` / `--purge` | 停服务 / 连中间件一起停 / 连数据卷一起删 |

脚本会自动加载根目录 `.env`；需要自定义 Maven settings 时设置 `MAVEN_SETTINGS` 环境变量。
后端日志与进程号写入根目录 `logs/`。集中日志（Kafka → Logstash → ES）默认不在 `dev-init.sh`
中启用：需要时先 `docker compose -f docker/docker-compose.yml up -d kafka kafka-init`，再设置
`IBOOT_LOGGING_KAFKA_ENABLED=true` 与 `IBOOT_LOGGING_KAFKA_BOOTSTRAP_SERVERS=localhost:29092`，
详见 [docs/11 §11.11.1](./docs/11-可观测性设计.md)。

**手工启动**

```bash
# 中间件：开发必需的一组
docker compose -f docker/docker-compose.yml up -d mysql redis minio elasticsearch \
  rocketmq-namesrv rocketmq-store-init rocketmq-broker rocketmq-proxy rocketmq-init

# 可选：集中日志链路
docker compose -f docker/docker-compose.yml up -d kafka kafka-init logstash

# 构建并启动系统服务（其余服务同理）
mvn -DskipTests clean package
java -jar iboot-system/target/iboot-system-1.0.0.jar

# 前端
cd iboot-portal
cnpm install
cnpm run dev --host
```

启动服务前先运行 `iboot-db-init` 建库建表；`scripts/dev-init.sh` 会自动完成这一步：

```bash
mvn -pl iboot-db-init -am package
MYSQL_HOST=localhost MYSQL_PORT=3306 MYSQL_USERNAME=root MYSQL_PASSWORD=root \
  java -jar iboot-db-init/target/iboot-db-init-1.0.0-exec.jar
```

**访问与验证**

- 前端开发地址：<http://localhost:5173>，API 经 Vite 代理到网关 `8088`
- 网关健康检查：<http://localhost:8088/actuator/health>
- Sentinel Dashboard：<http://localhost:8858>（本地开发必选，生产需认证）
- 默认管理员：`admin` / `admin123456`（baseline 种子数据，首次登录后请立即修改）

## 目录结构

```
iboot/
├── pom.xml                  # 父 POM，聚合 13 个后端模块
├── docker/                  # 中间件编排与初始化脚本
│   ├── docker-compose.yml
│   └── init.sql
├── scripts/
│   ├── dev-init.sh          # 一键拉起本地开发环境
│   ├── build.sh             # 前后端一键构建
│   ├── pull-docker-images.sh
│   └── regress/             # 契约快照、路由覆盖、冒烟、权限矩阵
├── iboot-kernel/            # 共享内核库：统一响应、鉴权、审计写入
├── iboot-db-init/           # 数据库初始化：建库与集中式迁移，运行后退出
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
├── iboot-sentinel/          # 治理指标桥接库（不独立部署）
├── iboot-portal/            # 前端（Vue 3 + Vite）
├── docs/                     # 项目文档，入口 docs/README.md
├── openspec/                # 能力契约与变更记录
├── ROADMAP.md               # 未完成路线与仍然生效的约束
└── README.md
```

业务服务内部遵循 DDD 四层：`interfaces` → `application` → `domain` → `infrastructure`；
网关与运维控制台是没有业务领域的可部署模块，不强行套用四层结构。

## 构建、测试与回归

```bash
# 一键构建（默认前后端都构建）
bash scripts/build.sh            # 或 frontend / backend / help

# 后端全量构建
mvn clean package

# 单个模块测试
mvn -pl iboot-system test

# 全栈回归：契约快照 → 路由覆盖 → 冒烟 → 权限矩阵
bash scripts/regress/regress.sh

# 链路追踪核验
bash scripts/regress/trace-chain.sh <traceId> --expect iboot-gateway,iboot-system
```

回归脚本要求九个业务服务、运维控制台与中间件已启动；`--refresh` 仅在确认接口差异合理时用于重建基线。
CI 见 `.github/workflows/regress.yml`：PR 阶段跑路由覆盖、构建与网关测试，定时任务跑全栈回归。

## 镜像与部署现状

后端统一使用 `docker/Dockerfile.backend` 共享模板（`eclipse-temurin:25-jre`、非 root 用户、
`script/run-java.sh` 启动），数据库初始化使用 `docker/Dockerfile.dbinit`，前端使用
`iboot-portal/Dockerfile`。所有服务由 `docker/docker-compose.yml` 编排，不再有模块级 Dockerfile。

```bash
# 一键构建并启动全栈（默认核心 + 可观测性）
scripts/docker-deploy.sh up

# 只启动核心组件
scripts/docker-deploy.sh up core
```

前端生产镜像由 nginx 提供静态资源，并把 `/iboot/api` 反向代理到 `iboot-gateway:8088`；
容器编排、端口、卷与排障见 [`docker/README.md`](docker/README.md)。Helm 组织与 ACK 部署见
[ROADMAP 阶段 3](ROADMAP.md#阶段-3云原生ack--helm)。

## 文档导航

| 文档 | 内容 |
|---|---|
| [docs/README.md](docs/README.md) | 全部项目文档的入口 |
| [ROADMAP.md](ROADMAP.md) | 未完成路线、版本矩阵、风险与决策记录 |
| [docs/23 微服务底座操作指南](<./docs/23-微服务底座操作指南.md>) | Nacos、网关、链路追踪与表级只读契约 |
| [docs/22 AI 工作台操作指南](<./docs/22-AI 工作台操作指南.md>) | 模型配置、RAG、智能体与知识图谱 |
| [docs/11 可观测性设计](<./docs/11-可观测性设计.md>) | 指标、日志、告警与追踪 |
| [openspec/specs](openspec/specs) | 25 份能力契约 |
| [openspec/changes/archive](openspec/changes/archive) | 历次变更的 proposal / design / tasks |

## 许可证

Licensed under the Apache License, Version 2.0. 详见 [LICENSE](LICENSE)。
