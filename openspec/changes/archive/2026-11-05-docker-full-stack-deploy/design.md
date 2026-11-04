# Design

## Context

当前部署由两部分拼成：`docker/docker-compose.yml` 只负责基础设施，`scripts/dev-init.sh` 负责在宿主机构建并启动 10 个后端进程；镜像构建依赖宿主机预生成的 `target/*.jar` 与 `iboot-portal/dist`。这种模式无法从干净检出复现，也无法表达服务间真实的启动依赖。

本次设计的约束来自实际排查：

- 宿主机 Maven 使用 `/home/lam/repo/settings.xml`，其中包含阿里云 mirror，也包含 `central` 的发布凭据，不能复制进镜像。
- 宿主机前端使用 `cnpm`，registry 为 `https://registry.npmmirror.com`；容器内可以访问该地址，但项目没有 `package-lock.json`。
- Docker Hub 在本机不可直连，基础镜像必须从现有华为云 SWR 镜像站拉取；镜像站对 `library/` 前缀的处理并不统一（`openjdk`/`nginx`/`maven`/`node` 不带，`eclipse-temurin` 必须带）。
- 服务之间的调用大量使用 `@LoadBalanced` + 无端口服务名（例如 `http://iboot-system/api/auth/scope`），依赖 Nacos 做服务发现；`iboot-ops` 的 Spring Boot Admin 也从 Nacos 拉实例。
- 多个服务默认写 `./uploads`，日志默认写 `logs/<应用名>.log`；容器内需要共享卷与非 root 可写目录。
- `iboot-job` 的备份默认命令是 `docker exec ... iboot-mysql mysqldump`，运行期镜像没有 docker CLI；一旦启用备份，启动自检会失败。
- `iboot-ai` 缺少 DashScope key 会直接退出；`iboot-ops` 开启认证且密码为空会直接退出。
- `openjdk`/`temurin` 的 slim 运行期镜像既不包含 `curl` 也不包含 `wget`。

## Goals / Non-Goals

**Goals:**

- 从干净检出开始，仅用 Docker/Compose 完成全部镜像构建与全栈启动。
- 后端只执行一次 Maven 构建，运行期镜像保持最小。
- 用健康检查与 `depends_on` 条件确定性表达启动顺序。
- 把容器网络地址、密钥、卷、日志与上传目录一次性配置正确。
- 保留 `script/run-java.sh` 作为统一启动入口。
- 核心启动与可观测性/图数据库组件解耦。

**Non-Goals:**

- 不引入 Kubernetes、Swarm 或外部编排平台。
- 不引入镜像仓库推送/签名、SBOM 生成或供应链扫描流程。
- 不引入 `package-lock.json`（本次继续使用 `npm install`；锁定依赖版本另立变更）。
- 不解决 `iboot-job` 在容器内通过 Docker Socket 执行备份的问题；Docker 部署默认关闭备份。
- 不改变业务代码、API 契约或数据库结构。
- 不追求生产级密钥管理；根 `.env` 仍用于本地/单机部署。

## Decisions

### 1. 多阶段构建：共享 builder 镜像 + 按模块运行期镜像

采用一个共享 builder 镜像完成整个 Maven reactor 构建，10 个运行期镜像只从 builder 拷贝各自的 jar。

```text
Dockerfile.backend-builder
  maven + settings.xml + cache mount
        │ 一次构建，产出全部 target/*.jar
        v
iboot-backend-builder:local
        │
        ├─ Dockerfile.backend  ARG MODULE=iboot-system
        ├─ Dockerfile.backend  ARG MODULE=iboot-auth
        ├─ ... 其余 8 个
        └─ Dockerfile.dbinit   （一次性任务）
```

**为什么不是每个模块各自多阶段**：10 个 Dockerfile 会各自执行一次多模块 Maven 构建，首次构建时间不可接受，且缓存利用率低。

**为什么不是单一全量运行期镜像**：镜像会包含全部服务的 jar，违背按服务独立部署的方向，也会放大镜像体积与攻击面。

**为什么不是宿主机构建**：用户希望从干净检出只依赖 Docker 完成构建；网络卡点已通过镜像配置与缓存解决。

`Dockerfile.backend` 使用 `ARG MODULE` 选择 jar 与 `script/run-java.sh`，差异仅限应用名、端口与 jar 文件名，符合修改后的 `service-runtime` 契约。

### 2. Maven 依赖：提交只含 mirror 的 settings.xml + BuildKit cache mount

新增 `docker/maven/settings.xml`，只包含：

- `aliyun` → `https://maven.aliyun.com/repository/public`
- `spring-aliyun` → `https://maven.aliyun.com/repository/spring`

不复制宿主机的 `settings.xml`，因为它包含 `central` 发布凭据。builder 阶段使用：

```dockerfile
COPY docker/maven/settings.xml /root/.m2/settings.xml
RUN --mount=type=cache,target=/root/.m2/repository,sharing=locked \
    mvn -B -DskipTests package
```

**替代方案**：使用 BuildKit secret 挂载宿主机 settings.xml。凭证不会进层，但构建会绑定宿主机路径、不可移植；本项目的 mirror 本身不含凭据，提交一份可移植配置更简单。

**可选加速**：如果首次下载仍然慢，可以把宿主机 `/home/lam/repo`（约 2.1GB）作为额外的 build context，先灌入 cache mount；该路径绑定本机，默认不启用。

### 3. 前端依赖：node 构建阶段 + npmmirror + npm cache

`iboot-portal/Dockerfile` 改为：

```text
node:22-bookworm-slim  → npm config set registry https://registry.npmmirror.com
                       → npm install
                       → npm run build
nginx:stable-alpine3.23 → COPY dist + nginx.conf
```

不在镜像中安装 `cnpm`；官方 npm 配置 npmmirror 后行为等价。项目暂无 `package-lock.json`，因此使用 `npm install` 而不是 `npm ci`；锁定依赖版本留作后续变更。

`iboot-portal/.dockerignore` 只排除 `node_modules` 与 `dist`，**不能排除 `.env`**，因为 Vite 构建需要 `VITE_API_BASE_URL` 与 `VITE_OIDC_ISSUER`。

### 4. 运行期基础镜像：eclipse-temurin:25-jre

运行期使用 `eclipse-temurin:25-jre`，构建期使用 `maven:3.9.14-eclipse-temurin-25`。

- `openjdk:25-jdk-slim` 仍可用，但官方镜像已标记废弃；JRE 镜像更小，且 `run-java.sh` 只需要 `java`。
- 两者都从 SWR 镜像站拉取：`docker.io/library/eclipse-temurin:25-jre` 必须带 `library/`，`docker.io/maven:3.9.14-eclipse-temurin-25` 不带。
- 运行期镜像需要安装 `curl` 或使用 TCP 检查；见决策 7。

### 5. 启动顺序与健康检查

依赖图：

```text
mysql (healthy)
  └─> db-init (exit 0)
        └─> iboot-system (healthy)
              ├─> 其余 7 个业务服务
              ├─> iboot-gateway (healthy) ──> iboot-portal
              └─> iboot-ops

redis (healthy) ──────────────┐
nacos (healthy) ──────────────┤ 所有后端服务
minio (healthy) ──────────────┴─> file / ai
elasticsearch (healthy) ───────> notify / monitor / ai
logstash (healthy) ────────────> 旁路，不阻塞启动
```

具体规则：

- `db-init` 设置 `restart: "no"`，后端服务统一使用 `depends_on: { db-init: { condition: service_completed_successfully } }`。
- 后端服务同时依赖 `redis`、`nacos` 健康；`file`/`ai` 额外依赖 `minio` 健康；`notify`/`monitor`/`ai` 额外依赖 `elasticsearch` 健康。
- `iboot-system` 健康后，其余业务服务才启动，因为它们启动时要读取共享库中的配置。
- `iboot-gateway` 依赖 `nacos` 与 `iboot-system` 健康；`iboot-portal` 依赖 `iboot-gateway` 健康（nginx 启动时解析上游）。
- `logstash` 只依赖 `elasticsearch` 健康，任何业务服务都不依赖 `logstash`。
- Nacos 在核心部署中常驻：服务间 `@LoadBalanced` 调用与 SBA 实例发现都需要它。

### 6. 一次性数据库初始化

`db-init` 使用独立的 `Dockerfile.dbinit`，复制 `iboot-db-init-1.0.0-exec.jar`，入口为 `java -jar`；不需要 `run-java.sh`。

- `restart: "no"`，避免退出 0 后被重启策略反复拉起。
- 容器连接 `mysql:3306`，使用与业务服务相同的 `MYSQL_*` 与 `IBOOT_DB_NAME`。
- MySQL 健康检查在初始化脚本期间可能提前通过；通过 `start_period` 与 MySQL 自身的就绪检查降低风险，必要时在 `db-init` 入口增加有限重试。
- 迁移失败时以非零退出，`service_completed_successfully` 会阻止所有后端服务启动。

### 7. 健康检查实现

- 基础设施：MySQL 使用 `mysqladmin ping`，Redis 使用 `redis-cli ping`，MinIO 使用镜像内自带的 `mc ready local`，Elasticsearch/Nacos 使用各自的 HTTP 端点。
- 后端服务：运行期镜像不包含 `curl`/`wget`。默认使用 `bash -c 'exec 3<>/dev/tcp/127.0.0.1/<port>'` 做 TCP 可达检查；这能反映 Tomcat 已开始接受连接，且不引入额外包。若后续需要 Actuator 级健康，再在运行期镜像中安装 `curl` 并改用 `/actuator/health`。
- 设置 `MANAGEMENT_HEALTH_MAIL_ENABLED=false`，避免外部邮件不可达把 `/actuator/health` 拉成 503。
- 每个常驻服务配置 `interval`、`timeout`、`retries`、`start_period`；`iboot-ai` 与 `iboot-system` 给更长的启动宽限期。

### 8. 配置注入：env_file + 显式 environment 覆盖

- 应用服务统一使用 `env_file: ../.env` 获取 DashScope、邮件、短信、通知 token、备份密钥等私有配置。
- 用 `environment:` 覆盖所有容器网络地址：`MYSQL_HOST=mysql`、`REDIS_HOST=redis`、`ELASTICSEARCH_URIS=http://elasticsearch:9200`、`NACOS_ADDR=nacos:8848`、`TEMPO_OTLP=http://tempo:4318/v1/traces`、`LOGSTASH_HOST=logstash`、`IBOOT_AI_MINIO_ENDPOINT=http://minio:9000`、`JWT_JWK_SET_URI=http://iboot-auth:8083/oauth2/jwks`。
- 网关设置 `SPRING_PROFILES_ACTIVE=nacos`，让 `lb://` 路由生效。
- 认证回调改为 `http://localhost/callback` 与 `http://localhost/login`；`IBOOT_AUTH_ISSUER` 保持 `http://localhost:8088`（浏览器可达）。
- 认证服务的 RSA 签名密钥默认每次启动生成；compose 为 `iboot-auth` 挂载 `auth_keys:/app/keys` 并设置 `IBOOT_AUTH_JWK_FILE=/app/keys/iboot-auth-jwk.json`，使密钥跨重启稳定，避免重启后旧令牌全部失效。
- 门户的 OIDC 回跳不能把 `/callback` 当作 `returnTo`：回调地址带一次性 `code/state`，再次导航只会复用回调组件而不重新执行登录流程。`beginLogin`、`requireLogin` 与 `CallbackView` 均忽略回调地址，失败时回到 `/dashboard` 或 `/login`。
- 门户 nginx 必须覆盖所有同源后端路径：`/iboot/api` → `/api/**`、`/iboot/ai` → `/api/ai/**`（关闭缓冲以支持 SSE）、`/druid` 与 Swagger 资源 → 系统服务、`/sba` → 运维控制台。未匹配路径才回退到 SPA；否则 AI JSON 与 iframe 会拿到 `index.html`。
- AI 智能体控制器由 `IBOOT_AI_AGENT_ENABLED=true` 开启；初始智能体列表为空，需要在“智能体管理”中创建后才会出现在“智能体”页。
- 部署脚本默认 scope 为 `observability`（核心 + 监控/日志展示）；`up core` 只启动核心组件，`up graph`/`full` 额外加入 Neo4j。
- `iboot-ops` 设置 `IBOOT_OPS_SECURITY_ENABLED=false` 作为本地全栈默认；生产部署必须改为开启并注入密码。
- `iboot-ai` 从 `.env` 获取 `DASHSCOPE_CHAT_API_KEY`，缺失时按既有校验失败。
- 根 `.dockerignore` 必须排除 `.env`，避免密钥进入 builder 镜像层。

### 9. 卷、权限与内存

- 数据卷：复用现有 `mysql_data`、`redis_data`、`minio_data`、`es_data`、`prometheus_data`、`grafana_data`、`alertmanager_data`、`neo4j_data`、`tempo_data`、Thanos 卷。
- 新增共享 `admin_logs:/app/logs`；日志文件名按应用名区分，多个服务共享不会互相覆盖。
- 新增共享 `uploads_data:/app/uploads`，挂载到 `iboot-system`、`iboot-file`、`iboot-job`；运行期 Dockerfile 预创建 `/app/uploads` 并 `chown app:app`，避免命名卷归 root。
- 每个后端服务通过 `JAVA_OPTIONS` 显式设置 `-Xms/-Xmx`，并保留 `--enable-native-access=ALL-UNNAMED`；compose 的 `environment` 会整体覆盖 Dockerfile 的 ENV。
- 本机 Docker Desktop 内存已调整为 23.47GiB；运行期堆配置为 system/ai 512m/1024m、其余 256m/512m。早期在 7.388GiB 下曾把 ES/Nacos 挤到重启，现保留显式 `-Xmx` 以避免 JVM 按宿主内存推导。

### 10. 可选组件与 profile

- 核心启动：MySQL、Redis、MinIO、Elasticsearch、Nacos、Tempo、Logstash、`db-init`、10 个后端服务、门户。
- `observability` profile：Prometheus、Grafana、Alertmanager、Thanos 全套、四个 exporter、Kibana。
- `graph` profile：Neo4j。
- Sentinel、Seata、RocketMQ 当前代码未引用，不进入 compose。
- Prometheus 的抓取目标从 `host.docker.internal` 改为 compose 服务名；`dev-init.sh` 不启动 Prometheus，因此不影响宿主机开发路径。

### 11. 构建与部署脚本

新增 `scripts/docker-deploy.sh`：

```text
1. scripts/dev-init.sh --stop            # 避免宿主机进程占用端口与 fat jar 冲突
2. 确保基础镜像存在（调用 pull-docker-images.sh 或逐项检查）
3. docker compose --profile build build iboot-backend-builder
4. docker compose build
5. docker compose up -d
6. 脚本轮询 compose ps，等待常驻服务 healthy；db-init / minio-init 以退出码 0 视为完成
```

- builder 在 compose 中只作为 `build` profile 的构建目标，不会常驻。
- 不使用 `compose up --wait`：它会把 `db-init`、`minio-init` 这类正常退出的一次性服务判为失败。脚本自行轮询，失败时输出未健康服务的最近日志。
- 另外提供 `down`、`purge`（显式删除卷）与 `status` 子命令。
- `docker compose build` 单独执行前必须先构建 builder；脚本是唯一受支持的入口，README 说明顺序。

### 12. 现有 Dockerfile 的处理

删除 10 个 `iboot-*/Dockerfile`，由 `docker/Dockerfile.backend` 统一替代；`iboot-*/script/run-java.sh` 保留不动，由运行期镜像逐模块复制。

这样做避免两套模板漂移；`service-runtime` 的要求已改为允许共享参数化模板。`iboot-portal/Dockerfile` 保留在原位置并改造为多阶段。

## Risks / Trade-offs

- **首次构建慢**：需要下载 Maven 依赖与前端依赖。→ BuildKit cache mount；必要时用宿主机 `/home/lam/repo` 预热；README 给出耗时预期。
- **镜像站标签差异**：`library/` 前缀不统一，标签缺失会导致 `FROM` 回退到 Docker Hub 并失败。→ 拉取脚本固定标签并覆盖全部基础镜像；`docker compose build` 前先执行拉取检查。
- **无 lockfile 导致前端依赖漂移**：`npm install` 可能解析到不同的次版本。→ 本次接受；后续变更提交 `package-lock.json` 并改用 `npm ci`。
- **10 个 JVM 的内存压力**：默认堆可能按宿主机内存推导。→ 每个服务显式 `-Xmx`（system/ai 1GB，其余 512m）；可观测性组件通过 profile 排除；Docker Desktop 内存已调至 23.47GiB。
- **TCP 健康检查不如 Actuator 精确**：端口可连不代表依赖全部就绪。→ `system` 的依赖（MySQL/Redis/Nacos）已单独健康检查；若需要更严格语义，后续在运行期镜像安装 curl 并改用 `/actuator/health`。
- **Nacos 成为核心启动依赖**：与 `service-discovery` 的“注册中心不可用不阻止启动”存在部署层张力。→ 服务自身仍保留 `fail-fast: false` 与 `optional:nacos`；全栈编排选择在 Nacos 健康后再启动，以保证服务间调用可用。该差异在 README 中说明。
- **备份功能默认关闭**：Docker 部署下 `IBOOT_BACKUP_ENABLED=false`，避免 `docker exec` 自检失败。→ 需要备份时显式挂 Docker Socket 并安装 CLI，另立变更。
- **密钥进入构建上下文**：根 `.env` 会被 `COPY . .` 带入。→ 根 `.dockerignore` 排除 `.env`；提交前用 `docker history`/镜像检查复核。
- **端口冲突**：宿主机 `dev-init.sh` 与全栈 compose 不能同时运行。→ 部署脚本先停止宿主机进程；README 明确二选一。

## Migration Plan

1. 停止宿主机进程：`scripts/dev-init.sh --stop`。
2. 拉取基础镜像：执行更新后的 `scripts/pull-docker-images.sh`。
3. 构建共享 builder：`docker compose --profile build build iboot-backend-builder`。
4. 构建全部运行期镜像：`docker compose build`。
5. 启动核心服务：`docker compose up -d --wait`；需要时追加 `--profile observability --profile graph`。
6. 验证：门户、网关、系统服务、`db-init` 退出码、各服务健康状态、日志与上传卷。
7. 回滚：`docker compose down` 后重新执行 `scripts/dev-init.sh`；数据卷保留，不会被删除。旧 compose 的项目名与 compose 文件目录一致，卷名不会变化。

## Open Questions

- 是否在后续变更中提交 `package-lock.json` 并改用 `npm ci`。
- 是否需要在容器内启用 `iboot-job` 备份（挂 Docker Socket + 安装 CLI），以及对应的安全边界。
- 是否需要把后端健康检查从 TCP 升级为 Actuator（运行期镜像安装 `curl`）。
