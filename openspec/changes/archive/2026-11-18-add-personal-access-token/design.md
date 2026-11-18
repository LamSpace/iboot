# Design

## Context

- CLI v0–v2 已完成；无浏览器场景只能注入 `IBOOT_TOKEN`，而该令牌来自浏览器授权码登录，依附 30 分钟滑动会话。
- 内核 `JwtAuthenticationFilter.authenticateStateless` 对自包含令牌的要求是 `login_tokens:<jti>` 存在，并在每次请求调用 `slideSession` 把会话与 `online_token:<userId>` 的 TTL 重置为 30 分钟。直接复用该命名空间无法表达长期、固定有效期的凭据。
- 认证服务已持有 RSA 签名密钥（`JWKSource`），但尚未暴露 `JwtEncoder`；资源服务通过 JWK 离线验签，不依赖认证服务在线。
- 内核把令牌声明转换为 `Map` 主体，下游 `@PreAuthorize`、数据范围与操作日志都从 `userId`、`username`、`permissions` 等键读取。
- 数据库为共享库 `iboot`，Flyway 迁移位于 `iboot-db-init`；`sys_*` 表归系统域。在线列表扫描 `login_tokens:*`，强退按 `tokenId` 删除单个会话。
- 网关的认证路由是显式路径列表，`/api/auth/**` 不能整段转发，因为 `/api/auth/scope` 由系统服务承载。

## Goals / Non-Goals

**Goals:**

- 提供长期有效、可撤销、可审计、无需浏览器的凭据，复用现有身份、RBAC 与数据权限。
- 与交互式会话完全隔离：不滑动、不写在线映射、不驱逐门户或 CLI 会话。
- 撤销、过期、用户停用或删除后立即失效。
- CI 可以安全注入令牌，不把令牌放进命令行参数或 shell 历史。

**Non-Goals:**

- PAT scope 细分（读写分离、按资源授权）。
- 已签发 PAT 的权限实时同步。
- 门户管理页面或管理员查看他人 PAT。
- 不透明令牌 + 认证服务逐请求内省。
- client credentials、device flow、`add-multi-client-sessions`。

## Decisions

### D1：PAT 使用认证服务签名的自包含 JWT

PAT 是 RS256 JWT，声明包含 `pat=true`、`jti`、`sub`、`userId`、`username`、`nickname`、`deptId`、`roles`、`permissions`、`iss`、`iat`、`exp`。资源服务沿用现有 JWK 验签与声明解析，不增加逐请求网络调用。

服务端不保存令牌原文，也不需要保存哈希：数据库只保存 `jti` 与元数据，`jti` 无法用于伪造签名。签发时通过新增的 `JwtEncoder`（`NimbusJwtEncoder` + 现有 `JWKSource`）生成令牌。

备选方案：不透明随机令牌 + 认证服务内省。每个资源服务都要在请求路径上访问认证服务，违反“认证服务停止不影响已签发令牌”的既有契约，且新增网络故障面，因此不采用。

### D2：PAT 使用独立 Redis 命名空间，固定 TTL

- `pat_tokens:<jti>`：存在即有效；TTL 等于令牌剩余有效期；撤销即删除该键。
- `pat_last_used:<jti>`：最近使用时间的尽力记录，TTL 与令牌一致；列表接口读取，不进入请求热路径的数据库写入。
- `JwtAuthenticationFilter` 在验签成功后按 `pat` 声明分流：PAT 分支只检查 `pat_tokens` 是否存在、`exp` 是否已过，然后建立主体；**不调用** `slideSession`，不触碰 `online_token:<userId>`。
- 过期令牌即使 `JwtTokenVerifier` 返回声明，PAT 分支仍显式比较 `exp` 并拒绝。

不复用 `login_tokens` 的原因：该命名空间的语义是“交互式会话”，每次请求滑动 30 分钟，并要求在线映射；PAT 需要固定有效期且不能延长或创建在线映射。

### D3：PAT 元数据表 `sys_user_pat`

```sql
CREATE TABLE `sys_user_pat` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `name` varchar(64) NOT NULL,
  `token_id` varchar(64) NOT NULL,
  `expires_at` datetime NOT NULL,
  `revoked` tinyint NOT NULL DEFAULT 0,
  `revoked_at` datetime DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pat_token_id` (`token_id`),
  KEY `idx_pat_user` (`user_id`, `revoked`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

表放在系统域（`sys_` 前缀），因为用户生命周期归系统域，停用与删除在同一事务内撤销令牌。认证服务只写入该用户自己的令牌元数据，作为 `system-management` 与 `auth-service` 已登记的第三个写入例外。

备选方案：认证服务自建 `auth_pat` 表。这样会增加一个新的 schema 所有权方，系统域停用时仍需跨域访问，反而扩大边界，因此不采用。

### D4：权限在签发时固化

签发时由 `AuthDirectory` 读取用户、角色编码与权限集合，写入 JWT 声明。资源服务继续按声明判定功能权限，数据范围仍由系统域按当前用户实时计算。

权衡：角色或权限变更不会自动改写已签发 PAT；这是把长期凭据的便利性限制在可接受范围的做法。有效期默认 30 天、上限 90 天，文档要求角色调整后撤销并重建 PAT。用户停用或删除会撤销全部 PAT，覆盖最危险的生命周期场景。

备选方案：PAT 只携带用户 ID，由每个服务实时查询权限。需要所有服务在请求路径访问数据库或认证服务，与现有离线验签模型冲突，不采用。

### D5：管理接口放在认证服务，只接受交互式会话

- `POST /api/auth/tokens`：创建，返回明文一次。
- `GET /api/auth/tokens`：列出当前用户令牌元数据。
- `DELETE /api/auth/tokens/{id}`：撤销当前用户自己的令牌。
- 网关认证路由增加 `/api/auth/tokens/**`；认证服务白名单不包含这些路径，因此必须携带有效令牌。
- 过滤器在主体中保留 `pat` 标记；控制器拒绝来自 PAT 的管理请求，避免泄漏的 PAT 自行续期或扩散。
- 签发与撤销使用 `@Log`，但创建接口关闭响应体记录（`isSaveResponseData=false`），避免操作日志保存明文。
- 新增 `PersonalAccessTokenProperties`（`iboot.auth.pat.*`）：`default-days`、`max-days`、`max-per-user`，对应 `IBOOT_AUTH_PAT_*` 环境变量。

### D6：CLI 命令与标准输入注入

- 新增 `iboot token create|list|revoke`，复用 `BaseCommand` 的写保护与 `Output`。
- `create` 支持 `--name`/位置参数、`--expires-days`；默认输出包含一次明文，`--json` 便于 CI 提取。
- `--token-stdin` 作为全局选项进入 `CliApplication` 的令牌覆盖链：标准输入 > `IBOOT_TOKEN` > 本地凭据。只读取一次并去除首尾空白；不回显、不写凭据文件。
- `token create/revoke` 需要 `--yes`；`--dry-run` 只打印请求。

### D7：用户生命周期撤销采用失败关闭

系统域新增一个窄职责组件（`PersonalAccessTokenRevoker`）：

1. 查询 `sys_user_pat` 中该用户未撤销的 `token_id`；
2. 删除 `pat_tokens:<jti>` 与 `pat_last_used:<jti>`；
3. 把元数据标记为已撤销。

在 `UserApplicationService.changeStatus`（停用）与 `deleteUser` 中调用。Redis 删除失败时抛出异常，使用户状态变更事务回滚：无法保证撤销就不允许停用或删除，避免出现“已停用但 PAT 仍可用”的窗口。重新启用用户不会恢复旧 PAT。

备选方案：写 `user_disabled:<userId>` 黑名单，由过滤器拒绝 PAT。省去跨域读表，但新增一种持久化状态，Redis 丢失后会静默恢复访问，可靠性不如直接删除 PAT 键，因此不采用。

### D8：不与在线名单和会话级强退集成

PAT 不进入 `login_tokens`，也不写 `online_token`，因此在线列表、按 tokenId 的强退、CLI 过渡期例外都不需要改动。PAT 的撤销入口是令牌管理接口与用户停用/删除；这一点作为显式例外写入 `auth-service` 的在线会话契约。

### D9：测试与验证

- 内核：PAT 有效、过期、撤销、闲置后仍可用、不延长 `online_token`、不写在线映射。
- 认证：创建/列表/撤销、有效期与数量上限、PAT 不能管理 PAT、明文不落日志。
- 系统：停用与删除撤销全部 PAT，重新启用不恢复。
- CLI：命令输出、`--yes`/`--dry-run`、`--token-stdin` 优先级与不回显。
- 回归：真实 Docker 环境签发 PAT，用 `IBOOT_TOKEN` 与 `--token-stdin` 各完成一次查询与备份触发；撤销后 401；停用用户后 401；在线列表不出现 PAT。

## Risks / Trade-offs

- **权限快照过期** → 默认 30 天、上限 90 天；停用/删除立即撤销；文档要求角色调整后重建。
- **Redis 故障或数据丢失使 PAT 立即失效** → 失败关闭，用户重新签发即可；不会出现“判定不了就放行”。
- **撤销依赖 Redis 删除成功** → 用户状态变更与撤销同事务，删除失败则回滚；重试幂等。
- **最近使用时间不是强一致数据** → 只用于运维参考，列表实时读取 Redis，允许缺失。
- **令牌可能进入终端历史或 CI 日志** → 提供 `--token-stdin`，文档要求使用 CI secret 与 `--json`；CLI 不持久化注入的令牌。
- **`sys_user_pat` 成为认证服务的第三个写入例外** → 已在两个能力 spec 中登记，退出条件与现有认证直连共享库的过渡期安排一致。

## Migration Plan

1. `V13__personal_access_token.sql` 仅新增表与索引，无存量数据迁移。
2. 先滚动发布内核与各资源服务，使过滤器认识 PAT；再发布认证服务与网关路由；最后发布 CLI 与文档。
3. 发布完成后再签发 PAT；旧实例收到 PAT 会按普通 JWT 处理并因缺少 `login_tokens` 拒绝，属于预期的滚动窗口行为。
4. 回滚：停止签发并撤销全部 PAT；旧版本过滤器忽略 `pat_tokens`，已签发 PAT 自然失效。表可保留，不影响旧版本。

## Open Questions

无。
