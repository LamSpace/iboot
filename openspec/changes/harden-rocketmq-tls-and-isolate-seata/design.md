# Design

## Context

B5.2 已让业务服务端口要求客户端证书，但 RocketMQ 仍由 Proxy 在同一端口同时接受明文与 TLS，且所有 `data` 网络容器都能访问 Seata 8091。RocketMQ 官方 Java 客户端 5.2.2 在 `enableSsl=true` 时硬编码 `InsecureTrustManagerFactory`，没有可配置的 CA/主机名校验；Seata 2.6.0 也没有原生传输层 TLS/认证。详见 `proposal.md` 与两份 delta spec。

## Goals / Non-Goals

**Goals:**

- 让 RocketMQ 的应用入口只接受 TLS，明文客户端无法建立可用连接。
- 把 RocketMQ 与 Seata 从通用 `data` 网络移出，收敛到专用 `mq` / `tx` 网络。
- 让 Seata 的“无原生 TLS/认证”成为可验证、可审计的显式例外，而不是事实上的遗漏。

**Non-Goals:**

- 不实现 RocketMQ ACL 或客户端证书认证。
- 不修改 RocketMQ 官方客户端源码，也不伪造服务端证书校验。
- 不为 Seata 引入 stunnel/nginx stream 终止器来宣称端到端加密。
- 不改变 RocketMQ/Seata 的业务语义与重试逻辑。

## Decisions

### 用 nginx stream `ssl_preread` 做 TLS-only 入口

新增 `rocketmq-tls`（nginx `stream`），`listen 8081`、`ssl_preread on`，按 `$ssl_preread_protocol` 分流：

- 有 TLS ClientHello → `rocketmq-proxy:8081`
- 非 TLS → `127.0.0.1:9`，立即拒绝

这里只做协议识别与 TCP 透传，不终止 TLS。这样：

- 客户端仍与 Proxy 完成端到端 TLS，ALPN/h2 不受影响；
- 明文字节无法到达 Proxy；
- 不需要在 nginx 上配置证书，也不会引入“nginx 终止后最后一跳明文”的新边界。

替代方案是 nginx 终止 TLS 再转发明文，但它会新增明文最后一跳、需要处理 gRPC ALPN，收益更低。

### `mq` 网络隔离 RocketMQ 组件

NameServer、Broker、Proxy、`rocketmq-store-init` 与 `rocketmq-init` 只加入 `mq`；`rocketmq-tls` 同时加入 `app` 与 `mq`。应用只解析 `rocketmq-tls:8081`，无法解析 `rocketmq-proxy:8081`。宿主机只发布 `127.0.0.1:18081` 到 `rocketmq-tls`，移除 Proxy 的 18080/18081 映射。

### 客户端边界保持诚实

客户端仍使用 `enableSsl=true`，但由于 5.2.2 的 `InsecureTrustManager`，只能证明“传输已加密、入口拒绝明文”，不能证明服务端身份。文档与回归继续使用“TLS-only 加密入口/无服务端校验”的描述，不写成 mTLS。

### Seata 用 `tx` 网络做补偿，不做假 TLS

`seata-server` 只加入 `tx`；`iboot-workflow` 与 `iboot-system` 加入 `tx`；其余服务仍在 `app`/`data`，无法解析 Seata。8091 继续只绑定宿主机 `127.0.0.1`。不引入 TLS 终止器，因为：

- 客户端到终止器、终止器到 Seata 仍有一跳明文；
- Seata 客户端没有原生 TLS 校验能力，无法形成端到端身份；
- 专用网络 + 回环 + 业务侧 mTLS/内部令牌已经是本地基线可验证的最小补偿。

### 回归同时验证“可用”和“不可用”

- `log-search-tls.sh`：RocketMQ 18081 TLS 握手成功、明文失败。
- `exposure.sh`：`iboot-file` 不能直连 `rocketmq-proxy` / `seata-server`；`rocketmq-tls` 与 workflow/system 的正向连接成功。
- `seata.sh` / `seata-tcc.sh`：在 `tx` 网络上继续通过。

## Risks / Trade-offs

- **移除 18080/18081 Proxy 映射** → 依赖直连 Proxy 的旧脚本失效；文档与回归改为 `rocketmq-tls`，宿主机仍可用 `localhost:18081`。
- **应用无法解析 `rocketmq-proxy`** → 需要确保所有客户端配置都改为 `rocketmq-tls:8081`；`dev-init.sh` 宿主机模式仍用 `localhost:18081`。
- **`ssl_preread` 对非 TLS 的处理** → 非 TLS 被路由到关闭端口；回归用明文 `curl` 与 TLS `openssl` 双向验证。
- **Seata 与 system/workflow 的依赖顺序** → 两者已依赖 Nacos/MySQL；新增 `tx` 只影响 DNS 解析，不改变启动顺序。
- **Seata 残余风险** → 被攻陷的 workflow/system 仍可干扰 TC；在文档中登记，复审触发为 Seata 原生 TLS/认证或服务网格引入。

## Migration Plan

1. 新增 `mq`/`tx` 网络与 `rocketmq-tls`，调整 RocketMQ/Seata/system/workflow 网络。
2. 更新应用端点与宿主机端口映射，重建 RocketMQ、Seata、system、workflow、notify。
3. 执行 `log-search-tls.sh`、`exposure.sh`、`seata.sh`、`seata-tcc.sh` 与全量回归。
4. 回滚：恢复 Proxy 端口映射与 `rocketmq-proxy:8081` 端点，把 Seata 放回 `data`；网络与代理配置可保留但不启用。

## Open Questions

无。
