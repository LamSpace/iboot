# Tasks

## 1. 基线与依赖配置

- [x] 1.1 在仓库根记录改造前基线：执行
  `mvn -s /home/lam/repo/settings.xml clean verify`，确认退出码 0；并把
  `mvn -s /home/lam/repo/settings.xml dependency:tree` 输出保存到
  `/tmp/iboot-storage-dep-tree-before.txt`。验证：基线命令绿、文件非空，且确认当前没有
  `src/test/java` 下的测试文件。
- [x] 1.2 在父 POM `dependencyManagement` 中锁定 `com.aliyun.oss:aliyun-sdk-oss` 的当前稳定
  3.x 版本；在 `iboot-admin/pom.xml` 增加 OSS SDK，以及 test scope 的
  `org.testcontainers:junit-jupiter`、`org.testcontainers:testcontainers`。验证：
  `mvn -s /home/lam/repo/settings.xml -pl iboot-admin -am dependency:tree` 输出包含上述坐标，
  且 `mvn -s /home/lam/repo/settings.xml -pl iboot-admin -am -DskipTests compile` 退出码 0。
- [x] 1.3 新增 Flyway `V15__add_oss_config.sql`，按 `V3__add_minio_config.sql` 的写法插入
  `sys.file.oss.endpoint`、`sys.file.oss.bucketName`、`sys.file.oss.urlExpireSeconds` 三行，
  不修改 `sys.file.storageType` 的默认值。验证：启动一次应用（或对演练库执行 Flyway）后，
  `SELECT config_key FROM sys_config WHERE config_key LIKE 'sys.file.oss.%'` 返回 3 行，
  `flyway_schema_history` 含版本 15。

## 2. SPI 调整与调用方解耦

- [x] 2.1 把 `StorageObject` 从 `infrastructure/storage` 迁到 `domain/system/model`；
  `FileStorageStrategy.save` 改为 `save(StorageObject, String)`，并新增
  `boolean isObjectStorage()`；同步更新类/方法 Javadoc（中文说明 + 英文标签）。验证：
  `mvn -s /home/lam/repo/settings.xml -pl iboot-admin -am -DskipTests compile` 在补齐 2.2 后
  退出码 0，且 `grep -rn "org.springframework.web.multipart.MultipartFile"` 不再命中
  `domain/` 下的接口文件。
- [x] 2.2 更新 `LocalFileStorageStrategy` 与 `MinioFileStorageStrategy` 到新签名，实现
  `isObjectStorage()`（Local=false，MinIO=true）；删除 `LocalFileStorageStrategy.getFullPath()`；
  `FileApplicationService` 负责把 `MultipartFile` 转成 `StorageObject`，删除
  `LocalFileStorageStrategy` 字段、`getFileDiskPath()` 与 `isMinioStorage()`，改为暴露
  `isObjectStorage()`。验证：`compile` 退出码 0，且
  `grep -rn "LocalFileStorageStrategy\|isMinioStorage" iboot-admin/src/main/java/com/iboot/admin/application`
  无命中。
- [x] 2.3 调整 `FileController.download()`：对象存储走 302 预签名 URL；非对象存储从
  `getFileStream()` 取 `StorageObject`，用 `InputStreamResource` 写响应，文件名用
  `ContentDisposition.attachment().filename(name, UTF_8)` 生成。验证：`compile` 退出码 0，
  且 `grep -n "UrlResource\|URLEncoder" FileController.java` 无命中。
- [x] 2.4 更新 `doc/04-功能模块.md` 的"后端实现 - MinIO 服务"表，把 `MinioService` 替换为当前
  Strategy 结构；更新 ROADMAP 0.7 的过时表述（`MinioService`/`StorageProvider`），改为
  "补 OSS/内存实现并把存储选择完全配置化"。验证：`grep -rn "MinioService" doc/ ROADMAP.md`
  无命中，`git diff` 仅含预期文档段落。

## 3. OSS、内存实现与装配

- [x] 3.1 新增 `OssProperties`（endpoint/bucket/urlExpireSeconds 读 `sys_config`，AccessKey
  从 `IBOOT_OSS_ACCESS_KEY_ID`/`IBOOT_OSS_ACCESS_KEY_SECRET` 读取）；扩展
  `FileStorageConfig` 为 `local`/`minio`/`oss` 三选一，缺省 `local`，非法值抛
  `IllegalStateException`。验证：新增 `FileStorageConfigTest`，覆盖四种输入并断言实例类型与
  非法值异常，`mvn -s /home/lam/repo/settings.xml -pl iboot-admin test -Dtest=FileStorageConfigTest`
  通过。
- [x] 3.2 新增 `OssFileStorageStrategy`：按 design D6 映射 save/getFileStream/getFileUrl/delete/
  exists；`@PostConstruct` 校验配置、构建 `OSS`、`doesBucketExist`，不自动建桶。验证：新增
  `OssFileStorageStrategyTest`，用 Mockito mock `OSS` 接口断言 putObject 的 bucket/key/
  content-type、预签名调用与删除调用，测试通过。
- [x] 3.3 在测试源码树新增 `InMemoryFileStorageStrategy`（map + 元数据，`isObjectStorage=false`）。
  验证：`mvn -s /home/lam/repo/settings.xml -pl iboot-admin -DskipTests test-compile` 退出码 0，
  且该类位于 `src/test/java` 而非 `src/main/java`。
- [x] 3.4 在 `doc/04-功能模块.md` 补充 OSS 配置说明：三个 `sys_config` 键、两个环境变量、
  bucket 需预创建、最小 RAM 权限（指定 bucket 前缀的 PutObject/GetObject/DeleteObject/
  ListObjects）。验证：文档中出现 `IBOOT_OSS_ACCESS_KEY_ID` 与 `sys.file.oss.bucketName`，
  且不含任何真实密钥值。

## 4. 契约测试与应用层测试

- [x] 4.1 新增抽象 `FileStorageStrategyContractTest`（save→exists、读回字节/长度/contentType、
  覆盖写、删除已存在/不存在、URL 非空），并提供 Memory 与 Local（`@TempDir`）两个子类。验证：
  `mvn -s /home/lam/repo/settings.xml -pl iboot-admin test` 中两组契约测试全部通过。
- [x] 4.2 新增 MinIO 契约测试子类，用 Testcontainers 启动
  `minio/minio:RELEASE.2025-09-07T16-13-09Z`，通过 mock `MinioProperties` 注入 endpoint/密钥/
  bucket，跑同一组契约用例。验证：Docker 可用时
  `mvn -s /home/lam/repo/settings.xml -pl iboot-admin test -Dtest=Minio*ContractTest` 通过；
  无 Docker 时测试失败而非跳过（确认失败信息指向 Docker 不可用）。
- [x] 4.3 新增 `OssFileStorageStrategyContractTest`，标注 `@Tag("cloud")`，仅在
  `IBOOT_OSS_ACCESS_KEY_ID` 存在时启用；在父 POM 或 `iboot-admin/pom.xml` 的 surefire 配置中
  默认排除 `cloud` 组。验证：默认 `mvn test` 不执行该类；设置两组环境变量并执行
  `mvn -s /home/lam/repo/settings.xml -pl iboot-admin test -Dgroups=cloud -Dsurefire.excludedGroups=disabled`
  可运行（无凭据时记录"未执行云集成"）。
  > 记录（2026-10-20）：使用真实 OSS 凭据执行，6/6 通过；测试结束后自动清理本次创建的 5 个契约对象。
- [x] 4.4 新增 `FileApplicationServiceTest`：mock `FileRepository` 与 `ConfigApplicationService`，
  使用内存实现覆盖上传（存储+元数据）、删除（逻辑删除+物理删除）、下载 URL 与存储类型判断；
  断言空文件/超限/非法扩展名在调用存储前被拒绝。验证：测试通过，且用 Mockito `verifyNoInteractions`
  证明校验失败时未调用存储实现。

## 5. 集成验证

- [x] 5.1 在仓库根执行 `mvn -s /home/lam/repo/settings.xml clean verify`。验证：退出码 0；
  测试报告中 Memory、Local、MinIO 三组契约测试均为通过（cloud 组被排除）。
- [x] 5.2 本地模式 E2E：以 `sys.file.storageType=local` 启动应用，使用 portal 或 curl 完成
  上传→列表→下载→删除，并下载一个中文文件名文件。验证：下载内容字节一致、文件名正确，
  删除后记录与磁盘文件均消失。
- [x] 5.3 MinIO 模式 E2E：docker compose 启动 MySQL/Redis/MinIO，把 `storageType` 改为
  `minio` 后重启，重复 5.2 的流程。验证：`GET /api/file/download/{id}` 返回 302 且
  Location 可直接下载，删除后 MinIO 对象消失。
- [x] 5.4 条件式 OSS 冒烟：仅当存在真实 OSS 凭据时，预创建 bucket、配置环境变量并把
  `storageType` 改为 `oss`，重复 5.2 的流程；无凭据时记录"未执行 OSS 真机冒烟"，
  以 4.3 的 mock 单测与 5.3 的对象存储 E2E 作为等价证据。验证：若执行，则 302 预签名 URL
  可下载且删除后对象消失。
  > 记录（2026-10-20）：已执行真实 OSS E2E：上传 → `/api/file/download/{id}` 返回 302 预签名 URL
  > → 下载内容一致 → 删除后同一 URL 返回 404。测试 bucket 中 `contract/` 前缀对象已清理为 0。
- [x] 5.5 确认变更范围：执行 `git status --short` 与 `git diff --stat`。验证：仅包含 POM、
  Java 源码/测试、`V15` 迁移、`doc/04`、`ROADMAP.md` 与本变更目录；无前端改动、无密钥值。
