# Spring Boot 4.x 版本升级计划

## 1. 升级背景与目标

### 1.1 当前状态
- **当前版本**: Spring Boot 3.5.9
- **目标版本**: Spring Boot 4.x（最新稳定版）
- **当前 JDK**: JDK 25（已满足 Spring Boot 4.x 要求）

### 1.2 升级目标
- 将 iboot-admin 模块从 Spring Boot 3.5.9 升级到 Spring Boot 4.x 最新稳定版
- 确保所有依赖与 Spring Boot 4.x 兼容
- 保持现有功能完整性，确保系统稳定运行

---

## 2. 升级必要性分析

### 2.1 升级收益
| 收益项 | 说明 |
|--------|------|
| 长期支持 | Spring Boot 4.x 提供更新的 LTS 支持周期 |
| 性能提升 | Spring Framework 6.x 带来虚拟线程、性能优化 |
| 安全更新 | 获得最新的安全补丁和漏洞修复 |
| 生态兼容 | 与最新版本的中间件、数据库驱动更好兼容 |
| 新特性 | 支持 Spring AI、Observability 等新特性 |

### 2.2 不升级风险
| 风险项 | 说明 |
|--------|------|
| 安全漏洞 | 旧版本可能存在的未修复安全漏洞 |
| 兼容性问题 | 新版本的依赖可能不再支持 Spring Boot 3.x |
| 社区支持减少 | 随着时间推移，3.x 版本社区支持将逐步减少 |

---

## 3. 关键依赖兼容性分析

### 3.1 需要升级的核心依赖

| 依赖项 | 当前版本 | 推荐升级版本 | 兼容性风险 | 备注 |
|--------|----------|-------------|-----------|------|
| **Spring Boot** | 3.5.9 | 4.2.x | - | 核心升级 |
| **Spring Framework** | 6.0.x | 6.2.x | 中 | 随 Spring Boot 自动升级 |
| **Spring Security** | 6.1.x | 6.4.x | 高 | 安全配置 API 可能有变更 |
| **Spring Data Redis** | 3.1.x | 3.4.x | 中 | 需检查 Redis 配置 |
| **Spring Boot Actuator** | 3.5.9 | 4.2.x | 低 | 端点配置需审查 |
| **MyBatis Spring Boot** | 3.0.4 | 3.1.x+ | 中 | 需确认与 Spring Boot 4 兼容性 |
| **Druid** | 1.2.24 | 1.2.24+ | 高 | 当前使用 druid-spring-boot-3-starter，需切换到 4.x 版本 |
| **Flyway** | 10.20.1 | 11.x | 中 | 重大版本更新可能有 API 变更 |
| **Spring Boot Admin** | 3.4.5 | 4.x | 高 | 需等待官方支持版本 |
| **springdoc-openapi** | 2.8.13 | 3.x | 中 | Spring Boot 4 需要新版本 |
| **jjwt (JWT)** | 0.12.5 | 0.13.x | 低 | 兼容性较好 |
| **MapStruct** | 1.6.3 | 1.6.3+ | 低 | 基本无影响 |
| **Lombok** | 1.18.44 | 1.18.44+ | 低 | 基本无影响 |

### 3.2 需要特别注意的依赖

#### 3.2.1 Druid 连接池
- **当前配置**: `druid-spring-boot-3-starter:1.2.24`
- **问题**: 该依赖专门为 Spring Boot 3.x 设计
- **解决方案**:
  - 方案 A: 等待官方发布 `druid-spring-boot-4-starter`
  - 方案 B: 使用通用依赖 + 手动配置
  - 方案 C: 切换到 HikariCP（Spring Boot 默认）

#### 3.2.2 Spring Boot Admin
- **当前版本**: 3.4.5
- **问题**: 截至分析时，Spring Boot Admin 4.x 可能尚未发布稳定版
- **解决方案**:
  - 方案 A: 等待官方发布 4.x 版本
  - 方案 B: 暂时移除监控，升级后再集成
  - 方案 C: 使用替代方案（如 Prometheus + Grafana）

#### 3.2.3 Elasticsearch Client
- **当前版本**: 8.17.0
- **风险**: Elasticsearch Java Client 8.x 应该与 Spring Boot 4 兼容，但需验证

---

## 4. 代码层变更风险评估

### 4.1 安全配置（高风险）

**文件**: `SecurityConfig.java`

**当前代码**:
```java
@EnableWebSecurity
@EnableMethodSecurity
```

**潜在变更**:
- Spring Security 6.4.x 可能对 `@EnableMethodSecurity` 的属性进行变更
- `HttpSecurity` 配置链式 API 可能有调整
- CORS 配置 `setAllowedOriginPatterns` 可能被废弃或变更

**影响程度**: 高
**预估工作量**: 2-4 小时

---

### 4.2 JWT 认证过滤器（中风险）

**文件**: `JwtAuthenticationFilter.java`

**当前实现**:
- 继承 `OncePerRequestFilter`
- 使用 `UsernamePasswordAuthenticationToken`
- `SecurityContextHolder.getContext().setAuthentication()`

**潜在变更**:
- Spring Security 6.4.x 推荐使用 `SecurityFilterChain` 中的 lambda 表达式
- `SecurityContextHolder` 的使用方式可能有变更

**影响程度**: 中
**预估工作量**: 2-3 小时

---

### 4.3 Redis 缓存配置（中风险）

**文件**: `RedisConfig.java`

**当前实现**:
- `Jackson2JsonRedisSerializer` 使用 `activateDefaultTyping`
- `RedisCacheManager` 配置
- `RedisMessageListenerContainer`

**潜在变更**:
- `activateDefaultTyping` 方法可能被标记为过时
- Redis 序列化器 API 可能有调整

**影响程度**: 中
**预估工作量**: 2-3 小时

---

### 4.4 MyBatis 配置（低风险）

**文件**: `MyBatisConfig.java`

**当前实现**:
- `SqlSessionFactoryBean` 配置
- MyBatis `Configuration` 设置

**潜在变更**: MyBatis Spring Boot 3.1.x 与 Spring Boot 4 兼容性较好，变更风险低

**影响程度**: 低
**预估工作量**: 1 小时

---

### 4.5 Actuator 配置（低风险）

**文件**: `application.yml`, `ActuatorSecurityConfig.java`

**当前配置**:
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,...
```

**潜在变更**:
- 某些端点名称可能变更
- 安全配置方式可能调整

**影响程度**: 低
**预估工作量**: 1 小时

---

### 4.6 Quartz 定时任务（低风险）

**文件**: `QuartzConfig.java`

**当前配置**:
- `SchedulerFactoryBean`
- Quartz 属性配置

**潜在变更**: Spring Boot 4 对 Quartz 的支持基本保持稳定

**影响程度**: 低
**预估工作量**: 1 小时

---

### 4.7 OpenAPI/Swagger（中风险）

**文件**: `OpenApiConfig.java`

**当前版本**: springdoc-openapi 2.8.13

**潜在变更**: 需要升级到 springdoc-openapi 3.x 以支持 Spring Boot 4

**影响程度**: 中
**预估工作量**: 1-2 小时

---

## 5. 配置文件变更

### 5.1 application.yml 可能需要调整的配置

```yaml
# 可能的变更项
management:
  # 端点名称可能变更
  endpoints:
    web:
      exposure:
        include: # 需审查所有端点名称

# Spring Boot 4 可能的新配置项
spring:
  threads:
    virtual:
      enabled: true  # 可选：启用虚拟线程
```

---

## 6. 升级步骤

### 阶段一：准备阶段（1-2 天）

#### 6.1.1 环境准备
- [ ] 确认 JDK 版本 >= 21（Spring Boot 4 最低要求）
- [ ] 备份当前代码和数据库
- [ ] 记录当前所有依赖版本
- [ ] 准备测试环境

#### 6.1.2 依赖调研
- [ ] 确认所有第三方依赖的 Spring Boot 4 兼容版本
- [ ] 检查 Spring Boot Admin 4.x 发布状态
- [ ] 确认 Druid 的 Spring Boot 4 支持情况
- [ ] 列出所有需要升级的依赖

### 阶段二：依赖升级（1-2 天）

#### 6.2.1 POM 文件修改
```xml
<!-- 父依赖 -->
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>4.2.0</version> <!-- 使用最新稳定版 -->
</parent>

<!-- 属性更新 -->
<properties>
    <java.version>25</java.version>
    <spring-boot.version>4.2.0</spring-boot.version>
    <!-- 其他依赖版本根据实际情况更新 -->
</properties>
```

#### 6.2.2 依赖兼容性处理
- [ ] 处理 Druid 依赖（切换或等待官方支持）
- [ ] 处理 Spring Boot Admin 依赖（切换或等待）
- [ ] 升级 springdoc-openapi 到 3.x
- [ ] 升级 Flyway 到 11.x
- [ ] 升级 MyBatis Spring Boot 到兼容版本
- [ ] 升级所有其他依赖

### 阶段三：代码修改（2-3 天）

#### 6.3.1 安全配置修改
- [ ] 修改 `SecurityConfig.java` 适配新 API
- [ ] 修改 `JwtAuthenticationFilter.java` 适配新安全链
- [ ] 修改 `ActuatorSecurityConfig.java` 端点安全配置

#### 6.3.2 缓存配置修改
- [ ] 修改 `RedisConfig.java` 序列化器配置
- [ ] 修改 `TwoLevelCache.java` 如有 API 变更
- [ ] 测试缓存失效通知机制

#### 6.3.3 其他配置修改
- [ ] 修改 `MyBatisConfig.java` 如有需要
- [ ] 修改 `OpenApiConfig.java` 适配新版本
- [ ] 修改 `QuartzConfig.java` 如有需要

### 阶段四：测试验证（2-3 天）

#### 6.4.1 编译构建
- [ ] Maven 编译通过
- [ ] 无编译错误和警告
- [ ] 生成可执行 JAR

#### 6.4.2 单元测试
- [ ] 运行所有单元测试
- [ ] 修复失败的测试用例
- [ ] 确保测试覆盖率不下降

#### 6.4.3 集成测试
- [ ] 数据库连接测试
- [ ] Redis 连接测试
- [ ] 认证授权功能测试
- [ ] 缓存功能测试
- [ ] 定时任务测试
- [ ] 文件上传下载测试

#### 6.4.4 功能测试
- [ ] 用户登录/登出
- [ ] RBAC 权限控制
- [ ] 系统监控（Actuator）
- [ ] API 文档（Swagger）
- [ ] 文件管理
- [ ] 定时任务管理

#### 6.4.5 性能测试
- [ ] 接口响应时间测试
- [ ] 并发用户测试
- [ ] 内存使用测试
- [ ] 数据库连接池测试

### 阶段五：部署上线（1 天）

#### 6.5.1 预发布环境
- [ ] 部署到预发布环境
- [ ] 冒烟测试
- [ ] 观察日志无异常
- [ ] 监控指标正常

#### 6.5.2 生产环境
- [ ] 制定回滚方案
- [ ] 选择低峰期部署
- [ ] 部署后验证
- [ ] 监控告警配置

---

## 7. 风险与应对措施

### 7.1 技术风险

| 风险项 | 可能性 | 影响程度 | 应对措施 |
|--------|--------|----------|----------|
| 依赖不兼容 | 高 | 高 | 提前调研，准备替代方案 |
| API 变更导致代码大量修改 | 中 | 高 | 预留充足开发时间 |
| 性能下降 | 低 | 高 | 充分性能测试，准备回滚 |
| 安全漏洞 | 低 | 高 | 安全测试，代码审查 |

### 7.2 项目风险

| 风险项 | 可能性 | 影响程度 | 应对措施 |
|--------|--------|----------|----------|
| 升级时间超出预期 | 中 | 中 | 分阶段进行，优先核心功能 |
| 测试覆盖不全导致线上问题 | 中 | 高 | 制定详细测试清单 |
| 第三方依赖不支持 | 高 | 中 | 准备临时替代方案 |

---

## 8. 工作量评估

### 8.1 总工作量预估

| 阶段 | 预估工时 | 说明 |
|------|----------|------|
| 准备阶段 | 8-16 小时 | 环境准备、依赖调研 |
| 依赖升级 | 8-16 小时 | POM 修改、依赖兼容性处理 |
| 代码修改 | 16-24 小时 | 安全配置、缓存配置等 |
| 测试验证 | 16-24 小时 | 单元测试、集成测试、性能测试 |
| 部署上线 | 4-8 小时 | 预发布、生产部署 |
| **总计** | **52-88 小时** | **约 7-11 个工作日** |

### 8.2 人力资源建议
- **后端开发**: 1-2 人
- **测试人员**: 1 人
- **DevOps**: 0.5 人（部署支持）

---

## 9. 决策建议

### 9.1 建议升级的情况
- 项目处于开发早期或中期，有足够时间进行升级和测试
- 需要使用 Spring Boot 4 的新特性（如虚拟线程、Spring AI 等）
- 当前版本存在安全漏洞或严重 bug
- 团队有足够的技术能力和时间投入

### 9.2 建议暂缓升级的情况
- 项目已上线且运行稳定，无紧急升级需求
- 关键依赖（如 Spring Boot Admin、Druid）尚未提供稳定兼容版本
- 团队资源紧张，无法保证充足的测试时间
- 近期有重大业务需求需要交付

### 9.3 推荐方案

**建议采用渐进式升级策略**：

1. **短期**（1-2 个月）：等待关键依赖发布 Spring Boot 4 兼容版本
2. **中期**（2-3 个月）：在开发分支进行升级尝试，解决兼容性问题
3. **长期**（3-6 个月）：充分测试后，选择合适的窗口期进行生产升级

---

## 10. 附录

### 10.1 参考资源
- [Spring Boot 4.0 Release Notes](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Release-Notes)
- [Spring Framework 6.2 Release Notes](https://github.com/spring-projects/spring-framework/wiki/Spring-Framework-6.2-Release-Notes)
- [Spring Security 6.4 Migration Guide](https://docs.spring.io/spring-security/reference/migration/index.html)
- [Spring Boot 依赖版本兼容性矩阵](https://docs.spring.io/spring-boot/docs/current/reference/html/dependency-versions.html)

### 10.2 关键依赖版本追踪

| 依赖 | 最新版本 | Spring Boot 4 支持 | 备注 |
|------|----------|-------------------|------|
| Druid | 1.2.24 | 待确认 | 需关注官方动态 |
| Spring Boot Admin | 3.4.5 | 待发布 | 需关注官方动态 |
| MyBatis Spring Boot | 3.0.4 | 3.1.x+ | 需确认 |
| springdoc-openapi | 2.8.13 | 3.x+ | 需升级 |

---

**文档版本**: v1.0
**创建日期**: 2026-03-13
**最后更新**: 2026-03-13
**负责人**: [待指定]
