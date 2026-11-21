# Spec Delta

## ADDED Requirements

### Requirement: Nacos 注册与配置链路使用账号认证

服务 SHALL 在 Nacos 启用鉴权后使用受管账号完成注册、发现与配置拉取；匿名请求 MUST NOT 成功。

- 每个服务 MUST 从环境获得 Nacos 用户名与口令，并同时配置在 discovery 与 config 客户端。
- 认证失败时服务 MUST 记录明确错误；MUST NOT 回退到匿名注册或匿名配置拉取。
- 注册中心鉴权开启后，服务的注册、实例列表与配置拉取 MUST 仍可正常工作。
- 服务 MUST NOT 在配置文件或镜像中保留可用的默认用户名与口令。

#### Scenario: 受管账号完成注册与配置

- **WHEN** 服务携带正确凭据启动
- **THEN** 注册成功且实例列表可见
- **AND** 配置拉取成功

#### Scenario: 匿名注册被拒绝

- **WHEN** 客户端不携带凭据注册实例
- **THEN** Nacos 返回未认证或禁止
- **AND** 实例不出现在可用列表

#### Scenario: 认证失败不回退匿名

- **WHEN** 服务使用错误凭据启动
- **THEN** 注册与配置拉取失败
- **AND** 日志记录认证错误且不尝试匿名重试
