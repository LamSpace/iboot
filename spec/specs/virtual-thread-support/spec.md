# Virtual Thread Support Specification

## Overview

iBoot Admin 使用 JDK 25 虚拟线程（JEP 444）提升高并发场景下的性能表现。

---

## Requirements

### Requirement: Virtual Thread Configuration

WHEN the application starts,
the system SHALL enable virtual threads for request processing.

#### Scenario: Enable virtual threads in application.yml
GIVEN the application uses Spring Boot 4.0.3 and JDK 25,
WHEN virtual threads are configured in `application.yml`,
THEN the application uses virtual threads for all HTTP request processing.

---

### Requirement: Virtual Thread Executor for Async Tasks

WHEN `@Async` annotated methods are invoked,
the system SHALL execute them using virtual thread executor.

#### Scenario: Async task execution with virtual threads
GIVEN a service method annotated with `@Async`,
WHEN the method is called,
THEN the system executes it on a virtual thread from the virtual thread executor.

---

### Requirement: Virtual Thread Metrics

WHEN virtual threads are enabled,
the system SHALL expose metrics for monitoring virtual thread activity.

#### Scenario: Monitor virtual thread metrics via Actuator
GIVEN Micrometer is configured,
WHEN virtual threads are active,
THEN the system exposes metrics including:
- `executor.active` - number of active virtual threads
- `executor.queue.remaining` - remaining capacity
- `tomcat.threads.current` - current thread count
- `tomcat.threads.busy` - busy thread count

---

### Requirement: ThreadLocal Cleanup

WHEN a request completes,
the system SHALL clean up any ThreadLocal variables to prevent memory leaks.

#### Scenario: ThreadLocal cleanup after request
GIVEN a filter or interceptor sets ThreadLocal variables,
WHEN the request processing completes,
THEN the system ensures ThreadLocal variables are removed via `ThreadLocal.remove()`.

---

### Requirement: Thread Pool Configuration

**Configuration:**
```yaml
server:
  tomcat:
    threads:
      virtual:
        enabled: true
    max-connections: 10000  # Virtual threads support higher concurrency
```

#### Scenario: Tomcat virtual thread configuration
GIVEN the application uses embedded Tomcat,
WHEN virtual threads are enabled,
THEN Tomcat creates virtual threads on-demand instead of pre-allocated platform threads.

---

### Requirement: Async Configuration

**Implementation:**
```java
@Configuration
@EnableAsync
public class AsyncConfig {
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
```

#### Scenario: Virtual thread executor for async operations
GIVEN async operations need to scale to high concurrency,
WHEN using virtual thread executor,
THEN the system can handle thousands of concurrent async tasks without thread exhaustion.

---

## Implementation Details

### Best Practices

**DO use virtual threads for:**
- HTTP request processing
- Database queries (MyBatis)
- Redis/cache operations
- External API calls
- File I/O operations

**DO NOT use virtual threads for:**
- CPU-intensive computations (use parallel streams instead)
- Long-running pinned operations (native calls that pin to platform threads)

### Thread Safety

- Virtual threads are compatible with existing synchronization
- `synchronized` blocks will pin virtual threads (use `ReentrantLock` for better performance)
- `ThreadLocal` works but requires explicit cleanup

### Monitoring Changes

| Metric | Before (Platform) | After (Virtual) |
|--------|------------------|-----------------|
| Thread count | 200-500 | 10,000+ |
| Memory per thread | ~1MB | ~few KB |
| Context switch overhead | High | Low |
| Thread creation time | Slow | Fast |

---

## Related Files

- `src/main/resources/application.yml` - 虚拟线程配置
- `src/main/java/com/iboot/admin/infrastructure/config/AsyncConfig.java` - 异步任务配置
- `src/main/java/com/iboot/admin/infrastructure/config/MetricsConfig.java` - 监控指标配置
- `VIRTUAL_THREADS.md` - 使用说明文档
