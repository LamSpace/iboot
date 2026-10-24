# Design

## Context

当前工程是跑在宿主机上的单体，已具备 Quartz（JDBC 存储、`isClustered=false`）、Flyway（最新 `V20`）、`FileStorageStrategy`（Local / MinIO / OSS / 内存四实现，启动期由 `sys.file.storageType` 选定）与一套管理端页面模式。备份功能必须落在这套四层结构内，且不改变现有文件存储契约。

本地环境的硬约束：

- `mysql:8.0.45` 镜像已在本地，镜像内 `mysqldump` 版本为 8.0.45，与数据库精确匹配。
- 宿主机没有 `mysqldump`/`mysql` 客户端，且不打算安装。
- `MinioFileStorageStrategy.save` 明确要求 `size > 0`，因此无法把未知长度的加密流直接上传。
- 应用不在 compose 中，因此应用进程可以调用 Docker CLI；上云后这一前提不成立。
- 备份内容包含 `sys_user` 密码哈希与 `sys_config` 中的 MinIO 密钥，加密是必需项。

## Goals / Non-Goals

**Goals**

- 让"产生备份"和"消费备份"与具体执行环境解耦，使本地 `docker exec` 与云端执行方式可替换。
- 用一次自动化演练证明备份可解密、可还原、行数一致。
- 复用已有存储与调度设施，不引入新中间件、不新增第三方依赖。
- 恢复路径在设计上就不可能触碰业务库。

**Non-Goals**

- 不使用 binlog、不提供时间点恢复。
- 不实现备份自动过期删除与密钥轮换。
- 不接入 RDS 原生备份 API，也不把备份做成跨实例容灾方案。
- 不改变 `FileStorageStrategy` 的接口或行为。

## Decisions

### 1. 用 `CommandRunner` 隔离 dump/restore 的执行环境

定义 `CommandRunner`（执行命令、返回退出码、提供 stdout 流、支持 stdin）与 `BackupExecutor`（`dump` / `restore` 两个语义方法）。生产实现用 `ProcessBuilder`；集成测试实现包装 Testcontainers 的 `execInContainer`。

本地默认命令：

```text
docker exec -i iboot-mysql mysqldump \
  --single-transaction --quick --default-character-set=utf8mb4 \
  --set-gtid-purged=OFF --no-tablespaces -uroot <db>
```

不使用 `--databases`，使 dump 可以被还原到不同库名。密码通过进程环境 `MYSQL_PWD` 传递，不拼进命令行；命令参数使用列表形式，禁止 shell 拼接。

**备选与否决理由**

- 宿主机安装 `mysql-client`：版本难以保证是 8.0.45，且需要 sudo，否决。
- 纯 JDBC 自行拼 SQL：需要处理二进制列、生成列、字符集、触发器与权限，风险远大于收益，否决。
- 专用 sidecar 容器：只解决本地问题，云上仍要替换，收益不足，否决。

**云端演进**：阶段 3 只需把 `CommandRunner` 换成"镜像内自带 mysql-client"或新增一个调用 RDS 备份 API 的 `BackupExecutor` 实现；表结构、编排、演练校验不变。

### 2. 备份产物复用 `FileStorageStrategy`，先落临时文件

对象键格式：`backup/{database}/{yyyy}/{MM}/{dd}/{backupNo}.sql.enc`。备份与普通文件共用同一个桶，靠 `backup/` 前缀隔离；这样 `sys.file.storageType` 切到 MinIO 或 OSS 时无需改代码。

由于存储实现要求已知大小，流水线为：

```text
mysqldump stdout
   -> 边读边算 SHA-256(明文)
   -> AES-256-GCM 加密
   -> 临时文件（java.io.tmpdir）
   -> FileStorageStrategy.save(已知 size)
   -> finally 删除临时文件
```

**备选**：扩展 `FileStorageStrategy` 支持未知大小流式上传。否决，因为会改动已归档的 0.7 契约，且本地磁盘实现同样要落文件。

### 3. AES-256-GCM 的自描述文件格式

密文文件头部包含 magic、版本、IV 长度与 IV，其后是密文与认证标签。解密用显式 `Cipher.update/doFinal` 循环读到 EOF，不用 `CipherInputStream`（部分 JDK 上认证失败可能表现为静默截断）。明文 SHA-256 在加密前计算并写入备份记录，演练时先校验校验和再还原。

密钥只来自环境变量，Base64 解码后必须为 32 字节；`enabled=true` 时启动期校验并 fail-fast。备份记录只保存算法标识，不保存密钥或 IV 以外的敏感材料。

**代价**：0.2 不支持密钥轮换；密钥丢失等于备份不可恢复，必须在操作指南和页面文案中明确。

### 4. 两张元数据表 + 备份时行数快照

- `db_backup_record`：触发方式、状态、库名、存储类型/路径、明文与密文大小、SHA-256、算法、工具版本、逐表行数快照 JSON、时间与脱敏错误。
- `db_restore_record`：关联备份、目标库、状态（成功/不一致/失败）、还原后的逐表行数、差异清单 JSON、时间与错误。

行数快照在备份时写入。演练时与**快照**比较而不是与当前业务库比较，避免备份后的正常写入造成误报。

**已知限制**：快照查询与 dump 不在同一个一致性事务中，备份期间有写入时可能产生差异。处置：演练应在安静窗口执行并在文档中说明；差异必须如实报告，不得自动判定为成功。

### 5. 恢复目标固定且受守卫保护

目标库固定为 `iboot_restore_drill`，每次演练先 `DROP DATABASE IF EXISTS` 再 `CREATE DATABASE`，演练结束后保留供人工查看（同一库下次演练时重建，占用有界）。守卫在服务层强制：

- 目标名必须匹配 `iboot_restore_` 前缀；
- 目标名不得等于配置的业务库名；
- 目标名只允许字母、数字与下划线。

接口不接受客户端传入的任意目标名。演练使用独立连接，不复用业务数据源。

### 6. 调度与并发

沿用 0.6 的模式：Flyway 种入 `sys_job`（`backupTask.run`，每天 02:00，`concurrent=0`），cron 由任务管理页维护；`iboot.backup.enabled=false` 时任务直接返回。

手动触发先插入运行中记录，再提交到应用异步执行器，接口立即返回。并发保护为进程内锁 + 运行中记录检查；阶段 2 集群化后需要数据库级锁，这一点记录在风险中。

### 7. 失败与残留处理

执行失败：记录置为失败、错误脱敏、临时文件删除、不登记可用产物。若"上传成功但记录更新失败"或进程崩溃导致记录停留在运行中，下一次执行先把超时运行中记录重置为失败（复用 0.6 通知重试任务的 stale-reset 思路）。

### 8. 管理端沿用通知投递页模式

新增 `monitor/BackupView.vue` 与 `api/backup.ts`，权限码 `backup:record:list` / `backup:record:create` / `backup:record:restore`，菜单与角色绑定由 Flyway 种入。页面不提供明文 SQL 下载；恢复演练是唯一的还原入口。

## Risks / Trade-offs

- **本地依赖 Docker CLI** → 显式配置命令模板，默认只在本地 profile 使用；命令不可用时记录失败，不影响应用启动。
- **快照与 dump 的时间差** → 演练在安静窗口执行，差异如实报告；不做容差。
- **密钥丢失即备份不可恢复** → 操作指南强提示，密钥同时存入密码管理器。
- **临时文件占用磁盘** → 使用 `java.io.tmpdir`，`finally` 删除；云端用 `emptyDir`，不假设持久盘。
- **dump 含敏感数据** → 强制加密；错误信息与日志脱敏。
- **RDS 权限模型** → 使用 `--no-tablespaces`、`--set-gtid-purged=OFF`；演练库需要有建库权限，云上由 IaC 预建或授予专用账号。
- **同一实例上的演练库消耗空间** → 固定一个演练库并在每次演练时重建；页面展示备份总占用，便于人工清理。
- **阶段 2 集群化后的并发** → 当前进程内锁只在单实例成立；拆分为多实例前必须改为数据库级锁或 Quartz 集群语义。
- **mysqldump 不是云上主备份** → 明确 RDS 自动备份仍是云端主路径，本能力承载可移植导出与演练验证。

## Migration Plan

1. Flyway `V21` 只做新增：两张表、`sys_job` 种子、菜单与权限种子；不修改既有表。
2. `iboot.backup.enabled` 默认 `false`，未配置密钥的环境（含现有测试）行为不变。
3. 部署顺序：先发布数据库迁移与代码（功能关闭）→ 配置密钥 → 通过参数配置把 `sys.file.storageType` 切到 `minio`（若尚未切换）→ 重启启用。
4. 回滚：关闭 `iboot.backup.enabled` 并停用 `sys_job` 即可停止新备份；已产生的记录与对象保留，不回滚表结构。
5. 验证：全新库执行 V1–V21、既有 V20 库增量迁移、Testcontainers 集成测试、手工 E2E（真实 MySQL + MinIO）。

## Open Questions

- 备份保留策略（按天数或份数自动清理）留待后续变更；当前只展示总占用。
- 密钥轮换与多密钥标识留待后续变更；当前假设单一长期密钥。
