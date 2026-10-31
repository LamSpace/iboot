# Proposal

## Why

`add-file-service` 把 `/api/file/**` 迁到了 `iboot-file`，但按绞杀者模式保留了管理端的旧实现一个发布周期。现在新服务已通过全量回归（契约、路径守恒、等价性、冒烟、权限矩阵、故障与回滚演练），旧实现可以从根上退休。

不退休的代价是三重的：

- **同一组路径有两份 OpenAPI 来源**，路径守恒只能停在 `transitional`，无法证明迁移完成；
- **`sys_file` 仍留在管理端的 schema 里**，拆分服务"各自独立 schema"的目标迟迟不成立，阶段 4 的分库分表也失去前置；
- **两份实现持续产生维护成本**：文件域的修复（例如数据权限）今后要记得改两处。

## What Changes

- **删除管理端文件域旧实现**：`FileController`、`FileApplicationService`、`FileInfo`、`FileRepository(+Impl)`、`FileMapper`（MyBatis 与 MapStruct 各一）、`FilePO`、Mapper XML、三个文件 DTO。
  - **保留**存储抽象与实现：`StorageObject`、`FileStorageStrategy`、Local/MinIO/OSS 三个实现、`FileStorageConfig`、`MinioProperties`、`OssProperties`——备份功能（未来的 `iboot-job`）仍在使用。
- **`iboot-file` 启用独立 schema `iboot_file`**：新增 Flyway `V1__initial_schema.sql` 创建 `sys_file`，数据源指向新 schema。
- **数据迁移**：新增一次性脚本把 `iboot_admin.sys_file` 的数据复制到 `iboot_file.sys_file`，并校验行数与主键自增起点。
- **回归登记翻转**：`scripts/regress/migrations.txt` 中 `/api/file admin file` 由 `transitional` 改为 `done`，路径守恒从此要求管理端快照不再出现 `/api/file/**`。
- **刷新基线**：管理端契约快照减去 7 条路径，文件服务快照保持；等价性基线重录。
- **文档**：`doc/23 §11.3` 的临时状态表收敛，`ROADMAP.md` 拆分状态表更新。

明确不做：

- **不删除管理端 schema 中的 `sys_file` 表**——先保留一个周期以便回滚；删除作为后续可选清理项另行登记。
- **不改任何接口路径、响应结构或权限码**；文件服务的行为与拆分后保持一致。
- **不动备份功能的存储调用**。

## Capabilities

### New Capabilities

（无。本变更是纯清理与数据搬迁，外部可见行为不变。）

### Modified Capabilities

（无。`api-contract-regression` 的新增要求已在 `add-file-service` 中同步，本次只是把登记状态改为 `done`，不涉及规格变更。）

> 本变更在 `.openspec.yaml` 中标记 `skip_specs: true`。

## Impact

**代码**

- `iboot-admin` 删除约 13 个文件与其单体测试；存储 SPI 与其实现保留。
- `iboot-file` 新增 Flyway 依赖、迁移目录与数据源 schema 配置。

**数据**

- 新增 schema `iboot_file`；`sys_file` 数据一次性复制。
- 管理端 schema 中的 `sys_file` 保留但不再被读写。

**回归**

- 管理端快照失去 7 条路径（已登记迁移）；路径守恒从 `transitional` 转为 `done`。
- 冒烟、权限矩阵、故障与回滚演练按原样复跑。

**风险**

- 数据迁移与 schema 切换必须可回退：迁移脚本只做「复制」不做「删除」，回退时只需把数据源指回管理端 schema。
- 删除旧实现后，路由回退到 admin 不再可行；这是本次变更的**不可逆点**，因此必须在数据迁移校验通过、且文件服务稳定运行之后才执行。
