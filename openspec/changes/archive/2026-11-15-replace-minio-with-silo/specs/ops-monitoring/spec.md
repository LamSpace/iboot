# Spec Delta

## MODIFIED Requirements

### Requirement: 监控能力作为独立可部署单元

监控与日志查询 SHALL 由独立模块、独立进程提供，并在注册中心注册自身、参与分布式追踪。

- 该服务 MUST 能在不启动其他业务服务的情况下独立完成启动与健康检查。
- 该服务 MUST NOT 依赖其他业务服务才能返回日志或会话数据；其持久化依赖仅限共享数据库中归其所有的表、共享 Redis、共享对象存储与外部检索后端。
- 该服务 MUST 通过网关对外暴露日志、会话与资源检查端点；除已登记的 Silo 重命名（`/api/monitor/minio` → `/api/monitor/silo`、`minio:query` → `silo:query`、`minioEnabled` → `siloEnabled`）外，路径与方法 MUST 与拆分前一致。
- 该服务的启停 SHALL NOT 影响用户、角色、组织、消息、审批等其它域的可用性。

#### Scenario: 独立启动

- **WHEN** 只启动中间件与监控服务
- **THEN** `/actuator/health` 返回 UP
- **AND** 该服务已在注册中心可见

#### Scenario: 契约不漂移

- **WHEN** 对比本次变更前后的接口基线，并排除已登记的 Silo 重命名项
- **THEN** 日志、会话与资源检查端点的路径与方法保持不变

#### Scenario: 监控服务不可用不影响其它域

- **WHEN** 监控服务停止运行
- **THEN** 日志、会话与资源检查端点返回明确的降级错误
- **AND** 用户信息、文件、消息、审批等接口仍正常响应
