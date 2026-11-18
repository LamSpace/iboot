# Tasks

## 1. Schema 与配置

- [x] 1.1 新增 `V13__personal_access_token.sql` 创建 `sys_user_pat`（`token_id` 唯一、`user_id + revoked` 索引、无令牌原文列），并验证 `mvn -s /home/lam/repo/settings.xml -pl iboot-db-init package` 通过
- [x] 1.2 新增 `PersonalAccessTokenProperties`（`iboot.auth.pat.*`）与 `iboot-auth/application.yml` 的 `default-days=30`、`max-days=90`、`max-per-user=10` 及 `IBOOT_AUTH_PAT_*` 环境变量，并补充属性绑定测试，验证 `mvn -s /home/lam/repo/settings.xml -pl iboot-auth -Dtest=PersonalAccessTokenPropertiesTest test` 通过

## 2. 内核令牌验证

- [x] 2.1 在 `Constants` 增加 `pat_tokens:`、`pat_last_used:` 与 `pat` 声明常量，并在 `JwtAuthenticationFilter` 增加 PAT 分支（存在性 + `exp` 校验），验证新增的 `JwtAuthenticationFilterPatTest` 通过
- [x] 2.2 为 PAT 分支补充“不滑动 TTL、不写 `online_token`、闲置后仍有效、撤销后拒绝”的测试，验证 `mvn -s /home/lam/repo/settings.xml -pl iboot-kernel -Dtest=JwtAuthenticationFilterPatTest,JwtAuthenticationFilterRedisTest test` 通过

## 3. 认证服务签发与管理

- [x] 3.1 在 `AuthorizationServerConfig` 增加 `JwtEncoder` Bean，并在 `AuthorizationServerConfigTest` 验证其可签发带 `kid` 的 RS256 令牌
- [x] 3.2 扩展 `AuthDirectory`（按 ID 读取用户、角色编码）并新增 `PersonalAccessTokenRepository`，验证仓储的插入、查询、撤销与数量统计测试通过
- [x] 3.3 新增 `PersonalAccessTokenService`：校验名称/有效期/数量上限、生成 `jti` 与声明、写 `sys_user_pat`、写 `pat_tokens`、列表合并 `pat_last_used`、撤销删除 Redis 并标记元数据，验证服务测试覆盖创建、列表、撤销与上限
- [x] 3.4 新增 `PersonalAccessTokenController` 与 DTO，挂载 `/api/auth/tokens`，使用 `@Log` 且创建接口关闭响应体记录，验证接口测试与“日志不含明文”断言通过
- [x] 3.5 在管理接口拒绝来自 PAT 的请求（依据主体中的 `pat` 标记），验证“PAT 调用创建接口返回 403”测试通过

## 4. 用户生命周期撤销

- [x] 4.1 在 `iboot-system` 新增 `PersonalAccessTokenRevoker`，在 `changeStatus`（停用）与 `deleteUser` 中调用，删除 `pat_tokens`/`pat_last_used` 并标记元数据，验证“停用/删除撤销全部 PAT、重新启用不恢复”测试通过
- [x] 4.2 验证撤销失败时用户状态变更回滚（Redis 删除抛异常即中断），补充失败关闭测试

## 5. 网关与 CLI

- [x] 5.1 在 `application-local.yml` 与 `application-nacos.yml` 的认证路由加入 `/api/auth/tokens/**`，验证网关模块测试通过且路由不吞掉 `/api/auth/scope`
- [x] 5.2 新增 `TokenCommand`（`create`/`list`/`revoke`）并注册到 `Main`，复用 `BaseCommand` 写保护，验证命令测试覆盖 `--yes`、`--dry-run`、JSON 输出与明文只输出一次
- [x] 5.3 在 `GlobalOptions` 与 `CliApplication` 增加 `--token-stdin`，优先级为 stdin > `IBOOT_TOKEN` > 本地凭据，验证优先级、去空白、不回显与不写凭据文件的测试通过
- [x] 5.4 在 `docs/25-开发者 CLI 操作指南.md` 增加 token 命令、`--token-stdin`、权限快照与撤销说明，验证文档中的命令可被 `iboot token --help` 与 `--token-stdin` 实际接受

## 6. 文档与自动化配方

- [x] 6.1 新增 `docs/26-CI 与个人访问令牌.md`，包含 GitHub Actions 示例、秘密管理、轮换与停用撤销说明，验证示例中的每条命令在 CLI 帮助中存在
- [x] 6.2 新增 `.claude/skills/iboot-cli/SKILL.md`，说明本地智能体如何用只读命令排障、写命令的确认要求与令牌安全约束，验证 frontmatter 与命令示例可解析
- [x] 6.3 更新 `README.md`、`ROADMAP.md`、`.env.example`，将 v3 标记为完成并补充 PAT 环境变量，验证链接与变量名与实际文件一致

## 7. 回归与集成

- [x] 7.1 在 `scripts/regress/cli.sh` 增加 PAT 用例：签发、`IBOOT_TOKEN` 与 `--token-stdin` 调用、在线列表不可见、撤销后 401、PAT 不能签发、停用用户后 401、审计不含明文，验证脚本在 Docker 全栈上通过
- [x] 7.2 运行 `mvn -s /home/lam/repo/settings.xml -pl iboot-kernel,iboot-auth,iboot-system,iboot-cli -am test`，确认后端与 CLI 单元测试全部通过
- [x] 7.3 重新构建 Docker 镜像并执行全量回归，验证 V13 迁移已应用、PAT 端到端可用且原有 19 项 CLI 回归未退化

## Workflow follow-up

- 推送功能分支并创建 PR，合并到 `master` 后更新本地分支。
- 归档变更并把 delta 同步到主 spec。
