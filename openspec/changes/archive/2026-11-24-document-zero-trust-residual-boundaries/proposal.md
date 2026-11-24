# Proposal

## Why

SPIRE SVID 切换已在本地完成可行性验证：SVID 可携带 DNS/URI SAN，但默认 TTL 为 1 小时，
Spring Boot 文件型 TLS bundle 没有热加载；要把十个服务切换过去，需要每服务 agent/sidecar 与滚动重启，
属于部署模型变更。该结论应写入残余边界，避免后续误判为“顺手可改”。

## What Changes

- 更新 `docs/27 §29` 第 2 项：记录 SVID 可行性验证结论、TTL/热加载限制与切换前置条件。
- 保留第 5 项轮换调度安装、第 6 项 Nacos HTTP 例外；跨主机调度在 §23.4 已说明为逐主机执行。

## Capabilities

### New Capabilities

无。

### Modified Capabilities

无。

## Impact

- 文档：`docs/27-TLS 与零信任操作指南.md`。
