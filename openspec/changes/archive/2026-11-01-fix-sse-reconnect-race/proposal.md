# Proposal

## Why

回归冒烟暴露了一个既有竞态：同一用户重新建立 SSE 连接时，`SseEmitterManager.connect` 先断开旧连接、再注册新连接，而旧连接的 `onCompletion` 回调稍后执行时**无条件**调用 `userEmitters.remove(userId)`，把刚注册的新连接删掉。表现为新连接建立后立刻「连接完成」，随后推送读到「用户不在线」，用户收不到实时消息。

回归连续跑冒烟时约 1/3 概率复现，导致全量回归随机失败。这是 `iboot-notify` 的既有缺陷，与操作审计改动无关，但会持续影响实时推送与回归可信度，需要单独修掉。

## What Changes

- `SseEmitterManager` 的连接回调改为**条件移除**：`onCompletion`、`onTimeout`、`onError` 使用 `userEmitters.remove(userId, emitter)`，只有回调所属的 emitter 仍是当前连接时才移除，旧连接不能误删新连接。
- 新增单测：同一用户连续 `connect` 两次，完成第一个 emitter 后，第二个仍在连接池中且 `isOnline` 为真。
- 不改 SSE 接口、事件格式、心跳与超时配置，不改前端。

## Capabilities

### New Capabilities

无。

### Modified Capabilities

- `notification`: 在「站内通知落库并经 SSE 推送」下补充「重连期间保持在线」的场景——新连接不得被旧连接的回调移除，重连后仍能收到实时事件。

## Impact

**代码**

- `iboot-notify`：`infrastructure/push/SseEmitterManager.java` 的三个回调；新增 `SseEmitterManagerTest`。
- 不改 `PushEndpoint`、事件生产者与消费者。

**接口**

- 对外契约不变；SSE 连接与事件格式不变，前端零改动。

**数据与配置**

- 无数据库、无配置、无中间件变化。

**回归资产**

- 冒烟的 SSE 链路会自然覆盖；修复后连续运行 `regress.sh` 不应再因 SSE 竞态随机失败。
