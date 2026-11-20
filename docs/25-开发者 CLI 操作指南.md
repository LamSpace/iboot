# 25-开发者 CLI 操作指南

> 适用版本：iBoot 1.0.0。CLI 是独立模块 `iboot-cli`，只经门户公开入口的 `/api/**`
> （门户反代到网关）调用既有接口，不直连数据库，不进入 Docker/Helm 部署清单。

## 1. 定位与边界

`iboot` 命令行工具面向两类使用者：

- 开发者在终端完成登录、查询与排障；
- 本地智能体/CI 在无浏览器环境中复用同一套身份与权限。

它**不是**门户的替代品，也**不是** Web 智能体的执行通道。Web 工作台继续使用
`AgentTool`（后续由 MCP 暴露）；需要在本机执行命令的智能体才用 CLI。

## 2. 构建与启动

```bash
# 在仓库根目录
mvn -pl iboot-cli package
./bin/iboot --help
```

`bin/iboot` 默认使用 `iboot-cli/target/iboot-cli-1.0.0.jar`；需要换 jar 时设置
`IBOOT_CLI_JAR`。CLI 只依赖 JDK 25，不依赖 Spring 或 `iboot-kernel`。

## 3. 登录与上下文

默认 context 为 `dev`，指向宿主机开发的门户 HTTPS 入口 `https://localhost:5173`。
首次使用先生成本地 CA 与 Java 信任库：

```bash
scripts/tls-init.sh
# 浏览器信任（一次性，可能需要管理员权限）
CAROOT=docker/certs/ca mkcert -install

# CLI 使用同一份 PKCS12 信任库
export IBOOT_CLI_TRUSTSTORE="$PWD/docker/certs/truststore.p12"

./bin/iboot login
./bin/iboot status
./bin/iboot logout
```

宿主机开发需要门户开发服务器正在运行（`scripts/dev-init.sh --frontend`），CLI 的 OIDC
发现、授权与 `/api` 请求都经过该入口。Docker 全量部署使用独立 context：

```bash
./bin/iboot config add docker \
  --base-url https://localhost --issuer https://localhost --use
```

非 loopback 的 `http://` base URL 会被拒绝；仅 loopback 地址允许 HTTP，用于
`IBOOT_TLS_ENABLED=false` 排障。证书生成、信任分发与轮换见
[docs/27](<./27-TLS 与零信任操作指南.md>)。

`login` 会启动 `127.0.0.1:8765` 的 loopback 回调并打开系统浏览器；密码只在认证服务的
登录页输入，不经过 CLI。无头环境使用：

```bash
./bin/iboot login --no-browser
# 终端会打印授权地址；在任意浏览器完成登录后，CLI 自动接收回调
```

`--timeout` 控制等待回调的秒数，默认 300。

配置文件与凭据默认位于 `~/.config/iboot/`，可用 `IBOOT_CONFIG_DIR` 覆盖：

```bash
./bin/iboot config list
./bin/iboot config use dev
./bin/iboot config show

# 新增/删除 context；--issuer 省略时使用 base URL
./bin/iboot config add staging --base-url https://staging.example.com --use
./bin/iboot config remove staging
```

新增 context 可用 `--issuer`、`--client-id`、`--redirect-uri` 覆盖默认值；删除当前
context 会被拒绝，需先切换到其他 context。删除 context 时同时清理该 context 的本地凭据。

`credentials.json` 权限为 `0600`，按 context 隔离；切换 context 不会复用其他环境的令牌。

## 4. 只读命令

| 命令 | 说明 |
|---|---|
| `iboot user list [--username] [--status] [-p/-s]` | 用户列表 |
| `iboot user get <id>` | 用户详情 |
| `iboot role list` | 角色列表 |
| `iboot dept tree` / `iboot dept org-chart` | 部门树 / 组织架构图数据 |
| `iboot dict types` / `iboot dict data <dictType>` | 字典类型 / 字典数据 |
| `iboot param list` / `iboot param get <key>` | 参数配置 |
| `iboot job list` / `iboot job logs` | 定时任务与执行日志 |
| `iboot backup list` / `iboot backup summary` | 备份记录与汇总 |
| `iboot log login` / `iboot log operate` | 登录日志 / 操作日志 |
| `iboot online list` | 在线会话 |
| `iboot token list` | 当前用户的个人访问令牌元数据 |
| `iboot api <METHOD> <PATH> [-d body]` | 任意已存在接口的逃生口 |

所有查询命令支持 `-p/--page`、`-s/--size`；分页命令还支持 `--all`：忽略 `--page`，
固定每页 100 条拉取全部页并合并，结果超过 10,000 条时拒绝执行，建议增加筛选条件。
`--json` 输出解包后的业务数据，`--raw` 输出原始响应（含 CloudEvents 信封）。

### 运维写命令

| 命令 | 说明 |
|---|---|
| `iboot job run <id> --yes` | 立即执行一次定时任务；结果用 `job logs --job-id <id>` 查看 |
| `iboot job run <id> --wait --yes` | 触发并等待本次执行进入终态；失败以业务错误退出 |
| `iboot job logs --job-id <id>` | 按任务过滤执行日志 |
| `iboot backup trigger --yes` | 手动触发一次备份 |
| `iboot backup trigger --wait --yes` | 触发并轮询到 `SUCCESS`/`FAILED` |
| `iboot online kick <tokenId> --yes` | 强制下线 `online list` 中的会话 |

写命令默认拒绝执行，必须带 `--yes`；`--dry-run` 只打印方法、路径和请求体，不发送请求。
`backup trigger --wait` 默认每 2 秒轮询、最多等待 300 秒，可用 `--interval` / `--timeout`
覆盖；等待失败或超时以业务错误退出码结束，不会删除或修改备份记录。

`job run --wait` 在触发前记录该任务最新执行日志，触发后只接受新产生的日志；默认每 2 秒
轮询、最多等待 300 秒。成功输出日志 ID，失败输出异常信息并以业务错误退出，超时提示
未出现新的终态日志。

写操作由服务端 `@Log` 记录，可用 `iboot log operate` 查看；Docker 全量部署默认关闭备份，
`backup trigger` 会返回“备份功能未启用”的业务错误，这是预期行为。

### 个人访问令牌

PAT 面向 CI 与无浏览器环境，使用交互式登录会话签发，之后可长期使用：

| 命令 | 说明 |
|---|---|
| `iboot token create <name> [--expires-days <n>] [--scope <perm>]... --yes` | 签发 PAT；明文只显示一次 |
| `iboot token list` | 列出当前用户的 PAT 元数据 |
| `iboot token revoke <id> --yes` | 撤销 PAT |

有效期默认 30 天、最长 90 天；每个用户最多 10 个活动令牌。签发与撤销是写操作，
必须带 `--yes`；`--dry-run` 只打印请求。列表中的 `status` 为 `ACTIVE`、`REVOKED`
或 `EXPIRED`，并显示最近使用时间（尽力记录，可能缺失）。

```bash
./bin/iboot token create ci --expires-days 30 --yes
# 立即保存输出的令牌明文；后续 list 不再返回
./bin/iboot token list
./bin/iboot token revoke 3 --yes
```

`--scope` 可重复指定权限码，必须是当前用户已有权限的子集；不指定时继承全部权限。
例如只允许 CI 查询用户与触发任务：

```bash
./bin/iboot token create ci --scope user:list --scope job:run --yes
```

有效权限为权限快照与 scope 的交集，超出范围的接口返回 403。PAT 的权限是签发时的快照：
角色或权限调整后不会自动同步，需要撤销并重建。用户被停用或删除时，该用户的全部 PAT
立即失效。PAT 不出现在在线列表，也不会被会话级强退覆盖；撤销入口是 `token revoke`
或用户停用/删除。

操作日志中的 `authType` 区分 `SESSION` 与 `PAT`；`iboot log operate --json` 与门户的
操作日志页均可查看，便于区分人工操作与 CI 触发。

CI 中优先使用标准输入注入，避免令牌进入命令行参数与 shell 历史：

```bash
printf '%s' "$IBOOT_PAT" | ./bin/iboot user list --token-stdin
```

令牌来源优先级为 `--token-stdin` > `IBOOT_TOKEN` > 本地凭据；标准输入的令牌不会
写入凭据文件。

## 5. 输出与退出码

默认输出表格；列表响应会自动识别 `rows`、`records`、`list`、`items` 等数组字段。

| 退出码 | 含义 |
|---|---|
| `0` | 成功 |
| `2` | 用法错误或参数非法 |
| `3` | 认证失效，需要重新登录 |
| `4` | 越权（服务端 403） |
| `5` | 网络错误或服务端 5xx |
| `6` | 业务错误或本地配置错误 |
| `7` | 写操作缺少 `--yes` |

`iboot api` 对 GET/HEAD/OPTIONS 默认放行；其他方法必须带 `--yes`，`--dry-run` 只打印
请求而不发送。CLI 不实现本地权限判断，功能权限与数据权限都由服务端完成。

## 6. 安全说明

- 密码不经过 CLI；CLI 保存访问令牌与身份令牌；服务端若返回刷新令牌也会保存。
- 令牌不写入日志、不输出到终端；`status` 只显示用户与 JWT 到期时间。
- Spring Authorization Server 对公共客户端 + 授权码**不下发刷新令牌**，会话语义由共享 Redis
  的滑动续期承担：资源服务不因 JWT 自身 `exp` 过期而拒绝仍在会话内的请求。401 表示会话已失效，
  CLI 直接提示重新登录；只有服务端确实返回刷新令牌时才尝试续期一次。
- `IBOOT_TOKEN` 与 `--token-stdin` 仅用于 CI/测试；`--token-stdin` 优先于环境变量，
  且不会写入凭据文件。不要在交互式终端或共享环境中使用长期令牌。
- PAT 是长期凭据：保存在 CI secret 中，不要提交到仓库或写入日志；怀疑泄漏时立即
  `token revoke`，必要时停用用户以撤销其全部 PAT。
- 认证服务重启会使内存中的授权记录失效；由于没有刷新令牌，重新 `login` 即可。

### 过渡期会话限制（已知限制）

CLI 会话在 `add-multi-client-sessions` 落地前**不写入在线名单**：

- 门户会话不会被 CLI 登录驱逐；
- CLI 会话不出现在“在线用户”列表；
- 管理员按用户执行强制下线时，不会撤销 CLI 会话；
- PAT 同样不出现在在线列表，也不会被会话级强退覆盖；PAT 只能通过 `token revoke`
  或用户停用/删除撤销。

> **状态标注（2026-11-18）**：以上为已登记的已知限制。当前阶段保留该限制，
> 不启动 `add-multi-client-sessions` 变更；需要管理员强制下线覆盖 CLI 会话，或需要
> 门户/小程序/CLI 多客户端并存时，再单独立项。CLI 自身的 `logout` 仍可撤销当前会话。

该限制已在 `openspec/specs/auth-service/spec.md` 与 `openspec/specs/developer-cli/spec.md`
中登记。

## 7. 排障

| 现象 | 处理 |
|---|---|
| `127.0.0.1:8765` 被占用 | 关闭占用进程；CLI 使用固定端口，需与认证服务登记的 redirect URI 一致 |
| 登录后立即 401 | 检查 context 的 issuer 与网关地址是否一致；确认认证服务已启动 |
| `403` | 当前用户没有该接口权限；换有权限的账号，不要绕过服务端权限 |
| PAT 请求 401 | 令牌已过期、已撤销，或用户被停用/删除；重新签发或恢复用户状态 |
| 连接失败 | 确认门户 `https://localhost:5173`（宿主机开发）或 `https://localhost`（Docker）可访问；网关 `http://localhost:8088` 不再是客户端入口 |
| 证书校验失败 | 设置 `IBOOT_CLI_TRUSTSTORE="$PWD/docker/certs/truststore.p12"`；先执行 `scripts/tls-init.sh`。不要用 `-k` 或取消校验绕过 |
| OIDC 发现失败 | 确认 `https://<公开来源>/.well-known/openid-configuration` 可达，且 discovery 的 issuer 与 context 的 issuer 一致 |
| Docker 环境连接失败 | 先执行 `scripts/docker-deploy.sh status`，再确认使用 Docker context（`https://localhost`） |

## 8. 能力边界与后续

CLI 已覆盖登录、只读查询、运维写命令、PAT 与 CI 自动化；功能面到此为止，不设新版本号，
后续由真实使用驱动。

- 可选的体验打磨：shell completion、预构建 jar/安装脚本、`iboot api -d @file`、
  `token list --expiring-in`、`job logs --follow`；不单独立项。
- 属于其他能力域：门户 PAT 管理页、管理员查看/撤销他人 PAT、scope 预设、PAT 权限实时同步。
- 明确不做：管理类 CRUD、`backup restore` 等破坏性操作、消息/审批/AI 命令、MCP 通道与
  多客户端会话。

路线、非目标与已登记的过渡期限制见 [ROADMAP](../ROADMAP.md)。

## 9. 相关文档

- [06-API 集成](<./06-API 集成.md>)
- [10-后端配置详解](<./10-后端配置详解.md>)（OAuth2 客户端配置）
- [23-微服务底座操作指南](./23-微服务底座操作指南.md)
- [ROADMAP](../ROADMAP.md)（开发者 CLI 路线与判据）
