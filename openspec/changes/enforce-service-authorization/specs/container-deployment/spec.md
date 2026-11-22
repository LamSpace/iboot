# Spec Delta

## ADDED Requirements

### Requirement: 服务授权开关注入

Docker 全栈编排与宿主机开发启动 SHALL 显式启用服务间授权，默认关闭只用于测试上下文。

- Docker 的服务公共环境 MUST 注入 `IBOOT_SERVICE_AUTHORIZATION_ENABLED=true`，网关与运维控制台同样继承。
- 宿主机 `dev-init.sh` MUST 在启动 Java 进程前导出同一开关。
- 未设置该开关时服务 MUST 保持既有行为，避免单元测试切片因缺少客户端证书失败。
- 启用后，门户、Prometheus、blackbox、SBA 与内部服务调用的证书 MUST 已在授权矩阵中登记。

#### Scenario: Docker 全栈启用授权

- **WHEN** 启动核心编排
- **THEN** 各服务与网关都启用服务授权
- **AND** 越权服务证书访问命中路径时返回 403

#### Scenario: 宿主机开发启用授权

- **WHEN** 以 `dev-init.sh` 启动后端
- **THEN** Java 进程带有服务授权开关
- **AND** 门户与探针链路正常

#### Scenario: 测试上下文不启用

- **WHEN** 单元测试未注入该开关
- **THEN** 授权过滤器不拦截请求
- **AND** 测试用例保持原有结果
