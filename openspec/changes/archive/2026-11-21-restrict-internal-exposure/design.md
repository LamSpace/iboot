# Design

## Context

当前全量编排把所有组件放在单个 `iboot-network` 上，并向宿主机发布 48 个端口，全部绑定 `0.0.0.0`。宿主机开发依赖这些映射访问中间件；Docker 全量部署只应暴露门户。约束与动机见 `proposal.md`，行为契约见 `specs/`。

已确认的现有依赖：

- 门户 nginx 只反向代理网关、系统服务与运维控制台，不代理 Nacos、ES、Silo。
- Prometheus 通过 `host.docker.internal:8080-8089` 抓取宿主机开发模式下的服务；Docker 全量模式下服务是容器。
- Grafana 依赖 Prometheus、Thanos 与 Elasticsearch；Kibana 与 Logstash 依赖 Elasticsearch；Thanos 与 Tempo 依赖 Silo。
- 宿主机开发脚本（`dev-init.sh`）、回归脚本与 CLI 全部使用 `localhost`。

## Goals / Non-Goals

**Goals:**

- 宿主机上只有门户 80/443 跨网卡监听，其余端口只监听回环地址。
- 用编排网络表达「门户不能直达数据面」这一边界，同时保持现有服务依赖与可观测链路可用。
- 暴露面可被回归自动判定，宿主机开发与 Docker 全量两种模式都不退化。

**Non-Goals:**

- 不处理中间件账号与鉴权（B4.3），不引入服务间 mTLS 或工作负载身份（B5）。
- 不改变门户、服务、数据面的 TLS 配置与证书。
- 不改变宿主机开发模式下 Java 进程的 `server.address`；该监听地址与 Prometheus 采集架构绑定，留待 B4.3/B5 一并收敛。
- 不引入 Kubernetes NetworkPolicy、服务网格或主机防火墙规则。
- 不为可观测 UI 增加 TLS 或登录。

## Decisions

### 1. 端口绑定：内部端口硬编码回环，不提供全接口开关

所有非门户端口改为 `127.0.0.1:<host>:<container>`，门户保持 `80:80` / `443:443`。不引入 `IBOOT_BIND_ADDR` 之类的覆盖变量，避免一次环境变量误设就把内部端口重新公开；需要临时从其它主机访问时，用 SSH 端口转发。

不采用「绑定 `0.0.0.0` 再靠主机防火墙限制」：Docker 的 DNAT 规则会绕过常见的主机防火墙链，结果不可验证。

**IPv6 取舍**：只绑定 IPv4 回环。Linux 上 `localhost` 通常同时解析到 `::1` 与 `127.0.0.1`，curl 与浏览器会回落，JDK 默认优先 IPv4；实施时用 `localhost` 与 `127.0.0.1` 各跑一遍回归。若发现某个客户端只尝试 `::1`，优先把该客户端地址改为 `127.0.0.1`，而不是增加第二套绑定。

### 2. 移除宿主映射：只保留开发与验收真正需要的端口

移除以下映射，组件改在编排网络内通信：

- `logstash:5044`：日志管道使用 Kafka 输入，没有 Beats 入口。
- `skywalking-banyandb:17913`、`skywalking-oap:17128`：存储与内部端口。
- `thanos-sidecar:19191`、`thanos-store:19193`、`thanos-query:19192`、`thanos-compact:19194`：Grafana 在 `obs` 网络内查询 `thanos-query`。
- `mysql-exporter:9104`、`redis-exporter:9121`、`node-exporter:9100`、`elasticsearch-exporter:9114`：Prometheus 在编排网络内抓取。

保留并改为回环的端口：网关、八个业务服务与运维控制台 8080-8089、MySQL 3306、Redis 6379、Silo 9000/9001、ES 9200、Nacos 8848/9848/8849、Sentinel 8858、Seata 8091、RocketMQ Proxy 18080/18081、Kafka 29092、Tempo 4317/4318/3200、SkyWalking OAP 11800/12800、SkyWalking UI 18082、Prometheus 9090、Grafana 3000、Alertmanager 9093、Kibana 5601、Neo4j 7474/7687。

### 3. 网络分段：四个网络，门户只在 edge

| 网络 | 成员 |
|---|---|
| `edge` | 门户、网关、系统服务、运维控制台 |
| `app` | 网关、系统服务、运维控制台、八个业务服务 |
| `data` | MySQL、Redis、Silo、ES、Nacos、Seata、RocketMQ、Kafka、Tempo、Neo4j、Sentinel、网关、系统服务、运维控制台、八个业务服务、全部初始化任务、Logstash、Kibana、Grafana、Thanos、exporter |
| `obs` | Prometheus、Grafana、Alertmanager、Thanos、SkyWalking、exporter、node-exporter |

关键点：

- 门户只加入 `edge`，因此对 `mysql`、`redis`、`elasticsearch`、`nacos` 等服务名无法解析，更无法连接。
- 系统服务与运维控制台加入 `edge`，因为门户直接代理它们的 `/druid`、`/swagger`、`/sba` 路径。
- Prometheus 加入 `app` 与 `obs`：前者用于抓取容器化业务服务，后者用于抓取 exporter 与自身。
- Grafana、Kibana、Logstash、Thanos 按实际依赖加入 `data`；Tempo 加入 `data`，因为业务服务经 `tempo:4318` 上报并写入 Silo。
- `x-app-common` 锚点默认给业务服务 `app` + `data`，网关、系统服务、运维控制台显式覆盖为三网络。

不采用更细的「每个服务一张网络」：本地单机栈里维护成本高，收益主要是限制横向移动，而当前目标是把门户与数据面切开；更细的微分段留给 B5 与生产编排。

### 4. Prometheus 抓取目标按运行模式切换

Docker 全量模式下，服务端口只绑定回环地址，Prometheus 容器无法再通过 `host.docker.internal` 访问宿主映射；宿主机开发模式则相反，服务是宿主进程，只能通过 `host.docker.internal` 抓取。

采用两份配置：

- `docker/scripts/prometheus/prometheus.yml`：保持宿主开发目标 `host.docker.internal:8080-8089`。
- `docker/scripts/prometheus/prometheus.docker.yml`：目标改为 `iboot-system:8080` 等服务名。
- Compose 的 `--config.file` 默认读取 `prometheus.docker.yml`，使直接 `docker compose up` 与 `docker-deploy.sh` 都得到正确配置；`dev-init.sh` 在宿主机开发模式导出 `IBOOT_PROMETHEUS_CONFIG=prometheus.yml`。

不采用「两份 job 都保留」：失败的 target 会持续产生 down 指标与告警，验收时无法区分真实故障。也不采用 host 网络模式：会破坏 Grafana 对 `prometheus:9090` 的服务名解析，并且在 Docker Desktop 上不可用。

### 5. 暴露面回归：静态绑定检查 + 运行时连通矩阵

新增 `scripts/regress/exposure.sh`，分三层：

1. **端口绑定**：用 `docker inspect` 读取全部 `iboot-*` 容器的 `HostConfig.PortBindings`，断言门户 80/443 为 `0.0.0.0`，其它绑定为 `127.0.0.1`；断言已移除的端口不再出现。
2. **非回环不可达**：取宿主机第一个非回环 IPv4，对 3306、6379、9200、8848、29092 发起带超时的 TCP 连接，必须失败；同时用 `127.0.0.1` 验证同一端口可用。
3. **容器矩阵**：门户容器能访问 `iboot-gateway:8088`，不能解析或连接 `mysql`/`redis`/`elasticsearch`/`nacos`；业务容器能连接 MySQL 与 Nacos。门户或业务容器不存在时（宿主机开发模式）明确跳过并输出原因。

脚本接入 `regress.sh` 的 TLS 检查之后、冒烟之前。CI 的全栈任务使用 `dev-init.sh`，会执行端口绑定与非回环检查，容器矩阵自动跳过；Docker 全量部署则三层全跑。

不采用 `nmap`/`ss` 解析作为唯一依据：`docker inspect` 给出的是编排的声明结果，比解析 `ss` 输出稳定，也不依赖宿主机工具。

## Risks / Trade-offs

- **`localhost` 解析到 `::1` 而回环只绑定 IPv4** → 回归同时用 `localhost` 与 `127.0.0.1` 验证；失败时把对应客户端地址固定为 `127.0.0.1`，不扩大监听面。
- **Prometheus 全量模式抓取中断** → 双配置 + `docker-deploy.sh` 显式选择；验收检查 Prometheus targets 中九个服务为 up。
- **网络分段遗漏依赖** → 按上表逐服务核对 `depends_on` 与端口；Docker 全量启动后用连通矩阵与全量回归验证，门户负向用例专门覆盖。
- **移除 exporter/Thanos 映射影响外部采集** → 仅本地开发栈，文档写明；需要外部采集时由使用方自行增加映射，不默认开放。
- **初始化任务需要 `data` 网络** → `db-init`、`silo-init`、`kafka-init`、`elasticsearch-init`、`rocketmq-*init` 统一加入 `data`，与对应中间件同网。
- **宿主机开发进程仍监听所有网卡** → 本变更只收敛 Docker 发布面；宿主机 Java 进程的绑定地址与 Prometheus 采集方式耦合，变更会牵动 host-gateway 抓取，留待 B4.3/B5 统一处理并在文档中登记为已知边界。
- **回滚成本** → 只改编排与脚本，不动数据卷；回滚为恢复上一版 compose 与 `regress.sh` 后重新 `up -d`，数据不受影响。

## Migration Plan

1. 修改 compose 的端口绑定、网络成员与网络定义。
2. 新增 Docker 模式 Prometheus 配置，`docker-deploy.sh` 导出配置文件名。
3. 新增 `exposure.sh` 并接入 `regress.sh`。
4. 更新部署指南、架构设计与 TLS 操作指南的端口表与边界说明。
5. 验证顺序：宿主机开发（`dev-init.sh`）→ 全量回归 → Docker 全量（`docker-deploy.sh up`）→ 暴露面矩阵。
6. 回滚：恢复上一版 compose/脚本并重启容器；不删除数据卷。

## Open Questions

无。剩余决策（中间件账号、服务身份）属于后续 B4.3 / B5 变更。
