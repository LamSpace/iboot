# Design

## Context

`SseEmitterManager` 以 `ConcurrentHashMap<Long, SseEmitter>` 维护每个用户的当前连接。`connect(userId)` 的顺序是：

1. `disconnect(userId)`：按 key 移除旧连接并 `complete()`；
2. `userEmitters.put(userId, newEmitter)`；
3. 给新 emitter 注册 `onCompletion` / `onTimeout` / `onError` 回调。

旧连接的 `complete()` 会让它的 `onCompletion` 回调稍后在容器线程执行，而该回调调用的是 `userEmitters.remove(userId)`——按 key 无条件移除。如果它在新连接 `put` 之后执行，就会把新连接删掉。日志中表现为新连接建立后 2ms 出现「SSE 连接完成」，随后消息推送读到「用户不在线」。

见 `proposal.md` 的 Why；行为契约见 `specs/notification/spec.md`。

## Goals / Non-Goals

**Goals:**

- 旧连接的任何生命周期回调都不能移除同一用户的新连接。
- 重连后推送仍能命中当前连接。
- 用回归可验证的方式覆盖这条竞态。

**Non-Goals:**

- 不改变「一个用户一个连接」的语义，不支持多端同时在线。
- 不改心跳间隔、超时时间、SSE 事件格式或前端。
- 不处理多实例下的跨节点推送（当前由 Redis Pub/Sub 桥接，属于既有设计）。

## Decisions

### D1：按「key + 实例」条件移除，而不是按 key

新增 `removeEmitter(userId, emitter)`，内部使用 `ConcurrentHashMap.remove(key, value)`；三个生命周期回调都改用它。只有回调所属的 emitter 仍是当前连接时才移除。

- **备选：对 connect 与回调加同一把锁**。能保证顺序，但把连接建立与容器回调串行化，且回调仍可能在锁外执行；不能从根本上表达「只移除自己」。否决。
- **备选：给每个连接加代际号，回调比较代际**。需要额外的 map 或包装对象，逻辑更重，收益与条件移除相同。否决。
- **备选：用反射/包装取消旧回调**。`ResponseBodyEmitter` 没有提供注销回调的 API，不可行。

### D2：失败路径同样条件断开

`send`、`broadcast` 与心跳失败时，手上已经有具体的 emitter，改用 `disconnect(userId, emitter)`（条件移除后 `complete`），避免旧连接的发送失败连带移除新连接。`connect` 仍然用按 key 的 `disconnect(userId)`，因为它的语义是「替换当前连接」。

### D3：用回调本身做单测，不引入可测性改造

测试通过 `ReflectionTestUtils` 取出第一个 emitter 的 `completionCallback` 并执行，模拟旧连接的完成回调；随后断言 `isOnline(userId)` 仍为真。这样不需要为了测试给生产代码加 emitter 工厂或包级方法。

## Risks / Trade-offs

| 风险 | 缓解 |
|---|---|
| 依赖 Spring `ResponseBodyEmitter` 的私有字段名 `completionCallback`，升级可能改名 | 字段名在 Spring 6/7 稳定；测试失败会立刻暴露，修复只需调整反射字段 |
| `updateMetrics()` 在回调与条件移除中可能被调用两次 | 它只读取 map 大小并设置 gauge，幂等 |
| 心跳调度器仍按连接创建，重连会留下旧的 daemon 调度器直到下次触发 | 既有行为，本次不改；旧调度器在发现 `containsKey` 后自行 `shutdown` |
| 条件移除后 `disconnect(userId)` 的日志仍打印「连接断开」 | 日志只表达「替换了当前连接」，不影响正确性；不调整措辞以免扩大改动 |

## Migration Plan

1. 修改 `SseEmitterManager` 的三个回调与失败路径，新增 `removeEmitter` / 条件 `disconnect`。
2. 新增单测并运行 `mvn -pl iboot-notify test`。
3. 重启 notify，连续运行 `scripts/regress/regress.sh` 多次，确认 SSE 断言稳定。
4. 回滚：回退提交即可；无数据与配置变化。

## Open Questions

无。
