# Spec Delta

## ADDED Requirements

### Requirement: 门户对外只提供 TLS 入口

全量容器部署中，门户 SHALL 对外提供 HTTPS 入口，明文端口只用于重定向。

- 门户 nginx MUST 监听 443 并加载本地证书；80 MUST 只返回到 HTTPS 的 301。
- 门户到网关的代理 MUST 保留原始 Host，使认证服务按公开来源生成页面与回调地址。
- 门户镜像 MUST 暴露 443；编排 MUST 同时发布 80 与 443。
- 门户健康检查 MUST 使用本地 CA 校验 HTTPS，MUST NOT 以跳过证书校验的方式通过。

#### Scenario: 容器部署门户可 HTTPS 访问

- **WHEN** 启动全量容器部署并访问门户
- **THEN** 页面通过 443 加载，证书由本地 CA 签发
- **AND** 80 端口返回 301

#### Scenario: 健康检查使用 CA

- **WHEN** 编排器执行门户健康检查
- **THEN** 检查请求通过本地 CA 校验 HTTPS
- **AND** 检查命令中没有关闭证书校验的参数

### Requirement: 证书与公开来源在构建和运行期注入

门户镜像与编排 SHALL 从运行环境获得证书与公开来源，不得把私钥或环境特定的 issuer 固化进镜像。

- 证书 MUST 通过只读挂载或构建期注入进入容器，私钥 MUST NOT 出现在镜像层或版本管理中。
- 门户构建 MUST 接受公开来源参数并向 Vite 注入 `VITE_OIDC_ISSUER`；Docker 模式的默认值 MUST 为 `https://localhost`。
- 宿主机开发模式 MUST 使用自己的 HTTPS 来源，不得复用 Docker 的 issuer 默认值。
- 启动脚本 MUST 在编排启动前确保证书存在；证书缺失时 MUST 明确失败，而不是以无证书的明文模式继续。

#### Scenario: 构建期注入 issuer

- **WHEN** 构建门户镜像
- **THEN** `VITE_OIDC_ISSUER` 来自构建参数，默认值为 Docker 公开来源
- **AND** 镜像内不包含证书私钥

#### Scenario: 证书缺失时明确失败

- **WHEN** 启动全量部署但证书尚未生成
- **THEN** 启动脚本在拉起容器前失败
- **AND** 输出生成证书的命令
