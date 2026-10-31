# Design

## Context

见 `proposal.md - Why`。影响实现方式的现状事实（均已在代码中确认）：

- 管理端 `sys_file` 由 `V1__initial_schema.sql` 创建，无外键；`FileMapper.xml` 只查这一张表。
- `BackupApplicationService` 使用 `StorageObject` 与 `FileStorageStrategy` 写入备份产物；`RestoreDrillService` 同样。
- 管理端固件测试中，`FileApplicationServiceTest`、`FileControllerDownloadTest` 依赖待删除的实现；`FileStorageConfigTest` 与 `InMemoryFileStorageStrategy` 依赖保留的存储 SPI。
- `iboot-file` 目前**没有** Flyway 依赖，数据源指向 `iboot_admin`。
- `migrations.txt` 中 `/api/file admin file transitional`，路径守恒允许管理端快照继续包含这 7 条路径。

## Goals / Non-Goals

**Goals**

- 管理端文件域实现彻底退休，文件服务成为 `/api/file/**` 的唯一实现。
- `sys_file` 迁到独立 schema `iboot_file`，由文件服务自己的 Flyway 管理。
- 数据搬迁可核验、可回退。

**Non-Goals**

- 不删除管理端 schema 中的 `sys_file` 表。
- 不改变存储抽象、备份链路或任何接口契约。
- 不引入服务间调用（文件服务仍只读共享库的 `sys_config` / `sys_dict_data`）。

## Decisions

### D1. 删除范围与保留范围

**删除**（管理端）：`FileController`、`FileApplicationService`、`FileInfo`、`FileRepository`、`FileRepositoryImpl`、`infrastructure/persistence/mapper/FileMapper`、`application/mapper/system/FileMapper`（MapStruct）、`FilePO`、`mapper/FileMapper.xml`、`FileResponse`、`FileUpdateRequest`、`FileExportVO`，以及 `FileApplicationServiceTest`、`FileControllerDownloadTest`。

**保留**：`StorageObject`、`FileStorageStrategy`、Local/MinIO/OSS 实现、`FileStorageConfig`、`MinioProperties`、`OssProperties`——备份功能仍依赖它们，且阶段 3 的 MinIO→OSS 替换依赖这一层抽象。

**为什么**：删除以「谁还在用」为准，而不是以包名归类。存储 SPI 与文件管理是两个职责，只是曾经住在同一批包里。

### D2. 数据搬迁用一次性脚本，不用 Flyway

新增 `scripts/migrate/sys_file_to_iboot_file.sql`：

```sql
CREATE DATABASE IF NOT EXISTS iboot_file CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
-- 结构由 iboot-file 的 Flyway V1 创建，这里只复制数据
INSERT INTO iboot_file.sys_file (id, file_name, file_path, ...)
SELECT id, file_name, file_path, ... FROM iboot_admin.sys_file;
```

**为什么**：Flyway 的职责是单个 schema 的结构版本化。跨 schema 的数据复制是一次性运维动作，需要可重复执行、可单独核验；塞进任一侧的迁移目录都会让 Flyway 变成「跨库搬运工」，且难以在别人的环境里重放。

**约束**：脚本只复制、不删除；重复执行前先 `TRUNCATE iboot_file.sys_file`（脚本内注释说明），保证幂等。

### D3. 管理端 `sys_file` 保留一个周期

不新增 `DROP TABLE`。回退时数据源指回 `iboot_admin` 即可，无需恢复数据。

**退出条件**：文件服务在新 schema 上稳定运行一个发布周期后，另起清理项删除。届时数据已无回退价值。

### D4. 执行顺序是硬约束

```
1. 建 schema + file 服务 Flyway V1（表结构）
2. 停写 → 执行数据复制 → 校验行数 / 最大 id / 抽样比对
3. 文件服务数据源切到 iboot_file，重启并复跑回归
4. 删除管理端旧实现 + 其单测
5. migrations.txt 改为 done，刷新基线，复跑全量回归
6. 更新文档
```

**为什么**：第 4 步是不可逆点。在数据校验与回归都通过之前删除代码，等于把回退路径提前烧掉。

**停写**：本地演练用 `dev-init.sh --stop` 停后端；生产语义是「迁移窗口内不写文件」，本项目的练习场景以停服实现。

### D5. 回退策略

| 时点 | 回退动作 |
|---|---|
| 第 3 步之前 | 什么都不用做；文件服务仍指向旧 schema |
| 第 3 步之后、第 4 步之前 | 数据源改回 `iboot_admin`，重启 |
| 第 4 步之后 | `git revert` 恢复旧实现；数据仍在两处，回退不丢数据 |

### D6. 验证

- 数据：`SELECT COUNT(*), MAX(id)` 两侧一致；抽样比对 `file_name` / `file_path` / `upload_time`。
- 应用：文件服务启动日志里 Flyway 执行了 `V1`；`/api/file/list` 返回与迁移前相同的条数。
- 回归：路径守恒（admin 快照不再含 `/api/file`，`done` 状态生效）、路由覆盖、冒烟、权限矩阵、等价性（重录基线后校验）。
- 管理端：`mvn -pl iboot-admin test` 通过，确认删除没留下悬空引用。

## Risks / Trade-offs

| 风险 | 处置 |
|---|---|
| 迁移窗口内有写入，复制后丢失 | 复制前停后端；脚本记录源表行数与最大 id 供核对 |
| 自增起点冲突 | 复制显式带 `id`，MySQL 会自动把 `AUTO_INCREMENT` 推到 `max(id)+1`；校验时检查 `SHOW TABLE STATUS` |
| 字符集/排序规则不一致导致比较行为变化 | 两侧统一 `utf8mb4` / `utf8mb4_unicode_ci`，建库语句显式声明 |
| 删除旧实现后忘记刷新基线，CI 路径守恒失败 | 顺序固定为「改登记 → 刷新基线 → 复跑」，并在 tasks 里逐项验证 |
| `iboot-file` 首次启用 Flyway，误连到 old schema 把表建错地方 | 启动日志核对「Migrating schema \`iboot_file\`」；数据源 URL 显式写库名 |

## Migration Plan

见 D4 的六步。每一步都可停下来复核，回退点见 D5。

## Open Questions

- 管理端 `sys_file` 的删除时机没有客观判据，建议以「文件服务稳定一个发布周期且无回退请求」为准，不影响本变更的任务拆分。
