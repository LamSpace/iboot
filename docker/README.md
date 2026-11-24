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
5. `docker compose up -d` 启动，然后由脚本轮询健康状态；`db-init`、`silo-init`
   这类一次性服务以退出码 0 视为完成；
6. 调用 `scripts/sentinel-init.sh --verify` 把 Sentinel 基线规则写入 Nacos。

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

Docker 全量部署中，宿主机只有门户 `80/443` 绑定全部网卡；其它容器发布端口只绑定 `127.0.0.1`，供宿主机开发、调试与回归使用。未列出的内部端口不发布到宿主机。宿主机开发模式下由 `dev-init.sh` 直接启动的 Java 进程只监听 `127.0.0.1`；原生 Docker 无法直连宿主机回环时，由只绑定 Docker 网桥网关的 relay 供 Prometheus/blackbox 抓取。

| 服务 | 宿主机端口 | 绑定 | 说明 |
|---|---|---|---|
| iboot-portal | 80 / 443 | `0.0.0.0` | 前端门户（nginx）；80 只做 HTTPS 重定向，唯一公开入口 |
| iboot-gateway | 8088 | `127.0.0.1` | 统一网关（HTTPS，内部地址） |
| iboot-system | 8080 | `127.0.0.1` | 系统服务 |
| iboot-ai | 8081 | `127.0.0.1` | AI 服务 |
| iboot-file | 8082 | `127.0.0.1` | 文件服务 |
| iboot-auth | 8083 | `127.0.0.1` | 认证服务 |
| iboot-notify | 8084 | `127.0.0.1` | 通知服务 |
| iboot-job | 8085 | `127.0.0.1` | 调度与备份 |
| iboot-workflow | 8086 | `127.0.0.1` | 审批工作流 |
| iboot-monitor | 8087 | `127.0.0.1` | 监控与日志查询 |
| iboot-ops | 8089 | `127.0.0.1` | 运维控制台（SBA） |
| sentinel-dashboard | 8858 | `127.0.0.1` | Sentinel 治理控制台；HTTPS + 登录 |
| opa | 8181 | `127.0.0.1` | 服务间授权决策；`IBOOT_SERVICE_AUTHORIZATION_OPA_URL` 非空时启用 |
| seata-server | 8091 | `127.0.0.1` | Seata TC（AT 全局事务）；只在 `tx` 网络对 workflow/system 可见，本地镜像无 7091 控制台 |
| mysql | 3306 | `127.0.0.1` | MySQL 8.4（只接受 TLS） |
| redis | 6379 | `127.0.0.1` | Redis 8（只监听 TLS 端口） |
| silo | 9000/9001 | `127.0.0.1` | 对象存储与控制台（HTTPS） |
| elasticsearch | 9200 | `127.0.0.1` | 检索（HTTPS + 认证） |
| kafka | 29092 | `127.0.0.1` | 宿主机监听；容器内使用 `kafka:9092`，全部监听 SSL |
| rocketmq-tls | 18081 | `127.0.0.1` | RocketMQ TLS-only 入口；Proxy 原生端口只在 `mq` 网络内，服务端证书校验见 [docs/27 §26](<../docs/27-TLS 与零信任操作指南.md>) |
| logstash | — | — | 不发布；在编排网络内从 Kafka 读取并写入 Elasticsearch |
| nacos | 8848/9848/8849 | `127.0.0.1` | HTTPS API / gRPC TLS / 控制台（8849） |
| tempo | 4317/4318/3200 | `127.0.0.1` | OTLP gRPC / OTLP HTTP / UI |

APM 对比 profile（`apm`，默认不启动，实验结束可整体删除）：

| 服务 | 宿主机端口 | 绑定 | 说明 |
|---|---|---|---|
| skywalking-oap | 11800 / 12800 | `127.0.0.1` | gRPC 上报 / GraphQL 查询；admin 端口不发布 |
| skywalking-ui | 18082 | `127.0.0.1` | Horizon UI；默认账号仅限本机 |
| skywalking-banyandb | — | — | 不发布；与 OAP 11.0.0 的 server API 0.11 精确配套 |

> 业务服务的 Agent 不由 compose 注入：当前开发路径是宿主机 `scripts/dev-init.sh` 在 `IBOOT_SKYWALKING_ENABLED=true` 时追加 `-javaagent`。容器化后如何挂载 Agent 留到阶段 3 再评估。

可观测性组件（默认启动；`up core` 可只启动核心）：

| 服务 | 宿主机端口 | 绑定 | 说明 |
|---|---|---|---|
| prometheus | 9090 | `127.0.0.1` | HTTPS；抓取目标按运行模式选择配置，见下文 |
| grafana | 3000 | `127.0.0.1` | HTTPS 看板 |
| alertmanager | 9093 | `127.0.0.1` | HTTPS 告警 |
| kibana | 5601 | `127.0.0.1` | HTTPS 日志检索 UI |
| thanos-query | 19192 | `127.0.0.1` | HTTPS 查询 UI |
| thanos-sidecar / store / compact | — | — | 不发布；Grafana 经 `obs` 网络以 HTTPS 访问 `thanos-query` |
| mysql/redis/node/elasticsearch exporter | — | — | 不发布；Prometheus 在编排网络内抓取 |

> 五个可观测控制台只提供 HTTPS，使用本地 CA 校验；Sentinel Dashboard 已在 B5.11 收敛为 HTTPS。

图数据库 profile：Neo4j `7474`（HTTP）、`7687`（Bolt TLS），均绑定 `127.0.0.1`。

SPIRE profile（`spire`，默认不启动）：`spire-server`、`spire-agent` 与 `spire-demo` 验证 Workload API/SVID；
不发布宿主机端口，启动与验收见 `scripts/spire-up.sh` 与 `scripts/regress/spire-svid.sh`。

B2 起 8080–8089 只提供 HTTPS；B5.2 起还要求客户端证书。MySQL/Redis/Silo/Neo4j 只接受 TLS。
B3 起 ES、Kafka、Logstash 也只接受 TLS；Nacos 在 B4 起同样只接受 TLS。

### 网络分段

compose 定义 `edge` / `app` / `data` / `obs` / `mq` / `tx` 六个网络：

| 网络 | 成员 | 边界 |
|---|---|---|
| `edge` | 门户、网关、系统服务、运维控制台 | 门户唯一入口；门户不加入数据面网络 |
| `app` | 网关、系统服务、运维控制台、八个业务服务、Prometheus、`rocketmq-tls` | 服务间调用、指标抓取与 RocketMQ TLS 入口 |
| `data` | 中间件（不含 Seata 与 RocketMQ 集群）、业务与初始化任务、Logstash/Kibana/Grafana/Thanos/exporter | 业务与数据面通信 |
| `mq` | RocketMQ NameServer/Broker/Proxy/初始化任务、`rocketmq-tls` | 仅 RocketMQ 集群与 TLS 前置代理 |
| `tx` | `seata-server`、`iboot-workflow`、`iboot-system` | 仅事务协调与两个事务参与方 |
| `obs` | Prometheus、Grafana、Alertmanager、Thanos、SkyWalking、exporter | 可观测组件内部通信 |

门户容器无法解析或连接 `mysql`、`redis`、`elasticsearch`、`nacos`；这是可回归验证的边界，
不是仅靠文档约定。暴露面检查见 [`scripts/regress/exposure.sh`](../scripts/regress/exposure.sh)。

Prometheus 有两种配置：Docker 全量部署与直接 `docker compose up` 默认用
`prometheus.docker.yml`（服务名目标）；`dev-init.sh` 在宿主机开发模式覆盖为
`prometheus.yml`（`host.docker.internal` 目标）。两者都通过 `/certs/rootCA.pem`
以 HTTPS 抓取服务指标。

## 启动顺序

```text
mysql (healthy)
  └─> db-init (exit 0)
        └─> iboot-system (healthy)
              ├─> 其余业务服务
              ├─> iboot-gateway (healthy) ──> iboot-portal
              └─> iboot-ops

redis / nacos ──> 所有后端服务
silo ──> iboot-file / iboot-ai
elasticsearch ──> iboot-notify / iboot-monitor / iboot-ai
kafka ──> logstash ──> 旁路，不作为启动依赖；kafka-init 一次性建 iboot-logs
sentinel-dashboard ──> 旁路，规则经 Nacos 下发；停控制台不影响业务
```

- `db-init` 的 `restart` 为 `no`；迁移失败时它以非零退出，所有后端服务都不会启动。
- 后端健康检查使用 `bash` 的 `/dev/tcp` 端口探测，不依赖镜像内的 curl/wget。
- `iboot-system` 健康后其余服务才启动，因为它们启动时要读取共享库中的配置。
- Nacos 在核心启动中常驻，服务间 `@LoadBalanced` 调用与 SBA 实例发现都依赖它。

## 环境变量

- 应用服务通过 `env_file: ../.env` 读取 DashScope、邮件、短信、通知 token、备份密钥等私有配置。
- compose 的 `environment` 会覆盖 `.env` 中的宿主机地址，例如：
  - `MYSQL_HOST=mysql`、`REDIS_HOST=redis`、`NACOS_ADDR=nacos:8848`
  - `ELASTICSEARCH_URIS=https://elasticsearch:9200`、`ELASTICSEARCH_USERNAME`、`ELASTICSEARCH_PASSWORD`
  - `TEMPO_OTLP=https://tempo:4318/v1/traces`
  - `IBOOT_LOGGING_KAFKA_SECURITY_PROTOCOL=SASL_SSL`、`IBOOT_LOGGING_KAFKA_SASL_USERNAME=iboot_log`
  - `JWT_JWK_SET_URI=https://iboot-auth:8083/oauth2/jwks`
- 网关使用 `SPRING_PROFILES_ACTIVE=nacos`。
- 认证 issuer 与回调为 `https://localhost`、`https://localhost/callback`、
  `https://localhost/login`；浏览器经门户 443 访问，网关 `8088` 不是客户端入口。
- 门户构建参数 `IBOOT_PORTAL_OIDC_ISSUER` 默认 `https://localhost`；证书由
  `scripts/tls-init.sh` 生成到 `docker/certs/`，compose 只读挂载到门户容器。
- ES 证书必须挂到 `/usr/share/elasticsearch/config/certs`（9.5 entitlement 限制）；
  Kafka keystore 与密码文件挂到 `/etc/kafka/secrets`。
- Nacos 挂载 `./certs/runtime:/certs:ro`，使用 `nacos-keystore.p12` 启用 HTTPS；
  gRPC TLS 与客户端证书由 `JAVA_TOOL_OPTIONS` 中的 `nacos.remote.*.rpc.tls.*` 系统属性开启。
- `.env` 必须提供 `ELASTIC_PASSWORD` 与 `KIBANA_SYSTEM_PASSWORD`；Kibana 9.5 不能使用
  `elastic` 超级用户，`elasticsearch-init` 会幂等同步 `kibana_system` 密码。
- 认证服务的签名密钥通过 `IBOOT_AUTH_JWK_FILE=/app/keys/iboot-auth-jwk.json` 写入
  `auth_keys` 卷。不要删除该卷，否则所有已签发令牌会在认证服务重启后失效。
- 本地全栈默认关闭运维控制台认证（`IBOOT_OPS_SECURITY_ENABLED=false`）；生产部署必须开启
  并注入 `IBOOT_OPS_PASSWORD`。
- 本地全栈默认开启 Sentinel Dashboard 登录（`sentinel` / `sentinel123`）；非本地环境必须叠加
  `docker/docker-compose.prod.yml` 并注入 `SENTINEL_DASHBOARD_USER` / `SENTINEL_DASHBOARD_PASSWORD`。
- Docker 部署默认关闭 `iboot-job` 备份（`IBOOT_BACKUP_ENABLED=false`），因为运行期镜像没有
  docker CLI，备份自检会失败。需要备份时应另行为该服务挂载 Docker Socket 并安装客户端；
  在此之前，Docker 全量回归中的备份用例会失败，属已知部署限制。

### 对象存储与 Silo

全量 Docker 部署默认使用 Silo（`IBOOT_FILE_STORAGE_TYPE=silo`）。Compose 会把容器内的
`IBOOT_FILE_SILO_ENDPOINT` 固定为 `https://silo:9000`，把预签名地址
`IBOOT_FILE_SILO_PUBLIC_ENDPOINT` 固定为 `https://localhost:9000`。宿主机开发读取
`sys_config` 中的 `https://localhost:9000`；如需本地磁盘，在 `.env` 设置
`IBOOT_FILE_STORAGE_TYPE=local`。

- 切换存储类型后需重启 `iboot-file`、`iboot-system`、`iboot-job`；
- 既有本地文件不会自动迁移到 Silo；需要保留时先导出，或把类型改回 `local`；
- `sys.silo.console.url` 保持 `http://localhost:9001`，因为它是浏览器访问地址；
- 监控服务的 Silo 检查只在存储类型为 `silo` 时真正连接。

## 内部 TLS 与 mTLS（B2 / B5.1 / B5.2 / B5.3）

- 业务服务与网关挂载 `./certs/runtime:/certs:ro`，通过 `IBOOT_TLS_CERT_DIR=/certs`
  使用 `server.ssl`；每个服务再通过 `IBOOT_TLS_CERT_FILE` / `IBOOT_TLS_KEY_FILE`
  加载 `services/<服务名>.pem` 的独立证书。`JAVA_TOOL_OPTIONS` 注入 PKCS12 信任库。
- 服务端口设置 `server.ssl.client-auth=need`，只接受本地 CA 签发的客户端证书；
  服务证书 EKU 同时包含 `serverAuth` 与 `clientAuth`。
- 门户 nginx 挂载 `portal.pem` 并用 `proxy_ssl_certificate` 访问上游；Prometheus 与
  blackbox 通过 `/certs/clients/` 的专用证书抓取服务端口。
- 四个 exporter 只提供 HTTPS + 客户端证书；Prometheus 用自己的客户端证书抓取，配置见
  `scripts/exporters/`。证书轮换入口为 `scripts/tls-rotate.sh`。
- 自动调度：`scripts/tls-rotate-schedule.sh --install --restart=docker` 安装每日检查，
  检查失败才重签并重建当前运行的 compose 服务；`--status` / `--uninstall` 管理调度。
  优先 systemd user timer，回退当前用户 crontab；说明见 [docs/27 §23.2](../docs/27-TLS%20与零信任操作指南.md)。
- RocketMQ 使用 `mq` 网络 + `rocketmq-tls`（nginx `stream` + `ssl_preread`）只转发 TLS；
  Broker/Proxy 启用 ACL 2.0，workflow/notify 各自持有 access key，运维账号只用于初始化；
  Seata 使用 `tx` 网络，仅 workflow/system 可访问。
- 服务间请求按客户端证书 CN 做路径级授权（`iboot-service-authz`）；
  `IBOOT_SERVICE_AUTHORIZATION_ENABLED=true` 由 compose 公共环境注入，矩阵见 [docs/27 §20](<../docs/27-TLS 与零信任操作指南.md>)。
- MySQL 以 `--require_secure_transport=ON` 启动；Redis 只监听 TLS 端口；Silo 以
  `--certs-dir /certs` 启动；Neo4j 的 `graph` profile 启用 Bolt TLS。
- 数据面健康检查使用本地 CA；ES/Kafka/Logstash/Nacos 只接受 TLS。
- `iboot-auth`、服务间与 JWKS 地址均为 `https://`；Nacos 实例以 `secure=true` 注册。
- Nacos gRPC 客户端证书使用各服务自己的 `services/iboot-<service>.pem`（挂载到 `/certs/nacos-client.pem`），
  不再共用 `portal.pem`。
- 回滚使用上一版镜像/compose，不提供运行期明文回退。

证书生成与负向用例见
[docs/27](<../docs/27-TLS 与零信任操作指南.md>) 与
[`scripts/regress/internal-tls.sh`](../scripts/regress/internal-tls.sh)。

## 中间件账号（B4.3 → B5.5）

| 组件 | 账号 | 用途 |
|---|---|---|
| MySQL | `root` | 仅容器内回环首次引导；无网络登录 |
| MySQL | `iboot_migrator` | `db-init` 的迁移账号，`iboot` 库 DDL/DML，无账号管理 |
| MySQL | `iboot_provisioner` | 账号与表级授权初始化，无全局管理能力 |
| MySQL | `iboot_<service>` | 八个数据服务各自账号，`iboot` 库 DML，无 DDL |
| MySQL | `iboot_exporter` | mysqld-exporter 只读 |
| Redis | `iboot_<service>` | 八个数据服务各自 ACL 用户；`default` 已禁用 |
| Redis | `iboot_exporter` | redis-exporter 与健康检查只读监控命令 |
| Silo | `iboot_file` / `iboot_job` | 文件服务与备份任务，仅 `iboot-files` 桶 |
| Silo | `iboot_ai` | AI 服务，仅 `iboot-ai` 桶 |
| Silo | 根账号 | 仅 `silo-init`、Thanos 与运维 |
| Nacos | `iboot_<service>` | 十个服务各自注册/配置账号 |
| Nacos | `iboot` | 管理脚本；管理员 `nacos` 仅初始化脚本使用 |
| Neo4j | `neo4j` | AI 图能力；`NEO4J_PASSWORD` 必填，缺失时编排失败 |
| Kafka | `iboot_log` | 九个服务日志生产，只能写 `iboot-logs` |
| Kafka | `iboot_logstash` | Logstash 消费，只能读该 topic 与消费组 |
| Kafka | `iboot_tools` / `broker` | 初始化运维与节点间通信，超级用户 |
| RocketMQ | `iboot_rocketmq` | 仅 Broker/Proxy 初始化、`mqadmin` 与排障 |
| RocketMQ | `iboot_workflow` | 只允许 PUB `iboot-notification` |
| RocketMQ | `iboot_notify` | 只允许 SUB 该 topic、其消费组与重试 topic |
| ES | `iboot_ai` / `iboot_notify` | 知识库与内容索引读写，按索引前缀授权 |
| ES | `iboot_monitor` / `iboot_grafana` | 日志索引只读 |
| ES | `iboot_logstash` | 日志索引写入 |
| ES | `iboot_exporter` | 集群与索引只读监控 |
| ES | `elastic` | 仅 `es-users-init`、初始化与运维脚本 |

- `.env` 必须提供 `MYSQL_<SERVICE>_PASSWORD`（八个数据服务）、
  `REDIS_<SERVICE>_PASSWORD`（八个数据服务）、`NACOS_<SERVICE>_PASSWORD`（十个服务）、`MYSQL_ROOT_PASSWORD`、
  `MYSQL_MIGRATOR_PASSWORD`、`MYSQL_PROVISIONER_PASSWORD`、
  `MYSQL_EXPORTER_PASSWORD`、`REDIS_EXPORTER_PASSWORD`、`KAFKA_BROKER_PASSWORD`、
  `KAFKA_LOG_PASSWORD`、`KAFKA_LOGSTASH_PASSWORD`、`KAFKA_TOOLS_PASSWORD`、
  `SILO_ROOT_USER/PASSWORD`、`SILO_FILE_*`、`SILO_JOB_*`、`SILO_AI_*`、
  `NACOS_USERNAME/PASSWORD`、`NACOS_ADMIN_PASSWORD`、`NACOS_AUTH_TOKEN`、`NEO4J_PASSWORD`、
  `ES_*_PASSWORD` 与 `GRAFANA_ADMIN_PASSWORD`。
- `mysql-users-init`、`nacos-init`、`silo-init` 与 `es-users-init` 是一次性任务；`db-init` 依赖它们
  完成后再迁移与启动服务。
- 业务服务只拿自己的 MySQL/Redis/Nacos 与 Silo 账号口令；`x-app-env` 会清空其它服务与根凭据。
- MySQL 写权限按 `scripts/mysql-table-grants.tsv` 的表级矩阵授予，`db-init` 后由 `mysql-grants-init` 应用；Redis 命名用户限制键/频道前缀。
- Nacos 客户端必须使用 `NACOS_SERVER_ADDR=https://nacos:8848`；`NACOS_ADDR` 仍是脚本使用的
  `host:port`。
- Grafana 匿名只读（Viewer），管理员口令来自 `GRAFANA_ADMIN_PASSWORD`；本地 Sentinel
  也开启登录，生产必须在 `docker-compose.prod.yml` 显式注入账号密码。
- 验收：`scripts/regress/middleware-auth.sh`，覆盖每服务身份矩阵（MySQL processlist、Redis
  `CLIENT LIST`、Nacos 逐账号登录、Silo 跨桶拒绝）、进程环境隔离、Kafka 无凭据/越权 topic 与
  ES 各账号边界。

## Sentinel 治理

- `sentinel-dashboard` 常驻 `8858`；本地与生产都开启登录，生产叠加 `docker-compose.prod.yml` 并显式注入账号密码；只提供 HTTPS。
- `scripts/docker-deploy.sh up` 在健康检查后写入 Nacos 基线规则；手动校验：
  `bash scripts/sentinel-init.sh --verify`。
- 规则 data-id：`iboot-gateway-flow-rules`、`iboot-notify-flow-rules`
  （group `SENTINEL_GROUP`）。Dashboard 上的修改只作用于运行期内存，重启或 Nacos 刷新后回到基线。
- 完整说明见 [docs/23 §13](../docs/23-微服务底座操作指南.md)。

## 微服务治理控制台

门户“微服务治理”菜单提供 Nacos 与 Sentinel 控制台入口。

- **宿主机 Vite 开发**：Nacos 控制台关闭登录、门户内嵌；Sentinel 控制台开启登录、门户新窗口打开；Nacos 经 Vite 的
  `/nacos-console` 代理访问。
- **Docker 全栈**：门户由 nginx 提供，不代理 Nacos 控制台，因此 compose 用
  `SYS_NACOS_CONSOLE_URL=https://localhost:8849` 与 `SYS_NACOS_CONSOLE_EMBED=false`
  覆盖 `sys_config` 中的开发默认值，页面显示说明卡并跳转到 `https://localhost:8849`。
  这两个值可在根目录 `.env` 中用同名变量覆盖。
- **非本地部署**：保持控制台鉴权，在参数配置中把 `sys.nacos.console.embed` 与
  `sys.sentinel.dashboard.embed` 改为 `false`，Nacos 地址改为内网绝对地址；生产
  nginx 不添加 `/nacos-console` 代理。

- Nacos 开发环境经门户同源代理 `/nacos-console/` 内嵌；Vite 代理转发控制台 SPA 与 `/v3` API 并剥离 `X-Frame-Options`，生产 nginx 不配置该代理；
- `NACOS_AUTH_CONSOLE_ENABLE=false` 只用于本地 compose，不要带到生产；
- Sentinel 本地也开启登录，生产必须显式注入账号密码；
- 详细说明见 [docs/23 §8.6](../docs/23-微服务底座操作指南.md)。

## 卷

| 卷 | 用途 |
|---|---|
| `mysql_data` | MySQL 数据 |
| `redis_data` | Redis 数据 |
| `silo_data` | 对象存储 |
| `es_data` | Elasticsearch 数据 |
| `prometheus_data` / `grafana_data` / `alertmanager_data` | 监控数据 |
| `thanos_store_data` / `thanos_compact_data` | Thanos 长期存储 |
| `admin_logs` | 所有服务的日志（按应用名分文件） |
| `uploads_data` | system / file / job 共享的本地上传目录 |
| `auth_keys` | 认证服务 RSA 签名密钥；认证服务重启后旧令牌仍可验证 |
| `neo4j_data` / `neo4j_logs` | 图数据库 |
| `tempo_data` | 链路追踪 |
| `skywalking_banyandb_data` | APM 对比实验的 SkyWalking 存储；随 `apm` profile 删除 |

普通 `down` 不会删除卷；只有 `purge --yes` 会执行 `down -v`。

## 门户同源代理

门户 nginx 在 443 提供页面，并把以下路径反代到网关：

- `/oauth2/**`、`/.well-known/**`、`/login`、`/connect/**` → 认证与 OIDC；
- `/api/**` → 网关（CLI 使用；门户自身仍用 `/iboot/api`）；
- `/iboot/api` → 网关；
- `/iboot/ai` → 网关 `/api/ai/**`（知识库、智能体、对话，支持 SSE）；
- `/druid` → 系统服务的 Druid 数据库监控；
- `/swagger-ui.html`、`/v3/api-docs`、`/webjars` 等 → 系统服务的 Swagger UI；
- `/sba` → 运维控制台。

新增后端同源路径时必须同步补充代理，否则 SPA 回退会把 `index.html` 返回给 JSON 或 iframe 请求，表现为 `Unexpected token '<'` 或空白页。

门户 80 只返回 301；443 加载 `docker/certs/portal.pem` 与 `portal-key.pem`，健康检查用
`docker/certs/ca/rootCA.pem` 校验，不使用跳过证书校验的参数。证书与浏览器信任见
[docs/27](<../docs/27-TLS 与零信任操作指南.md>)。

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
门户启动晚于网关由 `depends_on` 保证。HTTPS 无法打开时，先确认 `docker/certs/` 三个文件
已挂载、门户容器健康检查通过，再访问 `https://localhost`；浏览器需信任本地 CA。

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

- 旧部署入口只构建 `iboot-system`，并假设 `docker-compose` v1；现在由
  `scripts/docker-deploy.sh` 统一处理全部模块与 compose v2。
- 旧的 10 个 `iboot-*/Dockerfile` 已由 `docker/Dockerfile.backend` 共享模板替代；
  `iboot-*/script/run-java.sh` 保持不变。
- 旧 compose 只编排基础设施，应用由宿主机启动；现在应用、门户、Nacos、Tempo 都在同一网络中。
