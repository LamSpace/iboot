# Design

## Context

`x-app-env` 的 `JAVA_TOOL_OPTIONS` 把 Nacos 客户端证书固定为 `/certs/portal.pem` 与
`/certs/portal-key.pem`；`dev-init.sh` 的宿主机参数也指向同一份门户证书。Nacos Server 已启用
`mutualAuth`，但无法从证书主体区分服务。

## Goals / Non-Goals

**Goals:**

- 十个服务连接 Nacos 时提交各自的叶子证书。
- Docker 与宿主机两种启动方式使用同一身份来源。
- 回归能从证书指纹和启动参数两侧取证。

**Non-Goals:**

- 不改变 Nacos Server 的 TLS 配置或信任链。
- 不实现 Nacos 资源级授权；该边界仍登记在 `docs/27` §13.5。
- 不改变业务服务端口或服务间 mTLS 的证书使用方式。
- 不切换到 SPIRE SVID；本变更仍使用 mkcert 叶子证书。

## Decisions

### 1. Docker 使用稳定的挂载路径

`x-app-env` 保持一份 Nacos TLS 属性，指向 `/certs/nacos-client.pem` 与
`/certs/nacos-client-key.pem`。每个服务块在 `./certs/runtime:/certs:ro` 之外追加两个单文件挂载，
把 `services/iboot-<service>.pem` 与私钥映射到稳定路径。这样不需要复制十份 `JAVA_TOOL_OPTIONS`，
也不会误用其它服务的证书。

替代方案是为每个服务复制整段 `JAVA_TOOL_OPTIONS`；放弃，因为字符串长、易漂移，且新增服务容易漏改。

### 2. 宿主机直接使用每服务路径

`dev-init.sh` 的 `java_opts` 已有服务名 `$name`，把 `certChainFile` 与 `certPrivateKey` 改为
`$cert_dir/services/$name.pem` 与 `$name-key.pem` 即可，不需要额外挂载。

### 3. 回归从指纹取证

- Docker 模式：比较容器内 `/certs/nacos-client.pem` 与宿主 `docker/certs/services/iboot-<service>.pem`
  的 SHA-256；同时检查 `JAVA_TOOL_OPTIONS` 不含 `portal.pem`。
- 宿主机模式：读取 `/proc/<pid>/cmdline`，断言证书路径包含该服务名。
- 服务注册仍由现有 Nacos 身份检查覆盖；证书被拒会导致服务启动失败，回归自然暴露。

### 4. 身份语义边界

Nacos Server 只验证客户端证书由本地 CA 签发，不做 CN/SPIFFE 级授权。因此本变更提供的是**归因**：
连接日志与抓包可区分服务；吊销单个服务证书需要重签该服务证书或轮换 CA，Nacos 不会按 CN 黑名单。
该限制写入文档，不宣称资源级隔离。

## Risks / Trade-offs

- [单文件挂载依赖宿主机路径存在] → `tls-init.sh` 已在启动前生成全部服务证书；回归会检查文件存在。
- [已有容器不会自动应用新挂载] → Docker 部署需要重建服务容器；宿主机模式重启进程即可。
- [证书主体不参与 Nacos 授权] → 明确登记为归因而非授权；资源级隔离另立变更。
- [轮换单个服务证书] → 重建该服务容器/进程即可；其它服务不受影响。

## Migration Plan

1. 更新 compose 的 Nacos 客户端属性与十个服务的证书挂载。
2. 更新 `dev-init.sh` 的宿主机证书路径。
3. 重建 Docker 服务容器或重启宿主机进程。
4. 运行新增回归与全量回归。
5. 回滚：把 Nacos 客户端属性指回 `portal.pem` 并重建服务。

## Open Questions

无。
