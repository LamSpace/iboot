# Proposal

## Why

阶段 2 把单体拆成七个 schema 之后，代价开始累积：代码里有 68 处跨库限定名（`iboot_admin.` 与 `iboot_monitor.`），五个服务的 SQL 把 `sys_user` 拼进数据权限子查询，八个服务的 `operate-log-table` 配置写着 `iboot_monitor.sys_operate_log`；迁移分散在七个模块里，`scripts/dev-init.sh` 必须让监控与系统服务先就绪，建库还要手工执行六条命令；拆分遗留的 25 张表与 `iboot_admin` 改名一直悬而未决。

本项目的数据是练习数据，可以丢弃。在这个前提下，一次性收敛为单库并把迁移集中到一处，比继续维护七套 schema 的边界成本更低。

## What Changes

- **新建 `iboot-db-init` 模块**：纯 Java 入口 + Flyway，独占全部迁移目录；一次运行完成建库与全部迁移，成功退出 0，失败非零退出。
- **合并为单 schema `iboot`**：现有七个 schema 的结构与种子数据合并为一份 baseline；**现有数据丢弃**，重建空库。
- **服务不再执行迁移**：九个服务的数据源指向 `iboot`，`spring.flyway.enabled=false`；七个模块的 `db/migration` 目录删除。
- **清理跨库限定名**：2 个 Mapper XML、4 个 `DataScopeSqlBuilder`、5 个数据读取器与 8 处 `operate-log-table` 配置去掉库名前缀。
- **测试改用同一份迁移**：13 个集成测试从 `iboot-db-init` 提供的迁移 API 建立 schema，不再依赖模块内 SQL。
- **本地环境收敛**：`scripts/dev-init.sh` 只建一个库；`docker/init.sql` 与 compose 的 `MYSQL_DATABASE` 改为 `iboot`。
- **迁移规则**：版本按时间戳命名以避免跨服务撞号；变更遵循 expand-contract（先扩展、后收缩），保证滚动发布期间旧实例仍可运行。
- **已知代价**：旧备份产物无法直接恢复到新拓扑；`iboot_restore_drill` 演练库机制不变。

## Capabilities

### New Capabilities

- `database-lifecycle`: 数据库的建库、迁移归属与执行方式，以及服务与测试如何获得可用的 schema。

### Modified Capabilities

- `system-management`: 系统域数据的消费方式由「跨 schema 只读」改为「同库表级只读」；聚合视图对日志表的读取同步改判。
- `operation-logging`: 审计写入由「跨 schema 写入」改为「同库写入」，所有权切换窗口的表述随之调整。
- `ops-monitoring`: 审计表归属与持久化依赖的描述从「自身 schema」改为「共享数据库中的自有表」。
- `auth-service`: 认证写入边界中「管理端拥有的 schema」改为「系统域拥有的表」。
- `data-backup`: 业务库名由 `iboot_admin` 改为 `iboot`，恢复目标的拒绝规则同步。

## Impact

- 新增 `iboot-db-init` 模块；父 POM 的模块列表与 `scripts/dev-init.sh` 的启动顺序调整。
- 九个服务的数据源配置、Flyway 开关与审计表名；`iboot-auth` 不建自有 schema 的既有约定保持不变。
- 13 个集成测试的 schema 建立方式。
- 文档：`doc/23` §11.12 的跨 schema 契约、`doc/07` 的数据库设计、`doc/05` 与 README 的建库步骤、ROADMAP 的数据边界表述。
- 运维：需要一次停机重建；旧备份产物不再可用于新拓扑。
