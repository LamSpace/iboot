# Tasks

## 1. 连接回调

- [x] 1.1 在 `SseEmitterManager` 新增 `removeEmitter(userId, emitter)`（`ConcurrentHashMap.remove(key, value)`），`onCompletion`、`onTimeout`、`onError` 改为调用它，并同步更新类/方法 Javadoc；验证：代码审查确认三个回调都不再按 key 无条件移除
- [x] 1.2 新增 `SseEmitterManagerTest`：同一用户连续 `connect` 两次，反射取出第一个 emitter 的 `completionCallback` 并执行，断言 `isOnline(userId)` 仍为真；验证：`mvn -s /home/lam/repo/settings.xml -pl iboot-notify test` 通过
- [x] 1.3 增加超时与错误回调的同类断言（旧 emitter 的 timeout/error 回调执行后新连接仍在）；验证：三个回调各有一条测试覆盖

## 2. 失败路径

- [x] 2.1 新增 `disconnect(userId, emitter)` 条件断开重载，`send` 的两条失败分支、`broadcast` 的离线清理与心跳失败改为使用它；验证：编译通过，现有 notify 单测全部通过
- [x] 2.2 为 `send` 失败路径补一条单测：构造发送抛 `IOException` 的 emitter，确认只会断开它自身而不会移除同用户的新连接；验证：单测通过

## 3. 回归验证

- [x] 3.1 重新打包并重启 `iboot-notify`；验证：`/actuator/health` 为 `UP`，启动日志无异常
- [x] 3.2 连续运行 `bash scripts/regress/regress.sh` 三次；验证：三次均全绿，SSE 的两个断言不再随机失败
- [x] 3.3 在 `doc/23` §11.7 的 `iboot-notify` 服务状态中补一行「连接重连语义」；验证：文档说明「同一用户重连时旧连接回调不会移除新连接」，并指向对应单测
