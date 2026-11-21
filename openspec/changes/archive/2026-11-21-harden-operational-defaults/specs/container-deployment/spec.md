# Spec Delta

## ADDED Requirements

### Requirement: 可观测控制台不提供匿名管理权限

Grafana 与 Sentinel 等可观测控制台 SHALL 区分只读查看与管理操作；匿名访问 MUST NOT 获得管理权限，管理员凭据 MUST 来自环境注入。

- Grafana 匿名访问 MAY 保留用于门户内嵌看板，但匿名角色 MUST 至多拥有 Viewer 权限。
- Grafana 管理员口令 MUST 从环境或密钥注入；编排与示例配置 MUST NOT 硬编码可用口令。
- 非本地部署 MUST 要求显式提供管理员口令，缺失时编排 MUST 在启动前失败。
- Sentinel 在本地开发 MAY 关闭登录；非本地部署 MUST 开启登录并注入账号与口令。
- 控制台的宿主机端口 MUST 继续只绑定回环地址。

#### Scenario: 匿名用户不能执行管理操作

- **WHEN** 未登录客户端请求 Grafana 管理接口或修改数据源
- **THEN** 请求返回未认证或禁止
- **AND** 匿名用户仍可查看已授权的只读看板

#### Scenario: 管理员口令缺失时部署失败

- **WHEN** 非本地部署未设置 Grafana 管理员口令
- **THEN** 编排在启动容器之前失败
- **AND** 不会使用仓库内默认口令启动

#### Scenario: 生产环境 Sentinel 必须登录

- **WHEN** 非本地部署启动 Sentinel 控制台而未配置账号口令
- **THEN** 编排失败或因鉴权开启而拒绝匿名访问
- **AND** 匿名请求不能修改规则
