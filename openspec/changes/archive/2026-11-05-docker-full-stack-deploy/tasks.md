# Tasks

## 1. 基础镜像与拉取脚本

- [x] 1.1 更新 `scripts/pull-docker-images.sh`，加入 `maven:3.9.14-eclipse-temurin-25`、`node:22-bookworm-slim`、`eclipse-temurin:25-jre`（源地址带 `library/`）、`nginx:stable-alpine3.23`，并在注释中说明 `openjdk:25-jdk-slim` 仅作为回退；验证：执行脚本后 `docker image inspect` 能看到全部本地标签。
- [x] 1.2 对构建与运行期基础镜像做最小验证：`docker run --rm <maven-image> mvn -v`、`docker run --rm <temurin-image> java -version`、`docker run --rm <node-image> node -v`、`docker run --rm <nginx-image> nginx -v` 均成功；验证：命令退出码为 0。

## 2. 构建上下文与 Maven 配置

- [x] 2.1 新增 `docker/maven/settings.xml`，只保留阿里云 `public` 与 `spring` 两个 mirror，不包含 `<servers>`、用户名或密码；验证：文件中不存在 `<server>`/`<password>`，且包含两个 mirror URL。
- [x] 2.2 新增根 `.dockerignore`，排除 `.env`、`.env.*`、`**/target`、`iboot-portal/node_modules`、`iboot-portal/dist`、`.git`、`logs`、`uploads`、`.codegraph`、`.idea`；验证：`docker build` 的上下文不包含根 `.env`（可用 `docker build --check` 或构建日志/临时 `.dockerignore` 对比确认）。
- [x] 2.3 新增 `iboot-portal/.dockerignore`，排除 `node_modules` 与 `dist`，但保留 `.env`；验证：构建日志中 Vite 能读到 `VITE_API_BASE_URL` 与 `VITE_OIDC_ISSUER`。

## 3. 后端与前端多阶段镜像

- [x] 3.1 新增 `docker/Dockerfile.backend-builder`：基于 `maven:3.9.14-eclipse-temurin-25`，复制 `docker/maven/settings.xml` 到 `/root/.m2/settings.xml`，先复制 POM 预热依赖，再复制源码执行一次 `mvn -B -DskipTests package`，使用 `/root/.m2/repository` cache mount；验证：`docker compose --profile build build iboot-backend-builder` 成功，`docker run --rm --entrypoint ls <builder> /workspace/iboot-system/target` 能看到 jar。
- [x] 3.2 新增 `docker/Dockerfile.backend`：基于 `eclipse-temurin:25-jre`，声明 `ARG MODULE`，从 builder 复制 `${MODULE}/target/${MODULE}-1.0.0.jar` 与 `${MODULE}/script/run-java.sh`，创建非 root 用户、`/app/logs`、`/app/uploads`，入口为 `run-java.sh`；验证：分别构建 `iboot-system` 与 `iboot-ai` 镜像成功，且镜像内无 `mvn`/`javac`（`docker run --rm --entrypoint sh <image> -c 'command -v mvn; command -v javac'` 无输出）。
- [x] 3.3 新增 `docker/Dockerfile.dbinit`：基于 `eclipse-temurin:25-jre`，复制 `iboot-db-init-1.0.0-exec.jar`，入口为 `java -jar /app/app.jar`；验证：镜像构建成功，并能对可达的 MySQL 完成一次建库/迁移后以 0 退出。
- [x] 3.4 用共享模板替代现有 10 个 `iboot-*/Dockerfile`，删除旧文件并确认 `script/run-java.sh` 保留；验证：仓库中不再存在按模块重复的 Dockerfile，`git grep -n "COPY script/run-java.sh"` 只剩共享模板。
- [x] 3.5 将 `iboot-portal/Dockerfile` 改为多阶段：`node:22-bookworm-slim` 中配置 npmmirror、`npm install`、`npm run build`，`nginx:stable-alpine3.23` 中复制 `dist` 与 `nginx.conf`；验证：镜像构建成功，容器启动后 `curl -I http://localhost/` 返回 200 且页面可加载。

## 4. Compose 编排

- [x] 4.1 重写 `docker/docker-compose.yml` 的扩展锚点与基础设施段：为 MySQL、Redis、MinIO、Elasticsearch、Nacos 增加或修正健康检查（MinIO 使用 `mc ready local`，Redis 使用 `redis-cli ping`），新增 `db-init` 一次性服务并设置 `restart: "no"`；验证：`docker compose config` 通过，且 `db-init` 的 `restart` 为 `no`。
- [x] 4.2 加入 10 个后端服务与 `iboot-portal`，逐项设置 `depends_on` 条件：`db-init: service_completed_successfully`、`redis/nacos: service_healthy`、`iboot-system: service_healthy`，`portal` 依赖 `iboot-gateway: service_healthy`；验证：`docker compose config` 中每个服务的依赖条件正确，`docker compose up` 的实际启动顺序符合设计。
- [x] 4.3 用共享锚点注入公共环境变量，并逐服务覆盖：`MYSQL_HOST=mysql`、`REDIS_HOST=redis`、`ELASTICSEARCH_URIS=http://elasticsearch:9200`、`NACOS_ADDR=nacos:8848`、`TEMPO_OTLP=http://tempo:4318/v1/traces`、`LOGSTASH_HOST=logstash`、`JWT_JWK_SET_URI=http://iboot-auth:8083/oauth2/jwks`；为 `iboot-gateway` 设 `SPRING_PROFILES_ACTIVE=nacos`，为 `iboot-auth` 设 `IBOOT_AUTH_REDIRECT_URI=http://localhost/callback`、`IBOOT_AUTH_LOGOUT_REDIRECT_URI=http://localhost/login`，为 `iboot-ops` 设 `IBOOT_OPS_SECURITY_ENABLED=false`，为 `iboot-ai` 传入 `DASHSCOPE_*` 与 `IBOOT_AI_MINIO_ENDPOINT=http://minio:9000`；验证：`docker compose config` 中所有内部地址都不含 `localhost`，且 `iboot-job` 的 `IBOOT_BACKUP_ENABLED` 为 `false`。
- [x] 4.4 增加卷与权限：`admin_logs:/app/logs` 挂载到全部后端服务，`uploads_data:/app/uploads` 挂载到 `iboot-system`、`iboot-file`、`iboot-job`，复用现有数据卷；验证：`docker compose config` 显示卷挂载正确，非 root 容器能写入日志与上传目录（`docker exec <container> touch /app/logs/.probe /app/uploads/.probe`）。
- [x] 4.5 增加 `observability` 与 `graph` profile：Prometheus、Grafana、Alertmanager、Thanos、exporter、Kibana、Neo4j 放入对应 profile，核心服务不带 profile；验证：`docker compose config --services` 只列核心服务，追加 `--profile observability --profile graph` 后才列出可选组件。
- [x] 4.6 更新 `docker/scripts/prometheus/prometheus.yml`，把 `iboot-system`、`iboot-ai`、`iboot-ops` 的抓取目标从 `host.docker.internal` 改为 compose 服务名；验证：启动 `observability` profile 后，Prometheus targets 页面三个目标均为 UP。

## 5. 部署脚本与文档

- [x] 5.1 新增 `scripts/docker-deploy.sh`，支持 `up`（停止宿主机进程 → 确保基础镜像 → 构建 builder → compose build → `up -d` → 轮询健康，一次性服务以退出码 0 视为完成）、`status`、`down`、`purge`；验证：`bash -n` 通过，`--help` 输出完整用法，`up` 在干净环境完成启动。
- [x] 5.2 更新 `docker/README.md`：写明构建/启动顺序、基础镜像来源、profile 用法、端口、卷、常见失败（db-init 失败、健康检查不通过、首次构建耗时）与回滚到 `dev-init.sh` 的步骤；验证：按文档从零执行一遍命令，结果与文档一致。
- [x] 5.3 清理或替换已过期的 `docker/deploy.sh`（仍在使用 `docker-compose` v1 且只构建 `iboot-system`），避免与新入口冲突；验证：仓库中不再有指向旧流程的可执行入口，README 不再引用它。

## 6. 集成验证

- [x] 6.1 干净检出验证：在临时克隆（或删除 `target/`、`iboot-portal/dist`、`node_modules/` 后）执行 `scripts/docker-deploy.sh up`；验证：全部镜像构建成功，核心服务健康。
- [x] 6.2 启动顺序与失败语义验证：用错误的 MySQL 密码启动 `db-init`，确认它以非零退出且所有后端服务未启动；恢复正确密码后重新执行，确认按顺序启动；验证：`docker compose ps` 与 `db-init` 日志。
- [x] 6.3 配置正确性验证：分别在 `iboot-system` 与 `iboot-file` 容器内检查 JDBC、Redis、Nacos、JWK、上传路径；通过门户完成一次登录（OIDC 回调 `http://localhost/callback`），打开运维控制台；验证：登录成功、网关路由可达、`/actuator` 状态正常。
- [x] 6.4 持久化与幂等验证：首次启动后写入一条业务数据并上传一个文件，执行 `docker compose down` 再 `up`；验证：数据、文件与日志仍在，`db-init` 再次以 0 退出且不重复迁移。
- [x] 6.5 日志与集中检索验证：确认 `admin_logs` 中每个服务生成 `<应用名>.log` 与 `<应用名>-error.log`，Kibana 能按服务名检索；临时停止 Logstash，确认业务请求仍成功且服务不退出。
- [x] 6.6 资源与可观测性验证：在 `observability` profile 下检查 Grafana 数据源、Prometheus targets、Tempo 链路；用 `docker stats` 确认 10 个 JVM 未触发 OOM（Docker Desktop 已调至 23.47GiB，堆配置 system/ai 1GB、其余 512m）；验证：Grafana 面板有数据、`docker stats` 内存稳定。
