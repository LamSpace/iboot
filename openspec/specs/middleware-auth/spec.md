# middleware-auth Specification

## Purpose

定义本地与容器部署中 MySQL、Redis、Silo 与 Nacos 的账号认证、最小权限和凭据注入边界，使业务服务与运维脚本只能以明确身份执行其所需操作。

## Requirements

### Requirement: MySQL 应用账号最小权限

每个业务服务 SHALL 使用只属于自己的 MySQL 账号，MUST NOT 使用 root 或其它服务的账号；该账号只能对业务库中登记的表执行数据操作，不能执行 DDL 或管理账号。

- 每个服务 MUST 有独立账号（例如 `iboot_system`、`iboot_workflow`），账号名与服务身份稳定对应。
- 服务账号 MUST 只拥有共享只读表的 `SELECT`，以及登记为本服务写表的 `INSERT`、`UPDATE`、`DELETE`。
- 服务账号 MUST NOT 拥有其它服务写表的 `INSERT`、`UPDATE`、`DELETE`，MUST NOT 拥有 `iboot` 库级 DML。
- Flowable 表 `ACT_*`、`FLW_*` MUST 只授予 workflow；Quartz 表 `QRTZ_*` MUST 只授予 job。
- 所有数据服务 MAY 对共享审计表 `sys_operate_log` 执行 `INSERT`；只有 system 与 monitor 可以更新或删除审计记录。
- 账号创建 MUST 在业务表创建前完成；表级授权 MUST 在建表完成后执行，且 MUST 可重复执行。
- 共享业务账号 MUST NOT 再注入业务服务；保留时只能作为兼容工具账号，并由回归断言服务未使用它。
- 监控 exporter MUST 使用独立只读账号；MUST NOT 复用服务账号或 root。
- root MUST 只在 MySQL 容器内回环地址（`localhost` / `127.0.0.1`）可用；MUST NOT 存在可从编排网络或宿主机端口登录的 root 账号。
- 建表与迁移 MUST 使用专用账号 `iboot_migrator`；账号与授权初始化 MUST 使用专用账号 `iboot_provisioner`；两者 MUST NOT 注入业务服务。
- `iboot_migrator` MUST 只拥有 `iboot` 库的 DDL/DML；MUST NOT 创建或修改账号。
- `iboot_provisioner` MUST 只拥有创建/修改账号、授予既有业务权限与读取元数据所需的最小权限；MUST NOT 拥有 `iboot` 库的 DDL、root 或等价的全库任意授权能力。
- 凭据 MUST 从环境或密钥注入；MUST NOT 在应用配置中保留可用的 root 默认值。

#### Scenario: 业务账号执行数据操作

- **WHEN** 业务服务以专用账号连接 MySQL 并执行其登记表内的增删改查
- **THEN** 操作成功
- **AND** 连接同时使用 TLS

#### Scenario: 业务账号被拒绝 DDL

- **WHEN** 业务账号执行 `CREATE TABLE`、`DROP TABLE` 或 `GRANT`
- **THEN** MySQL 拒绝操作
- **AND** 返回权限错误

#### Scenario: 每个服务使用自己的账号

- **WHEN** 九个业务服务与运维控制台同时运行并持有连接池
- **THEN** MySQL 连接列表中出现与各服务一一对应的账号
- **AND** 不出现共享业务账号

#### Scenario: 一个服务的凭据不能冒充另一个服务

- **WHEN** 使用服务 A 的账号名与错误口令连接，或以服务 B 的账号统计连接来源
- **THEN** 认证失败或身份记为服务 B
- **AND** 不能得到服务 A 的身份

#### Scenario: exporter 不能写业务数据

- **WHEN** 监控 exporter 使用只读账号连接 MySQL
- **THEN** 指标采集成功
- **AND** 写入或修改业务数据被拒绝

#### Scenario: 错误密码被拒绝

- **WHEN** 客户端使用错误密码或缺失密码连接 MySQL
- **THEN** 认证失败
- **AND** 不建立可用会话

#### Scenario: 只能写本服务登记的表

- **WHEN** 服务 A 的账号对服务 B 的写表执行 `INSERT`、`UPDATE` 或 `DELETE`
- **THEN** MySQL 返回权限错误
- **AND** 服务 B 的数据不被修改

#### Scenario: 共享只读表仍可读

- **WHEN** 任一数据服务读取 `sys_user`、`sys_config` 或 `sys_dict_data`
- **THEN** 查询成功
- **AND** 写入这些表中未登记给该服务的表被拒绝

#### Scenario: 引擎表按归属授权

- **WHEN** workflow 写入 Flowable 表或 job 写入 Quartz 表
- **THEN** 操作成功
- **AND** 其它服务写入同一批引擎表被拒绝

#### Scenario: 表级授权可重复执行

- **WHEN** 在已有表结构上再次执行授权脚本
- **THEN** 授权矩阵保持不变
- **AND** 不产生重复授权或失败

#### Scenario: 远程 root 被拒绝

- **WHEN** 从编排网络中的其它容器，或从宿主机映射端口，以 root 连接 MySQL
- **THEN** 认证失败
- **AND** 该连接不能执行任何 SQL

#### Scenario: 本地 root 仍可完成首次引导

- **WHEN** MySQL 容器首次初始化，在容器内通过本地 socket 以 root 执行引导脚本
- **THEN** 引导成功
- **AND** 创建迁移账号与管理账号，并删除 `root@'%'`

#### Scenario: 迁移账号不能管理账号

- **WHEN** `iboot_migrator` 执行 `CREATE USER`、`GRANT` 或 `DROP USER`
- **THEN** MySQL 拒绝操作
- **AND** 迁移账号仍可对 `iboot` 库执行 DDL

#### Scenario: 管理账号不能取得 root 等价权限

- **WHEN** `iboot_provisioner` 尝试授予全局管理权限、读取任意文件、关闭实例或对 `iboot` 库执行 DDL
- **THEN** MySQL 拒绝操作
- **AND** 管理账号仍可创建服务账号并授予登记的表级权限

#### Scenario: 管理凭据缺失时启动失败

- **WHEN** 编排缺少 `MYSQL_MIGRATOR_PASSWORD` 或 `MYSQL_PROVISIONER_PASSWORD`
- **THEN** 启动在拉起容器前失败
- **AND** 输出指出缺失的变量

### Requirement: Redis 必须认证

Redis SHALL 要求客户端认证；未认证连接 MUST NOT 执行任何命令。

- 认证口令 MUST 从环境或密钥注入，MUST NOT 使用空口令或仓库内固定口令。
- 每个业务服务 SHALL 使用独立的命名 ACL 用户；健康检查与回归脚本 MUST 使用明确的命名账号。
- 认证 MUST 与 TLS 同时生效，MUST NOT 以关闭 TLS 换取连通性。

#### Scenario: 认证后可读写

- **WHEN** 服务通过 TLS 并携带正确口令连接 Redis
- **THEN** `PING` 返回 `PONG`
- **AND** 业务读写成功

#### Scenario: 未认证被拒绝

- **WHEN** 客户端只建立 TLS 连接但不发送认证
- **THEN** 命令被拒绝
- **AND** 返回未认证错误

#### Scenario: 错误口令被拒绝

- **WHEN** 客户端携带错误口令连接 Redis
- **THEN** 认证失败
- **AND** 不返回业务数据

#### Scenario: 连接可按服务归属

- **WHEN** 服务运行并持有 Redis 连接
- **THEN** `CLIENT LIST` 中连接带有各自服务的用户标签
- **AND** 不使用共享业务用户

### Requirement: Silo 不使用默认凭据

Silo SHALL 使用环境注入的凭据启动；`minioadmin/minioadmin` 等默认凭据 MUST NOT 在编排或应用配置中保留。

- 根凭据 MUST 从环境或密钥注入；缺失时启动 MUST 失败。
- 业务服务 MUST 使用各自的服务账号，MUST NOT 使用根账号；账号权限 MUST 限定在自身桶。
- `silo-init`、Thanos 与运维工具 MAY 使用根账号；根凭据 MUST NOT 注入业务服务。
- 凭据 MUST NOT 写入镜像层、compose 默认值或版本管理。
- 使用错误凭据访问 Silo MUST 被拒绝。

#### Scenario: 服务以受管凭据读写对象

- **WHEN** 文件或 AI 服务以注入的服务凭据访问 Silo
- **THEN** 上传、下载与预签名成功
- **AND** 连接使用 HTTPS

#### Scenario: 服务账号不能访问他人桶

- **WHEN** 文件服务账号尝试读取 AI 服务的桶，或 AI 服务账号尝试读取文件桶
- **THEN** 访问被拒绝

#### Scenario: 默认凭据不可用

- **WHEN** 客户端尝试使用 `minioadmin/minioadmin` 访问 Silo
- **THEN** 认证失败
- **AND** 不返回对象内容

#### Scenario: 凭据缺失时启动失败

- **WHEN** 编排缺少 Silo 凭据变量
- **THEN** 启动在拉起容器前失败
- **AND** 输出指出缺失的变量

### Requirement: Nacos 注册与配置使用账号认证

Nacos SHALL 启用客户端与服务端鉴权；服务注册、实例发现与配置拉取 MUST 使用受管账号，匿名请求 MUST NOT 成功。

- 每个服务 MUST 使用独立的 Nacos 账号，凭据从环境或密钥注入。
- 服务 MUST 在启用鉴权后完成注册、发现与配置拉取；MUST NOT 回退到匿名模式。
- 匿名注册、匿名配置写入与匿名管理接口 MUST 被拒绝。
- 管理脚本与管理账号凭据 MUST NOT 注入业务服务。
- 控制台鉴权在本地 MAY 关闭以支持门户内嵌；非本地 MUST 开启，且门户 MUST NOT 代填凭据。

#### Scenario: 服务以受管账号注册

- **WHEN** 服务携带正确 Nacos 凭据启动
- **THEN** 注册成功并出现在实例列表
- **AND** 配置拉取成功

#### Scenario: 每服务账号可独立登录

- **WHEN** 使用任一服务的账号与口令登录 Nacos
- **THEN** 登录成功并返回访问令牌
- **AND** 该账号能读取配置
- **AND** 使用共享账号或错误口令登录失败

#### Scenario: 匿名注册被拒绝

- **WHEN** 客户端不携带凭据注册实例或写入配置
- **THEN** Nacos 返回未认证或禁止
- **AND** 不产生可用的注册或配置记录

#### Scenario: 错误凭据被拒绝

- **WHEN** 服务使用错误用户名或口令连接 Nacos
- **THEN** 注册与配置拉取失败
- **AND** 服务启动记录明确的认证错误

### Requirement: Nacos 管理端初始化使用受管账号

写入 Nacos 的管理脚本 SHALL 先认证再调用管理接口，MUST NOT 依赖关闭管理端鉴权。

- 初始化脚本 MUST 使用受管账号登录并携带访问令牌。
- 管理端鉴权 MUST 在本地与容器部署中保持开启。
- 登录失败或令牌缺失时脚本 MUST 失败，MUST NOT 以匿名方式重试。

#### Scenario: 脚本认证后写入规则

- **WHEN** `sentinel-init.sh` 使用受管账号写入基线规则
- **THEN** 登录与写入成功
- **AND** 回读内容一致

#### Scenario: 脚本无凭据时失败

- **WHEN** 管理脚本缺少用户名、口令或访问令牌
- **THEN** 脚本失败
- **AND** 不以匿名请求继续

### Requirement: 中间件凭据注入与缺失失败

中间件凭据 SHALL 通过环境或密钥管理注入，并在缺失时尽早失败。

- `.env.example` MUST 登记全部新增变量，值 MUST NOT 是可直接用于生产的默认口令。
- 编排 MUST 为每个服务注入自己的凭据，MUST NOT 注入其它服务或共享账号的凭据。
- 编排 MUST 使用非空守卫；缺失变量时 MUST 在拉起容器前失败。
- 应用 MUST 在启动时校验必需的中间件凭据；缺失或为空时 MUST 失败，MUST NOT 回退到内置默认值。
- 凭据 MUST NOT 写入日志、镜像层或版本管理。

#### Scenario: 缺少凭据时部署失败

- **WHEN** 根 `.env` 缺少任一中间件凭据
- **THEN** 部署在启动前失败
- **AND** 输出指出缺失变量

#### Scenario: 凭据不进入日志

- **WHEN** 检查启动日志与回归输出
- **THEN** 不出现明文口令

#### Scenario: 服务只收到自己的凭据

- **WHEN** 检查任一业务容器的环境变量
- **THEN** 只包含本服务的 MySQL、Redis、Nacos 与 Silo 凭据
- **AND** 不包含其它服务或共享账号的口令

### Requirement: 中间件认证负向门禁

回归 SHALL 对 MySQL、Redis、Silo 与 Nacos 执行认证负向检查；缺失、错误或共享凭据 MUST 导致失败。

- 检查 MUST 覆盖 MySQL 业务账号越权与 DDL 拒绝、Redis 未认证、Silo 默认凭据与跨桶访问、Nacos 匿名请求。
- 检查 MUST 断言 MySQL、Redis、Nacos 的实际使用者按服务区分，MUST NOT 全部落到共享账号。
- 检查 MUST 接入全量回归与 CI，不得只作为手工步骤。
- 任一负向用例返回成功 MUST 阻止环境被判定为可验收。

#### Scenario: 未认证 Redis 被检出

- **WHEN** Redis 在无口令时仍接受命令
- **THEN** 认证检查失败并指出 Redis
- **AND** 环境不得判定为通过

#### Scenario: 默认 Silo 凭据被检出

- **WHEN** `minioadmin/minioadmin` 仍可访问 Silo
- **THEN** 认证检查失败并指出 Silo
- **AND** 环境不得判定为通过

#### Scenario: 全部认证检查通过

- **WHEN** 在启用中间件账号的环境中执行回归
- **THEN** 正向链路与负向用例全部通过

#### Scenario: 共享身份被检出

- **WHEN** 任一服务仍以共享 MySQL、Redis 或 Nacos 账号连接
- **THEN** 身份矩阵检查失败并指出该服务
- **AND** 环境不得判定为通过

### Requirement: Elasticsearch 应用访问使用最小权限账号

业务服务、日志写入与监控导出 SHALL 各自使用专用 Elasticsearch 账号，MUST NOT 使用 `elastic` 超级用户。

- 业务读账号 MUST 只能读取日志与检索索引，MUST NOT 调用安全、集群管理或写入接口。
- 日志写入账号 MUST 只能写入日志索引，MUST NOT 读取业务数据或修改安全配置。
- 监控导出账号 MUST 只有集群与索引的只读监控权限。
- 账号与角色 MUST 由幂等初始化任务创建，口令 MUST 从环境或密钥注入。
- `elastic` 超级用户口令 MUST NOT 注入业务服务、日志写入或监控导出容器。
- 凭据缺失、错误或账号未初始化时，对应组件 MUST 失败并记录明确错误，MUST NOT 回退到匿名或超级用户。

#### Scenario: 业务服务读取日志

- **WHEN** 业务服务使用应用读账号查询日志索引
- **THEN** 查询成功且返回授权范围内的数据
- **AND** 同一账号调用安全或集群管理接口被拒绝

#### Scenario: 日志写入账号不能读取业务数据

- **WHEN** 日志写入账号尝试读取业务索引
- **THEN** Elasticsearch 返回未授权
- **AND** 日志写入账号仍可写入当日日志索引

#### Scenario: 监控导出账号只读

- **WHEN** 监控导出账号读取集群健康与索引统计
- **THEN** 请求成功
- **AND** 同一账号写入或删除索引被拒绝

#### Scenario: 超级用户口令不进入业务服务

- **WHEN** 检查业务服务容器的环境变量
- **THEN** 不存在 `elastic` 超级用户口令
- **AND** 只存在该服务所需的专用账号口令

#### Scenario: 账号未初始化时失败关闭

- **WHEN** 业务服务使用尚未创建的账号启动
- **THEN** 访问 Elasticsearch 的请求失败并记录认证错误
- **AND** 不会回退为匿名访问或超级用户

### Requirement: Neo4j 凭据受管且不保留默认口令

Neo4j SHALL 使用环境或密钥注入的口令启动，编排与示例配置 MUST NOT 提供可直接使用的默认口令。

- 编排 MUST 对 `NEO4J_PASSWORD` 使用非空守卫，缺失时 MUST 在拉起容器前失败。
- `.env.example` MUST 只登记变量名与本地开发占位值，MUST NOT 给出仓库内可直接生效的固定口令。
- 应用 MUST 通过 TLS 连接 Neo4j，口令 MUST 与应用其它中间件凭据一样从环境注入。
- 社区版只支持单一服务账号的边界 MUST 在部署文档中登记；应用 MUST NOT 声称具备按服务身份隔离。

#### Scenario: 缺少口令时编排失败

- **WHEN** 环境中未设置 `NEO4J_PASSWORD` 就启动编排
- **THEN** 编排在创建 Neo4j 容器之前报错退出
- **AND** 不启动带空口令或默认口令的实例

#### Scenario: 旧默认口令被拒绝

- **WHEN** 使用仓库历史默认口令连接 Neo4j
- **THEN** 认证失败
- **AND** 失败记录在 Neo4j 与应用日志中

#### Scenario: 受管凭据连接成功

- **WHEN** 应用使用注入的口令与 `bolt+s://` 连接 Neo4j
- **THEN** 连接成功且服务端证书通过本地 CA 校验

### Requirement: Kafka 客户端使用 SASL 身份与 Topic 授权

Kafka 的客户端监听 SHALL 使用 SASL_SSL；未通过认证的连接 MUST NOT 生产或消费消息。

- 业务日志生产、Logstash 消费、运维工具与节点间通信 MUST 使用不同账号。
- 服务端 MUST 启用授权并默认拒绝：生产账号只能写日志 topic，消费账号只能读该 topic 与自己的消费组。
- 凭据 MUST 从环境或密钥注入，MUST NOT 写入镜像层或提交到仓库。

#### Scenario: 正确账号可生产与消费

- **WHEN** `iboot_log` 生产日志、`iboot_logstash` 消费同一 topic
- **THEN** 生产与消费均成功
- **AND** 客户端与服务端握手使用 TLS

#### Scenario: 无凭据连接被拒绝

- **WHEN** 客户端只配置 SSL 信任库、不提供 SASL 凭据
- **THEN** 连接失败
- **AND** 不产生或读取任何消息

#### Scenario: 越权 Topic 被拒绝

- **WHEN** `iboot_log` 尝试消费日志 topic 或访问其它 topic
- **THEN** 服务端返回授权失败
- **AND** 合法写入仍可继续

#### Scenario: ACL 初始化可重复执行

- **WHEN** 初始化任务重复创建 topic 与 ACL
- **THEN** 命令成功且不产生重复副作用
- **AND** 已有 topic 配置保持不变

### Requirement: Redis 禁用默认用户并使用命名 ACL 账号

Redis MUST 关闭 `default` 用户，并按用途使用命名 ACL 账号；默认用户登录或未认证命令 MUST 被拒绝。

- 每个业务服务 MUST 使用自己的命名 ACL 用户，指标导出 MUST 使用受限命令集的 `iboot_exporter`。
- 每个业务用户 MUST 按登记的键模式限制读写；MUST NOT 拥有 `~*` 全键空间权限。
- 所有服务 MUST 能读写 `login_tokens:*` 与 `online_token:*`（会话滑动续期）、只读 `pat_tokens:*` 并写 `pat_last_used:*`。
- auth MUST 能写 `login_retry:*` 与 `pat_tokens:*`；system MUST 能写 `pat_tokens:*`、`sys_config::*` 并订阅 `cache:evict`。
- ai 与 notify MUST 只能写各自的 `iboot:ai:*` / `iboot:notify:*` 前缀；notify MUST 能订阅 `iboot:push:events`。
- monitor 的键空间浏览 MUST 作为显式治理例外登记，宽权限账号 MUST 只注入 monitor。
- 共享业务用户 MUST NOT 再注入业务服务；回归 MUST 断言实际连接按服务区分。
- 口令 MUST 从环境或密钥注入；健康检查与回归脚本 MUST 使用命名账号。
- 认证 MUST 与 TLS 同时生效，MUST NOT 以关闭认证换取连通性。

#### Scenario: 命名账号通过 TLS 认证

- **WHEN** 服务使用自己的 ACL 用户与正确口令通过 TLS 连接 Redis
- **THEN** `PING` 返回 `PONG`
- **AND** 业务读写成功

#### Scenario: 默认用户被禁用

- **WHEN** 客户端使用 `default` 用户与任意口令认证
- **THEN** 认证失败
- **AND** 不返回业务数据

#### Scenario: 导出账号权限受限

- **WHEN** `iboot_exporter` 读取 `INFO` 等监控命令
- **THEN** 请求成功
- **AND** 写入或删除键被拒绝

#### Scenario: 连接按服务区分

- **WHEN** 多个服务同时连接 Redis
- **THEN** `CLIENT LIST` 显示各自服务的 ACL 用户
- **AND** 不出现共享 `iboot_app` 连接

#### Scenario: 只能访问登记的键前缀

- **WHEN** 服务 A 读写 `iboot:ai:*` 以外的其它服务业务前缀
- **THEN** Redis 返回 `NOPERM`
- **AND** 目标键不被修改

#### Scenario: 共享会话前缀可用

- **WHEN** 任一服务读取或续期 `login_tokens:*`、`online_token:*`，读取 `pat_tokens:*`，写 `pat_last_used:*`
- **THEN** 命令成功
- **AND** 其它未登记前缀仍被拒绝

#### Scenario: 监控例外已登记

- **WHEN** 检查 `iboot_monitor` 的 ACL
- **THEN** 只有该账号拥有键空间浏览所需的宽模式
- **AND** 其它服务账号不继承该权限

### Requirement: 中间件组件凭据独立

每个服务在每个中间件组件上 SHALL 使用独立口令；MUST NOT 在 MySQL、Redis 与 Nacos 之间复用同一口令。

- `MYSQL_<SERVICE>_PASSWORD`、`REDIS_<SERVICE>_PASSWORD`、`NACOS_<SERVICE>_PASSWORD` MUST 分别登记、分别注入。
- 缺失任一组件口令时，编排 MUST 在拉起容器前失败。
- 轮换某一组件口令 MUST 不影响其它组件的认证。
- 回归 MUST 断言同一服务的三类口令不同，且跨组件使用同一口令时认证失败。

#### Scenario: 独立轮换 MySQL 口令

- **WHEN** 只更新某服务的 `MYSQL_<SERVICE>_PASSWORD` 并重建该服务
- **THEN** MySQL 认证使用新口令成功
- **AND** Redis 与 Nacos 仍使用原口令正常工作

#### Scenario: 跨组件复用口令被拒绝

- **WHEN** 使用某服务的 MySQL 口令登录 Redis 或 Nacos，或使用 Redis 口令登录 MySQL
- **THEN** 认证失败
- **AND** 不建立可用会话

#### Scenario: 服务只收到自己的组件口令

- **WHEN** 检查任一业务容器的环境变量
- **THEN** 只包含本服务的 `MYSQL_*`、`REDIS_*`、`NACOS_*` 口令
- **AND** MySQL、Redis、Nacos 三个口令互不相同
- **AND** 不包含其它服务或共享变量

#### Scenario: 缺失组件口令时启动失败

- **WHEN** 编排缺少任一 `MYSQL_*`、`REDIS_*` 或 `NACOS_*` 口令
- **THEN** 启动在拉起容器前失败
- **AND** 输出指出缺失的变量
