# Proposal

## Why

CA 三阶段轮换只提供 prepare/activate/finalize 正向路径。若 prepare 后决定中止，或 activate 后
发现新 CA 有问题，运维只能手工复制文件、重签叶子并重建 bundle，容易遗漏信任库与运行期副本；
docs/28 将该项登记为 A3。

## What Changes

- `tls-ca-rotate.sh` 新增 `--rollback`：
  - `prepared`：丢弃下一张 CA，恢复上一张 CA 的信任 bundle，叶子证书不变。
  - `activated`：恢复上一张 CA 为签名者，重签全部叶子证书，bundle 收敛为上一张 CA。
  - `finalized`：旧 CA 已退役，拒绝自动回滚并提示从备份恢复或重新轮换。
- `--status` 增加 `rollback=available|unavailable`。
- `scripts/regress/ca-rotation.sh` 覆盖 prepared→rollback 与 prepare→activate→rollback，
  校验状态回到 initial、bundle 只剩一张、叶子签发者恢复且可验证。
- 更新 `docs/27` §23.4、`docs/28`、`scripts/README.md`；docs/28 移除 A3。

## Capabilities

### New Capabilities

无。

### Modified Capabilities

- `transport-security`：CA 轮换必须提供从 prepared/activated 回到上一张 CA 的回滚入口。

## Impact

- 脚本：`scripts/tls-ca-rotate.sh`、`scripts/regress/ca-rotation.sh`。
- 运行期：CA 轮换中止与恢复路径；不改变正向三阶段行为。
- 文档：`docs/27`、`docs/28`、`scripts/README.md`。
