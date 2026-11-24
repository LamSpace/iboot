# Proposal

## Why

B5 的 SPIFFE 身份只在 demo 中验证，十个服务仍使用 mkcert 叶子证书。宿主机开发是本地主路径，
可以在不改部署模型的前提下，用 SPIRE Workload API 为十个服务签发 SVID：
每服务一个 Docker label selector 的工作负载容器取回证书，服务读取 PEM 文件，信任 bundle 合并 SPIRE CA。

## What Changes

- 新增 `scripts/spire-svid-sync.sh`：启动 SPIRE、注册 `iboot-svid-<service>` 条目、取回 SVID、
  合并 SPIRE upstream CA 到 `rootCA.pem`、重建信任库；支持 `--refresh` 与 `--status`。
- `dev-init.sh` 默认 `IBOOT_SVID_ENABLED=true`：宿主机 Nacos 发现地址改为 `localhost`（SVID 不支持 IP SAN），
  服务以 `docker/certs/svid/<service>/svid.0.pem` 启动；`false` 回退 mkcert。
- SPIRE `ca_ttl` 与条目 `x509SVIDTTL` 设为 30 天；条目变更后重启 Agent 清理 SVID 缓存。
- 新增 `scripts/regress/spire-svid-services.sh`（12g）：SAN、有效期、信任链与 mTLS 健康检查。
- 修正 CA 轮换回归从多 CA bundle 中取单 CA 作为临时起点。
- 更新 README、ROADMAP、docker/README、scripts/README、docs/23、docs/27。

## Capabilities

### New Capabilities

无。

### Modified Capabilities

无。证书来源切换，不改变授权语义；SPIFFE URI SAN 解析沿用 B5.8。

## Impact

- 脚本：`scripts/spire-svid-sync.sh`、`scripts/dev-init.sh`。
- 编排：`docker/spire/server.conf` 的 CA TTL。
- 回归：`scripts/regress/regress.sh` 新增 12g；`ca-rotation.sh` 起点修正。
- 文档：README、ROADMAP、docker/README、scripts/README、docs/23、docs/27。

## 说明

Compose 全量部署仍使用 mkcert；切换需要每服务 sidecar/agent 与滚动重启，保留为独立部署变更。
