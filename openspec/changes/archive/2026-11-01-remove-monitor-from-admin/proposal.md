# Proposal

## Why

`add-monitor-service` 已交付后，日志、会话与资源检查的全部外部流量经网关指向 `iboot-monitor`，审计表的写入目标也已切到 `iboot_monitor`。但管理端仍保留整套旧实现（5 个控制器、7 个应用服务、10 个 DTO）并继续加载只服务于它们的 Elasticsearch 与 OSHI 依赖。这是绞杀者过渡期的临时状态：

- 同一份能力存在两套代码与两套测试，相关改动要改两处；
- 管理端的日志页读的是 `iboot_admin` 中**已停止写入**的表，页面看起来正常但永远是旧数据——比明确的错误更难被发现；
- `/api/log`、`/api/online`、`/api/monitor/{server,redis,minio}` 的 19 条路径在契约基线里出现两次（管理端与监控服务各一份），路径守恒只能靠 `transitional` 登记解释；
- 管理端为两个已迁走的服务继续携带 ES 客户端与 OSHI，启动面与内存都在为无人使用的代码付费。

项目尚未对外发布，不存在需要保护的在线旧数据，收敛越早成本越低。删除模式与 `remove-notify-from-admin` / `remove-job-from-admin` / `remove-workflow-from-admin` 保持一致。

## What Changes

- **删除管理端旧实现**：5 个控制器、7 个应用服务、7 个只服务于旧实现的 DTO 与导出 VO、2 个仓储接口与实现、`ElasticsearchConfig`。
- **管理端移除只服务于旧实现的依赖**：`spring-boot-starter-data-elasticsearch`、`oshi-core`，以及 `spring.elasticsearch` 配置块。
- **保留日志的只读查询件**：`LoginLogMapper` / `OperateLogMapper` 与 XML、PO、`LoginLogResponse` / `OperateLogResponse` 继续留在管理端，因为 dashboard 与 statistics 仍在聚合日志数据；但这些查询的表名**改为跨 schema 读 `iboot_monitor.*`**，这是本变更引入的临时债务，退出条件为 `iboot-system` 拆分时改为调用监控服务。
- **`DashboardApplicationService` 改为直接使用 Mapper**：原先依赖的两个日志应用服务被删除，dashboard 只做 `count` 与最近 5 条的读取，直接走 Mapper 可避免为它保留一套无人使用的应用服务。
- **回归登记收敛**：`migrations.txt` 的 5 条监控路径从 `transitional` 改为 `done`；刷新 `golden/admin.json`（预期消失 19 条路径）。
- **保留** `iboot_admin` 中的 `sys_login_log` 与 `sys_operate_log` 两张表一个发布周期，不删除；它们是回滚与数据核对的对象。
- **文档**：`ROADMAP` 状态表、`doc/23` §11.10、`doc/07` schema 归属表、`README` 项目结构。

**对外契约不变**：`/api/**` 路径、方法、响应结构、权限码均无变化，前端零改动。

**BREAKING（仅内部）**：回滚方式从「改回网关路由」变为 `git revert` + 8 处审计写入目标改回 `iboot_admin` + `monitor_back_to_iboot_admin.sql`。

## Capabilities

### New Capabilities

无。

### Modified Capabilities

无。本变更是代码归属与部署归属的收敛，不改变任何能力的外部行为；`ops-monitoring`、`operation-logging` 与 `auth-service` 的既有需求继续成立，由契约基线、路径守恒、冒烟与权限矩阵强制。

## Impact

**代码**

- `iboot-admin` 删除 5 个控制器、7 个应用服务、7 个 DTO/VO、2 个仓储接口与实现、1 个配置类；`DashboardApplicationService` 改为使用 Mapper。
- `iboot-admin` 的 `LoginLogMapper.xml` / `OperateLogMapper.xml` 中所有表名改为 `iboot_monitor.` 限定。

**接口**

- 对外路径零变化，前端零改动。
- 网关路由不变；`/api/log`、`/api/online`、`/api/monitor/{server,redis,minio}` 不再有管理端回退点。

**依赖与配置**

- `iboot-admin` 移除 Elasticsearch 客户端与 OSHI 依赖，以及 `spring.elasticsearch` 配置。
- 管理端不再启动任何日志写入路径（写入早已由各服务经内核完成）。

**数据**

- `iboot_admin` 的两张日志表保留一个发布周期；`iboot_monitor` 为唯一写入目标。
- dashboard 与 statistics 改为跨 schema 读日志表，属登记在案的临时债务。
