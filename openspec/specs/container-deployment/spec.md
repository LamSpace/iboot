# container-deployment Specification

## Purpose

定义从干净检出到全栈启动的构建、镜像、编排、启动顺序、健康检查、配置注入、持久化与失败语义，使整套部署可重复、可观测，并且不依赖宿主机预构建产物。

## Requirements

### Requirement: 干净检出可完成全栈构建

部署 SHALL 只依赖容器运行时、编排工具与 Docker Hub，从干净检出完成全部镜像构建与启动。

- 构建 MUST NOT 要求宿主机预先生成 jar、`dist` 或安装 Maven、Node.js、cnpm。
- 基础镜像与运行期镜像 MUST 从 Docker Hub 官方仓库获取，并使用固定的版本标签；MUST NOT 使用浮动标签。
- 对象存储镜像 MUST 使用 Docker Hub 的 `pgsty/silo` 仓库及固定的 `RELEASE.*` 标签；MUST NOT 依赖已不可用的 `minio/minio` 仓库或第三方镜像代理。
- Maven 与 npm 依赖 MUST 通过项目登记的内部镜像地址获取；重复构建 SHALL 复用构建缓存，不重复下载未变化的依赖。
- 构建失败时 MUST 明确指出失败的阶段与镜像，不得静默回退到宿主机构建或其它镜像源。

#### Scenario: 干净检出完成构建并启动

- **WHEN** 在一个没有 `target/`、`dist/`、`node_modules/` 的干净检出上执行部署命令
- **THEN** 全部镜像构建成功
- **AND** 核心服务启动并通过健康检查

#### Scenario: 基础镜像不可用时明确失败

- **WHEN** Docker Hub 不可达，且本地没有所需的对象存储或基础镜像
- **THEN** 构建在拉取阶段失败并指出缺失的镜像
- **AND** 不会改去其它镜像源拉取后继续

### Requirement: 构建阶段与运行期镜像分离

后端镜像 SHALL 采用共享构建阶段与独立运行期镜像的多阶段构建，使整套服务只执行一次完整的 Maven 构建。

- 后端构建 MUST 由一个共享构建阶段一次性产出全部模块的 jar。
- 每个服务的运行期镜像 MUST 只包含该服务自己的 jar、JRE、启动脚本与运行期必需内容。
- 运行期镜像 MUST NOT 包含 Maven、JDK 编译器、构建缓存或项目源码。
- 前端 MUST 使用独立的 Node.js 构建阶段与 nginx 运行阶段，运行期镜像 MUST NOT 包含 Node.js 或 `node_modules`。

#### Scenario: 一次 Maven 构建产出全部后端镜像

- **WHEN** 构建全部后端服务镜像
- **THEN** 多模块 Maven 构建只执行一次
- **AND** 每个运行期镜像包含且只包含对应服务的 jar

#### Scenario: 运行期镜像不含构建工具

- **WHEN** 检查任一后端运行期镜像的内容
- **THEN** 镜像内不存在 `mvn`、`javac`、Maven 本地仓库或项目源码
- **AND** 镜像仍可使用配置的启动脚本运行

### Requirement: 启动顺序由依赖健康状态确定

编排 SHALL 用依赖健康状态表达启动顺序，而不是依赖固定的等待时间。

- MySQL、Redis、Silo、Elasticsearch 与 Nacos MUST 在健康后才允许依赖它们的服务启动。
- 数据库初始化执行者 MUST 在全部后端服务之前启动，并 MUST 以 0 退出后，后端服务才允许启动。
- 系统服务 MUST 健康后，其余后端服务才允许启动，因为这些服务启动时要读取共享库中的配置。
- 网关 MUST 健康后，门户才允许启动，因为门户的 nginx 在启动时解析网关上游地址。
- 集中日志系统 MUST NOT 作为任何服务的启动依赖；其不可用不得阻止服务启动。

#### Scenario: 正常启动顺序

- **WHEN** 编排器启动整套服务
- **THEN** 基础设施先达到健康
- **AND** 数据库初始化以 0 退出
- **AND** 系统服务健康后其余后端服务启动
- **AND** 网关健康后门户启动

#### Scenario: 数据库初始化失败阻断后端

- **WHEN** 数据库初始化执行者以非零退出
- **THEN** 任何后端服务都不会启动
- **AND** 失败信息可从编排状态与初始化日志定位

#### Scenario: 依赖不健康时阻塞下游

- **WHEN** MySQL、Redis、Nacos 或系统服务未达到健康
- **THEN** 依赖它们的服务保持未启动状态
- **AND** 不会因为固定等待时间结束而强行启动

### Requirement: 常驻服务具备健康检查

每个常驻服务 SHALL 定义可被编排器判定的健康检查，一次性任务以退出码作为完成信号。

- 健康检查 MUST 配置检查命令、间隔、超时、重试次数与启动宽限期。
- 健康检查 MUST 反映服务是否已具备接受请求的能力，而不是仅检查容器进程存在。
- 一次性初始化服务 MUST NOT 设置自动重启策略；其退出码 MUST 作为成功或失败的唯一信号。
- 健康检查失败的依赖 MUST 阻止下游服务启动，而不是被忽略。

#### Scenario: 不健康的服务不释放下游

- **WHEN** 某服务的健康检查在宽限期内持续失败
- **THEN** 依赖它的服务保持未启动
- **AND** 编排状态将失败服务标记为不健康

#### Scenario: 一次性初始化只执行一次

- **WHEN** 执行一次完整启动
- **THEN** 数据库初始化执行者运行一次并以 0 退出
- **AND** 不会因重启策略被反复拉起

### Requirement: 容器网络配置不泄漏宿主机地址

容器内的服务间地址 SHALL 使用编排网络中的服务名，外部可见地址 SHALL 使用浏览器可达的地址。

- 数据库、Redis、Elasticsearch、Silo、Nacos、Tempo、Logstash 与认证公钥集地址 MUST 使用服务名或编排网络地址。
- 容器 MUST NOT 继承宿主机配置文件中的 `localhost` 默认值。
- 面向浏览器的授权服务器地址与登录回调地址 MUST 使用外部可达地址，而不是容器内部地址。
- 密钥与凭据 MUST 通过环境变量或编排密钥注入，MUST NOT 写入镜像层或提交到仓库。

#### Scenario: 服务间调用按服务名解析

- **WHEN** 某服务调用另一个服务或中间件
- **THEN** 请求发往编排网络中的服务名
- **AND** 不出现指向容器自身的 `localhost`

#### Scenario: 密钥不进入镜像

- **WHEN** 检查构建产物与镜像层
- **THEN** 镜像内不包含根 `.env` 或其它密钥文件
- **AND** 运行期通过环境变量获得所需密钥

### Requirement: 认证签名密钥跨重启保持稳定

部署 SHALL 持久化认证服务的签名密钥，使认证服务重启后已签发的令牌仍可通过验签。

- 认证服务 MUST 从持久化位置加载签名密钥；密钥不存在时生成并写回，后续启动复用同一把。
- 服务重启后 JWKS 中的 `kid` MUST 保持不变，重启前签发的未过期令牌 MUST 仍可被资源服务器接受。
- 删除密钥卷 MUST 被视为会使全部已签发令牌失效的操作，部署文档 MUST 明确提示。

#### Scenario: 认证服务重启后旧令牌仍有效
- **WHEN** 认证服务重启并重新提供 JWKS
- **THEN** JWKS 的 `kid` 与重启前一致
- **AND** 重启前签发的未过期令牌仍能访问受保护接口

#### Scenario: 密钥卷被删除
- **WHEN** 运维删除认证密钥卷并重启认证服务
- **THEN** 服务生成新的签名密钥
- **AND** 此前签发的令牌不再通过验签，文档已提示该后果

### Requirement: 门户同源后端路径完整代理

门户的反向代理 SHALL 覆盖浏览器同源访问的后端路径，未匹配路径才回退到 SPA 的 `index.html`。

- `/iboot/api` MUST 转发到网关并重写为 `/api/**`。
- `/iboot/ai` MUST 转发到网关并重写为 `/api/ai/**`，且 MUST 支持流式响应（关闭代理缓冲、延长读超时）。
- `/druid` MUST 转发到系统服务的 Druid 监控页面。
- `/swagger-ui.html`、`/swagger-ui/`、`/v3/api-docs`、`/swagger-resources/`、`/webjars/`、`/doc.html` MUST 转发到系统服务的 Swagger 资源。
- `/sba` MUST 转发到运维控制台。
- 未匹配的路径才允许返回 SPA 的 `index.html`。

#### Scenario: AI 页面拿到 JSON 而不是 HTML
- **WHEN** 门户请求 `/iboot/ai/knowledge-bases` 或 `/iboot/ai/agent/agents`
- **THEN** 返回 `application/json`
- **AND** 返回体中不出现 SPA 的 `<!DOCTYPE html>`

#### Scenario: AI 对话保持流式
- **WHEN** 门户请求 `/iboot/ai/chat/stream`
- **THEN** 返回 `text/event-stream`
- **AND** 首个事件与后续 token 事件按顺序到达，不被代理缓冲合并或截断

#### Scenario: Druid 与 Swagger 在门户内加载
- **WHEN** 数据库监控或 Swagger UI 的 iframe 请求 `/druid` 或 `/swagger-ui.html`
- **THEN** 返回对应后端页面或重定向后的页面
- **AND** 不会返回门户的 `index.html`

### Requirement: 持久化与可写目录

有状态组件 SHALL 使用命名卷持久化数据，需要写文件的业务目录 SHALL 在容器内可写。

- MySQL、Redis、Silo、Elasticsearch、Prometheus、Grafana、Alertmanager、Neo4j 与 Tempo 的数据 MUST 使用命名卷持久化。
- 所有服务 MUST 能写入各自的日志目录；日志文件名 MUST 按应用名区分，多个服务共享日志卷时不得互相覆盖。
- 系统、文件与调度服务使用的本地上传目录 MUST 通过共享卷保持一致。
- 非 root 运行的用户 MUST 对日志目录与上传目录具有写权限。
- 数据卷的删除 MUST 只在显式执行清理命令时发生，普通停止或重启 MUST NOT 删除数据。
- 从 MinIO 迁移到 Silo 时，逻辑卷名可变，但既有数据卷的物理名 MUST 通过显式映射继续挂载，MUST NOT 因改名产生新的空卷。

#### Scenario: 重启保留数据

- **WHEN** 停止并重新启动整套服务
- **THEN** 数据库、对象存储与监控数据仍然存在
- **AND** 普通重启不会删除任何数据卷

#### Scenario: 非 root 用户写入日志与上传

- **WHEN** 以非 root 用户启动系统、文件或调度服务
- **THEN** 服务能创建并写入日志文件
- **AND** 服务能写入共享上传目录

### Requirement: 可选组件不阻塞核心启动

核心部署 SHALL 只包含运行应用所必需的组件，可观测性与图数据库组件 SHALL 通过显式开关启用。

- 核心启动 MUST 包含数据库、缓存、对象存储、检索、注册中心、链路追踪后端与全部业务服务。
- 监控、日志展示、告警、长期存储与图数据库 MUST 通过显式 profile 或等价开关启用，默认不参与核心启动。
- 可选组件启动失败 MUST NOT 阻止核心服务启动。
- 集中日志投递失败 MUST 不影响业务请求，且服务恢复后 MUST 自动重新投递。

#### Scenario: 只启动核心组件

- **WHEN** 使用核心启动命令启动整套服务
- **THEN** 业务服务与必需中间件启动
- **AND** 可观测性与图数据库组件不启动

#### Scenario: 集中日志系统不可用

- **WHEN** Logstash 未启动或不可达
- **THEN** 业务服务仍能启动并处理请求
- **AND** 日志文件与控制台输出仍然可用

### Requirement: 部署幂等与失败可定位

部署 SHALL 可重复执行，失败 SHALL 留下足以定位问题的信息。

- 重复执行部署命令 MUST 是幂等的：已存在的镜像可复用，已迁移的数据库不重复迁移，已有数据卷不被覆盖。
- 数据库初始化执行者重复运行 MUST 成功退出，且不重复应用已完成的迁移。
- 任一服务启动失败时，编排器 MUST 返回非零状态并保留失败服务的日志。
- 部署脚本 MUST 提供停止、重建与清理数据的明确入口，清理数据 MUST 需要显式选择。

#### Scenario: 重复部署不破坏已有状态

- **WHEN** 在已部署的环境中再次执行部署命令
- **THEN** 部署成功完成
- **AND** 已有数据与已应用的迁移保持不变

#### Scenario: 启动失败可定位

- **WHEN** 某个服务在宽限期内未达到健康
- **THEN** 部署命令以非零状态结束
- **AND** 输出中能看到失败服务及其最近日志

### Requirement: 基础镜像拉取覆盖全部构建与运行期镜像

镜像拉取 SHALL 覆盖编排与 Dockerfile 使用的全部基础镜像，并使用固定标签。

- 拉取清单 MUST 包含构建阶段与运行期阶段使用的全部基础镜像。
- 所有镜像 MUST 从 Docker Hub 官方仓库拉取，标签 MUST 固定到明确版本，MUST NOT 使用 `latest` 或浮动标签。
- 对象存储镜像 MUST 使用 Docker Hub `pgsty/silo` 的固定 `RELEASE.*` 标签，MUST NOT 从 aityp/SWR 或其它镜像代理重打标签。
- 拉取后的本地标签 MUST 与 Dockerfile 及编排中引用的镜像名一致。
- 重复执行拉取 MUST 是幂等的，已存在的镜像不得导致失败。

#### Scenario: 新机器完成基础镜像准备

- **WHEN** 在一台没有相关基础镜像的机器上执行拉取
- **THEN** 全部构建与运行期基础镜像就绪
- **AND** 后续镜像构建直接使用本地基础镜像，不再访问网络拉取

#### Scenario: 重复拉取成功

- **WHEN** 在已有基础镜像的机器上再次执行拉取
- **THEN** 命令成功结束
- **AND** 不删除正在被使用的镜像

### Requirement: RocketMQ 开发栈与启动隔离

本地编排 SHALL 提供 RocketMQ NameServer、Broker、Proxy 与一次性 topic 初始化。Proxy MUST 向宿主机可达的 gRPC 端点返回路由；topic 与消费者组 MUST 在消费者首次订阅前存在或可自动创建。RocketMQ MUST NOT 作为任何业务服务的启动依赖：其不可用 MUST NOT 阻止服务启动、审批提交或通知事件的持久化。

#### Scenario: 干净环境启动

- **WHEN** 从干净环境启动核心编排
- **THEN** NameServer、Broker 与 Proxy 达到健康状态
- **AND** 通知 topic 与消费者组已创建
- **AND** 业务服务全部启动

#### Scenario: broker 未就绪不阻塞业务启动

- **WHEN** RocketMQ broker 尚未就绪或启动失败
- **THEN** 业务服务仍然启动并处理请求
- **AND** 审批提交成功，通知事件保留在 outbox 中等待补投

#### Scenario: 宿主机应用连接 Proxy

- **WHEN** 应用以宿主机进程方式运行并按映射端口连接 Proxy
- **THEN** Proxy 返回的路由端点可从宿主机访问
- **AND** 生产者与消费者可完成收发

#### Scenario: topic 尚未创建时消费者不拖垮服务

- **WHEN** 消费者启动时通知 topic 尚不存在
- **THEN** 消费者在后台重试订阅
- **AND** 通知服务本身保持启动并可处理其它请求

### Requirement: Kafka 日志缓冲栈与启动隔离

本地编排 SHALL 提供 KRaft 模式的单进程 Kafka，作为集中日志链路的缓冲层。Kafka MUST NOT 作为任何服务的启动依赖：其不可用 MUST NOT 阻止服务启动或请求处理。

- Kafka MUST 以 KRaft 模式运行，MUST NOT 部署 ZooKeeper。
- Kafka MUST 使用编排网络服务名对外提供地址，并 MUST 额外提供宿主机可达的监听地址，供宿主机进程与运维工具连接。
- Kafka 数据 MUST 使用命名卷持久化，普通停止或重启 MUST NOT 删除。
- Kafka 的容器内存上限 MUST 明确配置，使本机资源占用可预期。
- 健康检查 MUST 反映 broker 能否接受生产与消费，而不是仅检查容器进程存在。
- 集中日志系统与消息队列任一不可用，MUST NOT 阻止业务服务启动。

#### Scenario: 干净环境启动

- **WHEN** 从干净环境启动核心编排
- **THEN** Kafka 达到健康状态，日志 topic 初始化完成
- **AND** 业务服务全部启动并通过健康检查

#### Scenario: broker 未就绪不阻塞业务启动

- **WHEN** Kafka 尚未就绪或启动失败
- **THEN** 业务服务仍然启动并处理请求
- **AND** 控制台与文件日志保持可用

#### Scenario: 宿主机进程连接 Kafka

- **WHEN** 应用或运维工具以宿主机进程方式按映射端口连接 Kafka
- **THEN** 返回的路由地址可从宿主机访问
- **AND** 生产者与消费者可完成收发

#### Scenario: 健康检查反映真实可用性

- **WHEN** broker 进程存在但无法接受生产请求
- **THEN** 健康检查不将其判定为健康

### Requirement: 日志 topic 的初始化与保留

编排 SHALL 以一次性任务幂等创建日志 topic，并为其设定短保留期，使消息队列只承担缓冲而不承担长期存储。

- 初始化任务 MUST 在服务投递日志之前完成，重复执行 MUST 成功退出且不重建已有 topic。
- 日志 topic MUST 固定分区数与副本数，MUST NOT 依赖自动创建 topic。
- 日志 topic 的保留期 MUST 为 24 小时；超出保留期的记录 MUST 被清理。
- 集中日志系统的保留期 MUST 独立配置，MUST NOT 与消息队列保留期绑定。
- 消费组 MUST 在重启后从已提交位点继续消费，无需重建 topic。

#### Scenario: 重复执行初始化

- **WHEN** 在已存在日志 topic 的环境中再次执行初始化任务
- **THEN** 任务以 0 退出
- **AND** topic 的分区数与保留期保持不变

#### Scenario: 保留期到期清理

- **WHEN** 日志在 topic 中存放超过 24 小时
- **THEN** 超出保留期的记录被清理
- **AND** 集中日志系统中的历史记录不受影响

#### Scenario: 消费者重启后续消费

- **WHEN** 日志消费进程重启
- **THEN** 从已提交位点继续消费
- **AND** 无需重建 topic 或重新初始化

### Requirement: 门户对外只提供 TLS 入口

全量容器部署中，门户 SHALL 对外提供 HTTPS 入口，明文端口只用于重定向。

- 门户 nginx MUST 监听 443 并加载本地证书；80 MUST 只返回到 HTTPS 的 301。
- 门户到网关的代理 MUST 保留原始 Host，使认证服务按公开来源生成页面与回调地址。
- 门户镜像 MUST 暴露 443；编排 MUST 同时发布 80 与 443。
- 门户健康检查 MUST 使用本地 CA 校验 HTTPS，MUST NOT 以跳过证书校验的方式通过。

#### Scenario: 容器部署门户可 HTTPS 访问

- **WHEN** 启动全量容器部署并访问门户
- **THEN** 页面通过 443 加载，证书由本地 CA 签发
- **AND** 80 端口返回 301

#### Scenario: 健康检查使用 CA

- **WHEN** 编排器执行门户健康检查
- **THEN** 检查请求通过本地 CA 校验 HTTPS
- **AND** 检查命令中没有关闭证书校验的参数

### Requirement: 证书与公开来源在构建和运行期注入

门户镜像与编排 SHALL 从运行环境获得证书与公开来源，不得把私钥或环境特定的 issuer 固化进镜像。

- 证书 MUST 通过只读挂载或构建期注入进入容器，私钥 MUST NOT 出现在镜像层或版本管理中。
- 门户构建 MUST 接受公开来源参数并向 Vite 注入 `VITE_OIDC_ISSUER`；Docker 模式的默认值 MUST 为 `https://localhost`。
- 宿主机开发模式 MUST 使用自己的 HTTPS 来源，不得复用 Docker 的 issuer 默认值。
- 启动脚本 MUST 在编排启动前确保证书存在；证书缺失时 MUST 明确失败，而不是以无证书的明文模式继续。

#### Scenario: 构建期注入 issuer

- **WHEN** 构建门户镜像
- **THEN** `VITE_OIDC_ISSUER` 来自构建参数，默认值为 Docker 公开来源
- **AND** 镜像内不包含证书私钥

#### Scenario: 证书缺失时明确失败

- **WHEN** 启动全量部署但证书尚未生成
- **THEN** 启动脚本在拉起容器前失败
- **AND** 输出生成证书的命令

### Requirement: 数据面证书与服务 TLS 编排

容器编排 SHALL 为业务服务与数据面组件挂载同一份本地证书，并让健康检查通过 CA 校验 TLS。

- 网关、业务服务与运维控制台 MUST 挂载服务证书与信任库；数据面组件 MUST 挂载各自的证书。
- MySQL、Redis、Silo、Neo4j 的服务端 MUST 只提供 TLS 端口，容器内不得回退为明文。
- 数据面健康检查 MUST 使用本地 CA；MUST NOT 使用跳过证书校验的参数。
- 证书缺失或信任库不匹配时，容器 MUST 启动失败或健康检查失败，MUST NOT 以明文模式继续。

#### Scenario: 服务与数据面均加载证书

- **WHEN** 启动全量容器部署
- **THEN** 服务与数据面容器都加载本地证书
- **AND** 服务端口与数据面端口只接受 TLS

#### Scenario: 健康检查使用 CA

- **WHEN** 编排器执行服务或数据面健康检查
- **THEN** 检查请求使用本地 CA
- **AND** 检查命令中没有关闭证书校验的参数

#### Scenario: 证书缺失时失败

- **WHEN** 证书未生成或被移除
- **THEN** 启动或健康检查失败
- **AND** 不会以明文模式继续

### Requirement: Kafka 日志缓冲栈只提供 SSL 监听

容器编排中的 Kafka SHALL 在容器内、宿主机与 controller 监听上只接受 SSL，并继续作为非启动依赖运行。

- broker MUST 挂载包含私钥的 keystore 与本地 CA 信任库，MUST NOT 以 PLAINTEXT 提供服务监听。
- 容器内服务、`kafka-init`、健康检查与宿主机运维工具 MUST 使用带 truststore 的 SSL 客户端配置。
- Kafka 数据 MUST 继续使用命名卷持久化；普通停止或重启 MUST NOT 删除。
- Kafka 不可用 MUST NOT 阻止任何业务服务启动；集中日志投递失败 MUST 只降级为本地日志。

#### Scenario: 干净环境以 SSL 启动

- **WHEN** 从干净环境启动核心编排
- **THEN** Kafka 的三个监听只接受 SSL 并达到健康状态
- **AND** 日志 topic 初始化完成

#### Scenario: 健康检查使用 SSL

- **WHEN** 编排器执行 Kafka 健康检查
- **THEN** 检查使用 truststore 与本地 CA 完成 SSL 连接
- **AND** 检查命令中没有 PLAINTEXT 回退

#### Scenario: 明文客户端无法初始化

- **WHEN** `kafka-init` 或运维工具以 PLAINTEXT 连接 broker
- **THEN** 初始化或命令失败并指出协议错误

### Requirement: 日志与检索面证书编排

编排 SHALL 为 Elasticsearch 及其全部日志与检索客户端挂载同一份本地证书，并让健康检查通过 CA 与凭据执行。

- Elasticsearch MUST 加载 HTTP 与 transport 证书，MUST 启用 security，MUST NOT 以匿名明文模式启动。
- Logstash、Kibana、Grafana 与 elasticsearch-exporter MUST 挂载本地 CA，并在连接 Elasticsearch 时启用校验。
- ES 凭据 MUST 从 `.env` 或编排环境注入，MUST NOT 出现在 compose 文件、镜像或版本管理中。
- 健康检查 MUST 使用本地 CA 与有效凭据；MUST NOT 使用 `-k`、`--insecure` 或等价跳过校验参数。
- 证书缺失、凭据缺失或信任库不匹配时，容器 MUST 启动失败或健康检查失败，MUST NOT 回退为明文。

#### Scenario: 服务与日志栈都加载证书

- **WHEN** 启动全量容器部署
- **THEN** Elasticsearch 与全部日志客户端容器都加载本地证书
- **AND** 客户端到 Elasticsearch 的连接通过本地 CA 校验

#### Scenario: 证书缺失时失败

- **WHEN** 证书未生成或被移除
- **THEN** Elasticsearch 或其日志客户端启动或健康检查失败
- **AND** 不会以明文模式继续

#### Scenario: 凭据错误时健康检查失败

- **WHEN** 编排注入的 ES 凭据与集群不一致
- **THEN** 依赖 Elasticsearch 的客户端健康检查失败
- **AND** 失败信息指向认证错误

### Requirement: RocketMQ Proxy 的加密通道编排

编排 MAY 为 RocketMQ Proxy 挂载本地证书并开启 TLS，但 MUST 保持其非启动依赖语义与本变更登记的加密边界。

- Proxy MUST 加载本地证书；配置 MUST 明确 TLS 为可选加密，而不是强制入口。
- 业务服务 MUST NOT 依赖 Proxy 启动；Proxy 不可用时审批提交与通知事件持久化 MUST 仍然成功。
- 编排与文档 MUST NOT 将 Proxy 的 TLS 描述为客户端认证、服务端身份校验或零信任链路。
- 强制 TLS MUST 由后续变更或前置 TLS 终止代理引入，MUST NOT 在本变更中伪造该能力。

#### Scenario: Proxy 以 TLS 启动

- **WHEN** 启动核心编排
- **THEN** Proxy 加载本地证书并可完成 TLS 握手
- **AND** 业务服务仍不依赖 Proxy 健康状态启动

#### Scenario: Proxy 不可用不阻塞审批

- **WHEN** Proxy 停止运行
- **THEN** 审批提交成功
- **AND** 通知事件保留在 outbox 中等待补投

### Requirement: Nacos TLS 编排

容器编排 SHALL 为 Nacos 挂载本地 keystore 与 CA，并让健康检查、客户端与控制台通过 TLS 工作。

- Nacos MUST 挂载 `/certs` 并启用 HTTP/控制台 HTTPS 与 gRPC TLS。
- 后端服务 MUST 通过 `JAVA_TOOL_OPTIONS` 获得 Nacos client TLS 系统属性与客户端证书；MUST NOT 只依赖环境变量名猜测。
- Nacos 健康检查 MUST 使用 `--cacert` 访问 HTTPS 控制台；MUST NOT 使用 `-k`。
- 门户的 Nacos 控制台地址 MUST 使用 `https://`；Docker 默认 URL MUST 为 `https://localhost:8849`。
- 证书缺失或客户端属性缺失时，Nacos 或依赖服务 MUST 启动失败或健康检查失败，MUST NOT 回退明文。

#### Scenario: Nacos 以 TLS 启动

- **WHEN** 从干净环境启动核心编排
- **THEN** Nacos 的 HTTP、控制台与 gRPC 均只接受 TLS
- **AND** 后端服务全部注册成功

#### Scenario: 健康检查使用 CA

- **WHEN** 编排器执行 Nacos 健康检查
- **THEN** 检查使用本地 CA 访问 HTTPS 控制台
- **AND** 命令中没有跳过校验参数

#### Scenario: 证书缺失时失败

- **WHEN** Nacos keystore 未生成或被移除
- **THEN** Nacos 启动或健康检查失败
- **AND** 不会以明文模式继续
