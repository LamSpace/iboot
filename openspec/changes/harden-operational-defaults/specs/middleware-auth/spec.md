# Spec Delta

## ADDED Requirements

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
