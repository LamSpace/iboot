# Spec Delta

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
