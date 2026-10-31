# Proposal

## Why

阶段 2 的下一个动作是拆出第一个业务服务，但拆出来的服务需要一整套目前只以「复制粘贴」形式存在的公共能力：响应信封与异常处理、JWT 资源服务器过滤器、当前用户工具、操作日志切面。

证据在代码里：`iboot-ai` 为了成为资源服务器，已经把 `JwtAuthenticationFilter`、`JwtTokenUtil`、`SecurityUtils`、`SecurityWhitelistConfig` 等 683 行生产代码复制了一份；再拆 6 个服务就是 6 份复制。`add-microservice-foundation` 的 D3 约定「等出现第三个消费者再抽共享内核」——`iboot-file` 就是第三个消费者。

本变更只做**纯搬家**：把已经存在于 admin 的公共能力收敛为一个模块，不新增功能、不改接口、不改响应结构。目的是让后续每个拆出的服务都能复用同一份安全与日志实现，而不是继续复制。

## What Changes

- 新增 Maven 模块 `iboot-kernel`，由根 `pom.xml` 聚合；模块内按职责分包：
  - `com.iboot.kernel.web`：`Result`、`PageResult`、`BusinessException`、`GlobalExceptionHandler`、`I18nUtil`。
  - `com.iboot.kernel.security`：`JwtTokenUtil`、资源服务器过滤器、`SecurityUtils`、白名单配置、`Constants`、`JwtSecurityConfig`。
  - `com.iboot.kernel.logging`：`@Log`、`LogAspect`、`OperateLog` 模型与**写路径**（只做 insert 的 Mapper + 异步 recorder）。查询侧留在管理端——那里的 `OperateLogMapper` 带 `@DataScope`，搬进内核会引入一个在多数服务中静默失效的数据权限注解。
- 上述类从 `iboot-admin` 移入 `iboot-kernel`，包名同步改为 `com.iboot.kernel.*`；admin 改为依赖该模块。
- `iboot-ai` 删除自己那份安全实现，改为依赖 `iboot-kernel`；`PermissionScopeClient` 等 AI 专属类留在 ai。
- 白名单从「硬编码路径数组」改为「内核默认值 + 各服务配置项覆盖」（admin 与 ai 的白名单本来就不同，见 design）。
- 内核通过 Boot 自动配置注册 Bean，宿主服务只需加依赖，不要求改 `@SpringBootApplication` 的扫描路径。

明确不做（非目标）：

- **不抽** `DataScopeInterceptor`、二级缓存、SSE、`FileStorageStrategy`。它们各自的归属等对应服务拆分时再定，避免内核变成「什么都往里塞」。
- **不改任何 `/api/**` 路径、响应结构或鉴权语义**；唯一登记在案的差异是 AI 服务 401 响应体统一为 `Result` 信封（见 Impact）。
- **不动 `iboot-gateway`**：它是 WebFlux，与内核的 Servlet 组件无关。
- **不拆任何业务服务**：`iboot-file` 的拆分是下一个变更。

## Capabilities

### New Capabilities

（无。本变更是纯重构：外部行为不变，规格无需新增。）

### Modified Capabilities

（无。既有的 `api-gateway`、`service-discovery`、`ai-chat` 等能力的行为要求不变。）

> 本变更在 `.openspec.yaml` 中标记 `skip_specs: true`。

## Impact

**构建**

- 根 `pom.xml` 新增 `iboot-kernel` 模块。
- `iboot-admin` / `iboot-ai` 新增对 `iboot-kernel` 的依赖。
- 预计触及 admin 约 200 个文件的 `import` 语句（`common.result` 39、`common.exception` 50、`infrastructure.security` 60、`@Log` 20 等），全部为机械改名，无逻辑改动。

**行为**

- admin：行为零变化。测试文件只因包名迁移改 `import`（约 14 个文件），另有 `ApprovalControllerTest` 补一行 `@Import`——`@WebMvcTest` 的切片不再能扫描到迁出包名下的 advice。
- ai：安全过滤器换成内核实现；401 响应体**保持原样**（由 ai 自己的安全配置写出），仅「过滤器内 Redis 异常」这一分支改为 `Result` 信封。
- **内核的全局异常处理器在 ai 中默认关闭**（`iboot.kernel.exception-handler.enabled=false`）。ai 已有 `AiExceptionHandler`，引入内核处理器会把参数校验与未预期异常的响应结构一并改掉，导致契约快照漂移；保持关闭后 ai 的 OpenAPI 零 diff。
- ai 的 security 类不是 admin 的逐字复制（`JwtTokenUtil` 只验签、`SecurityUtils` 多两个方法且异常类型不同、过滤器为三参构造），因此删除副本后 ai 的 **5 个安全相关测试文件**必须调整：过滤器测试迁入内核改造为内核语义测试，4 个控制器测试只改 import。

**测试与质量**

- `mvn verify`（含 Testcontainers）在改造前后均须通过。
- 测试代码只做 import 级调整：admin 约 14 个文件改 import + 1 个补 `@Import`；ai 4 个控制器测试改 import，1 个过滤器测试迁入内核并改造。测试**断言**一处未改。
- 契约快照 `scripts/regress/golden/` 零 diff；路由覆盖、冒烟、权限矩阵全通过。

**风险**

- 包名迁移是机械改动，但 Spring 组件扫描范围会变：内核必须通过自动配置注册，否则宿主启动后过滤器不生效而接口仍返回 200（静默安全失效）。验证项为此单列。
