# Spec Delta

## ADDED Requirements

### Requirement: 数据面证书与服务 TLS 编排

容器编排 SHALL 为业务服务与数据面组件挂载同一份本地证书，并让健康检查通过 CA 校验 TLS。

- 网关、业务服务与运维控制台 MUST 挂载服务证书与信任库；数据面组件 MUST 挂载各自的证书。
- MySQL、Redis、Silo、Neo4j 的服务端 MUST 只提供 TLS 端口，容器内不得回退为明文。
- 数据面健康检查 MUST 使用本地 CA；MUST NOT 使用跳过证书校验的参数。
- 证书缺失或信任库不匹配时，容器 MUST 启动失败或健康检查失败，MUST NOT 以明文模式继续。

#### Scenario: 服务与数据面均加载证书

- **WHEN** 启动全量容器部署
- **THEN** 服务与数据面容器都加载本地证书
- **AND** 服务端口与数据面端口只接受 TLS

#### Scenario: 健康检查使用 CA

- **WHEN** 编排器执行服务或数据面健康检查
- **THEN** 检查请求使用本地 CA
- **AND** 检查命令中没有关闭证书校验的参数

#### Scenario: 证书缺失时失败

- **WHEN** 证书未生成或被移除
- **THEN** 启动或健康检查失败
- **AND** 不会以明文模式继续
