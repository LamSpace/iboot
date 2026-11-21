# Spec Delta

## Purpose

定义本地与容器部署中 MySQL、Redis、Silo 与 Nacos 的账号认证、最小权限和凭据注入边界，使业务服务与运维脚本只能以明确身份执行其所需操作。

## ADDED Requirements

### Requirement: MySQL 应用账号最小权限

业务服务 SHALL 使用专用账号访问 MySQL，MUST NOT 使用 root；该账号只能对业务库执行数据操作，不能执行 DDL 或管理账号。

- 业务账号 MUST 仅拥有 `iboot` 库的 `SELECT`、`INSERT`、`UPDATE`、`DELETE` 权限。
- 监控 exporter MUST 使用独立只读账号；MUST NOT 复用业务账号或 root。
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
- 业务服务、健康检查、exporter 与回归脚本 MUST 携带认证信息。
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

### Requirement: Silo 不使用默认凭据

Silo SHALL 使用环境注入的凭据启动；`minioadmin/minioadmin` 等默认凭据 MUST NOT 在编排或应用配置中保留。

- 根凭据 MUST 从环境或密钥注入；缺失时启动 MUST 失败。
- 业务服务、`silo-init`、Thanos 与健康检查 MUST 使用同一组受管凭据。
- 凭据 MUST NOT 写入镜像层、compose 默认值或版本管理。
- 使用错误凭据访问 Silo MUST 被拒绝。

#### Scenario: 服务以受管凭据读写对象

- **WHEN** 文件或 AI 服务以注入的凭据访问 Silo
- **THEN** 上传、下载与预签名成功
- **AND** 连接使用 HTTPS

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

- 每个服务 MUST 配置 Nacos 用户名与口令，凭据从环境或密钥注入。
- 服务 MUST 在启用鉴权后完成注册、发现与配置拉取；MUST NOT 回退到匿名模式。
- 匿名注册、匿名配置写入与匿名管理接口 MUST 被拒绝。
- 控制台鉴权在本地 MAY 关闭以支持门户内嵌；非本地 MUST 开启，且门户 MUST NOT 代填凭据。

#### Scenario: 服务以受管账号注册

- **WHEN** 服务携带正确 Nacos 凭据启动
- **THEN** 注册成功并出现在实例列表
- **AND** 配置拉取成功

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
- **AND** 只出现变量名或脱敏值

### Requirement: 中间件认证负向门禁

回归 SHALL 对 MySQL、Redis、Silo 与 Nacos 执行认证负向检查；缺失或错误凭据 MUST 导致失败。

- 检查 MUST 覆盖 MySQL 业务账号越权、Redis 未认证、Silo 默认凭据与 Nacos 匿名请求。
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
