# Proposal

## Why

Compose 全量模式默认叠加 `docker-compose.svid.yml`，服务证书来自命名卷中的 SVID。但
`tls-rotate.sh --restart docker|rolling` 只加载基础 compose，重建后服务的
`IBOOT_TLS_CERT_FILE` 回到 mkcert 路径，SVID 栈被静默降级；docs/28 将该项登记为 A2。

## What Changes

- `tls-rotate.sh` 检测 `docker-svid-*` 容器；存在时 `--restart docker|rolling` 委托
  `docker-svid-refresh.sh`，重新取回 SVID 后滚动重启，不再用基础 compose 重建。
- 不存在 SVID 栈时保持原行为（基础 compose 重建）。
- `tls-ca-rotate.sh` 的 `--restart docker|rolling` 复用同一入口，CA 阶段重启也不会把 SVID 栈降级。
- 新增回归 `scripts/regress/tls-rotate-svid.sh`（12j），用假 docker/刷新脚本验证委托与不误触发。
- 更新 `docs/27` §23.2/§23.4/§28.3、`docs/28`、`scripts/README.md`；docs/28 移除 A2。

## Capabilities

### New Capabilities

无。

### Modified Capabilities

- `transport-security`：证书轮换的 Docker 重启路径必须保持 Compose SVID 栈的证书来源。

## Impact

- 脚本：`scripts/tls-rotate.sh`、新增 `scripts/regress/tls-rotate-svid.sh`、`scripts/regress/regress.sh`。
- 运行期：Compose SVID 栈上的叶子/CA 轮换重启路径。
- 文档：`docs/27`、`docs/28`、`scripts/README.md`。
