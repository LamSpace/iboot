# Proposal

## Why

iBoot 目前没有任何数据库备份与恢复实现，是阶段 0 唯一遗留的功能缺口（ROADMAP 0.2）。数据一旦损坏或被误删，只能依赖 MySQL 容器卷，既无版本化产物，也无从验证备份是否真的可恢复。本变更补齐这条运维闭环，并用一次真实的恢复演练证明备份可用，而不是"备份文件存在即成功"。

## What Changes

- 新增数据库逻辑全量备份：可由 Quartz 定时触发，也可由管理员手动触发。
- 备份产物先经 AES-256-GCM 加密，再通过既有文件存储能力落盘（本地 / MinIO / OSS 由现有配置选择），并在元数据表中登记状态、大小、校验和与存储路径。
- 备份执行方式可配置：本地开发通过 `docker exec` 调用 MySQL 容器内的 `mysqldump`，不要求宿主机安装客户端；生产/云端可替换为镜像内客户端或云数据库原生备份，业务编排不变。
- 新增恢复演练：只允许还原到独立的 `iboot_restore_*` 演练库，禁止触碰业务库；还原后逐表比对行数与校验和，结果入库。
- 新增管理端"数据备份"页面：备份记录列表、手动备份、恢复演练、演练结果查看。
- 新增配置项 `iboot.backup.*` 与密钥环境变量 `IBOOT_BACKUP_ENCRYPTION_KEY`；功能默认关闭。
- 新增 Flyway 迁移：备份/演练元数据表、Quartz 任务种子、管理端菜单与权限。

明确不做：binlog 增量与时间点恢复、备份自动过期删除（仅展示占用）、阿里云 RDS 原生备份接入、备份失败通知（沿用日志与记录状态）。

## Capabilities

### New Capabilities

- `data-backup`: 数据库逻辑备份的触发、加密、存储、元数据追踪，以及限定在独立演练库中的恢复校验行为。

### Modified Capabilities

<!-- 无。文件存储能力被复用，但其对外契约不发生变化。 -->

## Impact

- **代码**：`iboot-admin` 新增 domain（备份记录模型与仓储接口）、application（备份与演练编排服务）、infrastructure（命令执行、加解密、Quartz 任务）、interfaces（管理端接口）。复用既有 `FileStorageStrategy` 与通知投递页的列表/操作页模式。
- **数据库**：新增 Flyway `V21`，创建 `db_backup_record`、`db_restore_record`，并种入 `sys_job` 与 `sys_menu`。
- **配置**：`application.yml` 新增 `iboot.backup.*`；`.env.example` 新增密钥占位符。默认 `iboot.backup.enabled=false`，未配置密钥时不影响应用启动。
- **依赖**：不新增第三方库；加密使用 JDK 自带 JCE，命令执行使用 JDK `ProcessBuilder`，集成测试复用现有 Testcontainers 与本地 `mysql:8.0.45` 镜像。
- **运维**：备份对象位于现有存储桶的 `backup/` 前缀下；恢复演练库固定为 `iboot_restore_drill`，每次演练重建，不影响业务库。
- **文档**：新增数据备份与恢复操作指南，补充密钥保管、演练窗口与恢复注意事项。
