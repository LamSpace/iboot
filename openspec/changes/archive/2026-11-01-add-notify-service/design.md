# Design

## Context

见 `proposal.md - Why`。影响实现方式的现状事实（均已核对到源码）：

- 消息域对外共 **5 组路径**：`/api/message/**`（含 `/api/message/template/**`）、`/api/notice/**`、`/api/notification/delivery/**`、`/api/push/**`、`/api/search`。
- `ApprovalApplicationService.submit()` 在 `@Transactional` 内同步调用 `notificationApplicationService.enqueue(...)`；模板停用时 `enqueue` 抛 `BusinessException`，事务回滚，审批提交失败。
- `infrastructure/search` 整个子系统只服务公告与消息：`ContentDocType` 只有 `NOTICE` / `MESSAGE` 两个值。
- 通知重试与内容索引对账都由 `iboot-admin` 的 Quartz 调度：`V16` 与 `V18` 各插入一行 `sys_job`（`contentIndexReconcileTask.reconcile`、`notificationRetryTask.retry`），由 `JobInvokeUtil` 反射调用管理端 Bean。
- 消息与公告**没有行级数据权限**：`@DataScope` 只出现在 `DeptMapper` / `PostMapper` / `LoginLogMapper` / `OperateLogMapper`，消息与公告 Mapper 没有该注解。因此 notify 不需要 `PermissionScopeClient`（与 `iboot-file` 不同）。
- 消息接收人 `receiverType=0` 时需要枚举全量用户（`userRepository.findAll()`），发外部渠道需要 `sys_user.email` / 手机号，公告需要读 `sys.notice.*` 配置——这些仍是跨 schema 只读。
- 网关已有 `/api/push/**` 长连接路由（`order: 0`、`response-timeout: -1`、`connect-timeout: 2000`），`/api/**` 兜底 `order: 100`。
- 回归装置：`regress.sh` 按「快照 → 路径守恒 → 路由覆盖 → 冒烟 → 权限矩阵」执行；`equivalence.sh` 目前硬编码 `golden/file-equivalence.json` 与 `/api/file/*`。

## Goals / Non-Goals

**Goals**

- 拆分前后 `/api/**` 的外部行为完全一致，前端零改动。
- 通知触发与审批主流程解耦，且失败可观测、可重试、不重复。
- 保留「改回网关路由即可回滚」的能力。

**Non-Goals**

- 不迁移数据到独立 schema，不启用 Flyway（留给 `remove-notify-from-admin`）。
- 不删除管理端旧实现。
- 不引入 RocketMQ 或其他新中间件。
- 不把维护任务统一到 `iboot-job`（该服务尚不存在）。
- 不改前端任何代码。

## Decisions

### D1. 审批触发走跨服务 HTTP + 提交后重试

管理端只依赖端口 `NotificationPort.enqueue(NotificationCommand)`；HTTP 适配器调用 `POST /internal/notifications`。

- 调用点从「事务内同步」改为「事务提交后异步」：沿用既有 `@TransactionalEventListener(AFTER_COMMIT)` + `@Async` 先例。
- 重试上限 3 次、指数退避；全部失败后记录结构化错误与指标 `notification_trigger_failed_total`，**不向上抛**。
- 内部端点使用共享密钥头 `X-Internal-Token`（环境变量注入），失败关闭；不经网关，`routes.txt` 不登记。
- 校验顺序保持在 notify 内部：模板停用、接收人不存在仍返回明确错误，只是错误不再回灌到审批提交。

**为什么不是 outbox**：outbox 要把通知状态回灌进管理端表，等于在拆分后仍让 admin 持有通知域的数据，违背本次拆分目的；且需要在 admin 再加一个扫描任务。

**为什么不是 RocketMQ**：为一个调用引入中间件，与 ROADMAP §2.1「服务间先走声明式 HTTP」冲突；MQ 应在业务事件成规模后统一引入。

**退出条件**：RocketMQ 引入时把 HTTP 适配器换成事件发布器，端口与业务代码不动。

### D2. 过渡期共享表，不启用 Flyway

`iboot-notify` 的数据源指向现有数据库，直接读写 6 张表；本变更不接管任何 DDL。

**为什么**：绞杀者回滚成立的前提是两边读写同一份数据。若本变更就迁 schema，回滚后旧实现看不到新写入的数据，回滚是假的。

**约束**：同一时刻只允许一条投递路径生效。本变更把审批触发切到 notify 后，管理端不再触发通知；管理端旧代码保留但不再被调用。

**退出条件**：`remove-notify-from-admin` 删除旧实现时，把 6 张表迁到 `iboot_notify` 并启用 Flyway。

### D3. 内容检索随数据一起迁走

`infrastructure/search` 全部随 notify 迁移：索引写入、对账、启动回填，以及查询端点 `/api/search`。

**为什么**：`ContentDocType` 只有公告与消息两种，索引的唯一数据来源就是迁走的这两张表。把索引留在 admin 意味着「数据在 B、索引在 A」，A 必须反向读 B 的表或调用 B，依赖方向被倒置。

**过渡期约束**：管理端的 `sys_job` 对账行继续运行（读共享表、写同一批文档 ID），与 notify 侧的索引写入幂等，不冲突；对账任务本身在第二个变更中迁走。

**备选（否决）**：admin 保留索引、通过跨 schema 只读 notify 表——可行但把两个服务的表耦合写死，且第二个变更还要再动一次。

### D4. 维护任务在第二个变更才迁移

- 本变更：`notificationRetryTask` 与 `contentIndexReconcileTask` 继续由管理端 Quartz 调度，读共享表，行为不变。
- 第二个变更：任务迁入 notify，用 `@Scheduled` + Redis 分布式锁（复用现成 Redis）执行，并删除两行 `sys_job`。

**为什么不在本变更迁**：调度迁移需要同时确认「旧 Job 行删除」与「新服务实例能跑」，放在同一个变更里会让回滚步骤失效——回滚到旧实现时旧 Job 行已删，任务直接消失。

**备选（否决）**：把任务做成「HTTP 通用 Job 处理器」由管理端调度——为一个尚未存在的 job 服务提前设计接口。

### D5. 服务身份与路由

`iboot-notify` / 端口 **8084** / 服务名与 `spring.application.name` 一致。

网关路由按既有顺序插入，均在 admin 兜底之前：

| order | id | 路径 | 备注 |
|---|---|---|---|
| 0 | `notify-sse` | `/api/push/**` | 继承 `response-timeout: -1`、`connect-timeout: 2000` |
| 20 | `file` | `/api/file/**` | 不变 |
| 25 | `notify` | `/api/message/**,/api/notice/**,/api/notification/**,/api/search` | order 需在 file 之后、admin 之前 |

**为什么不合并成 `/api/**` 兜底**：`routes.txt` 是路由覆盖检查的唯一事实来源，前缀必须与 `/api/auth/scope` 一样逐条可判定退出。

### D6. 不引入数据权限客户端

消息与公告的可见性由「接收人关联表」和 RBAC 权限码决定，不涉及行级数据权限；notify 不实现 `PermissionScopeClient`。

跨 schema 只读仍按 `iboot-file` 的先例，且不复制完整领域模型：

- `SysConfigReader` 读 `iboot_admin.sys_config`（消息/公告条数上限）；
- `DictLabelLookup` 读 `iboot_admin.sys_dict_data`（Excel 导出的字典翻译）；
- `NotifyUserReader` 读 `iboot_admin.sys_user`（全量接收人枚举、邮箱与手机号），只返回 `id / username / nickname / email / phone` 的最小视图。

同理，管理端的 `BusinessMetricsService` 不整体搬迁——它包含缓存与登录等本服务无关的指标。notify 只保留推送链路的三项指标，由 `PushMetrics` 提供，指标名不变，`application` 标签改为 `iboot-notify`。

**退出条件**：`iboot-system` 拆分后，改为调用系统服务的配置与用户查询接口。

## Risks / Trade-offs

- **通知在 notify 长时间不可用时丢失** → 重试 3 次后记失败指标 + 告警；`notification:delivery` 页面保留人工重试；退出条件见 D1。
- **过渡期双实现双写同一批表** → 只允许一条路径生效（D2 约束）；回归等价性在流量切换后立即验证。
- **ES 文档重复写入**（管理端对账 + notify 写入）→ 文档 ID 由 `docType + sourceId (+ receiverId)` 决定，幂等；对账只覆盖已存在数据。
- **`/api/push` 长连接迁移导致前端断连** → 路由切换后前端自动重连；冒烟用例必须验证经网关的 SSE 仍能收到消息。
- **本机内存 R4 再 +1 个 JVM** → 本地按需启动；notify 堆上限 512MB，与 `iboot-file` 一致。
- **内部端点成为越权入口** → 不经网关暴露、共享密钥失败关闭、端点只接受通知登记命令，不接受任意写操作。

## Migration Plan

1. 启动 `iboot-notify`（8084），数据源指向现有库，**不启用 Flyway**。
2. 逐个切换网关路由：先 `/api/push/**` 与 `/api/message/**`，再 `/api/notice/**`、`/api/notification/**`、`/api/search`；每切一条跑一次冒烟。
3. 切换审批触发：把 `ApprovalApplicationService` 的依赖改为 `NotificationPort`，管理端旧通知实现保留但不再被调用。
4. 全量回归通过后，`migrations.txt` 五条登记保持 `transitional`。

**回滚**：把网关路由改回 `iboot-admin` 即可；因两侧共享同一份表，切换期间写入的数据在回滚后仍可见。审批触发回滚需同时把 `NotificationPort` 适配器指回管理端内部调用（保留该实现一个发布周期）。
