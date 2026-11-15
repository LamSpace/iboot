# Spec Delta

## ADDED Requirements

### Requirement: 门户提供追踪查询入口

门户 SHALL 在可观测平台菜单中提供一个受权限控制的追踪查询入口，使用户无需手工配置查询后端即可按 traceId 或 TraceQL 查询链路。该入口 SHALL 使用已配置的 Tempo 数据源，并在查询后端不可用时给出明确错误而不是空白页面。

#### Scenario: 有权限的用户从菜单进入

- **WHEN** 管理员登录并打开“可观测平台”菜单
- **THEN** 能看到“Tempo 链路追踪”入口
- **AND** 点击后在同一系统页面内打开追踪查询界面
- **AND** 查询界面默认选中 Tempo 数据源

#### Scenario: 无权限用户看不到入口

- **WHEN** 用户没有被授予 `monitor:tempo` 权限
- **THEN** 菜单中不出现该入口

#### Scenario: 按 traceId 查询到完整链路

- **WHEN** 用户在查询界面输入一次请求的 traceId 并执行查询
- **THEN** 结果包含该请求经过的全部服务
- **AND** 结果标明各段的耗时

#### Scenario: 查询后端不可用时给出错误

- **WHEN** Tempo 或 Grafana 不可用
- **THEN** 页面显示明确的加载失败状态与重试入口
- **AND** 不显示空白页面
