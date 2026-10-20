# Design

## Context

参见 `proposal.md` 的 Why。当前实现事实（2026-10-19 核查）：

- `FileStorageStrategy`（`domain/system/service`）定义 save/getFileUrl/getFileStream/delete/exists/
  getStorageType 六个方法，Local 与 MinIO 两个实现位于 `infrastructure/storage`。
- `FileStorageConfig` 在启动时读取 `sys.file.storageType`，只有 `minio` 走 MinIO，其余值一律走本地。
- `FileApplicationService` 同时注入接口和具体类 `LocalFileStorageStrategy`，并通过
  `getFileDiskPath()` 把本地磁盘路径暴露给 Controller。
- `FileController.download()` 用 `isMinioStorage()` 判断：MinIO 返回 302 预签名 URL，本地用
  `UrlResource` 写文件。
- `FileStorageStrategy` 依赖 Spring Web 的 `MultipartFile`，并依赖 infrastructure 的
  `StorageObject`，domain 层存在反向依赖。
- 项目没有任何测试源码；`spring-boot-starter-test` 与 `spring-security-test` 已在
  `iboot-admin/pom.xml` 声明但未使用。MinIO 8.5.7 已由父 POM 管理。

## Goals / Non-Goals

**Goals:**

- 文件存储提供方成为启动期可配置的单选实现，调用方不再感知 MinIO/OSS 差异。
- MinIO 与 OSS 共用一套可自动执行的行为契约；内存实现让契约测试不依赖外部服务。
- domain 层的存储接口不再依赖 Web 或 infrastructure 类型。
- 现有 `/api/file/**` 契约与前端行为不变。

**Non-Goals:**

- 不做 MinIO 监控（`MinioAdminService` / `MinioMonitorController`）的 OSS 适配。
- 不做已有对象的跨提供方迁移（MinIO 中的历史文件不会自动复制到 OSS）。
- 不修复"先写存储、后写数据库导致孤儿对象"的存量一致性问题。
- 不引入运行期动态切换存储提供方；切换通过重启生效。
- 不改前端 `iboot-portal`。

## Decisions

### D1：保留 `FileStorageStrategy` 名称，不重命名为 `StorageProvider`

- 理由：接口与两个实现已经存在，重命名只产生跨 5 个文件的机械 diff，不改变任何行为；ROADMAP
  中的 `StorageProvider` 是抽象存在之前的命名。同步修正 ROADMAP 0.7 与 `doc/04` 的表述即可。
- 备选：按路线图重命名。收益仅是与文档用词一致，代价是 review 噪声和 merge 冲突面。

### D2：`save` 改为接收 `StorageObject`，并把 `StorageObject` 迁入 domain

- `FileStorageStrategy.save(MultipartFile, String)` → `save(StorageObject, String)`。
- `StorageObject` 从 `infrastructure/storage` 移到 `domain/system/model`，继续承载
  inputStream/size/contentType/fileName 四个字段。
- `FileApplicationService.uploadFile` 负责把 `MultipartFile` 转换成 `StorageObject`。
- 理由：domain 接口不应依赖 Spring Web；`StorageObject` 已经是"待写入/已读出的内容 + 元数据"
  的自然载体，复用它比新增 `StorageContent` 更收敛。改动只有一个调用点。
- 备选：保留 `MultipartFile`。改动最小，但把 Web 依赖留在 domain，阶段 2 拆 `iboot-file` 时仍要还债。
- 备选：`save(InputStream, long, String, String)`。参数列表易错，且与读取侧不对称。

### D3：用 `isObjectStorage()` 表达下载语义，而不是判断提供方 ID

- 接口新增 `boolean isObjectStorage()`；MinIO/OSS 返回 `true`，Local/InMemory 返回 `false`。
- `FileApplicationService` 删除 `isMinioStorage()`，改为委托 `isObjectStorage()`；
  `getStorageType()` 仍保留，仅用于展示与响应。
- 理由：调用方关心的是"能否重定向到预签名 URL"，不是"是不是 MinIO"。阶段 3 换 OSS 时
  Controller 无需再改。
- 备选：返回 `StorageType` 枚举。语义更强，但会改动 Controller 与前端响应类型，收益有限。

### D4：提供方在 `FileStorageConfig` 启动期选择，非法值快速失败

```
local → LocalFileStorageStrategy
minio → MinioFileStorageStrategy
oss   → OssFileStorageStrategy
缺省  → local
其他  → 抛 IllegalStateException，启动失败
```

- 理由：`sys.file.storageType` 存在数据库，Spring Environment 在 `@ConditionalOnProperty` 阶段
  拿不到该值，因此继续沿用现有 `@Bean` 工厂方式。静默回退到 local 会让"配置成 OSS 却写本地磁盘"
  这类事故难以发现。
- 行为变化：现状对任何非 `minio` 值都走本地；改为非法值启动失败。部署前需确认存量配置值只有
  `local`/`minio`。
- 备选：`@ConditionalOnProperty` + 环境变量。会与现有管理端配置方式冲突。

### D5：OSS 配置分两层，密钥只走环境变量

| 配置 | 来源 | 键 |
|---|---|---|
| 提供方 | `sys_config` | `sys.file.storageType`（已存在） |
| endpoint | `sys_config` | `sys.file.oss.endpoint` |
| bucket | `sys_config` | `sys.file.oss.bucketName` |
| 预签名有效期 | `sys_config` | `sys.file.oss.urlExpireSeconds`（默认 3600） |
| AccessKeyId | 环境变量 | `IBOOT_OSS_ACCESS_KEY_ID` |
| AccessKeySecret | 环境变量 | `IBOOT_OSS_ACCESS_KEY_SECRET` |

- 新增 `OssProperties`，读取方式与 `MinioProperties` 一致（`ConfigApplicationService`），
  密钥通过 `System.getenv` 读取。
- 理由：endpoint/bucket 属于可运营参数，放进管理端配置符合现有模式；密钥按 ROADMAP §6.3
  不落库、不入仓。`OssProperties` 在 `oss` 被选中且密钥缺失时抛出带变量名的异常。
- 备选：全部走环境变量。部署更简单，但与 MinIO 的配置入口不一致，管理端无法看到 endpoint。
- 备选：全部进 `sys_config`。AccessKey 会以明文存库，违反成本红线。

### D6：`OssFileStorageStrategy` 的 SDK 映射

依赖 `com.aliyun.oss:aliyun-sdk-oss`，版本在父 POM `dependencyManagement` 锁定。使用
`OSSClientBuilder` 构建 `com.aliyun.oss.OSS` 接口实例；`OSS` 是接口，便于 Mockito 单测。

| SPI 方法 | OSS SDK 调用 |
|---|---|
| `save` | `putObject(bucket, key, inputStream, ObjectMetadata)`，设置 contentLength/contentType |
| `getFileStream` | `getObject` → `OSSObject.getObjectContent()` + `getObjectMetadata()` |
| `getFileUrl` | `generatePresignedUrl(bucket, key, expiration)` |
| `delete` | `deleteObject` |
| `exists` | `doesObjectExist` |
| `getStorageType` | `"oss"` |
| `isObjectStorage` | `true` |

- 初始化（`@PostConstruct`）：校验 endpoint/bucket/密钥非空 → 构建客户端 → `doesBucketExist`。
  桶不存在直接失败并报出桶名，**不调用 `createBucket`**。
- 理由：云上 bucket 通常由 IaC/控制台预创建，自动建桶会要求额外权限并掩盖配置错误。
- 备选：与 MinIO 一样自动建桶。会扩大 AK 权限面，且与阶段 3 的声明式资源管理冲突。

### D7：内存实现只存在于测试源码树

`src/test/java/com/iboot/admin/infrastructure/storage/InMemoryFileStorageStrategy.java`。

- 使用 `ConcurrentHashMap<String, byte[]>` + contentType 元数据；`save` 读完输入流后落 map；
  `getFileUrl` 返回 `memory://<path>`；`isObjectStorage` 返回 `false`。
- 理由：它不是生产提供方，放进 `main` 会被打进 jar，也容易被误配置启用。
- 备选：`@Profile("test")` 的生产实现。仍需进 jar，且契约测试要启动 Spring 上下文，成本更高。

### D8：下载路径统一，移除具体实现依赖

- `FileApplicationService` 删除 `LocalFileStorageStrategy` 字段、`getFileDiskPath()` 与
  `isMinioStorage()`；新增 `isObjectStorage()`。
- `FileController.download()`：
  - `isObjectStorage()` 为 true → 302 到 `getFileUrl()`；
  - 否则 → `getFileStream()` 返回 `StorageObject`，用 `InputStreamResource` 写响应，content-type
    取 `StorageObject.contentType`，`Content-Disposition` 用 Spring `ContentDisposition.attachment()
    .filename(name, UTF_8)` 生成 RFC 5987 编码。
- `LocalFileStorageStrategy.getFullPath()` 在失去唯一调用点后删除。
- 理由：两条下载路径由 SPI 的语义开关决定，Controller 不再出现存储提供方名称；Spring 的
  `ContentDisposition` 替代手写 `URLEncoder`，避免 `+`/空格编码差异。
- 注意：本地模式响应由 `UrlResource` 改为 `InputStreamResource`，需在回归中确认下载文件名与
  内容长度不退化。

### D9：契约测试统一驱动四个实现

- 抽象基类 `FileStorageStrategyContractTest` 定义用例：save→exists、读回字节/长度/contentType、
  覆盖写、删除已存在/不存在、URL 非空。
- 子类：
  - `InMemory...ContractTest`：始终运行；
  - `Local...ContractTest`：JUnit `@TempDir`，始终运行；
  - `Minio...ContractTest`：Testcontainers 启动 `minio/minio:RELEASE.2025-09-07T16-13-09Z`，
    走真实网络调用；
  - `Oss...Test`：Mockito mock `OSS` 接口，只验证调用映射（putObject 的参数、预签名、删除）；
    真实 OSS 契约测试用 `@Tag("cloud")` 标记，默认不执行，通过
    `-Dgroups=cloud` + 环境变量显式启用。
- Testcontainers 缺少 Docker 时**失败而不是跳过**，避免 CI 在未真正验证 MinIO 的情况下变绿。
- MinIO 测试通过 Mockito mock `MinioProperties` 提供容器 endpoint/密钥/bucket。
- 应用层单测 `FileApplicationServiceTest` 使用内存实现 + mock `FileRepository`，覆盖上传、
  删除、下载 URL 与存储类型判断。
- `FileStorageConfigTest` 验证 `local`/`minio`/`oss`/非法值四种选择结果。

### D10：Flyway `V15` 只做增量配置

- 按 `V3__add_minio_config.sql` 的写法插入三行 `sys_config`：OSS endpoint、bucket、
  urlExpireSeconds；不改 `sys.file.storageType` 的默认值 `local`。
- 理由：纯增量，不触碰既有数据；回滚时多出的配置行不影响运行。

## Risks / Trade-offs

- [Testcontainers 依赖 Docker，开发者机器或 CI 未安装时 MinIO 契约测试失败] → 在 README/变更
  说明中列为测试前置；无 Docker 时仍可运行内存与本地契约测试，但 `verify` 不会绿，避免虚假通过。
- [`aliyun-sdk-oss` 引入旧版 HTTP 客户端或日志依赖，与 Boot 4 依赖树冲突] → 实施后执行
  `mvn dependency:tree` 对比，必要时对 `commons-logging`/`httpclient` 做排除。
- [非法 `storageType` 从静默本地回退改为启动失败，属行为变化] → 上线前核对 `sys_config` 值只有
  `local`/`minio`；在迁移说明中标注。
- [OSS 历史对象不随提供方切换迁移] → 明确列为 Non-Goal；阶段 3 需要单独的数据迁移方案。
- [本地下载从 `UrlResource` 改为 `InputStreamResource` 可能影响 content-length 或文件名] →
  回归用例显式断言 Chinese 文件名与字节长度。
- [OSS `doesBucketExist`/`doesObjectExist` 需要 `oss:GetObject` 权限] → 在文档中给出最小 RAM
  策略：指定 bucket 前缀的 PutObject/GetObject/DeleteObject/ListObjects。
- [上传成功但数据库失败产生孤儿对象] → 存量问题，不在本变更修复；测试只断言当前行为，不固化
  为"正确行为"。

## Migration Plan

1. 基线：在 `iboot-admin` 下执行 `mvn -s /home/lam/repo/settings.xml verify`，记录退出码。
2. 增加依赖与 `V15`，保持运行期行为不变（存储选择仍为 `local` 时）。
3. 完成 SPI 调整、OSS/内存实现、调用方解耦与契约测试；分别用 `local`、`minio` 启动冒烟。
4. OSS 切换：预创建 bucket、配置 RAM 子账号、设置两个环境变量、把 `sys.file.storageType`
   改为 `oss` 后重启。
5. 回滚：代码 `git revert`；`V15` 为增量配置，可保留；把 `storageType` 改回 `local`/`minio`
   即可恢复，无需数据回滚。

## Open Questions

- `aliyun-sdk-oss` 的具体小版本在实施时依据 Maven Central 当前稳定 3.x 选定并锁定；不改变本设计。
