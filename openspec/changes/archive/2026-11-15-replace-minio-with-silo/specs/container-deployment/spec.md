# Spec Delta

## MODIFIED Requirements

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

#### Scenario: 对象存储卷改名后数据仍可读

- **WHEN** 使用既有 MinIO 数据卷启动 Silo 服务
- **THEN** 之前写入的桶与对象仍可被列出和读取
- **AND** 不会创建新的空数据卷替代旧卷

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

## REMOVED Requirements

### Requirement: MinIO 镜像来源例外

**Reason**: MinIO 上游社区镜像已停止发布并从 Docker Hub 移除；项目改用同源 fork `pgsty/silo`，不再需要 aityp/SWR 镜像代理例外。

**Migration**: 使用 `pgsty/silo` 的固定 `RELEASE.*` 标签直接从 Docker Hub 拉取；编排、拉取脚本与文档中的镜像引用同步改名，详见 `干净检出可完成全栈构建` 与 `基础镜像拉取覆盖全部构建与运行期镜像`。
