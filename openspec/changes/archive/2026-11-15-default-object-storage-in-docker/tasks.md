# Tasks

## 1. 默认值与数据库迁移

- [x] 1.1 新增 `iboot-db-init/src/main/resources/db/migration/V8__default_storage_type_silo.sql`，把 `sys.file.storageType` 的 `local` 无条件更新为 `silo`，并同步更新该配置的 remark；验证：在 Testcontainers MySQL 上从空库执行迁移后查得 `silo`，从 V7 状态（值为 `local`）继续执行 V8 后同样查得 `silo`。
- [x] 1.2 更新 `SchemaMigratorTest` 的迁移数量与断言，覆盖 `sys.file.storageType = silo`；同时修正现有 `sys.file.silo.%` LIKE 断言未排除 endpoint 导致的矛盾；验证：`mvn -pl iboot-db-init test -Dtest=SchemaMigratorTest` 通过。

## 2. 配置覆盖层

- [x] 2.1 在 `iboot-file`、`iboot-job`、`iboot-system`、`iboot-monitor` 的 `SiloProperties` 中增加 `IBOOT_FILE_SILO_ENDPOINT`、`IBOOT_FILE_SILO_ACCESS_KEY`、`IBOOT_FILE_SILO_SECRET_KEY`、`IBOOT_FILE_SILO_BUCKET` 的 `@Value` 覆盖字段，非空时优先于 `sys_config`；验证：新增单元测试分别覆盖“env 存在时取 env”“env 为空时取 DB”“两者都为空时取默认值”。
- [x] 2.2 在 `iboot-file`、`iboot-job`、`iboot-system` 的 `FileStorageConfig` 中支持 `IBOOT_FILE_STORAGE_TYPE` 覆盖存储类型；验证：扩展 `FileStorageConfigTest`，断言 env 值优先、非法 env 值启动失败并包含原值。
- [x] 2.3 在 `iboot-file` 的 `SiloProperties` 增加 `IBOOT_FILE_SILO_PUBLIC_ENDPOINT`，未配置时回退到内网 endpoint；验证：单元测试覆盖“配置了公网地址”“未配置时等于内网地址”两种结果。
- [x] 2.4 更新 `docs/10-后端配置详解.md` 的存储章节，写明环境变量与 `sys_config` 的优先级和变量清单；验证：按文档设置变量后 `SiloProperties` 的生效值与文档一致。

## 3. 预签名双客户端

- [x] 3.1 重构 `SiloFileStorageStrategy`：内网客户端负责保存、读取、删除和桶检查，公网客户端只负责 `getPresignedObjectUrl`；两个地址相同时复用同一客户端；验证：单元测试断言上传走内网 endpoint，预签名 URL 的 host 为公网 endpoint。
- [x] 3.2 启动日志打印最终生效的存储类型、内网 endpoint 与公网 endpoint；验证：启动日志中能看到三个值，且不包含 secret key。

## 4. Docker 编排与部署文档

- [x] 4.1 在 `docker/docker-compose.yml` 的 `x-app-env` 增加 `IBOOT_FILE_STORAGE_TYPE`、`IBOOT_FILE_SILO_ENDPOINT`、`IBOOT_FILE_SILO_PUBLIC_ENDPOINT`，并为 `iboot-system`、`iboot-job` 补 `silo: condition: service_healthy`；验证：`docker compose -f docker/docker-compose.yml config` 解析成功，且三个服务的环境变量与依赖符合预期。
- [x] 4.2 更新 `.env.example`、`docs/05-快速开始.md` 与 README 的存储说明，写明宿主机默认值、容器内覆盖值、切换后需重启的服务以及存量本地文件不自动迁移；验证：按文档从干净检出执行快速开始命令，上传文件落到 Silo。

## 5. 集成验证

- [x] 5.1 Docker 全量验证：`docker compose up -d --wait` 后上传文件，执行 `docker exec iboot-silo mcli ls mysilo/iboot-files` 能看到对象；请求 `GET /api/file/download/{id}` 返回 302 且 `Location` 指向 `http://localhost:9000`，浏览器可直接下载；验证：三项结果全部符合。
- [x] 5.2 宿主机开发验证：执行 `scripts/dev-init.sh`，确认 `iboot-file`、`iboot-system`、`iboot-job` 正常启动，上传与下载都使用 `http://localhost:9000`；验证：服务日志与 Silo 桶内容一致。
- [x] 5.3 回滚验证：设置 `IBOOT_FILE_STORAGE_TYPE=local` 并重启三个服务后，上传重新落本地目录且下载走应用流式响应；验证：文件出现在 `uploads` 卷/目录，`/api/file/download/{id}` 不再 302。

## Workflow follow-up

- 完成后按项目评审要求归档该 change。
- 归档后确认 `openspec/specs/file-storage/spec.md` 已包含新增的三条 requirement。
