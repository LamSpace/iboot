# Tasks

## 1. 初始化模块

- [ ] 1.1 新建 `iboot-db-init` 模块：纯 Java `main` + Flyway + MySQL 驱动，读取与服务相同的连接环境变量，数据库名可由 `IBOOT_DB_NAME` 覆盖（默认 `iboot`）；在父 POM 中登记该模块，`mvn -pl iboot-db-init package` 成功
- [ ] 1.2 实现 `SchemaMigrator.migrate(DataSource, String)` 供测试复用，与 `main` 共用同一份迁移目录；编写一个单元测试验证「空库建全部表」与「重复执行幂等」
- [ ] 1.3 实现退出语义：全部成功退出 0，任一迁移失败打印失败脚本并非零退出且不继续；用一个故意失败的迁移脚本验证

## 2. 生成 baseline

- [ ] 2.1 在空 MySQL 上让七个 schema 各跑一遍现有 Flyway，得到「结构 + 种子」参考状态；记录每个 schema 的表清单与行数
- [ ] 2.2 按「当前所有者」合并结构与种子，生成 `db/migration/V1__baseline.sql`（含 360 条种子 INSERT）；确认 `system` 中已拆走的历史表副本（`sys_file`、`sys_message*`、`sys_job`、`ACT_*`、`QRTZ_*` 等）未进入 baseline
- [ ] 2.3 在空库执行 db-init，逐表比对 baseline 建出的表与第 2.1 步的清单；确认表数量、表名、列名一致，种子数据（admin 账号、菜单、角色、字典、参数）齐全
- [ ] 2.4 用 db-init 建出的库启动九个服务，确认全部健康检查 UP 且登录、菜单、数据权限可用

## 3. 服务切换与限定名清理

- [ ] 3.1 九个服务的数据源改为 `iboot`，`spring.flyway.enabled=false`；确认服务启动日志中不再出现 Flyway 迁移记录
- [ ] 3.2 删除七个模块的 `db/migration` 目录；确认 `find iboot-* -path "*db/migration*"` 只剩 `iboot-db-init`
- [ ] 3.3 清理 `OperateLogMapper.xml`、`LoginLogMapper.xml`、`FileMapper.xml` 中的库名前缀；确认 `grep -rn "iboot_admin\.\|iboot_monitor\." --include=*.xml` 无命中
- [ ] 3.4 清理四个 `DataScopeSqlBuilder` 与五个数据读取器中的库名前缀；确认 `grep -rn "iboot_admin\.\|iboot_monitor\." --include=*.java` 无命中（注释与登记文档除外）
- [ ] 3.5 把八处 `operate-log-table` 配置改为纯表名；启动服务确认审计写入仍落在 `sys_operate_log`，且启动校验对非法表名仍然失败

## 4. 测试改造

- [ ] 4.1 把 13 个使用 `Flyway.configure` 的集成测试改为调用 `SchemaMigrator.migrate`，测试以 test scope 依赖 `iboot-db-init`；逐模块运行 `mvn -pl <module> test` 确认通过
- [ ] 4.2 全仓搜索 `Flyway.configure` 与 `classpath:db/migration`，确认业务模块中无残留；测试资源中不存在第二套建表 SQL

## 5. 本地环境与文档

- [ ] 5.1 调整 `scripts/dev-init.sh`：先启动 db-init 并等待退出 0，再按顺序启动九个服务；删除六条手工建库命令；确认 `--status` 与 `--stop` 仍工作
- [ ] 5.2 修改 `docker/init.sql` 与 compose 的 `MYSQL_DATABASE` 为 `iboot`；确认全新数据卷启动后只有一个业务库
- [ ] 5.3 更新 `doc/23` §11.12（跨 schema 契约改为表级只读契约与写入例外）、`doc/07`（数据库设计）、`doc/05`（建库步骤）
- [ ] 5.4 更新 `README.md` 的 schema 表与手工启动说明、ROADMAP 的数据边界表述；确认两处不再出现七个 schema 的描述

## 6. 切换与验证

- [ ] 6.1 执行一次完整切换演练：停全部服务 → 建新库 → 运行 db-init → 起九个服务；确认切换耗时与失败回滚步骤写入操作记录
- [ ] 6.2 跑全量冒烟与 `bash scripts/regress/regress.sh`，确认登录、用户、文件、SSE、审批、日志、AI 问答全部可用
- [ ] 6.3 触发一次备份与恢复演练，确认备份目标库名为 `iboot`、演练库为 `iboot_restore_drill`，且业务库不被写入
- [ ] 6.4 验证通过后删除七个旧库，并在 `doc/23` 记录旧备份产物与新拓扑不兼容的结论
