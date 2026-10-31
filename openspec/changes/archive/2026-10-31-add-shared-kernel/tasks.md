# Tasks

> 构建命令统一使用 `mvn -s /home/lam/repo/settings.xml`；`verify` 需带
> `TESTCONTAINERS_RYUK_CONTAINER_IMAGE=testcontainers/ryuk:0.12.0`。

## 1. 模块骨架

- [x] 1.1 根 `pom.xml` 的 `<modules>` 增加 `iboot-kernel`，并在 `dependencyManagement` 声明其坐标；验证：`mvn -s /home/lam/repo/settings.xml validate` 通过，`mvn -s /home/lam/repo/settings.xml -N help:evaluate -Dexpression=project.modules` 输出含 `iboot-kernel`
- [x] 1.2 新建 `iboot-kernel` 模块：空的三层包 `com.iboot.kernel.web/.security/.logging`、`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 占位文件、模块级 Javadoc；验证：`mvn -s /home/lam/repo/settings.xml -pl iboot-kernel package` 成功，且 jar 内含 `AutoConfiguration.imports`

## 2. web 包迁移

- [x] 2.1 把 `Result` / `PageResult` / `BusinessException` / `GlobalExceptionHandler` / `I18nUtil` 移到 `com.iboot.kernel.web`，包名与 Javadoc 同步更新；验证：`mvn -s /home/lam/repo/settings.xml -pl iboot-kernel package` 通过
- [x] 2.2 更新 admin 与 ai 中全部引用（预计约 110 处 `import`），不改任何逻辑；验证：`mvn -s /home/lam/repo/settings.xml -pl iboot-admin,iboot-ai -am test-compile` 通过，且 `git diff --stat` 只显示 import 行变化
- [x] 2.3 新增 `KernelWebAutoConfiguration`，用 `@Import` 注册 `GlobalExceptionHandler`；验证：新增 `KernelWebAutoConfigurationTest` 断言宿主上下文里存在该 Bean 与 `Result` 的 Jackson 序列化形态，且测试通过

## 3. security 包迁移

- [x] 3.1 把 `JwtTokenUtil` / `SecurityUtils` / `Constants` / `JwtSecurityConfig` 移到 `com.iboot.kernel.security`，admin 改引用；验证：`mvn -s /home/lam/repo/settings.xml -pl iboot-admin -am test-compile` 通过
- [x] 3.2 把资源服务器过滤器移入内核，白名单从硬编码数组改为配置项 `iboot.security.whitelist`（内核只提供 Swagger / `/actuator/**` / `/error` 默认值）；验证：内核单测断言默认值生效、覆盖值生效
- [x] 3.3 在 admin 与 ai 的 `application.yml` 中分别写入**现有完整白名单**，逐条对照原常量数组；验证：新增测试断言两服务解析出的白名单与原数组完全一致（顺序无关的集合比较）
- [x] 3.4 删除 `iboot-ai` 自带的 `JwtAuthenticationFilter` / `JwtTokenUtil` / `SecurityUtils` / `SecurityWhitelistConfig`，改为依赖内核；ai 的 `JwtAuthenticationFilterRedisTest` 迁入内核并改造为内核过滤器的语义测试，4 个控制器测试只改 import；验证：`grep -r "com.iboot.ai.infrastructure.security.JwtAuthenticationFilter\|com.iboot.ai.infrastructure.security.JwtTokenUtil\|com.iboot.ai.infrastructure.security.SecurityUtils" iboot-ai/src` 无结果，`mvn -s /home/lam/repo/settings.xml -pl iboot-ai -am test` 通过
- [x] 3.5 补齐过滤器语义单测：白名单放行、无 token 放行、有效 token 建立上下文并续期两个 Redis 键、非法签名、会话缺失、Redis 异常返回 401 且响应体为 `Result` 信封；验证：上述用例全部通过
- [x] 3.6 新增注册测试：宿主只加依赖、不改启动类时，过滤器已被注册进安全链；验证：测试断言 `FilterChainProxy` 中包含该过滤器（防止 D3 静默失效）
- [x] 3.7 在 `doc/23-微服务底座操作指南.md` 增加「共享内核」章节：模块坐标、自动配置方式、白名单配置项写法、401 响应约定；验证：按文档新增一个白名单路径后测试通过，章节中的配置片段与代码逐字一致

## 4. logging 包迁移

- [x] 4.1 把写路径移入 `com.iboot.kernel.logging`：`@Log`、`BusinessTypeEnum`、`OperateLog` 模型、`LogAspect`，新增只做 insert 的 `OperateLogWriteMapper` 与异步 recorder；查询侧（仓储 / Mapper / PO / `OperateLogApplicationService`）留在管理端，`@DataScope` 不进入内核；验证：`mvn -s /home/lam/repo/settings.xml -pl iboot-kernel package` 通过，且 `grep -rn "import com.iboot.admin\|DataScope" iboot-kernel/src` 无结果
- [x] 4.2 更新 admin 的 `LogController`、`DashboardApplicationService` 及所有 `@Log` 使用点；删除因本变更而失去调用方的 `OperateLogApplicationService.recordOperateLog`；验证：`mvn -s /home/lam/repo/settings.xml -pl iboot-admin -am test-compile` 通过
- [x] 4.3 验证日志链路行为不变：带 `@Log` 的接口调用后 `sys_operate_log` 新增一条，且 `LogController` 查询可读到；验证：在内核内新增切面单测断言写入参数，并在本地全栈上执行一次上传后 `SELECT COUNT(*)` 增加 1

## 5. 集成验证

- [x] 5.1 全量构建与测试：`TESTCONTAINERS_RYUK_CONTAINER_IMAGE=testcontainers/ryuk:0.12.0 mvn -s /home/lam/repo/settings.xml verify`；验证：退出码 0，测试总数与预期一致；测试代码只做 import 级调整，唯一的非 import 改动是 `ApprovalControllerTest` 的一行 `@Import`，其余为新增的 `SecurityWhitelistBindingTest`
- [x] 5.2 契约零漂移：`bash scripts/regress/regress.sh`；验证：退出码 0，`golden/` 无 diff
- [x] 5.3 ai 的 401 响应体差异登记：确认 `Result` 信封生效；验证：不带 token 请求 `/api/ai/**` 返回 401 且响应体含 `code`/`message` 两字段，结果记入本变更说明
- [x] 5.4 更新文档：`ROADMAP.md` §4.2 增加「共享内核」状态说明与模块清单，`doc/03-架构设计.md` 更新模块拓扑；验证：文档中的模块名与 `pom.xml` 一致，链接可达
