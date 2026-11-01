# Design

## Context

当前是一个 MySQL 实例上的七个 schema：

```
iboot_admin   iboot_ai   iboot_file   iboot_notify   iboot_job   iboot_workflow   iboot_monitor
   system          ai        file        notify         job         workflow        monitor
   auth(直连)
```

代码中跨库限定名的分布：

| 位置 | 数量 | 说明 |
|---|---|---|
| `OperateLogMapper.xml` / `LoginLogMapper.xml` | 26 | 仪表盘与统计读 `iboot_monitor` |
| `FileMapper.xml` | 5 | 文件列表关联 `iboot_admin.sys_user` |
| 4 个 `DataScopeSqlBuilder` | 8 | 数据权限子查询拼 `iboot_admin.sys_user` |
| 5 个数据读取器（config/dict/user/dept） | 10 | 跨库只读 |
| 8 处 `operate-log-table` 配置 | 8 | 审计写入目标 |
| 其余散落 | 11 | 注释、配置与登记 |

迁移分散在七个模块的 `db/migration`，其中 `iboot-system` 的迁移仍包含拆分前建出的全部历史表（`sys_file`、`sys_message*`、`sys_job`、`ACT_*`、`QRTZ_*` 等），这些表已分别由其它服务拥有并在各自 schema 中重建。

`scripts/dev-init.sh` 目前必须让监控与系统服务先就绪，并手工创建六个 schema。

## Goals / Non-Goals

**Goals:**

- 所有服务共享一个数据库身份 `iboot`，跨库限定名归零。
- 建库与版本管理集中到一个一次性执行者，服务启动不再做 DDL。
- 迁移版本号全局唯一，滚动发布期间旧实例仍可运行。
- 集成测试与生产使用同一份迁移。

**Non-Goals:**

- 不迁移现有数据（练习数据，明确丢弃）。
- 不改变任何 HTTP 接口、权限语义或前端行为。
- 不引入分库分表或读写分离（属于阶段 4）。
- 不改变 `iboot_restore_drill` 演练库机制。

## Decisions

### D1 单库 `iboot`，写入所有权下沉到表

七个 schema 合并为一个 `iboot`。原先由库名表达的写入边界改为表级约定：每张表的拥有者服务唯一，非拥有者只能按登记清单只读。消费清单沿用 `doc/23` §11.12 的表与列，只去掉库名前缀。

代价是边界不再由数据库物理隔离，只由评审与测试保证；换来的是 68 处限定名归零、建库只需一条命令、仪表盘与数据权限的 SQL 不再依赖库名。

### D2 baseline 用 squash 生成，不搬七个迁移目录

不能把七个 `db/migration` 并排放进 db-init：`iboot-system` 的 V1–V28 会先把拆分前的全部历史表建一遍，随后与 file / notify / job / workflow / monitor 的 V1 撞名。正确做法是 squash：

```
空 MySQL -> 七个 schema 各跑一遍现有 Flyway（得到"结构 + 种子"参考状态）
        -> 逐库导出结构，合并为一份 DDL（以当前所有者为准）
        -> 导出种子数据（系统域的管理员、菜单、角色、字典、参数等 360 条 INSERT）
        -> 生成 V1__baseline.sql
        -> 删除七个模块的 db/migration
```

关键判断：表名冲突全部是「system 的历史副本 vs 当前所有者」，合并时**一律以当前所有者的结构为准**；system 中那些冻结副本不进入 baseline。

### D3 `iboot-db-init` 是纯 Java 执行者

不引入 Web 容器，也不依赖 Spring Boot 的启动流程。模块包含 Flyway、MySQL 驱动与一个 `main`：

- 读取与其它服务相同的连接环境变量（`MYSQL_HOST`/`MYSQL_PORT`/`MYSQL_USERNAME`/`MYSQL_PASSWORD`），数据库名固定为 `iboot` 或由 `IBOOT_DB_NAME` 覆盖。
- 不存在时创建数据库；执行 `classpath:db/migration` 下的全部迁移。
- 全部成功退出 0；失败打印失败脚本并以非零退出。
- 同时暴露一个可被测试调用的入口：`SchemaMigrator.migrate(dataSource, name)`，只执行迁移，不负责建库。

替代方案「Spring Boot 应用 + `spring.flyway`」被否决：会拉起一整套容器与自动配置，启动慢且退出语义不如 `main` 直接；迁移执行者不需要任何 Web 能力。

### D4 服务不执行迁移

九个服务 `spring.flyway.enabled=false`，数据源全部指向 `iboot`，`iboot-auth` 保持直连共享库、不建自有 schema 的既有约定。

副作用必须写进文档：单起一个服务打到空库，报的是「表不存在」而不是自动迁移；本地启动必须先跑 db-init。`scripts/dev-init.sh` 相应调整顺序。

### D5 版本号按时间戳，变更新增遵循 expand-contract

squash 之后所有服务共用一条版本序列。版本号使用 `V<yyyyMMddHHmm>__<service>__<desc>.sql`：

- 时间戳保证并行开发不撞号，也天然按时间排序。
- 文件名携带服务名，便于定位来源。
- 一次发布只允许扩展（加表/加列/加索引/放宽约束）；收缩（删列/改类型/收紧约束）留到后续发布，并在旧实例全部下线后执行。

### D6 测试从同一份迁移建立 schema

13 个集成测试目前各自 `Flyway.configure().locations("classpath:db/migration")`。改造后统一调用 `SchemaMigrator.migrate(dataSource, "iboot")`，测试以 test scope 依赖 `iboot-db-init`。

替代方案「测试保留一份建表 SQL」被否决：会出现第二套真相，且结构变更时测试不会失败。

### D7 限定名清理按面拆开

| 面 | 处理 |
|---|---|
| Mapper XML | 去掉 `iboot_admin.` / `iboot_monitor.` 前缀 |
| `DataScopeSqlBuilder` | 子查询改为 `sys_user` |
| 数据读取器 | 表名常量去掉库名前缀 |
| `operate-log-table` 配置 | 只保留表名 |
| 注释与文档 | 同步改写，保留「表所有权」语义 |

### D8 备份与旧产物

- 业务库名由 `iboot_admin` 变为 `iboot`，备份对象键中的数据库名随之变化。
- 已有的备份产物内部是 `CREATE TABLE iboot_admin.xxx`，无法直接恢复到新拓扑；本次在操作指南中明确「旧产物仅可用于旧拓扑」。
- `iboot_restore_drill` 与 `iboot_restore_` 前缀规则不变。

### D9 编排与执行顺序

```
mysql (healthy)
   -> db-init (restart: "no", 退出码 0)
        -> 九个服务（depends_on: service_completed_successfully）
```

本地由 `scripts/dev-init.sh` 调用 db-init 后启动服务；`directories` 中的一次性容器只在容器化或 K8s 阶段使用（阶段 3 用 Job + pre-upgrade hook）。

### D10 旧库的处置

新库验证通过前，七个旧库以只读方式保留作为回滚点；验证通过后删除。这一步只是运维收尾，不是数据迁移——旧数据不导入新库。

## Risks / Trade-offs

- **baseline 选错表结构** → 生成基线时逐表比对「当前所有者」清单，并在空库上启动九个服务做冒烟；冲突表一律取当前所有者的定义。
- **测试改造遗漏** → 全仓搜索 `Flyway.configure`，13 处全部替换；CI 中数据库相关测试失败即阻断。
- **停机窗口** → 本次接受一次停机：停服务 → 建新库 → 跑 db-init → 起服务。练习环境可承受。
- **发布耦合** → 结构变更必须先发 db-init；由 D5 的 expand-contract 规则保证旧实例在迁移后仍可运行。
- **旧备份不可恢复** → 文档明确；演练库机制不受影响。
- **单库后权限隔离消失** → 写入边界只剩表级约定；以评审与契约测试兜底。

## Migration Plan

1. 新建 `iboot-db-init` 模块与 `SchemaMigrator`。
2. 以空库跑现有七个 Flyway，导出结构与种子，生成 `V1__baseline.sql`。
3. 删除七个模块的 `db/migration`，服务关闭 Flyway，数据源改指 `iboot`。
4. 清理 68 处限定名与 8 处配置。
5. 改造 13 个集成测试。
6. `dev-init.sh`、`docker/init.sql`、compose 环境收敛。
7. 停机执行：建库 → db-init → 起九个服务 → 冒烟。
8. 更新 `doc/23` §11.12、`doc/07`、`doc/05`、README 与 ROADMAP 的数据边界表述。

回滚：保留旧库与旧镜像，数据源改回各自 schema 并恢复旧的 Flyway 启用状态；`git revert` 本次提交。

## Open Questions

无。
