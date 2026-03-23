# Proposal: Enable Virtual Threads in iBoot Admin

## Why

**Problem/Opportunity:**

1. **当前线程模型限制**：Spring Boot 4.0.3 默认使用平台线程（Platform Threads），每个请求占用一个 OS 线程。在高并发 I/O 密集型场景下（如数据库查询、Redis 调用、HTTP 请求），线程阻塞导致资源浪费。

2. **JDK 25 成熟度**：项目已升级至 JDK 25，虚拟线程（Virtual Threads, JEP 444）已进入生产就绪状态，可无缝集成。

3. **性能提升机会**：
   - 虚拟线程创建成本极低（~几 KB vs 平台线程~1MB）
   - 可支持百万级并发连接
   - I/O 阻塞时自动卸载，不占用 OS 线程
   - 无需改动业务代码即可获得吞吐量提升

4. **Spring Boot 原生支持**：Spring Boot 4.x 已全面适配虚拟线程，提供一键启用配置。

## What Changes

**配置修改**：
- `application.yml`：添加 `spring.threads.virtual.enabled=true`
- `pom.xml`：确认 JVM 参数兼容虚拟线程

**代码影响**：
- 无业务代码修改（虚拟线程对应用透明）
- 需审查 `ThreadLocal` 使用场景（虚拟线程下可能有内存泄漏风险）
- 需审查 `synchronized` 块（会阻塞虚拟线程，建议改用 `ReentrantLock`）

**运维影响**：
- 监控指标需适配虚拟线程（`ExecutorService` 指标变化）
- 线程转储（Thread Dump）分析方式变更
- 内存占用模式改变（栈空间更小，但并发量更高）

## Impact

**受影响的组件**：

| 组件 | 影响程度 | 说明 |
|------|----------|------|
| Tomcat 容器 | 中 | HTTP 请求处理线程改为虚拟线程 |
| MyBatis 数据库操作 | 低 | I/O 阻塞自动卸载，性能提升 |
| Redis 缓存操作 | 低 | I/O 阻塞自动卸载，性能提升 |
| Spring Security | 低 | 认证过滤器兼容虚拟线程 |
| Quartz 定时任务 | 低 | 定时任务执行器需单独配置 |
| 异步任务 `@Async` | 中 | 需配置虚拟线程执行器 |

**API 变更**：无（对外接口保持不变）

**用户影响**：无（内部优化，用户无感知）

**性能预期**：
- 吞吐量提升：30%-50%（I/O 密集型场景）
- 延迟降低：10%-20%（减少线程上下文切换）
- 内存占用：降低 40%-60%（相同并发下）

**风险评估**：
- **低风险**：虚拟线程已进入 JDK 稳定版，Spring Boot 4.x 充分测试
- **注意事项**：
  - 避免在虚拟线程中使用 `ThreadLocal` 存储大量数据
  - 避免长时间 CPU 密集型计算（会占用虚拟线程）
  - `synchronized` 会阻塞虚拟线程，建议改用 `ReentrantLock`

## Migration Plan

1. **开发环境验证**（1-2 天）
   - 启用虚拟线程配置
   - 运行单元测试
   - 本地压测验证

2. **测试环境验证**（3-5 天）
   - 集成测试
   - 性能基准测试
   - 监控指标适配

3. **生产环境灰度**（1-2 周）
   - 小流量灰度
   - 监控告警观察
   - 全量发布
