# Design

## Context

动机见 `proposal.md` 的 Why。这里只记录影响做法的事实与约束。

**现状**

- `iboot-admin`：单个 Boot 4.0.3 servlet 应用（8080，Tomcat 虚拟线程），433 个 Java 文件、25 个 Controller，内部 `interfaces → application → domain → infrastructure` 四层。
- `iboot-ai`：独立 Maven 模块与独立进程（8081），独立 schema `iboot_ai` 与独立 Flyway；跨服务访问 admin 已有两条链路——`PermissionScopeClient` 调 `/api/auth/scope`、`AdminApiClient` 调业务接口，两个客户端都透传调用者 JWT。**这是拆分的现成样板。**
- portal：开发态经 Vite 代理 `/iboot/api` → `8080`（去前缀为 `/api`）、`/iboot/ai` → `8081`（改写为 `/api/ai`）；生产态经 nginx 做同样改写。SSE 用 `GET /api/push/...?t=<token>`，**token 走查询参数而非请求头**。
- `docker/docker-compose.yml`：19 个容器，**没有任何 tracing backend**。
- 测试：119 个测试文件，其中 `@SpringBootTest` 仅 4 个、Testcontainers 约 10 个。

**约束**

- Boot 锁 `4.0.x`（SCA 对 4.1 适配未完成）；SCA `2025.1.0.0` 是本轮唯一可用版本线。
- Nacos 3.x **移除 bootstrap 方式**，必须走 `spring.config.import`（R9）。
- 本机内存有限（R4），现有 19 个中间件容器已占大头。
- 拆分硬前提：没有 TraceID 无法排障（R10）；没有契约与路由守护，路径漂移无法发现（R11）。
- 控制器普遍声明了 `version = "1"` 的 API 版本，但 `application.yml` 中没有显式版本解析配置。

**目标形态（后续 change 逐步到达，本变更只交付网关与两侧注册）**

```
                        +------------------+
   portal  --/iboot-->  |  iboot-gateway   |  (WebFlux, 8088)
                        +--------+---------+
                                 | lb://
     +-----------+-----------+---+--------+-----------+-----------+
     v           v           v            v           v           v
  iboot-auth  iboot-ai   iboot-file   iboot-notify  iboot-job  iboot-monitor
     |           |                                                  |
     +-----------+-----------+--------------------------------------+
                                 v
                            iboot-system   (最后拆，被所有服务依赖)
```

本变更结束时，这张图里只有两格存在：网关，以及它后面的 `iboot-admin` 与 `iboot-ai`。

## Goals / Non-Goals

**Goals:**

- 建立唯一入口，且 `/api/**` 路径、请求/响应结构、SSE 语义**逐字节不变**。
- 让「拆分」在任意时刻可回滚：回滚动作是改一行代理目标或一条路由，而不是回滚代码。
- 让「拆分」在任意时刻可观测：一次请求的 TraceID 能跨进程串起来。
- 让「拆分」在任意时刻可判定：路径漂移会被工具挡住，而不是靠人肉点 43 个页面。
- 本地开发不被中间件阻塞：Nacos 不可用时仍能跑起来。

**Non-Goals:**

- 不定义任何业务服务的内部实现，也不约定它们的接口（那是各自 change 的事）。
- 不设计鉴权链路——网关本变更只转发，不做 token 校验。
- 不追求网关高可用（本地单实例；多实例留给阶段 3）。

## Decisions

### D1. monorepo + 每服务一个 Maven module

服务全部留在同一个仓库，根 `pom.xml` 聚合；每个服务是独立 Boot 应用、独立启动类、独立端口。

**为什么**：共享契约、原子重构、一条 CI，对练习项目的收益远大于多仓库的隔离感。多仓库只在团队边界与发布节奏真正分裂时才划算。

**备选**：每服务独立仓库——本变更阶段每个服务都要从 admin 里搬代码，跨仓库搬运会把「move」变成「重写」，与 D2 冲突。

### D2. 绞杀者：路由表是开关，不是代码分支

拆分一个服务 = 把它在网关路由表里的前缀从 `lb://iboot-admin` 改成 `lb://iboot-<name>`。admin 里保留旧实现一个发布周期，出问题改回一行路由即可。

**为什么**：这是唯一能做到「可回滚」的拆分方式。若一上来就把 Controller 从 admin 删掉，回滚就得回滚代码、回滚 Flyway、回滚数据。

**代价**：过渡期存在同路径的两份实现，必须靠路由表保证只有一份被访问；因此**路由覆盖检查是硬性配套**（见 D6），不是可选项。

### D3. 本变更新建 `iboot-gateway`，不新建 `iboot-common`

网关的错误响应格式是网关自己的职责；此时还没有第二处需要 `Result` / `BusinessException`。共享内核等 `add-auth-center` 出现第三个消费者时再抽。

**为什么**：过早抽公共模块最典型的失败形态是「什么都能往里塞」，最后变成分布式单体。白名单要等真实需求出现才好定。

**备选**：本变更同时抽 `iboot-common` 并让 admin / ai 依赖它——会把「加网关」变成「加网关 + 重构 32 个类」，回归面翻倍而收益为零。

### D4. 网关路由按「具体前缀优先」排序

```
/api/ai/**   -> lb://iboot-ai
/api/**      -> lb://iboot-admin
```

**为什么**：`/api/**` 是兜底，必须最后。后续每拆一个服务，就在它上面插入一条更具体的前缀。

**约束**：路由表不能出现两条前缀重叠且无法判定的规则；D6 的覆盖检查负责发现。

### D5. 本地开发用双 profile，Nacos 缺失不阻塞

- `local` profile：路由写静态 URI（`http://localhost:8080`、`http://localhost:8081`），不连 Nacos。
- `nacos` profile：`lb://` + `spring.config.import=optional:nacos:...`。

`optional:` 前缀让配置中心不可用时应用仍能启动。

**为什么**：本地最常见的阻塞是「中间件没起来导致业务代码调不动」。门禁与 CI 需要 Nacos，日常开发不需要。

**备选**：本地强制依赖 Nacos——调试单个功能时要先起注册中心，收益与成本不成比例。

### D6. 契约守护用「OpenAPI 快照 + 路由覆盖」，不用 Spring Cloud Contract

- **快照**：从各服务 `/v3/api-docs` 导出，归一化（去掉 `servers`、排序、剔除示例值）后存 golden 文件，改造后重新导出并 diff，非零退出即失败。
- **覆盖**：解析快照里的全部路径，断言每条路径只在一条路由规则下解析，且没有路径同时落在「已拆服务」与 admin。
- **权限矩阵**：角色 × 端点 × 期望 allow/deny 的固定用例集，要求跑在种子数据上。

**为什么**：本项目真正要防的风险是**路径漂移**（R11），而不是消息结构漂移——所有调用方都是自家前端，没有第三方消费者。快照 + 覆盖直接命中这个风险，成本是一两个脚本；`spring-cloud-contract:5.0.x` 可行，但会给每个模块引入契约 DSL、stub jar 与额外构建阶段。

**备选**：Spring Cloud Contract——如果将来出现外部消费方，再引入它的 consumer-driven 部分。

### D7. 追踪后端选 Tempo，应用直连 OTLP

应用侧 Micrometer Tracing + OTLP exporter，直连 `grafana/tempo:2.10.8` 的 4318（HTTP）/ 4317（gRPC）；查询界面复用已有的 Grafana。

**为什么**：Grafana 已在栈内，Tempo 是它的原生数据源，不需要再引入一套 UI；镜像已就位且体积小（165MB）。Collector 镜像已备好但不启用——应用直连少一跳，等需要把 trace / metrics / 日志分流时再加。

**备选**：Jaeger all-in-one（零配置更省事，但与既有 Grafana 是割裂的第二套 UI）；SkyWalking agent（字节码增强，与虚拟线程、native 路线的关系需要额外验证，ROADMAP 2.0-B 已明确排除）。

### D8. Nacos 接入方式与端口规划

- 客户端用 `spring-cloud-starter-alibaba-nacos-discovery` + `nacos-config`，配置经 `spring.config.import` 引入，**不引入 `bootstrap.yml`**。
- 服务名与现有 `spring.application.name` 一致（`iboot-admin` / `iboot-ai`），不改名以免影响可观测性面板与日志标签。

端口规划（Nacos 3.x 控制台与主端口已分离，且去掉了 `/nacos` 前缀）：

| 组件 | 容器内 | 宿主机 | 说明 |
|---|---|---|---|
| Nacos HTTP | 8848 | 8848 | OpenAPI |
| Nacos gRPC | 9848 / 9849 | 同 | 客户端连接必开，否则注册不上 |
| Nacos 控制台 | 8080 | **8849** | 与 `iboot-admin` 的 8080 冲突 |
| Tempo OTLP | 4317 / 4318 | 同 | |
| Tempo 查询 | 3200 | 3200 | Grafana 数据源 |
| 网关 | 8088 | 8088 | 避开 8080 / 8081 |

**为什么控制台换端口**：Nacos 3.x 把控制台拆成独立端口，默认 8080 会与 admin 冲突，且旧地址 `:8848/nacos` 已 404。

### D9. 数据拆分蓝图：表归属先定，本变更不动库

本变更不拆库，但把归属写死，后续每个 change 按此切 Flyway 目录。

| 服务 | 表 |
|---|---|
| `iboot-system` | `sys_user` `sys_role` `sys_menu` `sys_dept` `sys_post` `sys_dict_type` `sys_dict_data` `sys_config` + `sys_user_role` `sys_role_menu` `sys_role_dept` `sys_user_post` |
| `iboot-file` | `sys_file` |
| `iboot-notify` | `sys_message` `sys_message_receiver` `sys_message_template` `sys_notice` `sys_notice_read` `sys_notification_delivery` |
| `iboot-job` | `sys_job` `sys_job_log` `db_backup_record` `db_restore_record` + 11 张 `QRTZ_*` |
| `iboot-workflow` | `biz_leave_application` + 33 张 `ACT_*` / `FLW_*` |
| `iboot-monitor` | `sys_login_log` `sys_operate_log` |

已知跨域 JOIN 只有两处：`MessageReceiverMapper` 与 `NoticeReadMapper` 都 join `sys_user`。改为 notify 存 userId、经 system 补用户名——**在 notify 外迁时一次解决，不留到 system 外迁**。

### D10. 横切能力的去向（现在只立规矩，不实现）

| 能力 | 归属 | 迁移后的形态 |
|---|---|---|
| 数据权限 | system 提供 scope，各服务本地缓存 | 复用 `iboot-ai` 已验证的 `PermissionScopeClient` 模式，调 `/api/auth/scope` |
| 操作 / 登录日志 | monitor 落库 | 各服务发 CloudEvent → MQ → monitor 消费（引入 MQ 的**第一个**真实理由） |
| SSE | notify 独占连接 | 其它服务发事件，不直连 `SseEmitterManager` |
| 二级缓存 | 各服务本地 Caffeine + 共享 Redis | Pub/Sub 频道按服务名前缀隔离 |
| 分布式事务 | 仅跨服务强一致写 | Seata AT，每个参与方 schema 加 `undo_log` |

**为什么现在写**：这些是拆分时最容易悄悄丢掉的横切能力。先把归属定下来，后面每个 change 的验收单直接引用。

### D11. 回归基线在改造之前捕获

`scripts/regress/` 的四件套必须先在**当前单体**上跑通并落 golden，再做网关改造。

**为什么**：基线只能在「已知正确」的状态下录。改造后再录，等于用被改坏的实现当标准。

顺序：`api-snapshot`（落 golden）→ `route-coverage`（此时恒真，用于验证工具本身）→ `smoke` → `permission-matrix`。

## Risks / Trade-offs

| 风险 | 处置 |
|---|---|
| SCA `2025.1.0.0` 是首个 Boot 4 版本线，starter 自动配置可能与 Boot 4 条件注解组合未覆盖 | 2.0-A 门禁先行，不过门禁不进 2.1 |
| 网关成为新的单点与新的延迟来源 | 本地接受；每步都有「静态 URI 直连」profile 可绕过网关 |
| 内存叠加（Nacos 0.5–1G + Tempo 0.3G + 19 个既有容器） | `platform` profile 按需启动 + 容器内存上限；不开 Collector |
| 快照归一化不足导致误报，团队最终选择忽略它 | 归一化规则写进脚本并纳入评审；首次落地后先跑三次再接入流程 |
| `version = "1"` 的 API 版本解析来源未确认，过网关后可能行为变化 | 改造前先测出解析来源（header / query / 默认），把结果写进快照与路由约束 |
| 过渡期同路径双实现，路由写错会把流量打到错误实现 | 路由覆盖检查设为 CI 门禁；每个服务外迁都要求它通过 |
| Nacos 3.x 端口与既有容器冲突（控制台 8080） | 已在 D8 固定映射表；compose 加注释说明原因 |

## Migration Plan

每一步都可在不改代码的前提下回退。

1. **基线**：在单体状态跑 `scripts/regress/`，落 golden；确认现有 119 个测试全绿。
2. **门禁 A**：两个最小应用 + Nacos，验证发现、热更新、Sentinel 限流；出 spike 报告。
3. **门禁 B**：同上加 Tempo，验证跨两跳 TraceID；出 spike 报告。**门禁不过即停止。**
4. **基础设施**：`docker-compose.yml` 增加 `nacos` / `tempo`（`platform` profile），补齐本机端口与内存上限。
5. **网关落地**：`iboot-gateway` 以 `local` profile 静态路由接入，先只做转发，portal 不动。
6. **切流量**：portal 的 Vite 代理与 nginx 目标改为 `8088`；这是第一次真实切流。
7. **接注册中心**：admin / ai 注册 Nacos，网关切到 `nacos` profile 使用 `lb://`。
8. **全量回归**：快照零 diff、路由覆盖通过、冒烟与权限矩阵通过、TraceID 跨三方贯通。

**回滚点**

| 步骤 | 回滚动作 |
|---|---|
| 6 之后 | portal 代理目标改回 `8080` / `8081` |
| 7 之后 | 网关切回 `local` profile（静态 URI） |
| 7 之后（注册中心故障） | `spring.config.import=optional:` + 关闭 discovery 即可回到步骤 6 的行为 |

## Open Questions

- `version = "1"` 的版本解析最终来自哪里（默认策略 / header / query）——实现第 1 步时实测确认，结果只影响路由约束的写法，不改变本设计。
- Spring Boot Admin 相关路径（`/sba`、`/actuator`、`/instances`、`/applications`）是否也要走网关——本地继续直连 `8080`，等阶段 3 上云时再决定。
