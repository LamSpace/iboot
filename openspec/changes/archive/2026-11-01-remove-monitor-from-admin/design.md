# Design

## Context

`add-monitor-service` 交付后，19 条监控路径经网关指向 `iboot-monitor`，审计表写入目标已切到 `iboot_monitor`。管理端仍保留旧实现，且其中两处**不是纯删除**：

- `DashboardApplicationService` 依赖 `LoginLogApplicationService` / `OperateLogApplicationService` 取最近 5 条与总数；
- `StatisticsApplicationService` 直接使用 `LoginLogMapper` / `OperateLogMapper` 的聚合查询（按日期、状态、模块、小时）。

这两处决定了「日志数据访问层」不能整体删除，只能收敛。动机见 `proposal.md`。

## Goals / Non-Goals

**Goals**

- 管理端不再有日志/会话/资源检查的控制器与业务实现，`/api/log` 等 19 条路径从基线中消失。
- 管理端不再加载 Elasticsearch 客户端与 OSHI。
- dashboard / statistics 继续可用，且读到的是**当前**数据而不是一张停止写入的旧表。

**Non-Goals**

- 不删除 `iboot_admin` 中的两张日志表（保留一个发布周期）。
- 不改变 dashboard / statistics 的响应结构。
- 不处理 Spring Boot Admin 与 Druid 控制台的归属。
- 不处理 `iboot-system` 拆分。

## Decisions

### D1：日志数据访问层留在管理端，但改读 `iboot_monitor`

**选择**：`LoginLogMapper` / `OperateLogMapper` 与 XML、PO 留在 `iboot-admin`，所有表名改为 `iboot_monitor.sys_login_log` / `iboot_monitor.sys_operate_log`。

**备选**：让 dashboard 与 statistics 调监控服务的 HTTP 接口取数。

**理由**：dashboard 与 statistics 是跨域聚合，改为跨服务调用会把一次页面加载拆成 6~8 次远程调用，并让 dashboard 在监控服务不可用时整体失败（而这些统计与监控域并不相关）。跨 schema 只读是本项目已登记的过渡手段（file / notify / job 都用过），代价明确、退出条件清晰。

**代价**：管理端与监控服务共享同一批表结构，schema 变更需要两处同步读取代码。已登记为临时债务，退出条件为 `iboot-system` 拆分。

### D2：Dashboard 直接使用 Mapper，不再为它保留应用服务

**选择**：删除两个日志应用服务，`DashboardApplicationService` 直接调用 Mapper 的 `count()` 与 `selectPage(0, 5)`。

**备选**：保留两个应用服务，只删控制器。

**理由**：它们只被 dashboard 使用，且只做「取总数 + 取前 5 条」。保留两个类意味着保留它们对 `LoginLog` 领域模型与仓储接口的依赖，进而要保留 `LoginLogRepository`、`OperateLogRepository` 与两个实现——一共 6 个文件，只为一个页面服务。统计服务已经是直接使用 Mapper 的写法，dashboard 跟随它更一致。

**代价**：dashboard 的 DTO 转换从领域模型改为 PO。转换逻辑本就逐字段复制，改动集中在两个私有方法。

### D3：直接删除依赖，不保留配置开关

**选择**：删除 `spring-boot-starter-data-elasticsearch`、`oshi-core` 与 `spring.elasticsearch` 配置块。

**理由**：与 `remove-*-from-admin` 的既有做法一致——用开关保留旧实现会让两套代码与两套测试继续存在，正是本变更要消除的成本。项目尚未发布，不需要在线回退开关；回滚方式是 `git revert`。

**验证**：`mvn -pl iboot-admin dependency:tree` 中两者消失；启动日志不再出现 `ElasticsearchClient` 或 `oshi.SystemInfo`。

### D4：两张日志表保留一个发布周期

**选择**：`iboot_admin.sys_login_log` 与 `sys_operate_log` 不删除，与审批 33 张表、任务、通知、文件表的处理方式一致。

**理由**：回滚需要它们；数据核对（`monitor_final_check.sql`）也依赖它们仍然存在。真正的清理另起变更。

## Risks / Trade-offs

- **[管理端读的是另一个服务的表，schema 变更会静默取不到数据]** → 两条查询都在 dashboard/statistics 的回归用例里（冒烟有仪表板与统计页断言）；`doc/07` 的 schema 归属表登记了这一依赖。
- **[删除后前端仍在调用旧路径]** → 前端只访问网关，路径不变；契约基线与路径守恒会在刷新基线时显示 admin 少 19 条、monitor 保持 19 条，总数不变。
- **[统计与仪表板在监控服务故障时仍可用]** → 这是 D1 的收益：它们只依赖数据库，不依赖监控服务进程。故障隔离用例中「停 monitor 时用户接口正常」应扩展为「仪表板与统计正常」。
- **[`migrations.txt` 误标 done 会让路径守恒失去意义]** → 只在 admin 的 19 条路径确实消失后才改；脚本自身会校验目标服务存在该前缀且源服务不再出现。

## Migration Plan

1. 先改 `DashboardApplicationService` 与两个 Mapper XML 的表名，索引与查询仍走旧表 → 启动验证 dashboard 与 statistics 正常（此时数据在 `iboot_monitor`，管理端应已能读到）。
2. 删除控制器、应用服务、DTO、仓储、`ElasticsearchConfig` 与两个依赖。
3. 刷新 `golden/admin.json`，把 5 条 `transitional` 改为 `done`。
4. `mvn -pl iboot-admin clean package`（必须 `clean`：删除的文件可能残留在 `target/classes` 中，此前拆 workflow 时踩过）。
5. 全量回归 + 冒烟 + 权限矩阵 + 故障隔离（停 monitor，验证日志接口降级而仪表板/统计正常）。

**回滚**：`git revert` 本提交 → 8 个服务的 `operate-log-table` 与认证服务的登录日志 INSERT 改回 `iboot_admin` → 执行 `scripts/migrate/monitor_back_to_iboot_admin.sql` → 重启。

## Open Questions

- 一个发布周期后删除 `iboot_admin` 的两张日志表，与审批 33 张表、任务/通知/文件表的清理是否可以合并为一个「遗留表清理」变更。
