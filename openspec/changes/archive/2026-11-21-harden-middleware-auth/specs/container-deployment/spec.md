# Spec Delta

## ADDED Requirements

### Requirement: 中间件账号初始化先于业务服务

编排 SHALL 在业务服务启动前创建或更新中间件账号，初始化 MUST 幂等，失败 MUST 阻止后续启动。

- MySQL 业务账号与 exporter 账号 MUST 在 db-init 与业务服务之前就绪。
- Nacos 受管账号 MUST 在服务注册与 `sentinel-init` 之前就绪。
- 初始化任务 MUST 可在已有数据卷上重复执行，MUST NOT 重复创建或重置已存在的账号。
- 初始化失败 MUST 以非零退出并阻止依赖服务启动。

#### Scenario: 账号先于服务就绪

- **WHEN** 启动全量编排
- **THEN** 账号初始化任务以 0 退出
- **AND** 业务服务随后启动并完成认证连接

#### Scenario: 重复启动不重置账号

- **WHEN** 在已有数据卷上再次启动
- **THEN** 账号初始化任务成功退出
- **AND** 账号口令与现有数据保持不变

#### Scenario: 初始化失败阻断服务

- **WHEN** 账号初始化任务失败
- **THEN** db-init 与业务服务不启动
- **AND** 编排状态指出失败任务

### Requirement: 健康检查与运维脚本使用受管凭据

容器的健康检查与运维脚本 SHALL 使用与业务相同的受管凭据完成认证，MUST NOT 依赖免认证或默认口令。

- MySQL、Redis 与 Silo 的健康检查 MUST 携带认证信息；Nacos 的只读 readiness 端点 MAY 匿名，但管理/写入脚本 MUST 认证。
- 回归与初始化脚本 MUST 从环境读取凭据，MUST NOT 在脚本中硬编码可用的默认口令。
- 凭据错误或缺失时，健康检查 MUST 失败，MUST NOT 判定为健康。

#### Scenario: 健康检查完成认证

- **WHEN** 编排器执行 Redis、MySQL、Silo 或 Nacos 健康检查
- **THEN** 检查携带受管凭据并通过
- **AND** 输出中没有跳过认证的参数

#### Scenario: 凭据错误导致不健康

- **WHEN** 健康检查使用错误凭据
- **THEN** 检查失败
- **AND** 依赖服务保持未启动
