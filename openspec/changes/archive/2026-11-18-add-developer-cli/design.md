# Design

## Context

See `proposal.md` for motivation and `specs/developer-cli/spec.md` for the behavior contract.

Current constraints that shape the implementation:

- `iboot-auth` 目前只登记一个公共客户端（`AuthorizationServerConfig.java:184-206`），`client-id` 是单值配置；新增 CLI 客户端需要扩展登记方式。
- 默认 `sys.session.exclusive=true`；`PortalTokenCustomizer.java:102-104` 在授权码换令牌时按 `userId` 驱逐旧会话，并写入 `online_token:<userId>`（`:169`）。
- 认证服务对公共客户端 + 授权码实际**不签发刷新令牌**：`AuthorizationServerConfig.java:190,202-203` 的 `refresh_token` 授权类型与 TTL 配置不会改变 Spring Authorization Server 的公共客户端判定。会话语义由共享 Redis 的滑动续期承担，资源服务接受 `JwtTokenVerifier` 放行的过期 JWT（只要 `login_tokens:<jti>` 仍存在）；401 表示会话已失效。
- 网关已放行 `/oauth2/**`、`/.well-known/**`、`/login`，CLI 的 loopback 回调不经过网关。
- 7 个服务用 CloudEvents 信封包装 `Result`，`iboot-ai` 直接返回 DTO；错误体也不同。
- 现有回归脚本 `scripts/regress/lib.sh:37` 已有完整的授权码 + PKCE 登录实现，可作为 CLI 登录流程的语义参照。

## Goals / Non-Goals

**Goals:**

- 新增一个独立的 `iboot-cli` 模块，以最小依赖实现 v0/v1 只读能力。
- 复用现有 OAuth2、RBAC、数据权限与操作审计，不引入新的权限模型。
- 让 CLI 与浏览器会话在过渡期内互不驱逐，并把限制显式记录为可退出的契约例外。
- 用单元测试、stub 协议测试和全栈回归证明 CLI 的行为，而不是只靠手工验证。

**Non-Goals:**

- v2 写操作与审计增强；v3 PAT 与 CI 自动化。
- 多会话索引与“强制下线覆盖全部令牌”（由 `add-multi-client-sessions` 单独完成）。
- Web 智能体执行通道、GraalVM native、完整 206 接口建模。
- 自定义本地 CA 信任；HTTPS 先走系统信任库，留到 TLS 工作。

## Decisions

### D1. 薄客户端：独立模块，不依赖 Spring / kernel

`iboot-cli` 作为第 15 个 Maven 模块，只依赖 JDK `HttpClient`、Jackson 3（`tools.jackson`）和 picocli。它不依赖 `iboot-kernel`，避免把 Spring Security、Redis、MyBatis 带进 CLI；也不引入第二套工具链。

打包用 shade 生成可执行 jar，配 `bin/iboot` 启动脚本；`mvn -pl iboot-cli package` 即可构建。

**Alternatives considered:**

- Spring Boot CLI：复用 BOM 与配置绑定，但启动慢、体积大，且会把服务端依赖拖进来。
- Go / Node：分发更简单，但要重写 DTO 与客户端封装，给纯 Java 仓库增加第二套工具链。
- Shell：零构建，但无类型、跨平台差，只适合回归脚本。

### D2. 登录：独立公共客户端 + 固定 loopback 端口

新增 `iboot-cli` 公共客户端：`authorization_code`（并保留 `refresh_token` 授权类型）、`ClientAuthenticationMethod.NONE`、`requireProofKey(true)`、`requireAuthorizationConsent(false)`、回调 `http://127.0.0.1:8765/callback`。CLI 从 OIDC 发现文档取授权与令牌端点，不硬编码路径。当前公共客户端不会拿到刷新令牌；只有服务端未来签发刷新令牌时，客户端才走续期分支。

固定 8765 而不是动态端口：授权服务器的 redirect URI 默认精确匹配，动态端口需要注册一片地址或改匹配规则；v1 不值得。端口被占用时直接给出明确错误。

**Alternatives considered:**

- 复用 `iboot-portal` 客户端：省掉客户端登记，但审计归属不清，后续 PAT/作用域也会混在一起。
- 动态端口：体验更好，但要先 spike 授权服务器的通配匹配，风险大于收益。
- 设备码流程：更接近 `gh`，但当前代码没有接入，且仍需解决在线映射问题；留作后续。

### D3. 会话：过渡期 CLI 不参与驱逐与在线映射

CLI 的授权码登录在 `PortalTokenCustomizer` 中按 `clientId` 跳过 `evictPreviousSession`，并跳过 `online_token:<userId>` 写入；令牌仍写入 `login_tokens:<jti>`，因此资源服务可以正常鉴权、CLI 登出也能撤销自身会话。

这明确是一个过渡期例外：

- CLI 登录不会删除门户的 `login_tokens`，门户不会被踢下线；
- CLI 会话不出现在在线列表；
- 管理员按用户强制下线不会撤销 CLI 会话；
- 退出条件：`add-multi-client-sessions` 落地后删除该分支，并把 CLI 会话纳入统一索引。

**Alternatives considered:**

- A. 多会话索引：语义正确，但要改 auth、kernel、monitor 与 auth-service 契约；作为独立变更排在小程序之前。
- C. PAT 优先：不碰交互式会话，适合 CI，但把 v2 的 PAT 提前，且仍需解决“强制下线全部令牌”的索引问题。
- D. 接受单会话：零改动，但 CLI 登录会驱逐门户，在线列表只剩一个会话，语义最混乱。

### D4. 响应解码：按结构判断，不按服务硬编码

CLI 先看响应 JSON 是否包含 `specversion` 与 `data`：

- 有：按 CloudEvents 解包，再检查内部 `Result.code`；`--json` 输出 `Result.data`。
- 无：按直接 DTO 处理，`--json` 原样输出。
- `--raw`：跳过所有解包，输出原始响应，便于排障。

错误映射按“HTTP 状态 + 结构”处理：401/40101 → 认证失效，403 → 越权，429 → 限流，5xx → 网络/服务端错误；业务码非 200 → 业务错误。这样不需要为 7 个服务各写一套 profile。

**Alternatives considered:**

- 按服务配置解包规则：明确但难维护，新增服务时容易漏。
- 只用 HTTP 状态码：会丢失 CloudEvent 内部 `Result` 的业务错误信息。

### D5. 命令面：20% 建模 + `iboot api` 逃生口

v1 只建模高频只读资源：用户、角色、部门、字典、参数、任务、备份、登录/操作日志、在线会话。其余接口走 `iboot api GET /api/...`。

`iboot api` 不是后门：GET/HEAD/OPTIONS 默认放行；其他方法必须 `--yes`；`--dry-run` 只打印请求。所有请求带 `Accept-Version: 1` 和 Bearer 令牌，权限仍由服务端判断。

**Alternatives considered:**

- 为 206 个接口批量生成命令：会让项目变成第二个门户，违背 YAGNI。
- 只做 `iboot api`：实现最快，但常用查询没有人类可读输出，体验不足。

### D6. 配置与凭据

```
~/.config/iboot/config.json        # contexts: baseUrl / issuer / clientId
~/.config/iboot/credentials.json   # 0600，按 context 存 access/id token；refresh token 若存在则保存
```

`iboot config use <context>` 切换；`IBOOT_BASE_URL`、`IBOOT_TOKEN` 只作为 CI/测试覆盖口。凭据原子写入（临时文件 + rename）；有刷新令牌时对凭据文件加文件锁并在锁内重新读取，避免多进程 refresh token 轮换互相作废。当前公共客户端无刷新令牌，401 直接走重新登录。

**Alternatives considered:**

- 系统 keyring：更安全，但引入平台相关依赖；v1 先用 0600 文件，keyring 作为后续可选。
- 只存 access token：30 分钟后就要重新登录，不可接受。

### D7. 测试与回归

- 单元测试：命令解析、解包、错误映射、退出码、PKCE、凭据权限、刷新单飞（有刷新令牌时的防御分支）。
- 协议测试：JDK `HttpServer` 模拟 OAuth 令牌端点与两类业务响应，覆盖 200/401/403/429/5xx 与 `invalid_grant`；同时覆盖“无刷新令牌的 401 直接重新登录”。
- 全栈回归：新增 `scripts/regress/cli.sh`，复用 `lib.sh` 的登录与断言；对比 `iboot user list --json` 与 `reg_get /api/user/list`，覆盖数据权限、403、`--dry-run`、logout 与凭据不留痕。
- CI：PR 跑 `mvn -B -pl iboot-cli test`；全栈任务在 `regress.sh` 之后跑 `cli.sh`。

**Alternatives considered:**

- 只做单元测试：无法证明与真实认证、网关、信封的兼容性。
- 只用 shell 回归：无法覆盖会话失效语义与错误映射分支。

### D8. 文档分工

- OpenSpec spec 是行为契约：`developer-cli` + `auth-service` 过渡期例外。
- `docs/25-开发者 CLI 操作指南.md` 是操作说明：安装、登录、命令、退出码、安全、排障。
- README 只保留功能清单与入口，ROADMAP 只保留未完成路线与判据；不复制命令清单。

## Risks / Trade-offs

- **CLI 会话不在在线列表、强制下线不覆盖** → 明确写入 auth-service 过渡期例外与 CLI 指南；`add-multi-client-sessions` 落地后移除，回归脚本断言该行为。
- **CLI 登录与门户共享单键 `online_token`** → CLI 不写该键；门户会话不受影响；测试覆盖“CLI 登录后门户请求仍成功”。
- **公共客户端无刷新令牌** → 会话语义由共享 Redis 滑动续期；401 直接提示重新登录，不循环重试；CI 用 `IBOOT_TOKEN` 测试口。服务端未来若签发刷新令牌，续期分支仍可用。
- **多进程刷新竞态（有刷新令牌时）** → 文件锁 + 锁内重读；协议测试覆盖并发刷新。
- **CloudEvents / DTO 解码漂移** → 全栈回归对比真实接口；`--raw` 提供排障出口。
- **网关默认 10s 响应超时** → v1 不做长时间 `--wait`；后续如需等待任务完成，用轮询而不是单请求挂起。
- **范围膨胀** → v1 严格只读，写操作与 PAT 都留给后续变更。
- **凭据文件泄露** → 0600、原子写入、日志打码；keyring 作为后续增强。
- **平台差异** → v1 先支持 Linux/macOS；Windows 的配置目录与浏览器打开方式后续补。

## Migration Plan

1. 在 `pom.xml` 增加 `iboot-cli` 模块；不影响现有服务构建与部署。
2. 在 `iboot-auth` 登记 `iboot-cli` 客户端，并加入过渡期会话例外；未使用 CLI 的现有客户端行为不变。
3. 发布 CLI 可执行 jar 与 `bin/iboot`；文档给出从源码构建的方式。
4. 回滚：移除 CLI 客户端登记与模块即可；不涉及数据库迁移，已签发的门户令牌不受影响。
5. `add-multi-client-sessions` 落地时删除例外分支，并把 CLI 会话纳入统一在线索引。

## Open Questions

- 是否在 `sys_operate_log` 中增加客户端来源字段？v1 先用 `X-Client: iboot-cli` 请求头，是否落库留到 v2 评估，不在本次改表。
- 是否在本机提供 keyring 集成？v1 先 0600 文件，按实际使用反馈再决定。
- v1 具体命令清单是否再增减？以“高频、只读、权限边界清晰”为准，实现时可按 OpenAPI 基线微调，不改变 spec 的行为契约。
