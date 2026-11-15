# Proposal

## Why

Docker Compose 全量启动时 Silo 容器已经常驻，但文件服务仍默认使用本地磁盘：`sys_config` 的 `sys.file.storageType` 种子值是 `local`，代码缺省值也是 `local`。同时，容器内 endpoint 若沿用 `localhost:9000` 会指向容器自身，而预签名下载 URL 又必须能被浏览器访问，单一地址无法同时满足服务端读写与浏览器下载。结果是“对象存储服务起来了，但系统从未使用它”，与生产环境形态不一致。

## What Changes

- 将 `sys.file.storageType` 的默认值从 `local` 改为 `silo`，使全新环境（含 Docker 全量演示）默认使用对象存储；宿主机开发仍可通过环境变量显式切回 `local`。
- 为存储配置增加环境变量覆盖层：存储类型、内网 endpoint、访问密钥、桶名、预签名对外 endpoint 均 env 优先、`sys_config` 兜底。
- 文件服务区分“服务端读写地址”和“浏览器预签名地址”：读写使用内网服务名，预签名使用宿主机/公网可达地址；预签名使用独立客户端，避免 SigV4 签名地址被改写后失效。
- Docker 编排注入对象存储默认值与内网 endpoint，并为启动期需要建连 `iboot-system`、`iboot-job` 补充 Silo 健康依赖。
- 更新部署与配置文档，写明切换存储类型需重启相关服务、存量本地文件不会自动迁移。
- **BREAKING**：默认存储从本地切换为 Silo 后，切换前已落在本地上传目录的文件不会自动出现在对象存储中；需要保留旧文件的环境应在切换前完成迁移，或显式把存储类型改回 `local`。

## Capabilities

### New Capabilities

无。

### Modified Capabilities

- `file-storage`: 存储提供方选择增加环境变量优先级的约定；新增内网 endpoint 与对外预签名 endpoint 的区分；明确对象存储下载 URL 必须使用浏览器可达地址签名。

## Impact

- 后端：`iboot-file`、`iboot-job`、`iboot-system`、`iboot-monitor` 的 `SiloProperties`；三个服务的 `FileStorageConfig`；`iboot-file` 的 `SiloFileStorageStrategy`。
- 数据：`iboot-db-init` 新增迁移，修改 `sys_config.sys.file.storageType` 默认值。
- 部署：`docker/docker-compose.yml` 的公共环境变量与 `iboot-system`、`iboot-job` 的依赖顺序；`.env.example` 增加宿主机默认值说明。
- 文档：快速开始、后端配置详解与 README 的存储章节。
- 测试：存储类型选择、endpoint/publicEndpoint 解析优先级与预签名客户端行为。
