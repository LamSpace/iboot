# 脚本索引

本目录按用途分三类：开发、部署与回归。所有脚本都在仓库根目录执行，
私有配置从根目录 `.env` 读取（模板见 `.env.example`）。

## 开发与初始化

| 脚本 | 用途 |
|---|---|
| `dev-init.sh` | 一键拉起本地环境：中间件 → 按需构建 → `iboot-db-init` 建库建表 → MySQL 账号与表级授权 → 启动九个业务服务与运维控制台 → 等待健康。支持 `--frontend`、`--status`、`--stop`、`--down`、`--purge`。 |
| `tls-init.sh` | 生成本地 TLS CA、门户/服务/客户端证书与 Java 信任库；证书默认写入 `docker/certs/`。支持 `--force` 重签与 `--check` 只检查剩余有效期。浏览器信任需另外执行一次 `mkcert -install`。 |
| `tls-rotate.sh` | 证书轮换：`--check` 只检查，满足阈值时才重签并分发；`--restart <mode>` 控制是否重启服务。 |
| `tls-rotate-schedule.sh` | 安装/卸载/查看证书轮换定时任务；优先 systemd user timer，回退到带标记的 crontab；`--scheduler docker` 使用 opt-in 的 `tls-rotate` compose profile。 |
| `tls-ca-rotate.sh` | 三阶段 CA 轮换（prepare/activate/finalize）：信任 bundle 过渡、重签叶子、收敛旧 CA；支持 `--restart rolling`。 |
| `build.sh` | 前后端一键构建。参数：`all`（默认）、`frontend`、`backend`、`help`。 |
| `mysql-users-init.sh` | 幂等创建每服务 MySQL/Redis/Nacos 账号与 exporter 只读账号。 |
| `mysql-grants-init.sh` | 按 `mysql-table-grants.tsv` 收紧到表级授权；先 `REVOKE ALL`，再授予只读与各服务写表。 |
| `mysql-table-grants.tsv` | 每服务可写表清单；`ACT_*`、`FLW_*`、`QRTZ_*` 等通配由脚本通过 `information_schema` 展开。 |
| `es-users-init.sh` | 创建 Elasticsearch 专用账号并设置口令。 |
| `nacos-init.sh` | 初始化 Nacos 鉴权与命名空间。 |
| `sentinel-init.sh` | 写入 Sentinel 基线规则（规则以 Nacos 为权威来源）。 |
| `silo-init.sh` | 初始化 Silo 根账号、服务桶与访问密钥。 |
| `spire-up.sh` / `spire-down.sh` | 启动/停止 SPIRE Server、Agent 与 demo 工作负载；`dev-init.sh` 默认启动 SPIRE 供服务 SVID 使用，`spire-down.sh` 只删除 SPIRE 容器，保留主栈。 |
| `spire-svid-sync.sh` | 为十个服务注册 SVID 条目、取回证书、合并 SPIRE CA 到信任 bundle；`--refresh` 重新取回，`--status` 查看有效期与 SAN，`--check` 按阈值判断是否需要刷新。 |
| `docker-svid-refresh.sh` | Compose 全量模式 SVID 刷新：重建 `svid-*` sidecar 写入命名卷，并滚动重启十个业务容器。 |
| `svid-refresh-schedule.sh` | SVID 到期检查与刷新调度：systemd user timer 或 cron 每日检查，低于阈值才刷新宿主机或 Compose 栈；结果写日志与 Prometheus 文本指标。 |
| `host-loopback-relay.py` | 原生 Docker 无法直连宿主机回环时的 TCP relay：只绑定 Docker 网桥网关，转发到 `127.0.0.1:8080-8089`。 |
| `apm/` | SkyWalking 对比实验：`fetch-agent.sh` 下载 Agent，`measure.sh` 采样，`verify.sh` 双后端验收。 |

## 部署

| 脚本 | 用途 |
|---|---|
| `docker-deploy.sh` | 全栈 Docker 入口：停止宿主机进程 → SVID 初始化（默认）→ 确保基础镜像 → 构建共享 builder → 构建运行期镜像 → Compose 启动并等待健康。支持 `up [core\|observability\|graph\|full]`、`status`、`logs`、`down`、`purge --yes`；`IBOOT_SVID_ENABLED=false` 回退 mkcert。 |
| `pull-docker-images.sh` | 从 Docker Hub 拉取构建与运行期镜像（对象存储使用 pgsty/silo），供 `docker-deploy.sh` 与 Compose 使用。 |

## 回归

回归脚本要求九个业务服务、网关与中间件已启动，详细用法见
`docs/23-微服务底座操作指南.md`。

TLS 环境下回归默认通过宿主机 HTTPS 门户（`https://localhost:5173`）访问，因此宿主机开发
需先执行 `dev-init.sh --frontend` 启动门户开发服务器；Docker 全量部署覆盖为
`https://localhost`，需门户容器健康。证书统一用 `REG_CACERT`（默认
`docker/certs/ca/rootCA.pem`）校验，禁止使用 `-k` 绕过。

Docker 全量回归中的备份用例需要为 `iboot-job` 挂载 Docker Socket 并启用
`IBOOT_BACKUP_ENABLED`；默认 Docker 部署关闭备份，该用例会失败，属于已登记的部署限制
（见 [`docker/README.md`](../docker/README.md)）。

### 回归分层

| 层 | 脚本 | 依赖 | 运行时机 |
|---|---|---|---|
| 契约快照 | `regress.sh` + `api-snapshot.sh` | 8 个服务已启动 | 定时 / 手动 |
| 路径守恒 | `path-conservation.sh` | `golden/` + `migrations.txt` | 全栈回归 |
| 路由覆盖 | `route-coverage.sh` | `golden/` + `routes.txt` | PR（静态） |
| 行为回归 | `smoke.sh` + `permission-matrix.sh` | 全栈 | 定时 / 手动 |
| 安全边界 | TLS / 授权 / 最小权限 / 暴露面等专项脚本 | 全栈 + 证书 | 定时 / 手动 |

CI 的 PR 任务只跑路由覆盖、构建与网关测试；定时 / 手动任务先用 `dev-init.sh` 拉起全栈，
再执行 `regress.sh`。临时快照写入 `regress/.tmp/`，该目录不入库。

### 回归脚本

`regress.sh` 是唯一入口，按下列顺序执行，任一失败即非零退出：

| 阶段 | 脚本 | 用途 |
|---|---|---|
| 1 | `api-snapshot.sh` | 导出服务的 OpenAPI 快照并归一化为基线。 |
| 2 | `path-conservation.sh` | 校验服务迁移前后的路径并集守恒。 |
| 3 | `route-coverage.sh` | 每条接口路径都能被唯一路由规则解析。 |
| 4 | `tls.sh` | 公开入口 TLS：证书、协议版本、明文重定向、issuer 与回调一致性。 |
| 5 | `internal-tls.sh` | 内部 TLS：服务端口、网关→服务、JWKS、MySQL/Redis/Silo/Nacos/Neo4j 负向矩阵。 |
| 5b | `service-authz.sh` | 服务间路径级授权矩阵与 403 行为。 |
| 5c | `opa-authz.sh` | OPA 健康、策略单测、Java 客户端与 fail-closed。 |
| 6 | `observability-tls.sh` | Grafana/Kibana/Alertmanager/Thanos 等控制台 TLS。 |
| 6b | `exporter-mtls.sh` | exporter 客户端证书与 Prometheus 抓取。 |
| 7 | `tls-cert-monitor.sh` | 证书到期指标与告警规则。 |
| 7a | `spire-svid-monitor.sh` | SPIRE 健康探针、SVID 指标、故障注入与 Alertmanager 路由。 |
| 7b | `tls-rotate.sh --check` | 证书轮换检查模式（不重签）。 |
| 7c | `tls-rotation-schedule.sh` | 轮换调度安装/卸载/状态（临时 HOME 与假命令）。 |
| 8 | `middleware-auth.sh` | MySQL/Redis/ES/Kafka/RocketMQ 认证与越权拒绝。 |
| 8a | `middleware-least-privilege.sh` | 表级与 Redis 键级最小权限。 |
| 8b | `rocketmq-acl.sh` | RocketMQ ACL 账号与主题权限。 |
| 8c | `rocketmq-server-auth.sh` | RocketMQ 服务端证书校验兼容层。 |
| 9 | `log-search-tls.sh` | ES HTTPS/认证、Kafka SSL-only、Logstash 全链路。 |
| 10 | `exposure.sh` | 宿主机暴露面与网络分段。 |
| 10b | `host-loopback.sh` | 宿主机进程与 relay 只绑定回环。 |
| 11 | `plaintext-audit.sh` | 默认地址与明文回退审计。 |
| 12 | `smoke.sh` | 登录、用户、文件、推送、任务、备份、审批关键链路。 |
| 12b | `spire-svid.sh` | SPIRE Workload API 与 demo SVID。 |
| 12c | `tls-rotate-scheduler.sh` | 证书轮换调度配置与一次性容器检查。 |
| 12d | `ca-rotation.sh` | 三阶段 CA 轮换与 prepared/activated 回滚（临时证书目录）。 |
| 12e | `silo-client-cert.sh` | Silo 客户端证书换取临时凭据。 |
| 12f | `nacos-namespace-isolation.sh` | Nacos 配置命名空间隔离。 |
| 12g | `spire-svid-services.sh` | 十个服务的 SVID SAN、有效期与 mTLS 健康检查。 |
| 12h | `compose-svid.sh` | Compose SVID 覆盖层、sidecar 镜像与单服务取证。 |
| 12i | `svid-refresh-schedule.sh` | SVID 刷新调度安装、幂等、检查/刷新路径、锁与指标。 |
| 12j | `tls-rotate-svid.sh` | `tls-rotate` 在 Compose SVID 栈上的重启路径委托。 |
| 13 | `permission-matrix.sh` | 角色 × 接口 × 期望结果的权限边界矩阵。 |

独立验收脚本（不在 `regress.sh` 主链路内）：

| 脚本 | 用途 |
|---|---|
| `seata.sh` | Seata AT 跨服务验收：正常提交、额度不足、后置失败回滚、TC/RM 不可用、并发与幂等。 |
| `seata-tcc.sh` | Seata TCC 额度划转验收：正常划转、Try 后取消、空回滚、并发、权限、TC/system 不可用。 |
| `trace-chain.sh` | 按 TraceID 查询链路并校验期望服务是否出现。 |
| `cli.sh` | 开发者 CLI 端到端验收。 |
| `log-pipeline.sh` | Kafka → Logstash → ES 日志管道单独验收。 |
| `equivalence.sh` | 迁移期一次性校验：迁移前从旧实现录制，迁移后经网关重放比对。 |

> `equivalence.sh` 的 golden 依赖录制时的数据库行 ID、时间戳与备份哈希，只在
> 「旧实现 → 新实现」的迁移窗口内有效。日常回归会写入新数据，出现差异属于状态漂移；
> 不要用 `--record` 覆盖历史基线来消除差异。

## 历史脚本

五次服务拆分使用的一次性 SQL 搬迁脚本、以及旧的系统级依赖安装脚本已经删除。
需要回看时使用：

```bash
git log --all --diff-filter=D -- 'scripts/*.sql'
```

`scripts/regress/golden/` 中的 `*-equivalence.json` 是当时改写的基线，仍供
`equivalence.sh` 使用，不属于历史脚本。
