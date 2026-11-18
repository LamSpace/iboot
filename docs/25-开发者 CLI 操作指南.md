# 25-开发者 CLI 操作指南

> 适用版本：iBoot 1.0.0。CLI 是独立模块 `iboot-cli`，只经网关 `/api/**` 调用既有接口，
> 不直连数据库，不进入 Docker/Helm 部署清单。

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

默认 context 为 `dev`，指向网关 `http://localhost:8088`：

```bash
./bin/iboot login
./bin/iboot status
./bin/iboot logout
```

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
```

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
| `iboot api <METHOD> <PATH> [-d body]` | 任意已存在接口的逃生口 |

所有查询命令支持 `-p/--page`、`-s/--size`；`--json` 输出解包后的业务数据，`--raw`
输出原始响应（含 CloudEvents 信封）。

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
- `IBOOT_TOKEN` 仅用于 CI/测试，优先级高于本地凭据；不要在交互式终端或共享环境中使用。
- 认证服务重启会使内存中的授权记录失效；由于没有刷新令牌，重新 `login` 即可。

### 过渡期会话限制（已知限制）

CLI 会话在 `add-multi-client-sessions` 落地前**不写入在线名单**：

- 门户会话不会被 CLI 登录驱逐；
- CLI 会话不出现在“在线用户”列表；
- 管理员按用户执行强制下线时，不会撤销 CLI 会话。

> **状态标注（2026-11-18）**：以上两项为已登记的已知限制。当前阶段保留该限制，
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
| 连接失败 | 确认 `http://localhost:8088/actuator/health` 可访问；Docker 环境先执行 `scripts/docker-deploy.sh status` |
| OIDC 发现失败 | 确认 `/.well-known/openid-configuration` 经网关可达 |

## 8. 相关文档

- [06-API 集成](<./06-API 集成.md>)
- [10-后端配置详解](<./10-后端配置详解.md>)（OAuth2 客户端配置）
- [23-微服务底座操作指南](./23-微服务底座操作指南.md)
- [ROADMAP](../ROADMAP.md)（开发者 CLI 路线与判据）
