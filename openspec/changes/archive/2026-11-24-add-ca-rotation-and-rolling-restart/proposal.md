# Proposal

## Why

叶子证书轮换已有入口，但 CA 本身没有轮换路径；直接替换 CA 会让新旧服务互相拒绝。
同时 `--restart docker` 一次性重建全部服务，缺少逐个重启的滚动能力。

## What Changes

- `tls-init.sh` 拆分“签名 CA”与“信任 bundle”：签名使用 `active-ca.pem`（存在时），信任使用
  `rootCA.pem`（可包含多张 CA）；新增 `--sync-runtime` 只重建信任库与运行期副本。
- 新增 `scripts/tls-ca-rotate.sh`，三阶段 `prepare → activate → finalize`，并提供 `--status` 与 `--dry-run`。
- `tls-rotate.sh` 新增 `--restart rolling`（逐个 compose 重建并等待 healthy/running）与
  `--restart-only`（只重启、不检查/重签）。
- 新增 `scripts/regress/ca-rotation.sh`，在临时证书目录验证三阶段，不触碰运行中栈。
- 更新 README、`scripts/README.md`、`docs/27 §23.4` 与残余边界说明。

## Capabilities

### New Capabilities

无。

### Modified Capabilities

无。CA 轮换属于运维入口，不改变授权语义。

## Impact

- 脚本：`tls-init.sh`、`tls-rotate.sh`、新增 `tls-ca-rotate.sh`。
- 回归：`scripts/regress/regress.sh` 新增 12d。
- 文档：README、scripts/README、docs/27。

## 说明

跨主机中心化调度未实现：仍需每台主机分别安装调度并协调阶段顺序，属于独立编排能力。
