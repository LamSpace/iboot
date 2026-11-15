# Proposal

## Why

MinIO 上游已停止发布社区容器镜像，Docker Hub 上的 `minio/minio` 仓库也已被移除；当前固定的 `RELEASE.2025-09-07T16-13-09Z` 镜像不再获得修复，并带有未修补的 CVE。`pgsty/silo` 是 MinIO 的同源社区 fork，保留 S3 API、`MINIO_*` 服务端环境变量、`/minio/*` 路由与磁盘格式，因此可以在不改变存储语义的前提下，把运行期切换到 Docker Hub 上持续维护的镜像。

同时，项目希望运行期、配置、代码与界面的命名统一反映 Silo，而不是继续沿用 MinIO。

## What Changes

- 编排与镜像：以固定标签 `pgsty/silo:RELEASE.2026-11-12T00-00-00Z` 替换 `minio/minio`，覆盖 compose 服务、初始化容器、Testcontainers 与镜像拉取脚本。
- 镜像来源：删除 aityp/SWR 重打标签的临时方案，Silo 直接从 Docker Hub 拉取。
- 编排命名：服务 `minio` → `silo`，容器 `iboot-minio` → `iboot-silo`，初始化容器 `minio-init` → `silo-init`，卷逻辑名 `minio_data` → `silo_data`；物理卷名通过兼容映射保留，避免既有数据失联。
- **BREAKING** 配置命名：`IBOOT_AI_MINIO_*` → `IBOOT_AI_SILO_*`；`sys.file.minio.*` → `sys.file.silo.*`；`sys.minio.*` → `sys.silo.*`；存储类型值 `minio` → `silo`。通过 Flyway `V7` 迁移既有 `sys_config` 数据。
- **BREAKING** Java 命名：`MinioProperties`、`MinioFileStorageStrategy`、`MinioKnowledgeStorage`、`MinioAdminService`、`MinioMonitorController` 等统一改为 `Silo*`；继续使用 `io.minio:minio` 客户端，因为 Silo 与其 API 兼容。
- **BREAKING** 对外契约：监控接口 `/api/monitor/minio` → `/api/monitor/silo`，响应字段 `minioEnabled` → `siloEnabled`，权限 `minio:query` → `silo:query`，菜单路径/组件与 CloudEvent 类型同步改名；接口基线必须显式登记这次重命名，而不是静默漂移。
- 前端：`MinioMonitorView` → `SiloMonitorView`，路由 `minio-monitor` → `silo-monitor`，API 客户端与 i18n 命名空间 `monitor.minio.*` → `monitor.silo.*`。
- 文档与规格：更新部署、配置、监控、备份、AI 工作台等文档，以及相关 OpenSpec 规格。
- 兼容例外：桶名（`iboot-files`、`iboot-ai`、`thanos`）与开发凭证（`minioadmin`/`minioadmin`）保持不变；`/minio/health/live` 作为 Silo 的兼容路由保留。它们属于数据面契约，不是服务品牌。

## Capabilities

### New Capabilities

无。

### Modified Capabilities

- `container-deployment`：镜像来源、拉取脚本、服务/卷/健康检查/初始化容器从 MinIO 切换为 Silo，并移除 aityp/SWR 例外。
- `file-storage`：存储类型取值与实现命名从 `minio` 改为 `silo`，存储行为与预签名 URL 语义不变。
- `data-backup`：备份记录与场景中的存储类型从 `minio` 改为 `silo`。
- `ops-monitoring`：对象存储监控端点与权限码重命名，并登记路径稳定性要求的例外。
- `api-contract-regression`：把这次破坏性重命名登记为允许差异，要求同步更新接口基线并保留失败门禁。

## Impact

- 容器与部署：`docker/docker-compose.yml`、`scripts/pull-docker-images.sh`、`scripts/docker-deploy.sh`、`docker/README.md`、`docker/scripts/thanos/bucket.yml`。
- 后端：`iboot-file`、`iboot-job`、`iboot-system`、`iboot-monitor`、`iboot-ai` 的类名、配置键、测试；`iboot-db-init` 新增 Flyway `V7` 迁移。
- 前端：`iboot-portal` 的监控视图、路由、API 客户端、国际化与图标映射。
- 数据：`sys_config` 配置键/值、`sys_menu` 菜单与权限行、`db_backup_record.storage_type` 历史值；Docker 卷 `docker_minio_data` 的物理名保留。
- 接口：监控路径、响应字段、权限码与 CloudEvent 类型（破坏性变更，需要基线登记）。
- 测试：Testcontainers 镜像、存储契约测试、配置选择测试、迁移测试与接口 golden 基线。
