# Proposal

## Why

当前项目已有 10 个后端服务的 Dockerfile 和一份 `docker/docker-compose.yml`，但 compose 只编排了 MySQL、Redis、MinIO、ELK、监控等基础设施，所有应用仍由 `scripts/dev-init.sh` 在宿主机启动；镜像构建依赖宿主机预先生成的 `target/*.jar` 与 `iboot-portal/dist`，无法从干净检出完成部署。同时，宿主机产物与容器网络默认值（`localhost`）不一致，服务间调用、JWK 验签、追踪上报都容易连错地址。

本次变更把项目收敛为一条可重复的完整部署链路：从干净检出开始，由 Docker Compose 构建全部镜像并编排全部服务，且启动顺序、依赖健康状态与失败语义都由编排文件确定性表达。

## What Changes

- 新增从干净检出到全栈启动的一键部署链路：构建共享 Maven builder 镜像 → 构建各运行期镜像与前端镜像 → 启动 compose → 等待健康。
- 后端改为多阶段构建：一个共享 Maven builder 镜像一次构建全部模块，各服务运行期镜像只从 builder 拷贝自己的 jar；运行期镜像不包含 Maven/JDK 工具链。
- 前端改为多阶段构建：`node` 构建阶段执行 `npm install` + `npm run build`，`nginx` 运行阶段只承载 `dist` 与 `nginx.conf`。
- 新增只含镜像地址、不含凭据的 `docker/maven/settings.xml`，配合 BuildKit cache mount 解决容器内 Maven 依赖拉取；不复制宿主机含发布凭据的 `settings.xml`。
- 前端构建在容器内使用 `https://registry.npmmirror.com` 与 npm cache mount，不再依赖宿主机 `cnpm`。
- 重写 `docker/docker-compose.yml`：加入 `db-init` 一次性服务、全部后端服务、门户、Nacos/Tempo，以及按 profile 分组的可观测性与 Neo4j 组件；补齐所有常驻服务的健康检查与 `depends_on` 条件。
- 统一容器网络内的环境变量：数据库/Redis/Nacos/ES/MinIO/Tempo/Logstash 主机名、`JWT_JWK_SET_URI`、网关 profile、认证回调地址、ops 凭据、AI 密钥等全部显式覆盖。
- 为 `system`、`file`、`job` 提供共享 `uploads` 卷，为全部服务提供共享 `logs` 卷，并保证非 root 用户可写。
- 更新 `scripts/pull-docker-images.sh`：加入 maven、node、temurin、nginx、openjdk 基础镜像，沿用现有 SWR 镜像站前缀。
- 新增 `scripts/docker-deploy.sh` 串起构建与启动；更新 `docker/README.md`。
- **BREAKING**：镜像不再使用宿主机预构建产物；`docker compose build` 前必须先构建共享 builder 镜像（由部署脚本保证）。现有 10 个 `iboot-*/Dockerfile` 的统一模板被共享参数化模板替代。

## Capabilities

### New Capabilities

- `container-deployment`: 定义从干净检出到全栈启动的构建、镜像、编排、启动顺序、健康检查、配置注入、卷与失败语义。

### Modified Capabilities

- `service-runtime`: 修改“容器启动契约”。允许共享参数化 Dockerfile 与多阶段构建；移除“镜像必须使用预构建 jar、不得在镜像构建阶段执行 Maven 构建”的限制；保留非 root、可写日志目录、端口声明与 JVM 参数可覆盖。
- `database-lifecycle`: 修改“服务只连接数据库，不改变结构”。在编排部署中明确要求服务启动等待 `db-init` 成功；服务进程本身仍不得执行迁移。

## Impact

- 构建与编排文件：`scripts/pull-docker-images.sh`、`docker/docker-compose.yml`、新增 `docker/maven/settings.xml`、新增 `docker/Dockerfile.backend-builder`、新增 `docker/Dockerfile.backend`、新增 `docker/Dockerfile.dbinit`、改造 `iboot-portal/Dockerfile`、新增根 `.dockerignore` 与 `iboot-portal/.dockerignore`、新增 `scripts/docker-deploy.sh`、更新 `docker/README.md`。
- 现有 `iboot-*/Dockerfile`（10 个）：由共享参数化模板替代；`script/run-java.sh` 仍逐模块复制，内容不变。
- 构建基础镜像：`maven:3.9.14-eclipse-temurin-25`、`node:22-bookworm-slim`、`eclipse-temurin:25-jre`、`nginx:stable-alpine3.23`，全部从现有华为云 SWR 镜像站拉取并打本地标签。
- 运行依赖：MySQL、Redis、MinIO、Elasticsearch、Logstash、Nacos、Tempo；可观测性组件（Prometheus、Grafana、Alertmanager、Thanos、ELK UI）与 Neo4j 按 profile 可选。
- 配置：根 `.env` 中的密钥通过 `env_file` 注入；容器网络地址由 compose 的 `environment` 覆盖，禁止把 `localhost` 默认值带进容器。
- 运行时约束：10 个 JVM 需要显式堆配置；首次构建需要下载约 1–1.5GB Maven 依赖与前端依赖，后续构建依赖 BuildKit 缓存。
