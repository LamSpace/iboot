# Proposal

## Why

`file` / `notify` / `job` 拆分后，控制器上的 `@Log` 仍在，但内核的 `OperateLogRecorder` 把记录写进**各服务自己的数据源**里的 `sys_operate_log`。这三个服务的 schema 中没有这张表，插入异常被 `catch` 后只打一行 `warn`，请求照常成功。结果是 **30 个带 `@Log` 的端点（file 5 / notify 16 / job 9）操作审计静默丢失**，管理端的操作日志页只显示 admin 自己的记录。

`add-job-service` 的归档材料已经把这个洞记录为「拆分 `iboot-monitor` 之前必须定下来的前置决策」。现在提前解决：`workflow` 即将拆出，若不先修，缺口会再多 3 个端点；而修法本身与拆分顺序无关，可以独立验证、独立回滚。

## What Changes

- **内核写入目标可配置**：`OperateLogRecorder` 的目标表由 `iboot.logging.operate-log-table` 决定，默认 `sys_operate_log`；配置值按 `schema.table` 白名单校验，非法值在启动阶段失败，而不是运行期静默写错。
- **拆分服务跨库写审计表**：`iboot-file` / `iboot-notify` / `iboot-job` 配置为 `iboot_admin.sys_operate_log`。这是临时约定，与现有的跨库只读（`SysConfigReader` / `DictLabelLookup` / `NotifyUserReader`）同一退出条件：`iboot-system` 拆分或 `iboot-monitor` 接管日志写入时移除。
- **失败可观测**：保持旁路语义（写失败不影响业务请求），但把 `warn` 改为 `error` 并累加指标；同一服务只告警一次，避免刷屏。
- **回归与文档**：冒烟增加一条「`@Log` 端点写入审计表」的数据库断言；`doc/23` §11.1 的接入步骤与 §11.2 的隐式契约各增加「操作日志写入目标」。
- **不做**：不把日志表迁到 monitor；不改日志查询接口；不引入 MQ。monitor 拆分时再把写入切换为内部接口或 MQ。

## Capabilities

### New Capabilities

- `operation-logging`: 跨服务的操作审计日志写入与可见性——拆分服务把 `@Log` 记录写入统一审计表，写入失败可观测且不阻断业务，管理端日志查询能看到全部服务的记录。

### Modified Capabilities

无。现有能力的外部行为不变。

## Impact

**代码**

- `iboot-kernel`：`OperateLogRecorder`、`KernelLoggingAutoConfiguration` 增加配置属性与校验；对应单测。
- `iboot-file` / `iboot-notify` / `iboot-job`：`application.yml` 各增加一行写入目标配置。
- `iboot-admin`：保持默认值，不改代码。
- `iboot-ai`：虽然当前没有 `@Log`，也指向共享审计表；否则内核新增的启动可达性检查会在 AI 启动时误报「目标表不可达」。

**接口**

- 无外部接口变化，前端零改动。

**依赖与配置**

- 跨 schema 写要求数据库账号对 `iboot_admin.sys_operate_log` 有 `INSERT` 权限；本地 root 账号已满足，生产环境需登记授权。
- 不新增中间件、不新增 JVM。

**数据**

- 不迁表、不建表、不改表结构。历史丢失的日志无法补回，本变更只保证从现在起不再丢。

**回归资产**

- `smoke.sh`：在既有的文件上传/删除链路后断言 `sys_operate_log` 出现对应记录（经 `docker exec iboot-mysql` 查询）。
- `doc/23` §11.1 / §11.2 更新。

**已知遗留**

- 跨库写是临时约定，退出条件与其它跨库依赖一起由 `iboot-system` / `iboot-monitor` 收敛；不随本变更解决。
