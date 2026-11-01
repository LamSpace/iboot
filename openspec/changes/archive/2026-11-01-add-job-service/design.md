# Design

## Context

见 `proposal.md` 的 Why。设计需要额外知道的现状：

- `JobInvokeUtil` 通过 `SpringUtils.getBean(beanName)` 反射调用**本进程**的 Bean。这是调度器与业务代码之间唯一的耦合点，也决定了拆出后它能调度什么。
- `JobMapper` 有 4 处 `@DataScope`，`sys_job` 自 `V11` 起带 `dept_id`；`JobController` 的 10 个端点全部用 `hasAuthority` 校验功能权限。
- 备份执行依赖宿主环境：`ProcessCommandRunner` 经 `docker exec` 调用 `iboot-mysql` 容器内的 `mysqldump` / `mysql`，恢复演练写入独立库 `iboot_restore_drill`。
- 备份产物通过 `FileStorageStrategy` 写入当前生效的存储提供方（`sys.file.storageType`，位于 `iboot_admin.sys_config`）。
- `iboot-ai` 的 `AdminApiClient` 由 4 个工具与 `AdminOrgSnapshotClient` 共用；其中只有备份的两个方法与本次拆分相关。
- `iboot-ai` 的 classpath 上**没有** `spring-cloud-starter-loadbalancer`（已用 `dependency:tree` 核实）。
- 现有拆分已确立的做法见 `doc/23` §11：内核依赖、十个接入步骤、四个静默出错的隐式契约。

## Goals / Non-Goals

**Goals:**

- 让调度与备份拥有独立的生命周期与故障半径，且可独立扩缩到多实例。
- 建立项目里第一处「按服务名调用」的服务间调用约定，为后续 `monitor` / `workflow` 复用。
- 保持对外契约与前端零改动。

**Non-Goals:**

- 不做跨服务的任务注册与派发（其他服务如何把任务登记进本调度器）。当前三个服务的后台任务都用自身进程内的 `@Scheduled`，本变更不改变这一点。
- 不修复已拆服务 `@Log` 写入静默失败的问题，也不在本变更中统一 `file` / `notify` 调 admin 的静态地址。
- 不引入 ElasticJob 或任何替代调度框架。

## Decisions

### D1 服务边界：调度与备份放在同一个部署单元

**决策**：新建一个 `iboot-job` 同时承载定时任务与数据备份。

**理由**：备份是当前唯一的真实定时任务，它由 Quartz 驱动、执行目标在本进程内。若只拆调度，服务里只剩三个示例任务，等于拆出一个空壳；若只拆备份，调度器留在单体里继续拥有一个无处调用的执行链。两者合并后，`iboot-job` 的 Bean 空间与任务定义自洽。

**代价（显式承认）**：这违反 `ROADMAP` §2.2 第 4 条「不破坏 DDD 边界」的精神——"调度域"与"数据保护域"是两个限界上下文。选择合并的首要理由是内存红线（R4）：本项目已运行 7 个 JVM，再拆一个是 8 个，拆两个是 9 个。若备份将来长出增量备份 / PITR 等独立能力，再把它剥离成独立服务，届时它已有足够的业务体量支撑独立部署。

### D2 独立 schema：`iboot_job`，16 张表

**决策**：`QRTZ_*`（11 张）、`sys_job`、`sys_job_log`、`db_backup_record`、`db_restore_record` 全部归入 `iboot_job`，由本服务 Flyway `V1__initial_schema.sql` 管理。

**理由**：Quartz 的集群锁、`QRTZ_LOCKS` 与 `QRTZ_SCHEDULER_STATE` 必须与任务表在同一事务边界内，留在别的 schema 只是形式上的独立。

**替代方案**：过渡期继续共享 `iboot_admin`（`notify` 第一阶段的做法）。否决原因是 Quartz 集群表与任务表强绑定，拆两段反而增加一次数据搬迁。

**代价**：本变更是**结构先行**——`iboot_job` 建表并启用，管理端旧实现继续读写 `iboot_admin` 的同名表。两个实现同时存在但它们的数据源不同，因此**任务列表在两侧会不一致**。这一点必须在过渡期文档里写清楚：路由决定用户看到哪一份，`remove-job-from-admin` 负责把数据搬迁过去并消除分叉。

### D3 数据权限：复用 `iboot-file` 的 scope 客户端模式

**决策**：新服务带调用者令牌请求 admin 的 `GET /api/auth/scope`，本地缓存（默认 60 秒），失败关闭，并为本服务提供 `DataScopeInterceptor` + `DataScopeSqlBuilder`。

**理由**：`sys_job` 的 `dept_id` 与 `@DataScope(deptAlias = "j")` 已经存在，忽略它会造成越权可见。scope 端点属于系统域，随 `iboot-system` 迁移，现在不能把它复制进 job。

**替代方案**：让 job 直接读 `iboot_admin.sys_user` 自己算范围——否决，等于把数据权限语义实现三份。

### D4 Quartz 集群：`isClustered: true` + `instanceId: AUTO`

**决策**：拆分时一并开启集群模式。

**理由**：`isClustered: false` 在同一 JVM 多实例或多个进程下会重复触发。`ROADMAP` §2.4 已把它列为拆分后的必做项，且 `iboot-job` 是唯一持有调度器的进程，此时改动的风险面最小。

**代价**：Quartz 集群表首次真实使用，需要两实例验证；`QRTZ_LOCKS` 与数据库时钟偏移会影响触发精度，本地验证时至少要跑过一次双实例。

### D5 服务间调用：Nacos 服务发现，直连目标服务

**决策**：`iboot-ai` 增加 `spring-cloud-starter-loadbalancer` 与 `@LoadBalanced RestClient.Builder`，用服务名作 host（`http://iboot-admin`、`http://iboot-job`）。`iboot-job` 调 admin 的 scope 同样按服务名。

**理由**：

- 网关的职责是"外部如何到达服务"，路由表不应承载"服务之间如何互相调用"。若工具调用回穿网关，改一条路由就会静默改变工具的目标。
- 边缘的 CORS、全局过滤器与 Sentinel 限流不应作用于内部调用；工具调用会与用户自己的请求争抢同一个前缀的配额。
- `ROADMAP` §2.3 已明确"服务间先走声明式 HTTP + Nacos 服务发现"。
- `scripts/dev-init.sh` 默认拉起 Nacos，本地不额外增加前置依赖。

**被否决的方案**：

- **回穿网关（`http://localhost:8088`）**：改动最小（一行配置），但让 AI 依赖"把流量路由到自己"的组件，且每次工具调用多一跳并计入边缘限流。简单在配置，错在职责。
- **静态地址（`http://localhost:8085`）**：与 `file` / `notify` 现状一致，但服务名与端口会随部署变化，且无法利用多实例负载。

**已知不一致**：`iboot-file` / `iboot-notify` 调 admin 的 scope 仍是静态地址。本变更不动它们（避免扩散到两个已完成的服务），但把"服务间地址统一为服务名"记入后续收敛项。

### D6 网关路由：路径级，而非前缀级

**决策**：新增两条具体路由 `path=/api/monitor/backup/**` 与 `path=/api/job/**`，排在 admin 兜底之前；`routes.txt` 与 `migrations.txt` 同样按这两条路径前缀登记。

**理由**：`/api/monitor/**` 的其余部分仍属 admin（monitor 未拆），不能登记 `/api/monitor`，否则 `path-conservation` 永远无法达到 `done`——这正是 `extract-auth-service` 时 `/api/auth/scope` 留下的教训（`doc/23` §12.1）。

### D7 AI 客户端：保留 `AdminApiClient`，新增 `JobApiClient`

**决策**：新增一个只有 `backupSummary` / `triggerBackup` 两个方法的 `JobApiClient`；`QueryBackupSummaryTool` 与 `TriggerBackupTool` 改依赖它。`AdminApiClient` 的名字与职责不变。

**替代方案**：把 `AdminApiClient` 改名为 `BackendApiClient` 并塞两个 base URL。否决原因：会波及 4 个工具、`AdminOrgSnapshotClient` 与它们的测试，而收益只是少一个类。

**测试影响**：为零。工具测试 mock 的是 `AdminApiClient` / 注入的客户端本身，`AdminOrgSnapshotClientTest` 自建 `RestClient.builder()`，都不经过 Spring Bean。

### D8 备份存储：复制 `FileStorageStrategy` 实现，而不是调用文件服务

**决策**：`iboot-job` 自带 `Local` / `MinIO` / `OSS` 三个存储实现与 `FileStorageConfig`，存储类型读 `iboot_admin.sys_config` 的 `sys.file.storageType`（跨 schema 只读限定名）。

**理由**：备份对象不是用户文件——它不应产生 `sys_file` 记录、不应受文件服务的数据权限约束，也不应把加密后的二进制流经第三个服务中转。备份记录已经自带存储类型与路径。

**代价**：`FileStorageStrategy` 的实现会在 `iboot-admin`、`iboot-file`、`iboot-job` 各存一份。这是**已知重复**，收敛它需要把存储 SPI 提升进 `iboot-kernel`，但那会牵动两个已完成的服务，超出本变更范围；记入后续收敛项。

### D9 备份执行的前置条件在启动时检查

**决策**：备份启用时，启动阶段校验三项——密钥、存储提供方可用、`mysqldump` 调用链可达（容器名与客户端路径可配置）。任一不满足则启动失败并指出具体原因。

**理由**：现有实现在**执行时**才失败，表现为一条失败的备份记录；而备份的价值恰恰在"出事时它真的在"，把问题推迟到那一天是最差的时点。

## Risks / Trade-offs

- **过渡期数据分叉**（D2） → `doc/23` 明确写出"两侧数据源不同、列表会不一致"，`remove-job-from-admin` 用行数 + `max(id)` 双向核对后搬迁；过渡期内禁止在管理端侧新建任务（路由已指向新服务，正常操作不会产生新行）。
- **Quartz 集群首次启用**（D4） → 双实例验收作为退出条件之一：同一 cron 只产生一条执行日志；强杀一个实例后任务继续触发。
  **实测到的语义边界**：节点被强杀时，Quartz 会把该节点执行中的触发器恢复并重新执行一次，因此在故障瞬间可能出现两条同秒执行记录——这是**至少一次**语义，不是配置错误。任务实现必须幂等；备份任务本身的并发唯一性约束（同一时刻最多一条运行中记录）正好兜住这种情况。
- **`iboot-job` 依赖 admin 的 scope** → 失败关闭，因此 admin 不可用时任务**手动**接口会拒绝（符合预期）；但定时执行不经过 scope，admin 停机不影响已排定任务——这一点需要在验收里单独证明，否则"独立启停"的承诺是空的。
- **`docker exec` 依赖宿主机容器名** → 容器名与 mysql 客户端路径配置化（沿用 `IBOOT_BACKUP_DOCKER_CONTAINER`），启动检查覆盖；阶段 3 容器化后这条调用链会失效，届时改为直连或 job 内自带客户端，属于阶段 3 的前置事项。
- **内存 +1 JVM**（R4） → 本地只跑正在改的服务；job 服务堆内存上限按 512MB 配置。
- **操作日志继续静默失败** → 本变更不修复；因为 `iboot-monitor` 是该数据的查询方，已把"操作日志归属"列为拆分 monitor 的前置决策项，写进 proposal 的已知遗留。

## Migration Plan

**本变更（结构先行）**

1. 建模块、建 `iboot_job` schema、迁代码与测试，管理端旧实现不动。
2. 网关加两条路由，`migrations.txt` 登记为 `transitional`，`routes.txt` 两行。
3. 全量回归通过后即可合并；此时用户可以经网关使用新服务，管理端保留旧实现作为回退点。

**回滚**：把两条网关路由改回指向 `iboot-admin` 即可，业务代码与数据库无需改动（`iboot_job` 与 `iboot_admin` 各自持有副本）。

**下一个变更 `remove-job-from-admin`**：搬迁数据（4 张表 + `QRTZ_*`）、删除管理端旧实现、`migrations.txt` 改 `done`、`iboot_admin` 的同名表保留一个发布周期。此后回滚方式变为 `git revert` + 数据源指回 `iboot_admin`。
