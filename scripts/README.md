# 脚本索引

本目录按用途分三类：开发、部署与回归。所有脚本都在仓库根目录执行，
私有配置从根目录 `.env` 读取（模板见 `.env.example`）。

## 开发

| 脚本 | 用途 |
|---|---|
| `dev-init.sh` | 一键拉起本地环境：中间件 → 按需构建 → `iboot-db-init` 建库建表 → 启动九个业务服务与运维控制台 → 等待健康。支持 `--frontend`、`--status`、`--stop`、`--down`、`--purge`。 |
| `build.sh` | 前后端一键构建。参数：`all`（默认）、`frontend`、`backend`、`help`。 |

## 部署

| 脚本 | 用途 |
|---|---|
| `docker-deploy.sh` | 全栈 Docker 入口：停止宿主机进程 → 确保基础镜像 → 构建共享 builder → 构建运行期镜像 → Compose 启动并等待健康。支持 `up [core\|observability\|graph\|full]`、`status`、`logs`、`down`、`purge --yes`。 |
| `pull-docker-images.sh` | 从 SWR 镜像站拉取构建与运行期基础镜像，供 `docker-deploy.sh` 与 Compose 使用。 |

## 回归

回归脚本要求九个业务服务、网关与中间件已启动，详细用法见
`docs/23-微服务底座操作指南.md`。

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
| `regress/regress.sh` | 总入口：契约快照 → 路径守恒 → 路由覆盖 → 冒烟 → 权限矩阵；`--refresh` 重建基线。 |
| `regress/api-snapshot.sh` | 导出服务的 OpenAPI 快照并归一化为基线。 |
| `regress/equivalence.sh` | 只读接口的响应等价性录制与重放比对。 |
| `regress/path-conservation.sh` | 校验服务迁移前后的路径并集守恒。 |
| `regress/permission-matrix.sh` | 角色 × 接口 × 期望结果的权限边界矩阵。 |
| `regress/smoke.sh` | 登录、用户、文件、推送、任务、备份、审批的关键链路冒烟。 |
| `regress/seata.sh` | Seata AT 跨服务验收：正常提交、额度不足、后置失败回滚、TC/RM 不可用、并发与幂等。 |
| `regress/trace-chain.sh` | 按 TraceID 查询链路并校验期望服务是否出现。 |

## 历史脚本

五次服务拆分使用的一次性 SQL 搬迁脚本、以及旧的系统级依赖安装脚本已经删除。
需要回看时使用：

```bash
git log --all --diff-filter=D -- 'scripts/*.sql'
```

`scripts/regress/golden/` 中的 `*-equivalence.json` 是当时改写的基线，仍供
`equivalence.sh` 使用，不属于历史脚本。
