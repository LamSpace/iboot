# Proposal

## Why

ROADMAP 阶段 0.7 要求把文件存储抽成可替换的衔接性抽象，为阶段 3 的 MinIO→阿里云 OSS 替换铺路。
代码中已经有 `FileStorageStrategy` 接口与 Local/MinIO 两个实现，但抽象尚未完成：没有 OSS 实现，
没有可用于测试的内存实现，全项目零测试，且调用方仍依赖具体实现（`FileApplicationService` 注入
`LocalFileStorageStrategy`、`FileController` 按 `isMinioStorage()` 分支），domain 接口反向依赖
Spring Web 与 infrastructure 类型。按现状进入阶段 3，OSS 替换仍要改动文件相关代码，也无法自动
验证"不同实现对同一操作行为一致"这条验收判据。

## What Changes

- 保留 `FileStorageStrategy` 名称，新增 `isObjectStorage()`，替代调用方对 `isMinioStorage()` 的
  判断；对象存储实现（MinIO/OSS）走预签名 URL 302，本地/内存实现走文件流。
- 把 `save(MultipartFile, String)` 改为 `save(StorageObject, String)`，并将 `StorageObject` 从
  infrastructure 迁到 domain，使 domain 接口不再依赖 Spring Web。
- 新增 `OssFileStorageStrategy`，基于阿里云 `aliyun-sdk-oss`，实现 save/getFileUrl/getFileStream/
  delete/exists/getStorageType；不自动创建 bucket。
- 新增 `InMemoryFileStorageStrategy`，仅放在测试源码树，供契约测试与应用服务单测使用。
- 新增 `OssProperties`，扩展 `FileStorageConfig` 的启动期选择逻辑：`local` / `minio` / `oss`。
- OSS 的 endpoint、bucketName、urlExpireSeconds 写入 `sys_config`（Flyway `V15`）；AccessKeyId/
  AccessKeySecret 只从环境变量读取，不入库、不入仓。
- 移除 `FileApplicationService` 对具体类 `LocalFileStorageStrategy` 的注入与 `getFileDiskPath()`；
  `FileController` 对非对象存储改为从 `getFileStream()` 写响应流。
- 引入契约测试：同一组用例分别跑内存、本地、Testcontainers MinIO；OSS 用 mock `OSS` 接口做单测，
  真实 OSS 测试以 `@Tag("cloud")` 默认排除。
- 同步 `doc/04-功能模块.md` 的存储实现说明与 ROADMAP 0.7 的过时表述。

## Capabilities

### New Capabilities

- `file-storage`: 定义文件存储提供方的选择规则、对象存储与本地存储的下载语义差异、配置与密钥边界，
  以及所有实现必须满足的行为一致性契约。

### Modified Capabilities

无。现有 `portal-design-system` 与本变更无关。

## Impact

- 代码：
  - `domain/system/service/FileStorageStrategy.java`、`domain/system/model/StorageObject.java`（迁移）
  - `infrastructure/storage/{Local,Minio,Oss}FileStorageStrategy.java`、`OssProperties.java`
  - `infrastructure/config/FileStorageConfig.java`
  - `application/service/FileApplicationService.java`
  - `interfaces/controller/FileController.java`
  - 新增 `src/test/java/**`（项目当前没有任何测试）
- 依赖：父 POM 新增 `com.aliyun.oss:aliyun-sdk-oss` 版本管理；`iboot-admin` 新增 OSS SDK；
  测试范围新增 Testcontainers（`junit-jupiter` + MinIO 容器）。
- 配置：新增 Flyway `V15`（OSS 的 endpoint/bucket/urlExpireSeconds 配置项）；新增环境变量
  `IBOOT_OSS_ACCESS_KEY_ID`、`IBOOT_OSS_ACCESS_KEY_SECRET`。
- API：`/api/file/**` 路径与响应结构不变；`storageType` 取值新增 `oss`。前端 `FileManageView`
  继续使用 `/api/file/download/{id}`，无需改动。
- 运行时：`storageType=minio|oss` 时启动需要对应存储可达（沿用 MinIO 现有快速失败行为）。
- 不在本次范围：MinIO 监控（`MinioAdminService` / `MinioMonitorController`）的 OSS 适配；
  上传成功但数据库写入失败产生孤儿对象的存量问题。
