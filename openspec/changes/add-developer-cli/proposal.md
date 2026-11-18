# Proposal

## Why

开发者需要一个非浏览器的稳定调用面：在终端完成登录、查询与排障，并让本地智能体/CI 能复用同一套身份与权限。现有 OAuth2、RBAC、数据权限与操作审计已经具备，缺的只是一个薄客户端；先做 CLI 是四个候选中成本最低、对阶段 3 干扰最小的一步。

## What Changes

- 新增 `iboot-cli` 模块：只依赖 JDK `HttpClient`、Jackson 3 与 picocli，不依赖 Spring / `iboot-kernel`，不直连数据库，不进部署清单。
- 认证服务登记公共客户端 `iboot-cli`：授权码 + PKCE，回调固定为 `http://127.0.0.1:8765/callback`。
- **过渡期会话例外**：CLI 的 `authorization_code` 登录不参加强制会话驱逐，也不写入 `online_token:<userId>`。因此 CLI 会话不出现在在线列表，管理员“强制下线”不覆盖 CLI 会话——这是已知限制，退出条件是后续 `add-multi-client-sessions` 变更落地。
- 提供 v0/v1 能力：`login` / `logout` / `status`、`user list`、`api` 逃生口，以及 10–15 条只读查询命令、`--json` 与多 context 切换。
- 写操作（`--yes` / `--dry-run`）、PAT 与 CI 示例不在本次范围；`iboot api` 对非幂等方法同样拒绝，避免逃生口绕过写保护。
- 增加单元测试、基于本地 stub 的协议测试、`scripts/regress/cli.sh` 端到端回归，并接入现有 CI。
- 新增 CLI 操作指南，并同步更新 README、ROADMAP、docs 导航与认证配置说明；校正仓库中已漂移的模块数与 specs 计数。

## Capabilities

### New Capabilities

- `developer-cli`: 定义开发者 CLI 的登录、令牌续期、只读命令、`--json` 输出、逃生口与安全边界。

### Modified Capabilities

- `auth-service`: 为过渡期增加 CLI 客户端的会话例外，限定在线列表与强制下线的覆盖范围，并写明退出条件。

## Impact

- `pom.xml`：新增 `iboot-cli` 模块（第 15 个）。
- `iboot-auth`：客户端登记从单客户端扩展为可登记第二个公共客户端；`PortalTokenCustomizer` 增加按 `clientId` 跳过驱逐与在线映射的分支。
- `iboot-gateway`：无改动，`/oauth2/**`、`/.well-known/**`、`/login` 已放行。
- `iboot-db-init`：无建表或迁移。
- `scripts/regress`：新增 `cli.sh`，复用 `lib.sh` 的登录与断言语义。
- `.github/workflows/regress.yml`：PR 增加 `iboot-cli` 单元测试；全栈任务增加 CLI 回归。
- 文档：新增 `docs/25-开发者 CLI 操作指南.md`；更新 `README.md`、`docs/README.md`、`ROADMAP.md`、`docs/06-API 集成.md`、`docs/10-后端配置详解.md`、`.env.example`。

**非目标**：不做门户替代；不做 Web 智能体的执行通道；不覆盖全部 206 个接口；不做 v2 写操作与 v3 PAT/CI；不在本地做多会话索引改造；不引入 GraalVM native。
