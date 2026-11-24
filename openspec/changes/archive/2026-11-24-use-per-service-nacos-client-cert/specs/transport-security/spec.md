# Spec Delta

## ADDED Requirements

### Requirement: Nacos 客户端注册使用每服务工作负载证书

每个服务连接 Nacos gRPC 时 SHALL 提交自己的工作负载证书；MUST NOT 使用共享 `portal.pem`。

- Docker 部署 MUST 为每个服务把 `services/iboot-<service>.pem` 与私钥挂载为稳定的客户端证书路径。
- 宿主机开发 MUST 把 Nacos 客户端证书属性指向该服务的证书与私钥文件。
- 回归 MUST 断言每个服务实际使用的证书指纹等于自己的服务证书。
- 回归 MUST 断言 Nacos 客户端属性不再引用 `portal.pem`。
- 该证书用于身份归因；Nacos Server 的授权仍只校验信任链，MUST NOT 宣称资源级授权。

#### Scenario: Docker 服务提交自己的证书

- **WHEN** 检查任一服务容器的 Nacos 客户端证书挂载与内容
- **THEN** 指纹等于 `docker/certs/services/iboot-<service>.pem`
- **AND** 不引用 `portal.pem`

#### Scenario: 宿主机服务提交自己的证书

- **WHEN** 检查 `dev-init.sh` 启动的任一服务进程参数
- **THEN** `nacos.remote.client.rpc.tls.certChainFile` 指向该服务的证书文件
- **AND** `certPrivateKey` 指向该服务的私钥文件

#### Scenario: 服务注册仍成功

- **WHEN** 十个服务以各自证书启动
- **THEN** 均完成 Nacos 注册与配置拉取
- **AND** Nacos 服务端不因证书主体不同而拒绝连接
