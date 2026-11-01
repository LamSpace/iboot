# Design

## Context

动机与范围见 `proposal.md`。这里只记录现状中影响实现的约束。

`iboot-admin` 目前的监控域由五块拼成，它们的依赖形状并不相同：

| 组成 | 数据来源 | 写方 | 迁移难度 |
|---|---|---|---|
| 登录日志 | `sys_login_log` | `iboot-auth` 跨 schema INSERT | 写侧切换 |
| 操作日志 | `sys_operate_log` | 7 个服务的 `OperateLogRecorder` 异步 INSERT | 写侧切换 |
| 运行日志 | Elasticsearch（Logstash 写入） | Logstash | 只读，改配置即可 |
| 在线会话 | 共享 Redis `login_tokens:*` | `iboot-auth` | 只读 + 强退删键 |
| 资源检查 | Oshi / Redis / MinIO | 本进程 | 语义会变 |

两个日志表的过滤依赖 `@DataScope(deptAlias = "ol"|"ll")`，被过滤列在日志表自身，因此数据范围解析仍需向 admin 取 scope。展示字段（部门名）来自 `sys_user` / `sys_dept`，按既有拆分惯例跨 schema 只读。

## Goals / Non-Goals

**Goals**

- 19 条路径对外契约零变化，前端零改动。
- 审计表的所有者与读者合一；写入方只需改一行配置。
- 切换过程不丢审计记录，且失败可回滚到「路由指回 admin」。

**Non-Goals**

- 不改变审计写入的通信方式（仍是内核异步 JDBC INSERT）。
- 不引入按服务聚合的多实例资源视图。
- 不迁移 Spring Boot Admin 控制台、Druid 控制台、dashboard、statistics。
- 不收敛其它服务对 `sys_user` / `sys_config` / `sys_dict_data` 的跨库读取。

## Decisions

### D1 审计表由监控服务拥有，写入方跨 schema 写

**选择**：`sys_operate_log`、`sys_login_log` 归 `iboot_monitor`，其余服务把 `iboot.logging.operate-log-table` 指向 `iboot_monitor.sys_operate_log`。

**备选**：监控服务提供内部写入端点，内核改发 HTTP；或直接上 RocketMQ。

**理由**：内核的审计写入是「异步、尽力而为、失败不阻断」的旁路行为，改成 HTTP 会把一次本地 INSERT 变成一次网络往返，并引入超时、重试、批量等新问题，而它并不需要被通知方保证送达。RocketMQ 是 §2.4 规划的终态，但 MQ 尚未引入，把它和一次结构拆分捆在一起会让回滚面扩大。跨 schema 写在 file/notify/job 的只读路径上已有先例，这次只是把方向从「读别人的表」扩展为「写别人的表」，并明确所有者。

**代价**：`iboot_monitor` 的 schema 会被 7 个服务写入，数据库账号权限面扩大。这一点在 D7 中约束。

### D2 表结构由本服务 Flyway 单点定义

`V1__initial_schema.sql` 只建两张表，DDL 由 admin 的 `V1`（基础列）+ `V9`（`dept_id`）+ `V12`（`deleted`）合并而成，不引入 admin 的其它表与菜单种子。

**理由**：与 `iboot_workflow` 的做法一致——新服务的 schema 版本从 V1 开始自成体系，不继承源服务的迁移历史。菜单与权限种子留在 admin，随 `iboot-system` 迁移。

### D3 停机窗口内全量复制，不做双写

**选择**：停全部服务 → 复制两张表 → 校验行数与 `max(id)` → 改配置 → 启动 monitor → 启动其余服务。

**备选**：双写过渡，先写两边再切读。

**理由**：审计表是只追加的日志表，双写期间两台写入会让「切换后哪张表是权威」变得模糊，而停机窗口只有几分钟。前四次拆分都用停机迁移，运维流程已经验证过；双写会引入新的失败模式却没有对应收益。

**约束**：窗口内 MUST 保证所有写入方都已停止，否则窗口内产生的记录会落在旧 schema 且不再被复制（复制脚本在窗口内只跑一次）。

### D4 自监控组件不搬迁

`MonitorFilter`、`MonitoringInterceptor`、`BusinessMetricsService`、`DruidMetricsBinder`、`CustomHealthIndicator` 名字里带 monitor，但它们是**每个服务对自身**的埋点：HTTP 耗时、业务指标、连接池指标、健康检查。搬进 `iboot-monitor` 会让 admin 及其它服务丢失这些指标，Grafana 面板会在无人察觉的情况下变空。

**处置**：留在原服务。`iboot-monitor` 只实现「读取」侧。两者在 `doc/11` 中用一节写清区别，避免下次再混。

### D5 服务资源检查的语义收敛为「本进程」

拆分后 `/api/monitor/server` 的 Oshi 数据来自 `iboot-monitor` 自身。这不是缺陷，而是拆分后的正确语义：一个进程无法报告另一个进程的内存与线程。

**处置**：接受现状并在响应与页面提示中表明它是监控服务的运行环境；按服务聚合的视图（Prometheus 查询）另立变更。**不**在本变更内修改响应结构，否则契约快照与前端都要跟着动，等价性验证失去意义。

### D6 dashboard / statistics 留在 admin

两者依赖用户、角色、部门、岗位、菜单与日志五个域，属于跨域聚合。若随监控服务迁走，监控服务会反向读系统域的表，扇入立刻失控。

**处置**：留在 admin，把其中对日志的访问改成跨库读 `iboot_monitor.sys_operate_log` / `sys_login_log`，**登记为临时债务**，退出条件为 `iboot-system` 拆分。

### D7 数据库权限

`iboot_monitor` 的账号需要：本 schema 全量权限、对 `iboot_admin.sys_user` / `sys_dept` / `sys_config` / `sys_dict_data` 的 SELECT，以及对自身审计表的基线权限。反过来，其余 8 个服务的账号需要对 `iboot_monitor.sys_operate_log` 的 INSERT，`iboot-auth` 另外需要对 `sys_login_log` 的 INSERT。

**理由**：跨 schema 写入是有意的设计，必须在开发环境（单账号）与生产环境（分账号）都成立。开发环境沿用 root，生产环境的授权清单写进 `doc/23`。

### D8 数据范围解析沿用既有做法

监控服务不复制 `UserDataScopeResolver`，改为带调用者令牌调用 admin `GET /api/auth/scope`，本地缓存 60 秒，失败关闭——与 `iboot-file`、`iboot-job` 相同。**理由**：scope 属于系统域，随 `iboot-system` 迁移；现在复制一份会在 system 拆分时多一处删除点。

### D9 运行日志继续走 Elasticsearch

Logstash 直接写 ES，admin 只做查询。监控服务复制 `ElasticsearchConfig` 与索引名配置，不改变采集链路。

**代价**：ES 地址与凭据在新服务中重复一份。可接受，因为采集链路不动，风险最小。

### D10 在线会话强退保持共享 Redis 语义

强退删除 `login_tokens:<jti>`（以及用户的 `online_token:<userId>`），键前缀与序列化由内核提供。**不**改为调用 auth 的内部端点——会话撤销本就是各方共用共享存储的既有契约，auth 与资源服务都依赖它；改成 HTTP 会让强退在 auth 不可用时失效，反而退化。

## Risks / Trade-offs

- **[切换窗口内审计记录分裂到两个 schema]** → 窗口内停止全部写入方；迁移脚本在复制后比对行数与 `max(id)`；切换完成后断言旧表行数不再增长。
- **[漏改某个服务的 `operate-log-table`，静默丢审计]** → 启动校验只保证表存在，不保证指向正确。用回归断言兜底：冒烟分别经 file 与 monitor 触发一次 `@Log` 操作，再从 `iboot_monitor.sys_operate_log` 查询；同时用 `grep` 检查全仓 `operate-log-table` 配置。
- **[服务资源页面被误解为全系统资源]** → 页面文案与文档同步改为「监控服务运行环境」；聚合视图另立变更。
- **[强退与 auth 的 Redis 键空间耦合]** → 键前缀与序列化走内核常量，回归中验证强退后令牌立即失效。
- **[dashboard 跨库读日志表形成新债务]** → 在 `doc/23` 登记退出条件；`iboot-system` 拆分时改为调用监控服务。
- **[第 9 个 JVM 的内存压力]** → 受 R4 约束，本地按需启动；`dev-init.sh` 支持只拉起指定服务。

## Migration Plan

1. **建结构**：仓库内新增模块与 `iboot_monitor` schema 的建库语句；服务以 Flyway 建两张空表启动一次。
2. **迁移数据**（停机窗口）：
   - 停止全部后端服务与前端；
   - 执行 `scripts/migrate/monitor_to_iboot_monitor.sql`；
   - 校验两表行数与 `max(id)`；
   - 修改 7 个服务的 `iboot.logging.operate-log-table` 与 auth 的登录日志 INSERT；
   - 启动 `iboot-monitor`，健康后启动其余服务；
   - 执行 `scripts/migrate/monitor_final_check.sql` 确认无新记录落回旧 schema。
3. **验收**：契约快照、路径守恒、路由覆盖、冒烟、权限矩阵、只读等价性、链路追踪、故障隔离。
4. **回滚**（验收未通过时）：
   - 网关路由改回 admin；
   - 7 个服务的 `operate-log-table` 与 auth 的 INSERT 改回 `iboot_admin`；
   - 重启。窗口内新增的审计记录需要反向复制，脚本 `monitor_back_to_iboot_admin.sql` 提供该能力。

## Open Questions

- 生产环境按账号分权时，审计表的 INSERT 授权由谁维护（DBA 流程还是应用初始化脚本）。
- Spring Boot Admin 控制台与 Druid 控制台在 `iboot-system` 拆分后归属哪个服务。
