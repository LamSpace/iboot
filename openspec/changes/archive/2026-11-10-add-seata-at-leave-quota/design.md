# Design

## Context

- 全部服务共用数据库 `iboot`，但每张表只有一个所有者：`iboot-workflow` 拥有 `biz_leave_application` 与 Flowable 表，`iboot-system` 拥有 `sys_user` 等系统表。跨服务写入不能靠“同一个本地事务”解决，否则会破坏表所有权。
- 当前审批通过是一个本地事务：`ApprovalApplicationService.approve` 依次执行 Flowable `completeTask`、申请状态更新；通知通过本地 outbox 异步投递，不在主链路。
- 服务间调用统一使用 `RestClient` + `@LoadBalanced`，不走网关、不用 Feign。Seata 2.5.0 没有 `RestClient` 的 XID 透传实现，需要自己补拦截器。
- Spring Cloud Alibaba 2025.1.0.0 的 BOM 已钉住 `org.apache.seata:seata-spring-boot-starter:2.5.0`；`apache/seata-server:2.5.0` 镜像已在本地。
- 运行环境是 Java 25 + Boot 4.0.3 + 虚拟线程；7 个服务用 Druid，`iboot-ai` 用 Hikari。Seata 客户端与 Druid、虚拟线程的兼容性必须先用 spike 验证。
- 需求见 `specs/`；动机见 `proposal.md`，本文不重复。

## Goals / Non-Goals

**Goals:**

- 审批通过与额度扣减在同一个 Seata AT 全局事务内完成，任一步失败可回滚。
- 额度成为主线上真实生效的规则：默认值可配置、首次扣减懒创建、只读查询、按申请幂等扣减。
- 建立可复用的 XID 传播与失败关闭约定，为后续 TCC 变更留出边界。
- 给出可重复的端到端验证：正常提交、业务失败、后置失败回滚、TC/RM 不可用、并发、幂等。

**Non-Goals:**

- 不做 TCC；不做提交时预占额度；不做审批驳回/撤回后的额度返还。
- 不做管理员按人调整额度，不改通知 outbox 与 RocketMQ 链路。
- 不引入多 schema、不解决拆库后的 resourceId 规划（本次只处理共享库下的唯一性问题）。
- 不做阶段 3 的 K8s/Helm 部署形态。

## Decisions

### D1. 用 AT，而不是 TCC / XA / outbox

本次的写是“纯 DB、短事务、无外部副作用”的扣减，AT 的 undo_log 能直接覆盖；TCC 需要额外的 Try/Confirm/Cancel 与防悬挂逻辑，留给独立实验；XA 对 MySQL 的锁与性能代价更高；outbox 只能保证最终一致，无法满足“批了但没扣”不可接受的要求。

**备选**：把额度表放进 `iboot-workflow`，退回本地事务。否决原因：这会掩盖跨服务一致性问题，也不符合“额度随用户、由系统域拥有”的边界。

### D2. 次数配额 + 懒创建

按 `user_id + year` 存 `total_count / used_count / frozen_count`，一次审批通过扣 1。默认值取 `sys_config` 的 `leave.quota.default-count`，缺失或非法时按 10。

额度行在**首次扣减**时懒创建；查询无行时按默认值返回且不写库。这样新增用户、跨年、测试环境都不需要额外的 seed 脚本。`frozen_count` 本次恒为 0，为下一个 TCC 变更预留，避免二次改表。

**备选**：迁移时给所有现有用户 seed 当前年度额度。否决原因：跨年会过期，新增用户会漏，且迁移无法读取运行时配置。

### D3. 内部扣减接口 + 申请 ID 幂等

`iboot-system` 暴露 `POST /internal/leave/quota/deduct`，路径不在 `/api/**` 下，加白名单并用 `X-Internal-Token` 鉴权（与 notify 的内部接口同构）。请求体包含申请 ID、用户 ID、年度；申请 ID 作为幂等键写入 `sys_leave_quota_log` 的唯一索引。

扣减用一条带条件的 `UPDATE`：

```sql
UPDATE sys_user_leave_quota
   SET used_count = used_count + 1
 WHERE user_id = ? AND year = ?
   AND total_count - used_count >= 1
```

影响行数为 0 → 抛“额度不足”。不先查后改，避免并发下的检查-更新竞态。

**备选**：workflow 直接写 `sys_user_leave_quota`。否决原因：违反表所有权，且把系统域的规则复制到调用方。

### D4. 全局事务边界与调用顺序

`@GlobalTransactional` 放在 `ApprovalApplicationService.approve` 上，顺序固定为：

1. 校验申请与任务归属；
2. 远程扣减额度（此时 system 的本地分支已提交）；
3. Flowable `completeTask`；
4. 更新申请状态。

远程扣减放在本地写之前，是为了让“远程已提交、后续本地失败”成为可复现的回滚场景；如果本地先写、远程后扣，失败只会回滚本地，证明不了 AT 的跨服务回滚。

`@GlobalTransactional` 必须由外部 Bean 调用（Controller → Service），不能走同类内部调用，否则代理不生效。

### D5. XID 传播：自定义 RestClient 拦截器 + MVC 服务端拦截器

- 客户端：给 workflow 指向 system 的 `RestClient` 注册 `ClientHttpRequestInterceptor`，在 `RootContext.getXID()` 非空时写入 `TX_XID` 头。
- 服务端：在 system 与 workflow 注册 Seata 2.5.0 自带的 `JakartaTransactionPropagationInterceptor`，在 Controller 之前绑定 XID、请求结束后解绑。该拦截器是否被自动装配需 spike 确认；不自动装配就显式注册。
- 虚拟线程：`RootContext` 是普通 `ThreadLocal`。请求线程是虚拟线程不影响，但一旦切线程（`@Async`、`CompletableFuture`、Reactor）XID 就丢失。因此参与全局事务的调用链必须保持同步；这条写进文档并在评审中检查。

**备选**：把内部调用改成 Feign 或 RestTemplate，使用 Seata 自带拦截器。否决原因：项目已统一 `RestClient`，为一个调用引入第二套 HTTP 客户端不划算。

### D6. seata-server 常驻，file store + 卷

`docker-compose.yml` 默认启动 `apache/seata-server:2.5.0`，`dev-init.sh` 把它列入中间件并等待 `8091` 可连。本地用 file store，挂 named volume 保留会话；控制台 `7091` 只绑 localhost，不经网关。阶段 3 再换 db/redis store。

先不用 Nacos 作为 Seata 的注册/配置中心：共享库 + RestClient + JDK 25 已经有足够多的变量，spike 通过后再决定是否切 Nacos。

### D7. 共享库的 undo_log 与 resourceId

- `undo_log` 只有一张，由 `iboot-db-init` 创建；所有 RM 共同写入。这是“单库过渡形态”，在 `docs/23 §11.12` 登记为写入例外，拆库后每 schema 一张。
- Seata 默认用 JDBC URL 作为 `resourceId`。workflow 与 system 连同一个 `iboot`，resourceId 会相同。spike 实测：TC 仍按 `branchId` 分别登记与提交，回滚由持有通道的 RM 读取共享 `undo_log` 执行，单库下未出现回滚失败。
- 拆库后必须唯一化，写法为手工声明 `new DataSourceProxy(dataSource, "<service-name>")` 并关闭自动数据源代理。本次不实施：Druid starter 的 `@ConditionalOnMissingBean` 与自定义代理存在启动次序耦合，且单库场景没有回滚失败证据；该事项留给拆库变更。

### D8. 失败关闭，不做静默降级

- TC 不可用 → `@GlobalTransactional` 在开启阶段失败，审批返回明确错误。
- system 不可用 → 远程扣减失败，全局回滚，审批不落库。
- 参与方未启用 Seata 却收到 `TX_XID` → 拒绝请求，避免“本地提交但不登记分支”。实现上在 RM 侧加一个轻量检查：`RootContext.getXID() != null && !seata.enabled` 时返回 503。
- 不提供“关掉 Seata 继续审批”的运维开关；`IBOOT_SEATA_ENABLED` 只用于本地排障，文档注明它不是降级手段。

### D9. 额度查询与前端

- 查询：`GET /api/approval/leave/quota?year=...` 由 workflow 提供，用当前登录用户 ID 调 system 的内部只读接口。前端请假表单展示“剩余 x/10”。
- 提交只做软提示：剩余为 0 时提示“审批时可能因额度不足失败”，不阻断提交；硬约束在审批通过。这样不会把额度查询变成提交路径的强依赖。
- system 不可用时查询返回明确错误，前端展示“—”，不阻断提交。

### D10. 可观测性与测试钩子

- Seata 的 `RootContext` 自带 `MDC_KEY_XID`，把 XID 写进日志 MDC，与 Tempo 的 TraceID 一起输出；全局事务控制台 + SQL 查询作为排障入口。
- 后置失败回滚用一个**仅测试 profile 激活**的故障注入拦截器：识别 `X-Seata-Demo-Fail: after-remote`，在远程扣减返回后抛异常。默认关闭，单测断言默认不注册。

### D11. 验证分层

- 单测：XID 拦截器、服务端绑定/解绑、额度幂等、并发扣减、默认值非法。
- 端到端脚本 `scripts/regress/seata.sh`：登录、造额度、提交、审批、SQL 断言；覆盖额度不足、后置失败回滚、TC/RM 不可用、并发、重复请求。
- 不在 JUnit 里启动两个服务 + TC 做端到端：成本高且不稳定；真实回滚证明放在脚本层，CI 定时任务执行。

## Risks / Trade-offs

- **TC 成为审批路径的单点** → fail-closed + 审批 5xx 告警 + seata-server 健康检查；文档明确这是强一致的代价。
- **AT 无法撤销副作用** → 全局事务内只放纯 DB 写；不把 MinIO、SSE、邮件、MQ、异步任务拉进来；通知继续走 outbox。
- **共享库 resourceId 冲突** → spike 优先验证；手动 `DataSourceProxy` 指定唯一 resourceId 作为兜底。
- **Boot 4 / Java 25 / Druid / 虚拟线程兼容性未知** → spike 门禁，不通过就停止，不写业务代码。
- **全局锁与回滚日志** → 单行条件更新、短事务；监控 `undo_log` 残留与锁等待。
- **故障注入钩子进入主代码** → 仅测试 profile 注册、默认关闭、单测锁定默认行为，文档标注测试用途。
- **额度规则过于简化** → 次数制、按开始时间归属年度、驳回不返还；这是本轮明确的范围，按天与返还另起变更。
- **只读查询增加对 system 的依赖** → 查询失败只影响 badge，不阻断提交；审批仍 fail-closed。

## Migration Plan

1. **Spike 门禁**：两个最小服务端点 + seata-server，验证 Boot 4/Java 25 启动、Druid 代理、RestClient XID、resourceId、TC 故障行为；产出 `docs/spike/seata-2.5-spike-report.md`。不通过即停止。
2. **基础设施**：compose 增加 seata-server（默认常驻、卷、内存上限、控制台绑定）；`dev-init.sh` 增加启动顺序与健康等待；`.env.example` 增加变量。
3. **数据库**：`iboot-db-init` 新增迁移，只加表与索引（`undo_log`、`sys_user_leave_quota`、`sys_leave_quota_log`），无数据迁移。
4. **system**：内部扣减与只读接口、幂等、失败关闭、单测。
5. **workflow**：额度客户端、XID 拦截器、`@GlobalTransactional`、查询接口与单测。
6. **portal**：请假表单额度 badge 与提示。
7. **验证与文档**：`scripts/regress/seata.sh`、ROADMAP §7.10/§7.11、docs/23 §16、docs/02、README、版本矩阵与风险登记。

回滚策略：本次只新增表与接口，数据库变更向前兼容；回滚以回退发布为主。由于审批路径已强依赖 TC，`seata.enabled=false` 不是安全降级手段，文档必须写明。

## Spike 结果（2026-11-09）

spike 已通过，详细证据见 `docs/spike/seata-2.5-spike-report.md`：

- Seata 2.5.0 客户端在 Java 25 + Boot 4 + 虚拟线程下正常启动，完成一次两分支提交与一次“远程已提交后本地失败”的回滚。
- Druid 1.2.28 被自动代理（`Auto proxy data source 'dataSource' by 'AT' mode`），无需手工 `DataSourceProxy`。
- `RestClient` + `JakartaSeataWebMvcConfigurer` 的 XID 透传有效；system 在 workflow 的 XID 下登记分支。
- 共享库下两个服务得到相同 resourceId；TC 仍按 branchId 工作，单库回滚成功。拆库时必须唯一化，本次不实施。
- `apache/seata-server:2.5.0` 的 TC 端口为 8091；镜像不含 7091 控制台；file store 目录为 `/seata-server/sessionStore`；堆通过 `JVM_XMS` / `JVM_XMX` 覆盖；无 HTTP 健康端点，使用 TCP 探测。

## Open Questions

无。spike 已回答全部实现前未知项。
