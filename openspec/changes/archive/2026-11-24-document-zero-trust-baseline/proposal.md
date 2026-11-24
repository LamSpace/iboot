# Proposal

## Why

TLS/零信任事项在实现过程中不断暴露新的运维面与新运行模式，按“遗留任务”口径会出现越处理越多的错觉。
需要冻结当前阶段范围，并把“行动项”与“已接受/外部/阶段项”分开，避免例外项反复计入未完成。

## What Changes

- 新增 `docs/28-TLS 与零信任当前基线与例外.md`：冻结范围（宿主机开发 + Compose），列出 5 项行动项
  与 E1–E11 例外登记（类型、触发条件、复核日期、Owner）。
- `docs/27 §29` 改为指向 docs/28 的章节映射，并更新轮换调度“本机已安装”的事实。
- README、docs/README、ROADMAP 增加 docs/28 入口。

## Capabilities

### New Capabilities

无。

### Modified Capabilities

无。

## Impact

- 文档：`docs/28`、`docs/27`、`docs/README.md`、README、ROADMAP。
