# Spec Delta

## ADDED Requirements

### Requirement: Nacos TLS 编排

容器编排 SHALL 为 Nacos 挂载本地 keystore 与 CA，并让健康检查、客户端与控制台通过 TLS 工作。

- Nacos MUST 挂载 `/certs` 并启用 HTTP/控制台 HTTPS 与 gRPC TLS。
- 后端服务 MUST 通过 `JAVA_TOOL_OPTIONS` 获得 Nacos client TLS 系统属性与客户端证书；MUST NOT 只依赖环境变量名猜测。
- Nacos 健康检查 MUST 使用 `--cacert` 访问 HTTPS 控制台；MUST NOT 使用 `-k`。
- 门户的 Nacos 控制台地址 MUST 使用 `https://`；Docker 默认 URL MUST 为 `https://localhost:8849`。
- 证书缺失或客户端属性缺失时，Nacos 或依赖服务 MUST 启动失败或健康检查失败，MUST NOT 回退明文。

#### Scenario: Nacos 以 TLS 启动

- **WHEN** 从干净环境启动核心编排
- **THEN** Nacos 的 HTTP、控制台与 gRPC 均只接受 TLS
- **AND** 后端服务全部注册成功

#### Scenario: 健康检查使用 CA

- **WHEN** 编排器执行 Nacos 健康检查
- **THEN** 检查使用本地 CA 访问 HTTPS 控制台
- **AND** 命令中没有跳过校验参数

#### Scenario: 证书缺失时失败

- **WHEN** Nacos keystore 未生成或被移除
- **THEN** Nacos 启动或健康检查失败
- **AND** 不会以明文模式继续
