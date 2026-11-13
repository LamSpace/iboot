# container-deployment Specification

## Purpose

定义从干净检出到全栈启动的构建、镜像、编排、启动顺序、健康检查、配置注入、持久化与失败语义，使整套部署可重复、可观测，并且不依赖宿主机预构建产物。

## Requirements

### Requirement: 干净检出可完成全栈构建

部署 SHALL 只依赖容器运行时、编排工具与 Docker Hub（MinIO 除外），从干净检出完成全部镜像构建与启动。

- 构建 MUST NOT 要求宿主机预先生成 jar、`dist` 或安装 Maven、Node.js、cnpm。
- 除 MinIO 外，基础镜像与运行期镜像 MUST 从 Docker Hub 官方仓库获取，并使用固定的版本标签；MUST NOT 使用浮动标签。
- MinIO MUST 从 `https://docker.aityp.com/` 展示的 SWR 镜像路径获取，并重打为编排引用的标签；MUST NOT 依赖已不可用的 Docker Hub `minio/minio` 仓库。
- Maven 与 npm 依赖 MUST 通过项目登记的内部镜像地址获取；重复构建 SHALL 复用构建缓存，不重复下载未变化的依赖。
- 构建失败时 MUST 明确指出失败的阶段与镜像，不得静默回退到宿主机构建或其它镜像源。

#### Scenario: 干净检出完成构建并启动

- **WHEN** 在一个没有 `target/`、`dist/`、`node_modules/` 的干净检出上执行部署命令
- **THEN** 全部镜像构建成功
- **AND** 核心服务启动并通过健康检查

#### Scenario: 基础镜像不可用时明确失败

- **WHEN** Docker Hub 或 MinIO 的 aityp/SWR 镜像源不可达，且本地没有所需镜像
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

- MySQL、Redis、MinIO、Elasticsearch 与 Nacos MUST 在健康后才允许依赖它们的服务启动。
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

- 数据库、Redis、Elasticsearch、MinIO、Nacos、Tempo、Logstash 与认证公钥集地址 MUST 使用服务名或编排网络地址。
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

- MySQL、Redis、MinIO、Elasticsearch、Prometheus、Grafana、Alertmanager、Neo4j 与 Tempo 的数据 MUST 使用命名卷持久化。
- 所有服务 MUST 能写入各自的日志目录；日志文件名 MUST 按应用名区分，多个服务共享日志卷时不得互相覆盖。
- 系统、文件与调度服务使用的本地上传目录 MUST 通过共享卷保持一致。
- 非 root 运行的用户 MUST 对日志目录与上传目录具有写权限。
- 数据卷的删除 MUST 只在显式执行清理命令时发生，普通停止或重启 MUST NOT 删除数据。

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
- 除 MinIO 外，镜像 MUST 从 Docker Hub 官方仓库拉取，标签 MUST 固定到明确版本，MUST NOT 使用 `latest` 或浮动标签。
- MinIO MUST 从 `https://docker.aityp.com/` 展示的 SWR 镜像路径拉取，并重打为编排引用的标签。
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

### Requirement: MinIO 镜像来源例外

MinIO SHALL 通过 aityp/SWR 镜像路径获取，因为上游已停止发布官方容器镜像。

- 拉取脚本 MUST 使用 `https://docker.aityp.com/` 展示的 `minio/minio` SWR 路径，并在拉取后重打为 `docker.io/minio/minio:<tag>`。
- 版本 MUST 固定为 aityp 上可用的最后一个官方发布标签，MUST NOT 回退到 Docker Hub 或 Quay 的不可访问仓库。
- 若 aityp/SWR 缺失目标标签，拉取 MUST 明确失败，MUST NOT 静默使用其它 MinIO 镜像替代。

#### Scenario: 拉取 MinIO 并重打标签

- **WHEN** 执行项目镜像拉取脚本
- **THEN** MinIO 从 aityp/SWR 路径拉取成功
- **AND** 本地存在与编排引用一致的 `docker.io/minio/minio:<tag>` 标签

#### Scenario: MinIO 标签不可用

- **WHEN** aityp/SWR 上不存在脚本指定的 MinIO 标签
- **THEN** 拉取以非零状态失败并输出缺失的镜像
- **AND** 不会改用其它来源或版本继续
