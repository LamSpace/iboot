# Design

## Context

见 `proposal.md - Why`。影响实现方式的现状事实（均已核对到源码与脚本）：

- 管理端旧实现共 **88 个 Java 文件、约 1.9 万行**，另有 6 个 mapper XML 与 19 个测试类；除通知域自身外，管理端没有其它模块引用这些服务。
- 必须保留的契约件只有四类：`NotificationPort`、`NotificationCommand`、`HttpNotificationPort`、`NotificationEventType`（审批提交与端口适配器在用）。
- 六张表目前由管理端 Flyway 创建（`V1`、`V18`、`V20` 等）；`iboot-admin` 已推进到 `V27`。
- `iboot-notify` 当前数据源直连 `iboot_admin`，未启用 Flyway；其 mapper XML 中表名**不带库名**，切换数据源后会自动解析到 `iboot_notify`；跨 schema 只读的三处（`SysConfigReader`、`DictLabelLookup`、`NotifyUserReader`）已显式限定 `iboot_admin.`。
- 两个维护任务由管理端 Quartz 驱动：`V16` 插入 `contentIndexReconcileTask.reconcile`（每 5 分钟），`V18` 插入 `notificationRetryTask.retry`（每分钟）。
- 数据搬迁有现成先例：`scripts/migrate/sys_file_to_iboot_file.sql`（建库幂等、显式 id 复制、不删源表、附校验查询）。
- 回归装置：`migrations.txt` 五条当前为 `transitional`；`golden/admin.json` 仍含这五组路径。

## Goals / Non-Goals

**Goals**

- 删除过渡期双份实现，管理端不再持有任何消息/公告/通知/推送/内容检索代码。
- 六张表由 `iboot-notify` 独占，DDL 由本服务 Flyway 管理。
- 维护任务只有一个执行者，且不依赖管理端调度。
- 外部契约零变化：路径、响应、权限码、通知语义不变。

**Non-Goals**

- 不改造 `iboot-system` 的用户/配置/字典读取（仍走跨 schema 只读，退出条件不变）。
- 不引入 Quartz 或 ElasticJob 到 notify（统一调度留给 `iboot-job` 拆分）。
- 不删除 `iboot_admin` 中的六张源表（保留一个周期）。
- 不改前端，不改 `/internal/notifications` 契约。

## Decisions

### D1. 数据搬迁：复制不删除，显式 id，停写窗口内执行

新增 `scripts/migrate/sys_*_to_iboot_notify.sql`，沿用 `sys_file` 的做法：

- `CREATE DATABASE IF NOT EXISTS iboot_notify`（幂等）；
- 六张表按依赖顺序复制，显式携带 `id`，让 `AUTO_INCREMENT` 推到 `max(id)+1`；
- 脚本末尾附行数与 `max(id)` 对照查询，供执行人贴入变更记录；
- 只复制、不删除源表。

**顺序**：停全部后端 → 建库 → 单独启动 notify（Flyway 建六张表）→ 执行复制脚本 → 校验行数 → 启动其余服务。Flyway 必须是 DDL 的唯一来源，脚本不建表，避免结构漂移。

**备选（否决）**：让复制脚本自己 `CREATE TABLE`——与 Flyway 形成两份 DDL，后续任何加列都要改两处。

### D2. 源表保留一个发布周期

`iboot_admin` 里的六张表在本变更中**不删除**，与管理端 `sys_file` 的处理一致：Flyway 历史不可改写，删表需要额外迁移；保留也不影响新服务（没人再读）。清理另起条目。

**收益**：数据层回退只需把 notify 的数据源指回 `iboot_admin`，不依赖备份恢复。

### D3. 维护任务改为进程内调度

`notificationRetryTask` 与 `contentIndexReconcileTask` 迁入 notify：

- 用 `@Scheduled(fixedDelay)` 驱动，间隔沿用 `1 分钟` / `5 分钟`；
- 用 Redis 锁（`SET key value NX EX`）保证多实例下只有一个执行者，key 为 `iboot:notify:task:retry` / `iboot:notify:task:reconcile`，TTL 略大于单轮预计耗时；
- 删除 `sys_job` 两行——先删行再启用新调度，避免双跑。

**刻意的用户可见变化**：任务管理页不再显示这两条内部维护任务。它们不是用户可配置的业务任务，文档需要说明。

**备选（否决）**：保留 `sys_job` 行、把 `invoke_target` 改成 HTTP 调用——为一个尚不存在的 job 服务提前造通用 Job 执行器。

### D4. 删除边界以「契约 vs 实现」划分

删除：消息/公告/模板/投递的接口、应用、领域、mapper、XML、推送、检索、渠道实现及其测试。

保留（管理端仍需要）：

- `application/service/notification/NotificationPort`、`NotificationCommand`；
- `infrastructure/notification/HttpNotificationPort` 及其测试；
- `domain/notification/model/NotificationEventType`。

同时清理本次删除造成的孤儿：`BusinessMetricsService` 中的推送指标方法与字段（`push_connection_active` 等已由 notify 的 `PushMetrics` 上报），以及 `application.yml` 中 `iboot.push.*`、`iboot.notification.*` 的渠道配置（只留 `base-url` 与 `internal-token`）。

### D5. 回滚策略：数据可回退，代码走 revert

- 代码回滚：`git revert` 本变更的合并提交。
- 数据回滚：源表仍在 `iboot_admin`，把 notify 的数据源改回 `iboot_admin` 即可；若在搬迁后产生过新数据，需要反向复制（脚本注释中说明）。

**为什么可以接受**：项目尚未对外发布，没有需要保护的在线旧版本；这与 `remove-file-from-admin` 同一取舍，已记录在 `doc/23`。

### D6. 回归登记与基线刷新

- `migrations.txt` 五条改为 `done`——路径守恒会强制校验「管理端确实不再有这些路径」；
- `golden/admin.json` 用 `regress.sh --refresh` 重新生成，需人工确认差异仅为这五组路径；
- 冒烟与权限矩阵用例不变（全部经网关）。

## Risks / Trade-offs

- **漏搬一张表或漏搬数据** → 脚本逐表校验行数与 `max(id)`；搬迁后立即跑 notify 只读等价性（对比源库与服务响应）。
- **搬迁窗口内双写** → 先停全部后端再执行；脚本注释声明只允许停写窗口执行。
- **任务双跑**（管理端 sys_job 与 notify 调度同时存在）→ `sys_job` 行删除编排在启用新调度之前；Redis 锁兜底。
- **删除后仍有隐藏引用** → 编译器强制；`HttpNotificationPortTest` 与审批单测保留并必须通过。
- **任务列表少两条记录被误认为功能缺失** → 文档与变更记录显式说明。
- **ES 对账任务在迁移窗口内停止 5 分钟** → 索引最终一致由下次对账补齐，可接受。

## Migration Plan

1. 停全部后端（`scripts/dev-init.sh --stop`）。
2. 建库：`dev-init.sh` 增加 `iboot_notify` 建库语句（与 `iboot_file` 同格式）。
3. 单独启动 `iboot-notify`：Flyway 在 `iboot_notify` 建六张表。
4. 执行 `scripts/migrate/sys_*_to_iboot_notify.sql`，贴出两侧行数与 `max(id)` 校验结果。
5. 删除 `sys_job` 两行内部任务；启动其余服务；确认 notify 的两项调度已运行（日志可见）。
6. 跑 `regress.sh --refresh`（人工确认差异合理）后再跑 `regress.sh` 全量。
7. 删除管理端旧实现与孤儿配置，重新构建、启动、再跑全量回归。

**回滚**：`git revert` + 数据源指回 `iboot_admin`。
