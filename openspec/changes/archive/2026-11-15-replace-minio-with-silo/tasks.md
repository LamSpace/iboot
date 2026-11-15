# Tasks

## 1. 变更准备与回滚基线

- [x] 1.1 从 `master` 创建功能分支 `feat/replace-minio-with-silo`；验证：`git branch --show-current` 输出该分支且工作区干净。
- [x] 1.2 拉取并固定 `docker.io/pgsty/silo:RELEASE.2026-11-12T00-00-00Z`；验证：`docker image inspect` 成功，并记录镜像 digest。
- [x] 1.3 确认当前对象存储卷物理名；验证：`docker volume ls` 或 `docker compose -f docker/docker-compose.yml config` 显示 `docker_minio_data`（不存在时记录为“无历史卷”）。
- [x] 1.4 备份 `iboot.sys_config`、`iboot.sys_menu`、`iboot.db_backup_record`，并在本机保留可回滚的 MinIO 镜像；验证：备份文件存在且可读取，`docker image inspect minio/minio:RELEASE.2025-09-07T16-13-09Z` 成功。

## 2. 编排与镜像切换

- [x] 2.1 修改 `docker/docker-compose.yml`：服务 `minio` → `silo`，容器 `iboot-minio` → `iboot-silo`，初始化容器 `minio-init` → `silo-init`，镜像改为 `pgsty/silo:RELEASE.2026-11-12T00-00-00Z`，初始化命令改用 `mcli`，所有 `depends_on` 同步改名；验证：`docker compose -f docker/docker-compose.yml config` 不再出现 `minio` 服务名，且能解析出 `silo`。
- [x] 2.2 在 compose 中把卷改为逻辑名 `silo_data` 并通过 `name: ${IBOOT_SILO_DATA_VOLUME:-docker_minio_data}` 保留物理卷；验证：`docker compose config` 显示 `silo_data` 映射到既有卷名，启动后旧桶与对象仍可见。
- [x] 2.3 验证并固定 Silo 健康检查：启动容器，分别测试 `silo healthcheck ready` 与 `mcli ready local`，选择能反映 S3 可用性的最严格命令；验证：`docker compose ps` 显示 `healthy`，且健康检查命令在容器内返回 0。
- [x] 2.4 修改 `scripts/pull-docker-images.sh`：删除 aityp/SWR 的 `pull`/`tag`/`rmi` 块，改为直接拉取固定标签的 `pgsty/silo`；验证：`bash -n` 通过，脚本中不再出现 `aityp`、`SWR`、`minio/minio`。
- [x] 2.5 修改 `scripts/docker-deploy.sh` 头部注释与镜像说明中的 MinIO/SWR 描述；验证：`bash -n` 通过，`grep -n "aityp\|SWR\|minio/minio"` 无命中。
- [x] 2.6 修改 `docker/scripts/thanos/bucket.yml` 的 endpoint 为 `silo:9000`；验证：文件内容与 compose 服务名一致，`thanos` 配置可读取。
- [x] 2.7 更新 `docker/README.md` 与 `docs/08-部署指南.md` 中的服务名、卷名、`docker exec` 与初始化命令；验证：文档中的命令逐条可在部署后的容器上执行。

## 3. 数据库与配置迁移

- [x] 3.1 新增 `iboot-db-init/src/main/resources/db/migration/V7__rename_minio_to_silo.sql`：更新 `sys_config` 的 `sys.file.minio.*` → `sys.file.silo.*`、`sys.minio.*` → `sys.silo.*`、`sys.file.storageType` 值 `minio` → `silo` 及名称/说明；更新 `sys_menu` 706/7061 的名称、路径、组件与权限码；更新 `db_backup_record.storage_type` 的 `minio` → `silo`；验证：在 Testcontainers MySQL 或本地库执行 Flyway 后，查询结果符合预期，且中文文本无乱码。
- [x] 3.2 把 `iboot-file`、`iboot-job`、`iboot-system`、`iboot-monitor` 的配置键常量改为 `sys.file.silo.*` 与 `sys.silo.*`，存储类型判断改为 `silo`；验证：`grep -rn "sys.file.minio\|sys.minio\|isMinioEnabled"` 无源码命中（归档与历史文档除外）。
- [x] 3.3 更新 `FileStorageConfig` 的 `STORAGE_TYPE_SILO` 与 `FileStorageConfigTest`；验证：该测试的 `silo` 分支、`minio` 旧值拒绝分支与非法值分支全部通过。
- [x] 3.4 更新 `.env.example`、`docs/05-快速开始.md`、`docs/10-后端配置详解.md`、`docs/04-功能模块.md` 中的 `IBOOT_AI_MINIO_*`、`sys.file.minio.*`、`sys.file.storageType=minio`；验证：`grep -rn "IBOOT_AI_MINIO\|sys.file.minio\|storageType.*minio"` 在非归档文档中无命中。
- [x] 3.5 更新 `openspec/specs/file-storage/spec.md` 的 Purpose，把 MinIO 改为 Silo；验证：`grep -n "MinIO" openspec/specs/file-storage/spec.md` 无命中。

## 4. 后端代码全量改名

- [x] 4.1 在 `iboot-file`、`iboot-job`、`iboot-system` 中把 `MinioProperties` → `SiloProperties`、`MinioFileStorageStrategy` → `SiloFileStorageStrategy`，同步构造器、Bean 引用、Javadoc 与 `FileStorageConfig` 导入；验证：三个模块编译通过，相关存储契约测试通过。
- [x] 4.2 在 `iboot-monitor` 中把 `MinioProperties` → `SiloProperties`、`MinioAdminService` → `SiloAdminService`、`MinioMonitorController` → `SiloMonitorController`，方法 `getMinioStatus`/`pingMinio` 改为 Silo 命名，保留 `/minio/health/live` 并注明兼容原因；验证：模块编译与控制器测试通过。
- [x] 4.3 在 `iboot-ai` 中把 `MinioKnowledgeStorage` → `SiloKnowledgeStorage` 及其测试引用改名，环境变量改用 `IBOOT_AI_SILO_*`；验证：AI 存储与 RAG 相关测试通过。
- [x] 4.4 把 6 个模块的 `CloudEventTypes.MONITOR_MINIO_INFO`/`com.iboot.monitor.minio.info` 改为 `MONITOR_SILO_INFO`/`com.iboot.monitor.silo.info`，并更新 `docs/13-CloudEvents 集成.md`；验证：`grep -rn "MONITOR_MINIO_INFO\|monitor.minio.info"` 无源码与文档命中。
- [x] 4.5 把两个 Testcontainers 测试（`MinioFileStorageStrategyContractTest`、`MinioKnowledgeStorageTest`）改名为 Silo 版本，镜像改为 `pgsty/silo:RELEASE.2026-11-12T00-00-00Z`，命令改为 `server /data`，保留 `/minio/health/live` 等待；验证：测试类可运行且断言通过（受本机 Docker 拉取限制时记录原因）。
- [x] 4.6 更新 `docs/09-后端技术架构详解.md`、`docs/11-可观测性设计.md`、`docs/21-数据备份与恢复操作指南.md`、`docs/22-AI 工作台操作指南.md` 中的类名、环境变量、存储类型与容器命令；验证：文档中的示例与环境变量与代码一致。

## 5. 监控接口与契约基线

- [x] 5.1 把监控控制器映射改为 `/api/monitor/silo`，响应字段 `minioEnabled` → `siloEnabled`，权限 `minio:query` → `silo:query`；验证：启动后 `GET /api/monitor/silo` 与 `/ping` 返回预期结构，无权限请求返回 403。
- [x] 5.2 更新 `scripts/regress/routes.txt` 与 `scripts/regress/migrations.txt` 的 `/api/monitor/minio` 登记为 `/api/monitor/silo`；验证：`route-coverage.sh` 与 `path-conservation.sh` 通过。
- [x] 5.3 重新导出并提交 `scripts/regress/golden/monitor.json`，同步更新 `scripts/regress/golden/file.json` 的 MinIO 描述；验证：与旧基线的差异仅包含路径、operationId、标签、描述与登记项。
- [x] 5.4 更新 `docs/11-可观测性设计.md` 与 `docs/13-CloudEvents 集成.md` 中的监控端点、权限码与事件类型；验证：文档中的路径与权限码与控制器一致。
- [x] 5.5 运行 API 契约回归与权限矩阵；验证：`scripts/regress/api-contract` 相关检查与 `permission-matrix.sh` 通过，破坏性重命名在登记中可追溯。

## 6. 前端改名

- [x] 6.1 把 `iboot-portal/src/views/monitor/MinioMonitorView.vue` 重命名为 `SiloMonitorView.vue`，内部状态与 CSS 类改为 Silo 命名；验证：路由能加载新组件，页面无 `Minio` 文案残留。
- [x] 6.2 更新 `iboot-portal/src/router/index.ts` 的 `minio-monitor`/`MinioMonitor` 为 `silo-monitor`/`SiloMonitor`；验证：菜单路径 `/monitor/silo-monitor` 可打开页面。
- [x] 6.3 更新 `iboot-portal/src/api/system.ts` 的 `MinioStatus`/`getMinioStatus`/`pingMinio` 与请求 URL 为 Silo 命名；验证：页面调用 `/monitor/silo` 与 `/monitor/silo/ping` 成功。
- [x] 6.4 把 `iboot-portal/src/locales/{zh-CN,en-US}/monitor/minio.json` 重命名为 `silo.json`，更新 `index.ts` 导入与 `monitor.minio.*` → `monitor.silo.*`，并把 `icon-map.ts` 的 `minio` 改为 `silo`；验证：中英文页面文案完整，`grep -rn "monitor\.minio"` 无命中。
- [x] 6.5 运行前端构建与类型检查；验证：`cnpm run build`（或项目登记的等价命令）通过，浏览器打开 `/monitor/silo-monitor` 无报错。

## 7. 集成验证

- [x] 7.1 用 `scripts/docker-deploy.sh up core` 启动核心栈；验证：全部服务健康，`silo` 使用既有卷，`minio-init` 对应的 `silo-init` 成功创建 `thanos` 桶。
- [x] 7.2 验证文件上传/下载：上传一个文件并获取预签名 URL；验证：302 跳转后的 URL 可下载，文件内容一致。
- [x] 7.3 验证 AI 知识库：上传文档并完成入库/检索；验证：对象落在 `iboot-ai` 桶，检索返回引用。
- [x] 7.4 验证备份与恢复演练：以 `sys.file.storageType=silo` 触发一次备份；验证：Silo `backup/` 前缀出现对象，备份记录存储类型为 `silo`，演练结果符合预期。
- [x] 7.5 验证监控页面与权限：具备 `silo:query` 的用户可打开 Silo 监控页，无权限用户被拒绝；验证：页面数据与 `/api/monitor/silo` 响应一致。
- [x] 7.6 全仓搜索 `minio|MinIO|MINIO`（排除 `openspec/changes/archive`、`docs/spike`、`iboot-portal/dist`、`.tmp` 与兼容例外）；验证：剩余命中仅为桶名、凭证、`/minio/health/live`、`io.minio` SDK 与历史文档。

## Workflow follow-up

- 按项目流程提交功能分支并创建 PR，合并后更新本地 `master`。
- 评审通过后归档本 change，并确认 `openspec/specs` 中的相关能力已同步。
