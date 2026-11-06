# Design

## Context

动机见 `proposal.md`。实现约束来自三处：

1. `scripts/regress/regress.sh` 采集 `system/ai/file/auth/notify/job/workflow/monitor`
   八个服务的 OpenAPI，随后执行冒烟与权限矩阵；`lib.sh` 的登录固定走 `REG_AUTH_BASE`
   （默认 8083）。
2. `scripts/dev-init.sh` 已经是本地启动的权威路径：中间件（mysql/redis/minio/es/nacos/tempo）
   → 按需构建 → `iboot-db-init` → 九个业务服务与网关 → 健康等待。
3. `iboot-ai` 的 `AiConfigValidator` 在 `DASHSCOPE_CHAT_API_KEY` 为空时拒绝启动；
   `iboot-notify` 的 mail host 默认为空，默认 mail health 可能把 `/actuator/health` 拉成 DOWN。

## Goals / Non-Goals

**Goals:**

- 让定时/手动的 `full-regress` 真正覆盖 `regress.sh` 要求的全部服务。
- 让 CI 的启动方式与本地开发共用同一套脚本，避免 YAML 与脚本再次分叉。
- 在标准 GitHub 托管 runner 的内存范围内完成启动。

**Non-Goals:**

- 不修改 `regress.sh` 的检查范围，不引入服务过滤。
- 不改变 PR 阶段的轻量检查（路由覆盖 + 构建 + 网关测试）。
- 不为 CI 新建 Docker 镜像或改变 Compose 编排。

## Decisions

### D1. 用 `dev-init.sh` 取代 YAML 中的手写启动

`full-regress` 只保留三步：Maven 构建 → `dev-init.sh` → `regress.sh`。

**备选**：在 YAML 中继续逐个 `nohup java -jar`。**不采用**：需要复制服务顺序、db-init、
Nacos/ES/MinIO 健康等待与日志路径，任何一项变化都会造成 CI 与本地不一致；当前故障就是
这种分叉的结果。

### D2. 先构建，再以 `JAVA_TOOL_OPTIONS` 启动服务

Maven 构建单独一步执行，随后运行 `dev-init.sh`（不带 `--build`）。脚本检测到 jar 已存在会
跳过构建，因此 `JAVA_TOOL_OPTIONS` 只作用于 `iboot-db-init` 与各服务 JVM，不会把 Maven
本身限制到 384MB 而构建 OOM。

**备选**：把 heap 参数写进 `dev-init.sh`。**不采用**：本地开发不应被 CI 的内存上限绑死；
用 CI 侧环境变量表达更合适。

### D3. CI 环境变量

| 变量 | 值 | 原因 |
|---|---|---|
| `DASHSCOPE_CHAT_API_KEY` | `ci-placeholder` | 通过 `AiConfigValidator` 非空校验；回归不调用真实模型 |
| `DASHSCOPE_EMBEDDING_API_KEY` | `ci-placeholder` | 避免 Embedding 客户端在构造期因空 key 失败 |
| `IBOOT_OPS_ENABLED` | `false` | 回归不覆盖运维控制台，省内存 |
| `IBOOT_TRACING_ENABLED` | `false` | 不依赖 Tempo |
| `IBOOT_BACKUP_ENABLED` | `true` | 冒烟包含备份链路 |
| `IBOOT_BACKUP_ENCRYPTION_KEY` | `ci-only-key-do-not-use-in-prod` | 仅 CI 使用的临时密钥 |
| `MANAGEMENT_HEALTH_MAIL_ENABLED` | `false` | mail host 为空时避免健康检查 DOWN |
| `JAVA_TOOL_OPTIONS` | `-Xms128m -Xmx384m` | 9 个服务 + 中间件在 runner 上的内存上限 |

`dev-init.sh` 本身已经会导出 `IBOOT_BACKUP_ENABLED=true` 与开发密钥；这里显式写出，
让 CI 的意图可读，也避免脚本默认值变化时静默改变 CI 行为。

### D4. PR 任务保持不变

PR 阶段仍只跑 `route-coverage.sh` + 全量构建 + 网关测试。它不依赖服务启动，几分钟内完成；
全栈回归放在定时与手动触发，符合成本与反馈速度的取舍。

### D5. 文档同步

- `docs/23` §1 增加 `path-conservation.sh`，把 smoke 的依赖改为全栈；§2 用
  `dev-init.sh` 作为前置条件，并说明备份能力在 `iboot-job`。
- `scripts/README.md` 增加“回归分层”小节：契约快照、路径守恒、路由覆盖、行为回归，
  以及 PR / 定时任务分别跑哪一层。

## Risks / Trade-offs

- **[runner 内存不足]** → 用 `JAVA_TOOL_OPTIONS` 把每个服务堆限制在 384MB；job 设置
  `timeout-minutes: 45`，失败时输出各服务日志。若仍不足，后续再考虑拆分 full-regress 或换 larger runner。
- **[AI dummy key 掩盖真实模型问题]** → 回归本来不调用对话/RAG；真实模型验证仍由开发环境或专门测试承担。
- **[dev-init 启动 Tempo 增加少量开销]** → 这是现有脚本的行为；为一个 CI 场景改造脚本不值得，保留。
- **[CI 运行时间变长]** → 只影响定时/手动任务，PR 反馈不受影响；首次构建可复用 Maven cache。
- **[dev-init 与 CI 环境差异]** → 本地执行同一命令做一次验证（见 tasks 3.2），把差异暴露在合并前。

## Migration Plan

无运行时迁移。合并后先手动触发一次 `workflow_dispatch`，确认 `full-regress` 全绿，再依赖定时任务。

## Open Questions

无。
