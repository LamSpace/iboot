# iBoot Admin 虚拟线程配置说明

## 配置概述

iBoot Admin 已启用虚拟线程（Virtual Threads）以提升高并发场景下的性能。

**技术栈**：
- JDK 25 (支持 JEP 444 虚拟线程)
- Spring Boot 4.0.3 (原生支持虚拟线程)
- Tomcat 嵌入式容器

## 配置内容

### 1. application.yml 配置

```yaml
# 启用 Spring 虚拟线程
spring:
  threads:
    virtual:
      enabled: true

# 启用 Tomcat 虚拟线程
server:
  tomcat:
    threads:
      virtual:
        enabled: true
    max-connections: 10000
```

### 2. 异步任务执行器

创建 `AsyncConfig.java` 配置类，使用虚拟线程执行 `@Async` 异步任务：

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

### 3. 监控指标

Micrometer 会自动收集虚拟线程执行器的指标：
- `executor.active` - 活跃虚拟线程数
- `executor.queue.remaining` - 队列剩余容量
- `tomcat.threads.current` - 当前线程数
- `tomcat.threads.busy` - 繁忙线程数

## 验证方式

### 1. 启动应用

```bash
cd /home/lam/workspace/iboot/iboot-admin
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### 2. 检查线程信息

访问 Actuator 端点查看线程状态：

```bash
# 查看线程转储
curl -s http://localhost:8080/actuator/threaddump | jq

# 查看指标
curl -s http://localhost:8080/actuator/metrics | jq
```

### 3. 手动测试并发场景

1. **多标签页访问**：打开多个浏览器标签页访问系统
2. **核心功能测试**：登录、用户管理、菜单管理等
3. **观察响应速度**：对比启用前后的响应时间

### 4. 观察监控指标

访问 Prometheus 或 Grafana 查看：
- HTTP 请求吞吐量
- 线程活跃度
- 内存占用情况

## 性能预期

| 指标 | 预期提升 |
|------|----------|
| 吞吐量 | 30%-50% (I/O 密集型) |
| 内存占用 | 降低 40%-60% |
| 响应延迟 | 降低 10%-20% |

## 注意事项

### ThreadLocal 使用

项目中已有两处 `ThreadLocal` 使用，均已正确清理：
1. `LogAspect.java` - 记录操作日志耗时
2. `AbstractQuartzJob.java` - 记录定时任务开始时间

### 不适用场景

虚拟线程不适合以下场景：
- CPU 密集型计算（建议使用并行流）
- 长时间持有 `synchronized` 锁的代码（会阻塞虚拟线程）

## 故障排查

### 虚拟线程未生效

检查 JVM 版本：
```bash
java -version
# 应为 JDK 25
```

### 内存占用异常

检查是否有 ThreadLocal 泄漏：
```bash
# 查看线程转储，观察虚拟线程数量
curl http://localhost:8080/actuator/threaddump
```

## 相关文件

- 配置文件：`src/main/resources/application.yml`
- 异步配置：`src/main/java/com/iboot/admin/infrastructure/config/AsyncConfig.java`
- 监控配置：`src/main/java/com/iboot/admin/infrastructure/config/MetricsConfig.java`
- 启动类：`src/main/java/com/iboot/admin/IBootAdminApplication.java`
