# Design

## Context

CA 轮换状态机为 `initial → prepared → activated → finalized`。prepare 会保存
`rotation/previous-ca.*`（旧签名 CA）并生成 `rotation/next-ca.*`；activate 切换签名 CA；
finalize 把 active CA 固化为 rootCA 并删除信任 bundle 中的旧 CA。现有脚本没有逆向转换。

## Goals / Non-Goals

**Goals:**

- prepared/activated 阶段可一键回到上一张 CA。
- 回滚后信任 bundle、运行期副本、叶子证书与状态机一致。
- finalized 后明确拒绝，避免用已退役的 CA 重签。

**Non-Goals:**

- 不保留 finalize 后的旧 CA 备份；旧 CA 退役是安全属性。
- 不实现按服务分批回滚。
- 不改变 prepare/activate/finalize 的正向顺序与重启范围。

## Decisions

### 1. 回滚目标是 `previous-ca`

prepare 时保存的 `previous-ca.*` 即轮换前签名 CA；两个阶段的回滚都以它为目标。
prepare 阶段只是丢弃 `next-ca` 并恢复单 CA bundle；activate 阶段还需要用旧 CA 重签全部叶子。

### 2. 状态回到 initial

回滚是“本轮轮换未发生”，因此删除 `state`、`active-ca.*` 与 `next-ca.*`，`--status` 回到
`initial`。保留 `previous-ca.*` 不影响下一轮 prepare（会被当前 CA 覆盖）。

### 3. 复用 tls-init --force

activate 回滚通过 `IBOOT_TLS_SIGN_CA=previous-ca` 调用 `tls-init.sh --force`，与 activate 使用
同一签发/信任库重建路径，避免手工拼装证书链。
