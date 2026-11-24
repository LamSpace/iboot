# Design

## Context

所有服务通过 `rootCA.pem` 校验对端，叶子证书由同一 CA 签发。CA 轮换如果直接替换
`rootCA.pem`，旧服务仍信任旧 CA、新服务只信任新 CA，滚动过程中会互相拒绝。需要
“信任先过渡、签名后切换、最后收敛”的三阶段。

## Decisions

- `tls-init.sh` 引入 `active-ca.pem`/`active-ca-key.pem` 作为签名 CA；`rootCA.pem` 只作为信任
  bundle，可包含多张 CA。初始状态没有 active 时回退到 `rootCA.pem`。
- `prepare`：保存旧 CA，生成新 CA，`rootCA.pem=旧+新`，签名仍用旧 CA；服务重启后信任新旧两张。
- `activate`：签名切到新 CA，`rootCA.pem=新+旧`，`tls-init.sh --force` 重签全部叶子。
- `finalize`：`rootCA.pem=新`，重建信任库，旧 CA 退役。
- 信任库按 bundle 逐张导入；运行期副本与 PKCS12 使用同一 bundle。
- `--restart rolling` 逐个 compose 重建并等待 healthy/running；`--restart-only` 让 CA 脚本只触发重启。
- 回归在临时证书目录执行三阶段，避免影响运行中栈。

## Risks / Trade-offs

- [阶段顺序错误] → 脚本按 state 文件阻止 activate/finalize 越级；文档要求先 prepare 全部主机。
- [重启窗口内旧服务拒绝新叶子] → 必须先 prepare 并重启全部服务，再 activate。
- [信任库多证书导入] → 逐张导入，回归校验 bundle 数量与叶子签发者。
- [跨主机顺序] → 本仓库不提供中心化编排；每台主机分别执行，文档登记为边界。

## Migration Plan

1. 合并后默认行为不变；现有 CA 仍是 `rootCA.pem`。
2. CA 轮换按 prepare → activate → finalize 执行，每阶段选择 `--restart rolling` 或维护窗口。
3. 回滚：保留 `rotation/previous-ca.pem` 与 `previous-ca-key.pem`，可人工恢复后重签。

## Open Questions

无。
