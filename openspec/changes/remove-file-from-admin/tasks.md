# Tasks

> 前置：`add-file-service` 已合并（文件服务在线、回归全绿）。
> 构建命令统一使用 `mvn -s /home/lam/repo/settings.xml`；本地全栈用 `bash scripts/dev-init.sh` 拉起。
> 顺序不可调整：删除代码是**不可逆点**，必须排在数据校验与回归通过之后。

## 1. 文件服务独立 schema

- [ ] 1.1 `iboot-file/pom.xml` 增加 Flyway 依赖，`application.yml` 的数据源库名改为 `iboot_file`，并配置 `spring.flyway.locations=classpath:db/migration`；验证：`mvn -s /home/lam/repo/settings.xml -pl iboot-file -am -DskipTests package` 通过
- [ ] 1.2 新增 `iboot-file/src/main/resources/db/migration/V1__initial_schema.sql`：按 `iboot_admin.sys_file` 的定义创建 `sys_file`（字段、索引、字符集一致，含 `idx_file_name` / `idx_file_category` / `idx_upload_by`）；验证：启动服务后日志出现 `Migrating schema \`iboot_file\``，`SHOW CREATE TABLE iboot_file.sys_file` 与旧表结构逐列一致
- [ ] 1.3 数据源切换后先不迁移数据，确认文件列表为空且接口不报错；验证：经网关 `GET /api/file/list` 返回 `code=200`、`total=0`

## 2. 数据搬迁与校验

- [ ] 2.1 新增 `scripts/migrate/sys_file_to_iboot_file.sql`：建库（幂等）+ 复制数据（显式带 `id`），脚本头写明「只复制不删除、重跑前先 TRUNCATE」；验证：在本地库连续执行两次，第二次因 TRUNCATE 注释步骤执行后行数正确、无主键冲突
- [ ] 2.2 停后端执行迁移，校验两侧 `COUNT(*)`、`MAX(id)` 一致，并抽样比对 5 条记录的 `file_name` / `file_path` / `upload_time`；验证：校验查询结果完全一致，输出贴入本变更说明
- [ ] 2.3 启动全套服务，确认文件列表条数与迁移前相同、下载与导出可用；验证：经网关 `GET /api/file/list` 的 `total` 与迁移前一致，任取一条 `download-url` 返回 200

## 3. 删除管理端旧实现

- [ ] 3.1 删除管理端文件域实现：`FileController`、`FileApplicationService`、`FileInfo`、`FileRepository`、`FileRepositoryImpl`、`infrastructure/persistence/mapper/FileMapper`、`application/mapper/system/FileMapper`、`FilePO`、`mapper/FileMapper.xml`、`FileResponse`、`FileUpdateRequest`、`FileExportVO`；**保留** `StorageObject`、`FileStorageStrategy` 与三个存储实现、`FileStorageConfig`、`MinioProperties`、`OssProperties`；验证：`grep -rn "FileApplicationService\|FileInfo" iboot-admin/src/main` 无结果，`mvn -s /home/lam/repo/settings.xml -pl iboot-admin -am test-compile` 通过
- [ ] 3.2 删除对应的两个测试：`FileApplicationServiceTest`、`FileControllerDownloadTest`；保留 `FileStorageConfigTest` 与 `InMemoryFileStorageStrategy`；验证：`mvn -s /home/lam/repo/settings.xml -pl iboot-admin -am test` 通过
- [ ] 3.3 确认备份链路未受影响：`BackupApplicationService` / `RestoreDrillService` 仍编译通过，`BackupApplicationServiceTest`、`RestoreDrillServiceTest` 通过；验证：上述测试全绿
- [ ] 3.4 重启管理端，确认 OpenAPI 中不再出现 `/api/file/**`；验证：`curl -s http://localhost:8080/v3/api-docs | jq '.paths | keys[]' | grep -c '/api/file'` 输出 `0`

## 4. 回归登记、基线与演练

- [ ] 4.1 `scripts/regress/migrations.txt` 把 `/api/file admin file transitional` 改为 `done`；验证：`bash scripts/regress/path-conservation.sh` 在刷新基线前失败并指出「admin 仍有 7 条 /api/file 路径」——这证明确实读到了新状态
- [ ] 4.2 刷新基线：`bash scripts/regress/regress.sh --refresh`，确认 `golden/admin.json` 减少 7 条路径、`golden/file.json` 不变；验证：`jq '.paths | keys | map(select(startswith("/api/file"))) | length'` 在 admin 基线为 0、file 基线为 7
- [ ] 4.3 重录等价性基线并校验：`equivalence.sh --record` 后再 `--verify`；验证：四条只读请求一致
- [ ] 4.4 全量回归：`bash scripts/regress/regress.sh`；验证：退出码 0，路径守恒（`done` 状态通过）、路由覆盖（156 条）、冒烟 16 项、权限矩阵 14 项全通过
- [ ] 4.5 故障与追踪复跑：停 `iboot-file` 后 `/api/file/**` 返回 503，其余前缀正常；一次经网关的文件请求在 Tempo 中为 `iboot-gateway → iboot-file`；验证：两项结果与拆分时一致

## 5. 构建与文档

- [ ] 5.1 全量构建：`TESTCONTAINERS_RYUK_CONTAINER_IMAGE=testcontainers/ryuk:0.12.0 mvn -s /home/lam/repo/settings.xml verify`；验证：退出码 0，各模块测试数为 kernel 16 / file 5 / admin ≤ 253 / ai 223
- [ ] 5.2 更新 `doc/23 §11.3`：把「数据库 schema」「管理端旧实现」两行的状态改为已完成，登记「管理端 `sys_file` 保留一个周期」的退出条件；验证：表格内容与当前代码一致
- [ ] 5.3 更新 `ROADMAP.md` §4.2 拆分状态表：`iboot-file` 由「旧实现待删除」改为「已完成」，删除 `remove-file-from-admin` 待办标记；验证：状态与变更目录一致
