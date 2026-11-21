# 脚本索引

本目录按用途分三类：开发、部署与回归。所有脚本都在仓库根目录执行，
私有配置从根目录 `.env` 读取（模板见 `.env.example`）。

## 开发

| 脚本 | 用途 |
|---|---|
| `dev-init.sh` | 一键拉起本地环境：中间件 → 按需构建 → `iboot-db-init` 建库建表 → 启动九个业务服务与运维控制台 → 等待健康。支持 `--frontend`、`--status`、`--stop`、`--down`、`--purge`。 |
| `tls-init.sh` | 生成本地 TLS CA、门户证书与 Java 信任库；证书默认写入 `docker/certs/`。支持 `--force` 重签与 `--check` 只检查剩余有效期。浏览器信任需另外执行一次 `mkcert -install`。 |
| `build.sh` | 前后端一键构建。参数：`all`（默认）、`frontend`、`backend`、`help`。 |

## 部署

| 脚本 | 用途 |
|---|---|
| `docker-deploy.sh` | 全栈 Docker 入口：停止宿主机进程 → 确保基础镜像 → 构建共享 builder → 构建运行期镜像 → Compose 启动并等待健康。支持 `up [core\|observability\|graph\|full]`、`status`、`logs`、`down`、`purge --yes`。 |
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

CI 的 PR 任务只跑路由覆盖、构建与网关测试；定时 / 手动任务先用 `dev-init.sh` 拉起全栈，
再执行 `regress.sh`。临时快照写入 `regress/.tmp/`，该目录不入库。

| 脚本 | 用途 |
|---|---|
| `regress/regress.sh` | 总入口：契约快照 → 路径守恒 → 路由覆盖 → 公开 TLS → 内部 TLS → 日志与检索 TLS → 冒烟 → 权限矩阵；`--refresh` 重建基线。 |
| `regress/tls.sh` | 公开入口 TLS：证书、协议版本、明文重定向、issuer 与回调一致性。 |
| `regress/internal-tls.sh` | 内部 TLS：服务端口、网关→服务、JWKS、MySQL/Redis/Silo/Neo4j 的负向矩阵。 |
| `regress/log-search-tls.sh` | 日志与检索 TLS：ES HTTPS/认证、Kafka SSL-only、Logstash 全链路、RocketMQ 加密边界。 |
| `regress/api-snapshot.sh` | 导出服务的 OpenAPI 快照并归一化为基线。 |
| `regress/equivalence.sh` | 迁移期一次性校验：迁移前从旧实现录制，迁移后经网关重放比对。 |
| `regress/path-conservation.sh` | 校验服务迁移前后的路径并集守恒。 |
| `regress/permission-matrix.sh` | 角色 × 接口 × 期望结果的权限边界矩阵。 |
| `regress/smoke.sh` | 登录、用户、文件、推送、任务、备份、审批的关键链路冒烟。 |
| `regress/seata.sh` | Seata AT 跨服务验收：正常提交、额度不足、后置失败回滚、TC/RM 不可用、并发与幂等。 |
| `regress/seata-tcc.sh` | Seata TCC 额度划转验收：正常划转、Try 后取消、空回滚、并发、权限、TC/system 不可用。 |
| `regress/trace-chain.sh` | 按 TraceID 查询链路并校验期望服务是否出现。 |

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
