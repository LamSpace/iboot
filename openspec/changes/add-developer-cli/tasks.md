# Tasks

## 1. 模块骨架与核心管道

- [x] 1.1 新增 `iboot-cli` 模块并加入 `pom.xml` reactor：父 POM 继承、picocli + Jackson 3 依赖、shade 可执行 jar、`bin/iboot` 启动脚本；验证 `mvn -pl iboot-cli package` 产出 jar，且 `java -jar iboot-cli/target/iboot-cli-1.0.0.jar --help` 正常退出。
- [x] 1.2 实现 context 配置与凭据存储：`~/.config/iboot/config.json`、`credentials.json` 0600、原子写入、按 context 隔离；验证单元测试覆盖文件权限、原子替换与 context 切换。
- [x] 1.3 实现网关 HTTP 客户端、CloudEvents/DTO 解包与错误映射；验证单元测试覆盖两类响应、`--raw`、401/40101/403/429/5xx/业务码非 200 的退出码。
- [x] 1.4 更新 `README.md` 与 `docs/README.md` 的项目结构、模块计数与 CLI 入口；验证目录树包含 `iboot-cli/`，计数字符串与 `pom.xml` 的模块数一致。

## 2. 认证客户端与登录

- [x] 2.1 扩展 `iboot-auth` 的客户端登记，支持第二个公共客户端 `iboot-cli`（PKCE、loopback 回调、无客户端密钥、跳过授权同意）；配置项可通过环境变量覆盖；验证认证服务启动成功，未登记的回调地址被拒绝、登记的 `http://127.0.0.1:8765/callback` 可走通授权请求。
- [x] 2.2 在 `PortalTokenCustomizer` 增加过渡期例外：`client_id=iboot-cli` 跳过 `evictPreviousSession` 与 `online_token:<userId>` 写入，但仍写 `login_tokens:<jti>`；验证单元测试分别覆盖门户客户端仍驱逐、CLI 客户端不驱逐、CLI 会话可被登出撤销。
- [x] 2.3 实现 `iboot login` / `logout` / `status`：OIDC 发现、授权码 + PKCE、固定 8765 loopback、令牌落盘、登出撤销；验证本地全栈登录成功、`status` 显示到期时间且不打印令牌、`logout` 后同一令牌请求被拒绝。
- [x] 2.4 实现令牌到期与会话失效处理：会话有效期内依赖服务端 Redis 滑动续期；401 且无刷新令牌时以认证失效退出并提示重新登录；有刷新令牌时单飞续期并持久化；验证 stub 测试覆盖两条分支与并发刷新，全栈回归覆盖“无刷新令牌的 401”。
- [x] 2.5 更新认证配置文档与示例：`docs/10-后端配置详解.md`、`.env.example` 增加 CLI 客户端相关变量；验证文档中的变量名与 `application.yml` / 属性绑定一致，且认证服务测试 `mvn -pl iboot-auth test` 通过。

## 3. 只读命令与输出

- [x] 3.1 实现用户、角色、部门、字典、参数的只读命令与表格/`--json` 输出；验证 stub 测试覆盖两类响应解包，且在本地全栈执行 `iboot user list --json` 与 `GET /api/user/list` 的业务数据一致。
- [x] 3.2 实现任务、备份、登录/操作日志、在线会话的只读命令；验证每条命令在本地全栈返回非空或明确的空结果，字段与 OpenAPI 基线一致。
- [x] 3.3 实现 `iboot api` 逃生口：GET/HEAD/OPTIONS 默认放行，其他方法要求 `--yes`，`--dry-run` 只打印请求，`--raw` 保留原始响应；验证单元测试与本地全栈 GET 调用。
- [x] 3.4 实现数据权限与越权行为的端到端验证：受限用户只看到本人数据，无 `user:list` 权限返回 403 且退出码为越权；验证用 `permission-matrix.sh` 的测试账号复跑。
- [x] 3.5 新建 `docs/25-开发者 CLI 操作指南.md`：安装、登录/登出、context、命令、`--json`、退出码、安全与排障；验证文档中的命令在本地全栈按原文执行成功，链接可打开。

## 4. 回归与持续集成

- [x] 4.1 新增 `scripts/regress/cli.sh`：复用 `lib.sh` 的登录与断言，覆盖登录、`user list --json` 对比、受限数据范围、403、`--dry-run`、logout 与凭据不留痕；验证在 `dev-init.sh` 拉起的环境下脚本通过。
- [x] 4.2 在 `.github/workflows/regress.yml` 接入 CLI：PR 任务执行 `mvn -B -pl iboot-cli test`，全栈任务在 `regress.sh` 之后执行 `cli.sh`；验证本地按工作流中的命令逐条执行成功，并确认 CLI 模块被 `mvn -DskipTests install` 覆盖。
- [x] 4.3 更新 `README.md` 的构建与测试命令，加入 CLI 单元测试与回归入口；验证文档命令与 CI 中实际命令一致。

## 5. 文档收口与契约校验

- [x] 5.1 在 `ROADMAP.md` 增加“开发者 CLI”条目与 v0/v1 判据，并复核 §7.5：公共客户端 + 授权码不签发刷新令牌，会话语义由共享 Redis 滑动续期承担；验证条目包含非目标与退出条件，且与 `openspec/specs/developer-cli/spec.md` 不矛盾。
- [x] 5.2 更新 `docs/06-API 集成.md` 与 `docs/02-技术栈.md`：只加入 CLI 入口链接与 picocli/shade 版本说明，不复制命令清单；验证 docs/README 的文档导航能到达新文档。
- [x] 5.3 运行 `openspec validate add-developer-cli --strict` 与 `mvn -pl iboot-cli,iboot-auth test`，确认 spec/design/tasks 无校验错误、测试全部通过；把失败项修到通过后再进入实现验收。

## Workflow follow-up

- 实现完成后按项目约定走功能分支、PR 与 `gh pr merge`，不要把变更直接提交到 `master`。
- 归档前确认 CLI 指南、README、ROADMAP 与 spec 的表述一致，并把 `add-multi-client-sessions` 作为后续变更登记。
