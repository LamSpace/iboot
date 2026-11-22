# Spec Delta

## MODIFIED Requirements

### Requirement: MySQL 应用账号最小权限

每个业务服务 SHALL 使用只属于自己的 MySQL 账号，MUST NOT 使用 root 或其它服务的账号；该账号只能对业务库执行数据操作，不能执行 DDL 或管理账号。

- 每个服务 MUST 有独立账号（例如 `iboot_system`、`iboot_workflow`），账号名与服务身份稳定对应。
- 服务账号 MUST 仅拥有 `iboot` 库的 `SELECT`、`INSERT`、`UPDATE`、`DELETE` 权限。
- 共享业务账号 MUST NOT 再注入业务服务；保留时只能作为兼容工具账号，并由回归断言服务未使用它。
- 监控 exporter MUST 使用独立只读账号；MUST NOT 复用服务账号或 root。
- 建表、迁移与账号初始化 MAY 使用 root 或专用迁移账号，该账号 MUST NOT 注入业务服务。
- 凭据 MUST 从环境或密钥注入；MUST NOT 在应用配置中保留可用的 root 默认值。

#### Scenario: 业务账号执行数据操作

- **WHEN** 业务服务以专用账号连接 MySQL 并执行增删改查
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

### Requirement: Redis 禁用默认用户并使用命名 ACL 账号

Redis MUST 关闭 `default` 用户，并按用途使用命名 ACL 账号；默认用户登录或未认证命令 MUST 被拒绝。

- 每个业务服务 MUST 使用自己的命名 ACL 用户，指标导出 MUST 使用受限命令集的 `iboot_exporter`。
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
