# Spec Delta

## ADDED Requirements

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
