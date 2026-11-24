# Proposal

## Why

宿主机开发已切换到 SPIRE SVID，但 Compose 全量部署仍使用 mkcert 服务证书。Compose 服务无法直接访问
宿主机上的 SVID 文件；需要为每个服务提供一个一次性取证 sidecar，把 SVID 写入共享命名卷。

## What Changes

- 新增 `docker/svid-sidecar` 镜像：复用 SPIRE Agent CLI 与 nginx 基础镜像的 shell，取回证书后放宽私钥权限。
- 新增 `docker/docker-compose.svid.yml` 覆盖层：十个应用挂载 `/svid` 并使用 SVID；十个一次性 sidecar
  通过 `com.iboot.svid.service` 标签取证；命名卷 `svid_<service>`。
- `docker-deploy.sh` 默认 `IBOOT_SVID_ENABLED=true`：先跑 `spire-svid-sync --setup` 合并 CA/信任库，
  再叠加覆盖层与 `spire`/`svid` profile，构建并启动 sidecar 与业务容器；`false` 回退 mkcert。
- 新增 `docker-svid-refresh.sh`：重建 sidecar 写回新 SVID，并滚动重启业务容器。
- 新增 `scripts/regress/compose-svid.sh`（12h）：覆盖层静态检查、sidecar 镜像、单服务取证。
- 更新 README、ROADMAP、docker/README、scripts/README、docs/27。

## Capabilities

### New Capabilities

无。

### Modified Capabilities

无。证书来源切换，不改变授权语义。

## Impact

- 镜像/编排：`docker/svid-sidecar`、`docker/docker-compose.svid.yml`。
- 脚本：`docker-deploy.sh`、`docker-svid-refresh.sh`。
- 回归：`regress.sh` 新增 12h。
- 文档：README、ROADMAP、docker/README、scripts/README、docs/27。
