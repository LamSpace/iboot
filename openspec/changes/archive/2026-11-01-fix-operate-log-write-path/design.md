# Design

## Context

`iboot-kernel` 的 `OperateLogRecorder` 用 `JdbcTemplate` 把 `LogAspect` 采集的记录写入硬编码的 `sys_operate_log`，数据源取自宿主服务。`@Log` 的写入是异步旁路：单线程池、队列满时由调用线程执行，异常被 `catch` 后只打一行 `warn`。

拆分后的 `iboot-file`、`iboot-notify`、`iboot-job` 各自指向独立 schema，其中没有 `sys_operate_log`。因此这三个服务共 30 个 `@Log` 端点（file 5 / notify 16 / job 9）的审计记录全部丢失。管理端的日志查询仍从 `iboot_admin.sys_operate_log` 读取，看不到这些记录。

见 `proposal.md` 的 Why；能力契约见 `specs/operation-logging/spec.md`。

## Goals / Non-Goals

**Goals:**

- 让拆分服务的操作审计重新写入管理端可查询的同一张表。
- 把写入目标变成显式配置，并让错误配置在启动阶段失败。
- 让写入失败从「静默 warn」变成可观测事件。
- 用一个独立、可回滚的变更完成，不阻塞 workflow 拆分。

**Non-Goals:**

- 不把日志表迁到尚未拆分的 monitor；不引入 MQ。
- 不改日志查询、清理、导出的接口与页面。
- 不补历史丢失的日志。

## Decisions

### D1：配置化的表名，而不是每服务建表或内部接口

选择 `iboot.logging.operate-log-table`（默认 `sys_operate_log`），拆分服务配置为 `iboot_admin.sys_operate_log`。

- **备选 A：每个服务 schema 建一张 `sys_operate_log`**。改动同样小，但管理端查询仍看不到其它服务的数据，等于把问题推迟到 monitor 拆分，且届时要再迁一次数据。否决。
- **备选 B：monitor 提供内部写入接口**。方向正确，但 monitor 尚未拆分，现在做会把一个未定义边界的服务变成所有服务的运行时依赖。否决。
- **备选 C：RocketMQ**。阶段 2.4 才引入，当前没有 broker。否决。

跨库写与现有的 `SysConfigReader` / `DictLabelLookup` / `NotifyUserReader` 属于同一类临时债务，退出条件统一登记在 `doc/23`。

### D2：表名在启动阶段校验

表名会拼进 SQL，不能作为绑定参数。配置值按 `^[A-Za-z0-9_]+(\.[A-Za-z0-9_]+)?$` 校验，不合法直接让启动失败。

- **备选：运行期字符串拼接并信任配置**。一个拼写错误会表现为「日志又丢了」，正是本次要消除的失败模式。否决。
- **备选：对标识符加反引号**。仍然无法区分「合法但不存在」与「合法且存在」，校验仍然必要。

### D3：失败可观测，但保持旁路

写入仍在异步线程执行，业务请求不受影响；失败时：

- 每个服务进程只打一次 `error`（`AtomicBoolean` 去重），避免数据库长时间不可用时刷屏；
- 累加 Micrometer 计数 `operate_log_write_failed_total{application="..."}`，Grafana 可按应用观察。

- **备选：失败时向调用方抛异常**。违反审计日志的旁路语义，会让一次日志故障升级为业务故障。否决。
- **备选：失败重试**。单线程队列会积压，且重试会放大数据库压力；审计是尽力而为，不做重试。

### D4：启动时做一次轻量可达性检查，但不阻断启动

启动完成后尝试 `SELECT 1 FROM <table> LIMIT 1`，失败只打一次 `error`，不阻止服务启动。使用 `LIMIT 1` 而不是 `LIMIT 0`：Druid 的 WallFilter 会拒绝 `LIMIT 0`，把它误判为探测性 SQL。

理由：配置合法但表不存在（例如生产授权漏配）是本次要防的第二个静默失败；启动期暴露比等第一次业务操作更早。不阻断启动是因为审计不可用不应让业务服务起不来。

## Risks / Trade-offs

| 风险 | 缓解 |
|---|---|
| 跨库写需要生产数据库授权，漏配时日志仍会丢 | 启动可达性检查打 `error`；`doc/23` 的接入清单登记授权要求；冒烟用数据库断言覆盖 |
| 数据库长时间不可用时，单线程队列（容量 1000）被填满，`CallerRunsPolicy` 会阻塞业务线程 | 这是既有风险，本变更不改变执行器语义；在 `doc/23` 记录，后续可改为有界丢弃并计数 |
| 历史丢失的日志无法恢复 | 提案与文档显式记录，不承诺补数 |
| 各服务配置不一致（有的指共享表、有的用默认） | 在 `doc/23` §11.1 增加一条必做步骤；回归冒烟只覆盖 file，其余服务由启动日志检查 |

## Migration Plan

1. 合并内核改动与服务配置；重启 file/notify/job。
2. 对 file 执行一次上传与删除，确认 `iboot_admin.sys_operate_log` 新增两条记录。
3. 对 notify 与 job 各触发一个 `@Log` 端点，确认记录出现（人工检查，冒烟只固定 file）。
4. 执行全量回归。
5. 回滚：回退提交即可；若只回退配置，服务会回到「写入不存在的本地表」的旧行为，不会造成数据损坏，但会重新丢日志。

## Open Questions

无。monitor 拆分时的长期写入方案（内部接口或 MQ）留到那个变更，不影响本变更的规格、实现与任务拆分。
