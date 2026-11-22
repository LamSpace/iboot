# Proposal

## Why

B5.4 的服务间授权以证书 Subject CN 作为身份。CN 是展示字段，改名或复制证书时容易与真实工作负载脱节；
SPIFFE 用 URI SAN 把身份写进证书，是服务网格与零信任体系的通用身份格式。当前证书只有 DNS/IP SAN，
授权层无法使用结构化身份。

## What Changes

- `tls-init.sh` 为每个服务与探针证书写入 URI SAN：`spiffe://iboot.local/service/<name>` 与
  `spiffe://iboot.local/client/<name>`。
- `iboot-service-authz` 优先从 URI SAN 提取身份，CN 仅作为旧证书回退；信任域固定为 `iboot.local`。
- 回归断言每个叶子证书带正确的 SPIFFE ID，并用“CN 与 SPIFFE ID 不一致”的证书验证 URI 优先。
- 文档登记 SPIFFE 身份格式、与 OPA/服务网格的边界。

## Capabilities

### New Capabilities

无。

### Modified Capabilities

- `service-authorization`：身份来源从 CN 扩展为 SPIFFE URI SAN（CN 回退）。
- `transport-security`：叶子证书增加 SPIFFE URI SAN 要求。

## Impact

- 证书：`scripts/tls-init.sh`、`scripts/regress/internal-tls.sh`。
- 代码：`iboot-service-authz` 身份提取与单元测试。
- 回归：`scripts/regress/service-authz.sh` 新增 URI 优先用例。
- 文档：`docs/27` §18/§20、`docs/10` §10.26.3、`ROADMAP.md`。
