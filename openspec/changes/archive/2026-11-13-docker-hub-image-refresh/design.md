# Design

## Context

- 当前 `scripts/pull-docker-images.sh` 把所有镜像从华为 SWR（`swr.cn-north-4.myhuaweicloud.com/ddn-k8s/...`）拉取，再重打成 `docker.io/...` 标签；编排与 Dockerfile 只引用后者。
- 镜像标签同时出现在四处：拉取脚本、`docker/docker-compose.yml`、Dockerfile 的 `FROM`、`scripts/docker-deploy.sh` 的 `BASE_IMAGES`。只改脚本会让其余三处继续引用旧标签。
- 本机 Docker daemon 已配置 registry mirror `hubproxy.docker.internal:5555` 与 HTTP(S) proxy；Docker Hub 元数据查询已验证可达，但实际 pull 需在应用阶段先做 preflight。
- 本项目 Docker 环境按临时环境使用：每次测试结束后删除数据卷，因此跨版本升级不涉及存量数据迁移；变更中仍会显式清理项目卷，避免残留卷让回归结果失真。
- MinIO 上游已改为 source-only distribution，Docker Hub `minio/minio` 与 Quay 镜像均不可公开访问；aityp 展示的 SWR 路径仍可拉到 `RELEASE.2025-09-07T16-13-09Z`。
- 现有 `container-deployment` 与 `apm-comparison` 规格明确禁止 Docker Hub 直连，本次以 delta 规格改写（见 proposal 的 Modified Capabilities）。

## Goals / Non-Goals

**Goals:**

- 建立一份单一版本矩阵，并让拉取脚本、compose、Dockerfile、`docker-deploy.sh` 四处引用完全一致。
- 非 MinIO 镜像全部改为 Docker Hub 官方仓库 + 精确版本标签；MinIO 保持 aityp/SWR 来源。
- 版本选择以 LTS/稳定线为准，兼顾与当前客户端依赖的兼容性。
- 通过完整回归证明升级没有引入接口契约、权限、链路或数据管道漂移。

**Non-Goals:**

- 不修改业务代码、数据库 schema、端口、网络拓扑或 compose profile 划分。
- 不为 MinIO 引入社区重建镜像或自建镜像；该问题单独评估。
- 不安装 Trivy/Grype 等漏洞扫描器，不产出 SBOM。
- 不把迁移期 `equivalence.sh` 纳入本次镜像升级的验收：其 golden 依赖录制时的数据状态，当前库已被回归写入，差异属于状态漂移；不覆盖历史基线。
- 不升级前端框架、Java/Node 语言主版本（Node 保持 22 LTS，JDK 保持 25）。

## Decisions

### D1. 镜像来源：Docker Hub 官方仓库，MinIO 例外

| 组件 | 目标镜像 |
|---|---|
| MySQL / Redis / Neo4j | `docker.io/library/mysql`、`library/redis`、`library/neo4j` |
| Elasticsearch / Logstash / Kibana | `docker.io/library/elasticsearch`、`library/logstash`、`library/kibana` |
| Maven / Node / Temurin / Nginx | `docker.io/library/maven`、`library/node`、`library/eclipse-temurin`、`library/nginx` |
| Nacos / Seata / RocketMQ / Kafka | `docker.io/nacos/nacos-server`、`apache/seata-server`、`apache/rocketmq`、`apache/kafka` |
| Prometheus 生态 | `docker.io/prom/prometheus`、`prom/alertmanager`、`prom/node-exporter`、`prom/mysqld-exporter` |
| Grafana 生态 | `docker.io/grafana/grafana`、`grafana/tempo` |
| Thanos / OTel | `docker.io/thanosio/thanos`、`otel/opentelemetry-collector-contrib` |
| SkyWalking | `docker.io/apache/skywalking-banyandb`、`apache/skywalking-oap-server`、`apache/skywalking-ui` |
| Redis / ES exporter | `docker.io/oliver006/redis_exporter`、`prometheuscommunity/elasticsearch-exporter` |
| Sentinel Dashboard | `docker.io/bladex/sentinel-dashboard`（Sentinel 官方不发布镜像） |
| MinIO | `swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/minio/minio`，拉取后重打为 `docker.io/minio/minio` |

**弃选方案**：继续统一走 SWR（不满足需求）；Thanos 走 `quay.io/thanos/thanos`（与“除 MinIO 外统一 Docker Hub”冲突）；Elastic 保持 `docker.elastic.co`（同上）。

### D2. 版本策略：LTS/稳定线 + 精确标签

以下标签均于 2026-11-12 在 Docker Hub 验证存在，并按“最新稳定补丁”选取：

| 组件 | 当前 | 目标 | 选型依据 |
|---|---|---|---|
| MySQL | `8.0.45` | `8.4.13` | 8.4 为当前 LTS；8.0 已 EOL；8.4.12 未发布 |
| Redis | `7.0.14` | `8.8.4` | 最新稳定线 |
| MinIO | `RELEASE.2025-09-07T16-13-09Z` | 不变 | 官方最后可用镜像；无 2025-10-15 安全版镜像 |
| Neo4j | `5.26.29-community` | `5.26.33-community` | 5.26 为 LTS 线，仍在更新 |
| Elasticsearch / Logstash / Kibana | `9.2.3` | `9.5.5` | 9.x 最新稳定补丁，三者必须同版本 |
| Nacos | `v3.1.1` | `v3.3.0` | 最新正式版；与 3.x 客户端同主版本 |
| Sentinel Dashboard | `1.8.9` | `1.8.10` | 最新正式版；与 sentinel-core 1.8.x 兼容 |
| Seata Server | `2.5.0` | `2.6.0` | 最新正式版；2.x 客户端向后兼容 |
| RocketMQ | `5.3.1` | `5.5.0` | 最新正式版；5.x gRPC 客户端兼容 |
| Kafka | `4.1.1` | `4.4.0` | 最新正式版；broker 向下兼容 4.1 客户端 |
| Tempo | `2.10.8` | `2.10.9` | 2.11/2.12 尚无稳定标签 |
| SkyWalking BanyanDB | `0.11.0` | `0.11.2` | 保持 server API 0.11，与 OAP 11.1 匹配 |
| SkyWalking OAP | `11.0.0` | `11.1.0` | 与 Horizon 1.1.0 同批发布 |
| SkyWalking Horizon UI | `horizon-1.0.0` | `horizon-1.1.0` | 与 OAP 11.1.0 配套 |
| Prometheus | `v2.53.4` | `v3.16.0` | 3.x 最新稳定；2.x 已进入维护尾部 |
| Grafana | `12.2.0` | `13.2.6` | 13.x 最新稳定补丁 |
| Alertmanager | `v0.29.0` | `v0.35.0` | 最新稳定 |
| Thanos | `v0.36.1` | `v0.42.5` | 最新稳定 |
| mysqld-exporter | `v0.15.0` | `v0.21.0` | 最新稳定 |
| redis_exporter | `v1.69.0` | `v1.97.0` | 最新稳定 |
| node-exporter | `v1.10.2` | `v1.12.1` | 最新稳定 |
| elasticsearch-exporter | `v1.5.0` | `v1.11.0` | 最新稳定 |
| OTel Collector | `0.140.0` | `0.165.0` | 最新稳定（0.166 仅 nightly） |
| Maven | `3.9.14-eclipse-temurin-25` | `3.10.0-eclipse-temurin-25` | 3.10.0 正式版，JDK 25 与运行期一致 |
| Node | `22-bookworm-slim` | `22.23.5-bookworm-slim` | 保持 22 LTS，固定补丁 |
| Temurin | `25-jre` | `25.0.5_8-jre` | JDK 25 最新补丁，固定标签 |
| Nginx | `stable-alpine3.23` | `1.30.6-alpine3.24` | stable 线最新补丁，替代浮动标签 |

**弃选方案**：MySQL 9.7 innovation（非 LTS，生命周期短）；Node 24（前端工程未声明 engines，保持 22 降低工具链风险）；Neo4j 2026.x CalVer（驱动兼容性需额外验证，5.26 LTS 仍在维护）。

### D3. MinIO 例外与安全风险

- 拉取路径固定为 `swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/minio/minio:RELEASE.2025-09-07T16-13-09Z`，随后 `docker tag` 为 `docker.io/minio/minio:...`，并删除临时 SWR 标签（`docker rmi ... || true`，避免重复执行失败）。
- 该版本是上游停止发布镜像前最后一批官方镜像，**不含 2025-10-15 安全修复**。本次不更换来源；风险与后续选项（社区重建、自建镜像、替代 S3 实现）记录在 Risks。
- 若 aityp/SWR 不可达，脚本必须失败，不回退到 Docker Hub/Quay。

### D4. Elastic 镜像来源差异

Docker Hub 的 `library/elasticsearch|logstash|kibana:9.5.5` 与 `docker.elastic.co` 的同版本镜像 **digest 不同**（实测 amd64 子清单不同），说明不是同一份构建产物。由于需求明确要求 Docker Hub，本次接受该差异，并用 ES 健康、Logstash 管道、Kibana 可达性回归验证行为；若出现行为差异，另起变更评估是否回到 `docker.elastic.co`。

### D5. 脚本实现

- 非 MinIO 直接 `docker pull docker.io/<repo>:<tag>`；官方镜像显式写 `docker.io/library/...`，避免依赖短名解析。
- 保留逐镜像的 `echo` 分段输出，便于定位失败点；`set -e` 保持不变。
- MinIO 分支保留 pull → tag → rmi 三步；`rmi` 加 `|| true` 保证幂等。
- 删除“全部从 SWR 拉取再重打标签”的统一逻辑；不引入镜像清单数组或配置文件，保持脚本直白可读。
- 末尾镜像计数改为使用脚本维护的镜像列表计算（或保留现行为），避免 `docker images | wc -l` 把无关镜像算进去。

### D6. 引用同步

- `docker/docker-compose.yml`：替换全部 `image:` 标签；`docker.elastic.co/...` 改为 `library/...` 短名。
- `docker/Dockerfile.backend`、`Dockerfile.dbinit`：`eclipse-temurin:25.0.5_8-jre`。
- `docker/Dockerfile.backend-builder`：`maven:3.10.0-eclipse-temurin-25`。
- `iboot-portal/Dockerfile`：`node:22.23.5-bookworm-slim` 与 `nginx:1.30.6-alpine3.24`。
- `scripts/docker-deploy.sh` 的 `BASE_IMAGES` 同步为四个新标签。
- RocketMQ 5.5.0：`docker/docker-compose.yml` 的 proxy 挂载路径与 `docker/rocketmq/init-topics.sh` 的 `ROCKETMQ_HOME` 默认值都从 `rocketmq-5.3.1` 改为 `rocketmq-5.5.0`；启动后用 `docker exec` 确认目录存在。
- 文档：`scripts/README.md` 的镜像站描述改为 Docker Hub（MinIO 例外）。

### D7. 清理策略（仅项目范围）

1. 记录当前项目镜像清单：`docker compose -f docker/docker-compose.yml --profile build --profile apm --profile observability --profile graph config --images`，再补充 `iboot-backend-builder:local`。
2. 停止并删除项目容器与卷：
   `docker compose -f docker/docker-compose.yml --profile build --profile apm --profile observability --profile graph down -v --remove-orphans`
3. 仅删除上一步清单中的镜像；**不执行** `docker image prune -a`，保留 `getmeili/meilisearch:v1.49.0` 等无关镜像。
4. 若项目卷仍残留（例如旧 project name），用 `docker volume ls` 过滤项目前缀后逐个删除；不删除非项目卷。

### D8. 回归方案

按“基础设施 → 应用 → 契约 → 业务链路 → 观测链路”分层，任一层失败即停止并保留日志：

1. **构建**：`scripts/build.sh all`（前端用 cnpm，后端 Maven 使用 `/home/lam/repo/settings.xml`）；再用 `docker compose ... build` 验证多阶段镜像。
2. **启动**：`scripts/docker-deploy.sh up full` 构建并拉起容器化核心 + observability + graph 栈；再单独以 `--profile apm` 启动 SkyWalking 三件套，`docker compose ps` 全部 healthy。**不**使用 `dev-init.sh` 验证应用镜像，因为它运行宿主机 fat jar 而非新构建的运行期镜像。
3. **契约与权限**：`scripts/regress/regress.sh`（契约快照、路径守恒、路由覆盖、冒烟、权限矩阵）。**禁止**用 `--refresh` 掩盖漂移。
4. **分布式事务**：`scripts/regress/seata.sh`、`scripts/regress/seata-tcc.sh`；按脚本要求分别以 `seata-demo` / `seata-tcc-demo` profile 重启 workflow。
5. **日志管道**：`scripts/regress/log-pipeline.sh --drills`（Kafka/Logstash 停启演练）。
6. **链路追踪**：发起一次审批请求，取 `X-Trace-Id` 后执行 `scripts/regress/trace-chain.sh <traceId> --expect iboot-gateway,iboot-workflow`；APM profile 下确认 OAP/BanyanDB/UI 健康。
7. **前端**：`scripts/build.sh frontend` 与门户容器健康检查。
8. **证据留存**：保存每步命令、退出码、失败日志与 `docker compose ps` 输出。

### D9. 漏洞姿态

当前机器未安装 Trivy/Grype/Docker Scout。选型以“最新稳定补丁”作为已知漏洞修复的代理指标，并在设计/任务中明确该限制；不在本次引入扫描器。若后续需要量化，另起变更接入 `trivy image --severity HIGH,CRITICAL`。

## Risks / Trade-offs

- **Docker Hub 拉取失败**（代理/镜像站限流） → 应用阶段先做 preflight：逐镜像 `docker manifest inspect` 或拉取最小镜像；失败即停止，不改用其它来源。
- **跨大版本行为变化**（Prometheus 3、Grafana 13、Nacos 3.3、Seata 2.6、RocketMQ 5.5） → 分阶段启动 + 全量回归；失败时按组件回退到上一稳定 minor，并记录差异。
- **Elastic Docker Hub 镜像与官方 registry 产物不同** → 用 ES 健康、Logstash 管道、Kibana 页面回归验证；若行为不兼容，另起变更评估来源。
- **MinIO 无安全补丁镜像** → 保持 2025-09-07 并在文档中标注；后续可选社区重建/自建/替代实现，但不混入本次变更。
- **RocketMQ 版本路径硬编码** → 同时改 compose 与 `init-topics.sh`，启动后确认 `/home/rocketmq/rocketmq-5.5.0/conf` 存在。
- **MySQL 8.4 兼容性**（`utf8mb3` 弃用、`mysql_native_password` 默认关闭） → connector 9.1.0 使用 `caching_sha2_password`；`iboot-db-init` 全量建库回归；若迁移 SQL 报错，修复 SQL 而不是降级服务端。
- **内存压力**（Docker VM 停容器后仍占约 25GB） → 全量启动前用 `docker system df` 检查，必要时重启 Docker 释放内存。
- **删除项目镜像后无法快速回滚到旧版本** → 旧标签记录在变更说明中；回滚时按旧矩阵重新 pull（镜像可重新获取）。
- **APM 三件套版本错配** → 必须同批升级；启动顺序按 BanyanDB → OAP → UI，健康检查失败即停止。

## Migration Plan

1. Preflight：记录当前镜像标签/digest；验证 Docker Hub 与 aityp 可达；检查 Docker VM 内存。
2. 停止并清理项目栈、项目卷（全 profile）。
3. 删除本项目镜像（按 `config --images` 清单 + builder），保留无关镜像。
4. 修改拉取脚本、compose、Dockerfile、`docker-deploy.sh`、RocketMQ 路径与文档。
5. 执行新的 `scripts/pull-docker-images.sh`，确认每个镜像拉取成功且本地标签与引用一致。
6. 构建并启动全量栈，等待全部健康。
7. 执行 D8 的完整回归并留存证据。
8. 回滚策略：revert 本次提交 → 按旧矩阵重新 pull → 重建 → 重启；因数据卷已按临时环境清空，无需数据回滚。

## Open Questions

（无会改变规格、方案或任务拆分的未决问题。MinIO 安全升级路径作为后续独立变更评估。）
