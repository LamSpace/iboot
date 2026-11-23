# middleware-auth 增量规格

## MODIFIED Requirements

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
- 建表、迁移与账号初始化 MAY 使用 root 或专用迁移账号，该账号 MUST NOT 注入业务服务。
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
