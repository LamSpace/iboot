# iBoot Docker 全栈部署

本目录的 `docker-compose.yml` 负责编排全部服务；构建与启动由仓库根目录的
`scripts/docker-deploy.sh` 串联。整个流程从干净检出开始，只依赖 Docker 与镜像站，
不需要宿主机预先生成 jar 或 dist。

## 前置要求

- Docker 24+，Docker Compose v2.20+
- 可访问 `swr.cn-north-4.myhuaweicloud.com`（基础镜像）
- 可访问 `maven.aliyun.com` 与 `registry.npmmirror.com`（构建依赖）
- 根目录 `.env`（模板见 `.env.example`），至少包含 `DASHSCOPE_CHAT_API_KEY`
- **内存**：核心栈建议 Docker 可用内存 12GB 以上；默认包含可观测性组件时建议 16GB+。
  当前环境已调整为 23.47GiB。各服务堆配置为 system/ai 1GB、其余 512m。

## 一键启动

```bash
# 在仓库根目录执行
scripts/docker-deploy.sh up
```

该命令依次完成：

1. 停止宿主机上由 `scripts/dev-init.sh` 启动的进程，避免端口与 fat jar 冲突；
2. 检查并拉取构建/运行期基础镜像；
3. 构建共享 Maven builder 镜像（整个 reactor 只构建一次）；
4. 构建全部后端与前端运行期镜像；
5. `docker compose up -d` 启动，然后由脚本轮询健康状态；`db-init`、`minio-init`
   这类一次性服务以退出码 0 视为完成。

首次构建需要下载 Maven 与 npm 依赖，通常 10–15 分钟；后续构建使用 BuildKit
cache mount，通常 3–5 分钟。

## 常用命令

```bash
scripts/docker-deploy.sh up                  # 核心 + 可观测性（默认）
scripts/docker-deploy.sh up core             # 只启动核心组件
scripts/docker-deploy.sh up graph            # 核心 + Neo4j
scripts/docker-deploy.sh up full             # 核心 + 可观测性 + Neo4j
scripts/docker-deploy.sh status              # 查看状态
scripts/docker-deploy.sh logs iboot-system   # 查看某个服务日志
scripts/docker-deploy.sh down                # 停止并移除容器，保留数据卷
scripts/docker-deploy.sh purge --yes         # 删除容器与数据卷（不可恢复）
```

`docker/deploy.sh` 仍保留为兼容入口，等价于 `scripts/docker-deploy.sh`。

## 构建流程

```text
scripts/pull-docker-images.sh
        │  maven / node / temurin / nginx 基础镜像
        v
docker/Dockerfile.backend-builder
        │  mvn -B -DskipTests package（一次构建全部模块）
        v
iboot-backend-builder:local
        │
        ├─ docker/Dockerfile.backend  (ARG MODULE=iboot-system)
        ├─ ... 其余后端模块
        └─ docker/Dockerfile.dbinit

iboot-portal/Dockerfile
        │  node:22-bookworm-slim 构建 dist
        v
nginx:stable-alpine3.23
```

- Maven 依赖走 `docker/maven/settings.xml` 中的阿里云 mirror；该文件只含公共镜像地址，
  不包含任何凭据。宿主机 `/home/lam/repo/settings.xml` 含发布凭据，禁止复制进镜像。
- npm 使用 `https://registry.npmmirror.com`，不依赖宿主机 `cnpm`。
- 运行期镜像只包含 JRE、对应服务的 jar 与 `script/run-java.sh`，不包含 Maven/JDK 工具链。

## 服务与端口

| 服务 | 端口 | 说明 |
|---|---|---|
| iboot-portal | 80 | 前端门户（nginx） |
| iboot-gateway | 8088 | 统一网关 |
| iboot-system | 8080 | 系统服务 |
| iboot-ai | 8081 | AI 服务 |
| iboot-file | 8082 | 文件服务 |
| iboot-auth | 8083 | 认证服务 |
| iboot-notify | 8084 | 通知服务 |
| iboot-job | 8085 | 调度与备份 |
| iboot-workflow | 8086 | 审批工作流 |
| iboot-monitor | 8087 | 监控与日志查询 |
| iboot-ops | 8089 | 运维控制台（SBA） |
| mysql | 3306 | MySQL 8.0 |
| redis | 6379 | Redis 7 |
| minio | 9000/9001 | 对象存储与控制台 |
| elasticsearch | 9200 | 检索 |
| logstash | 5000/5044 | 集中日志接收 |
| nacos | 8848/8849/9848/9849 | 注册/配置中心，控制台在 8849 |
| tempo | 4317/4318/3200 | 链路追踪 |

可观测性组件（默认启动；`up core` 可只启动核心）：

| 服务 | 端口 |
|---|---|
| prometheus | 9090 |
| grafana | 3000 |
| alertmanager | 9093 |
| kibana | 5601 |
| thanos-sidecar / store / query / compact | 19191 / 19193 / 19192 / 19194 |
| mysql/redis/node/elasticsearch exporter | 9104 / 9121 / 9100 / 9114 |

图数据库 profile：Neo4j `7474`（HTTP）、`7687`（Bolt）。

## 启动顺序

```text
mysql (healthy)
  └─> db-init (exit 0)
        └─> iboot-system (healthy)
              ├─> 其余业务服务
              ├─> iboot-gateway (healthy) ──> iboot-portal
              └─> iboot-ops

redis / nacos ──> 所有后端服务
minio ──> iboot-file / iboot-ai
elasticsearch ──> iboot-notify / iboot-monitor / iboot-ai
logstash ──> 旁路，不作为启动依赖
```

- `db-init` 的 `restart` 为 `no`；迁移失败时它以非零退出，所有后端服务都不会启动。
- 后端健康检查使用 `bash` 的 `/dev/tcp` 端口探测，不依赖镜像内的 curl/wget。
- `iboot-system` 健康后其余服务才启动，因为它们启动时要读取共享库中的配置。
- Nacos 在核心启动中常驻，服务间 `@LoadBalanced` 调用与 SBA 实例发现都依赖它。

## 环境变量

- 应用服务通过 `env_file: ../.env` 读取 DashScope、邮件、短信、通知 token、备份密钥等私有配置。
- compose 的 `environment` 会覆盖 `.env` 中的宿主机地址，例如：
  - `MYSQL_HOST=mysql`、`REDIS_HOST=redis`、`NACOS_ADDR=nacos:8848`
  - `ELASTICSEARCH_URIS=http://elasticsearch:9200`
  - `TEMPO_OTLP=http://tempo:4318/v1/traces`
  - `JWT_JWK_SET_URI=http://iboot-auth:8083/oauth2/jwks`
- 网关使用 `SPRING_PROFILES_ACTIVE=nacos`。
- 认证回调为 `http://localhost/callback` 与 `http://localhost/login`；授权服务器地址仍为
  `http://localhost:8088`（浏览器经宿主机端口访问）。
- 认证服务的签名密钥通过 `IBOOT_AUTH_JWK_FILE=/app/keys/iboot-auth-jwk.json` 写入
  `auth_keys` 卷。不要删除该卷，否则所有已签发令牌会在认证服务重启后失效。
- 本地全栈默认关闭运维控制台认证（`IBOOT_OPS_SECURITY_ENABLED=false`）；生产部署必须开启
  并注入 `IBOOT_OPS_PASSWORD`。
- Docker 部署默认关闭 `iboot-job` 备份（`IBOOT_BACKUP_ENABLED=false`），因为运行期镜像没有
  docker CLI，备份自检会失败。需要备份时应另行为该服务挂载 Docker Socket 并安装客户端。

### 切换到 MinIO 存储

默认 `sys.file.storageType=local`，文件写入共享的 `uploads_data` 卷。若在系统配置中改为
`minio`，还需要把 `sys.file.minio.endpoint` 从种子值 `http://localhost:9000` 改为
`http://minio:9000`；`sys.minio.console.url` 保持 `http://localhost:9001`，因为它是浏览器
访问的地址。监控服务的 MinIO 检查只在存储类型为 `minio` 时才真正连接。

## 卷

| 卷 | 用途 |
|---|---|
| `mysql_data` | MySQL 数据 |
| `redis_data` | Redis 数据 |
| `minio_data` | 对象存储 |
| `es_data` | Elasticsearch 数据 |
| `prometheus_data` / `grafana_data` / `alertmanager_data` | 监控数据 |
| `thanos_store_data` / `thanos_compact_data` | Thanos 长期存储 |
| `admin_logs` | 所有服务的日志（按应用名分文件） |
| `uploads_data` | system / file / job 共享的本地上传目录 |
| `auth_keys` | 认证服务 RSA 签名密钥；认证服务重启后旧令牌仍可验证 |
| `neo4j_data` / `neo4j_logs` | 图数据库 |
| `tempo_data` | 链路追踪 |

普通 `down` 不会删除卷；只有 `purge --yes` 会执行 `down -v`。

## 门户同源代理

门户 nginx 除 `/iboot/api` 外，还代理：

- `/iboot/ai` → 网关 `/api/ai/**`（知识库、智能体、对话，支持 SSE）；
- `/druid` → 系统服务的 Druid 数据库监控；
- `/swagger-ui.html`、`/v3/api-docs`、`/webjars` 等 → 系统服务的 Swagger UI；
- `/sba` → 运维控制台。

新增后端同源路径时必须同步补充代理，否则 SPA 回退会把 `index.html` 返回给 JSON 或 iframe 请求，表现为 `Unexpected token '<'` 或空白页。

AI 智能体默认开启（`IBOOT_AI_AGENT_ENABLED=true`）；初始智能体列表为空，需要先在“AI 工作台 → 智能体管理”中创建智能体。

## 故障排查

### 首次构建很慢或依赖下载失败

- 确认容器内可访问 `https://maven.aliyun.com` 与 `https://registry.npmmirror.com`。
- 构建缓存位于 BuildKit；不要让 `docker builder prune` 清理后立刻重建。
- 如果镜像站标签缺失，`docker compose build` 会尝试回退 Docker Hub 并失败；先执行
  `scripts/pull-docker-images.sh`，并确认 `library/` 前缀与脚本一致。

### `db-init` 失败

```bash
docker compose -f docker/docker-compose.yml logs db-init
docker compose -f docker/docker-compose.yml ps mysql
```

常见原因：MySQL 尚未健康、`MYSQL_*` 与实际容器不一致、迁移脚本本身失败。`db-init`
未以 0 退出时，后端服务不会启动。

### 服务一直不健康

```bash
docker compose -f docker/docker-compose.yml ps
docker compose -f docker/docker-compose.yml logs iboot-system
```

重点检查：Nacos 是否可注册、JWK 地址是否指向 `iboot-auth:8083`、数据库连接是否可用。
健康检查使用 TCP 端口探测；端口已开但业务 500 时，仍需看应用日志和 `/actuator/health`。

### 门户 502 或无法访问

确认 `iboot-gateway` 健康，且 `iboot-portal` 的 nginx 能解析 `iboot-gateway:8088`。
门户启动晚于网关由 `depends_on` 保证。

### 端口被占用

宿主机 `scripts/dev-init.sh` 与全栈 compose 不能同时运行。先执行
`scripts/dev-init.sh --stop`，或直接使用 `scripts/docker-deploy.sh up`（它会先停止宿主机进程）。

## 回滚到宿主机开发模式

```bash
scripts/docker-deploy.sh down
scripts/dev-init.sh
```

数据卷会保留；`dev-init.sh` 会继续使用同一个 MySQL 容器与数据库。若需要彻底清理，
使用 `scripts/docker-deploy.sh purge --yes`。

## 与旧部署方式的差异

- 旧 `docker/deploy.sh` 只构建 `iboot-system`，并假设 `docker-compose` v1；现在由
  `scripts/docker-deploy.sh` 统一处理全部模块与 compose v2。
- 旧的 10 个 `iboot-*/Dockerfile` 已由 `docker/Dockerfile.backend` 共享模板替代；
  `iboot-*/script/run-java.sh` 保持不变。
- 旧 compose 只编排基础设施，应用由宿主机启动；现在应用、门户、Nacos、Tempo 都在同一网络中。
