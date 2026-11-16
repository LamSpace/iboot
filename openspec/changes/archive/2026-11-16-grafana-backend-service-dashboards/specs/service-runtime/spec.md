# service-runtime Specification

## ADDED Requirements

### Requirement: 运行期指标暴露

每个可部署服务 SHALL 在运行期间暴露 Prometheus 指标端点，并以应用名为标签上报，使监控栈能够按服务区分指标。

- 每个可部署服务 MUST 提供可被 Prometheus 抓取的指标端点，且该端点不得因缺少指标实现依赖而返回 404 或 500。
- 该端点暴露的每一条指标 MUST 携带 `application` 标签，其值 MUST 等于该服务的 `spring.application.name`。
- 指标 MUST 携带稳定的命名空间标签，使不同部署环境的同名列可以区分。
- 指标端点 MUST 至少提供 JVM、HTTP 请求、进程与线程类指标，使按服务维度的通用看板可用。
- 指标暴露 MUST NOT 成为启动前置条件：指标后端不可用不得导致服务启动失败或业务请求失败。
- 新增可部署服务时 MUST 同时满足本要求，MUST NOT 依赖后续补丁才具备指标。

#### Scenario: 任意服务都能被抓取

- **WHEN** 启动任一可部署服务并访问其指标端点
- **THEN** 端点返回 200 与 Prometheus 文本格式的样本
- **AND** 响应中不存在 404 或 500

#### Scenario: 指标可按应用名区分

- **WHEN** 同时运行两个不同的可部署服务
- **THEN** 两者上报的 JVM 与 HTTP 指标都带有各自的应用名标签
- **AND** 查询某一应用名时只返回该服务的样本

#### Scenario: 指标缺失不会阻塞启动

- **WHEN** 指标抓取方不可达或指标端点暂时未被访问
- **THEN** 服务仍正常启动并处理业务请求
- **AND** 指标端点恢复可达后能返回样本
