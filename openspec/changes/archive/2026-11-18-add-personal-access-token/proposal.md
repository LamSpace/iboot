# Proposal

## Why

CLI v0–v2 已经覆盖登录、只读查询与运维写命令，但无浏览器场景仍走不通：`IBOOT_TOKEN` 只能注入一个由浏览器授权码登录换来的访问令牌，而该令牌依附 30 分钟滑动会话，闲置即 401，定时 CI 无法使用。ROADMAP 的 v3 判据“无浏览器环境完成一次备份触发”因此无法满足。

需要一种长期有效、可撤销、可审计、无需浏览器的凭据，同时不引入新的权限模型。

## What Changes

- 新增个人访问令牌（PAT）：用户可在已登录状态下签发、查看与撤销自己的令牌；明文只在签发时返回一次，服务端不保存令牌原文。
- PAT 是认证服务签名的自包含令牌，携带 `pat=true` 与用户、角色、权限声明；有效期默认 30 天、上限 90 天，每用户活动令牌数量有上限。
- 内核令牌过滤器以独立的 `pat_tokens:<jti>` 命名空间判定 PAT 是否有效与是否撤销；PAT 的有效期固定，不参与 30 分钟滑动续期。
- PAT 不写入 `online_token:<userId>`，不驱逐门户会话，也不出现在在线列表；撤销、过期、用户停用或删除后立即失效。
- 权限在签发时固化为快照；角色或权限调整后，需要撤销并重建 PAT 才生效，文档明确说明。
- 用户停用或删除时，撤销该用户的全部 PAT；会话级强制下线不覆盖 PAT，与 CLI 会话的过渡期限制保持一致。
- CLI 新增 `iboot token create|list|revoke`，并新增 `--token-stdin`，让 CI 从标准输入注入 PAT，避免令牌进入命令行参数与 shell 历史。
- 文档新增 GitHub Actions 示例、本地 `iboot-cli` SKILL.md、PAT 安全与轮换说明；回归脚本增加 PAT 用例。

## Capabilities

### New Capabilities

- `personal-access-token`: 定义 PAT 的签发、验证、撤销、有效期与用户生命周期契约，以及“不参与交互式会话、不进入在线名单”的边界。

### Modified Capabilities

- `auth-service`: 认证写入的过渡期边界新增 `sys_user_pat` 令牌元数据例外；令牌撤销名单与在线会话契约补充 PAT 作为独立凭据类别。
- `system-management`: 系统域写入例外新增认证服务写入 `sys_user_pat`；用户停用或删除时撤销该用户的全部 PAT。
- `developer-cli`: 新增 token 子命令与 `--token-stdin`，并明确 PAT 管理只能使用交互式登录会话。

## Impact

- `iboot-db-init`：新增迁移 `V13__personal_access_token.sql`，创建 `sys_user_pat` 元数据表。
- `iboot-kernel`：`Constants` 新增 PAT 键与声明常量；`JwtAuthenticationFilter` 增加 PAT 分支（固定 TTL、不滑动、不写在线映射、拒绝过期令牌）。
- `iboot-auth`：新增 PAT 控制器、应用服务与 JDBC 仓储；`AuthDirectory` 增加按 ID 读取用户与角色的方法；`AuthorizationServerConfig` 增加 `JwtEncoder`；`AuthServerProperties` 增加 PAT 策略配置。
- `iboot-system`：用户停用、删除时撤销 PAT；与在线强退入口对齐。
- `iboot-gateway`：认证路由增加 `/api/auth/tokens/**`。
- `iboot-cli`：新增 `TokenCommand`；`GlobalOptions`、`CliApplication`、`Main` 支持 `--token-stdin` 与新子命令。
- 测试与文档：内核与认证单元测试、CLI 单元测试、`scripts/regress/cli.sh`、`docs/25`、新增 CI/PAT 文档与 `.claude/skills/iboot-cli/SKILL.md`、`README.md`、`ROADMAP.md`。

**非目标**：PAT scope 细分；门户令牌管理 UI；管理员查看或撤销他人 PAT 的页面；client credentials 与 device flow；PAT 权限实时同步；管理类 CRUD 命令；`add-multi-client-sessions`。
