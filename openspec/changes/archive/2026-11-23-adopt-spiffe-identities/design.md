# Design

## Context

`iboot-service-authz` 现在从 `X500Principal` 解析 CN；`tls-init.sh` 的叶子证书只有 DNS/IP SAN。
SPIFFE 身份是服务网格与 OPA 生态的通用输入，先在证书里写入 URI SAN，再让授权层优先读取它，
可以在不引入 SPIRE 的前提下把身份格式对齐。

## Goals / Non-Goals

**Goals:**

- 每张本地签发的服务/探针证书带 `spiffe://iboot.local/...` URI SAN。
- 授权身份优先取 SPIFFE ID，CN 仅回退；信任域固定为 `iboot.local`。
- 回归能证明 URI 优先于 CN，并覆盖旧证书回退。

**Non-Goals:**

- 不部署 SPIRE/SPIFFE Workload API；证书仍由本地 CA 直接签发。
- 不引入 OPA 或服务网格；策略仍由 `ServiceAuthorizationPolicy` 执行。
- 不改变 JWT 用户身份与业务权限。

## Decisions

### 1. URI 路径即身份名

`spiffe://iboot.local/service/<name>` 与 `spiffe://iboot.local/client/<name>` 的 `<name>` 直接作为
调用方名，与现有规则中的 `iboot-gateway`、`prometheus` 等保持一致。路径层级让服务与客户端可区分，
但授权规则不区分来源类型。

### 2. 信任域固定为 `iboot.local`

签发脚本与 Java 解析器使用同一常量。证书已经由本地 CA 验证，信任域校验用于防止误用其它体系的证书；
未来切换信任域时同时修改脚本、解析器与文档。

### 3. URI SAN 优先，CN 回退

`ClientCertificateIdentity.from(cert)` 先遍历 `getSubjectAlternativeNames()` 的 type 6（URI），
匹配信任域后取路径最后一段；没有匹配时回退 CN。过滤器与策略不变，授权规则仍按服务名匹配。

### 4. 回归用“CN 与 URI 不一致”的证书取证

在 `service-authz.sh` 中用本地 CA 临时签发一张 CN=`iboot-file`、SPIFFE ID=`.../service/iboot-system`
的证书，访问仅允许网关的路径：应以 `iboot-system` 身份被拒绝；访问仅允许 system 的路径：应被允许。
这直接证明 URI 是权威身份，而不是 CN。

## Risks / Trade-offs

- [旧证书没有 URI SAN] → CN 回退保持兼容；回归覆盖回退用例。
- [URI SAN 解析失败] → 视为无身份，命中规则时 fail-closed。
- [信任域硬编码] → 文档明确修改点；本地开发不需要多信任域。
- [证书重签导致服务需重启] → 与 B5.7 的轮换脚本共用重启入口。

## Migration Plan

1. 执行 `tls-init.sh --force` 重签全部叶子证书。
2. 重建业务服务、门户与探针，加载新证书。
3. 运行 `service-authz.sh` 与全量回归。
4. 回滚：移除 URI SAN 与解析逻辑，CN 回退仍在；证书可继续使用。

## Open Questions

无。
