# Proposal

## Why

阶段 2 只剩系统域。file、notify、job、workflow、monitor 五次拆分之后，`iboot-admin` 里已经没有任何非系统域的功能：11 个控制器、57 条路径分属用户、角色、菜单、部门、岗位、字典、参数，外加依赖系统域聚合的 dashboard、statistics 与 i18n。继续以「admin」为名，等于让最后一个服务的名字与它的边界互相矛盾。

同时，系统域是整个阶段唯一没有能力契约的域：`openspec/specs/` 下 19 个 spec 覆盖了文件、消息、调度、审批、监控、认证与 AI，却没有一条描述「用户/角色/菜单由谁拥有、数据权限范围由谁提供、其余服务如何消费系统域数据」。而它恰恰是其余 6 个服务共同依赖的根——这份契约的缺失比名字的错误更值得处理。

## What Changes

- **模块改名**：`iboot-admin` → `iboot-system`，包名 `com.iboot.admin` → `com.iboot.system`，artifactId 与模块目录同步重命名。
- **运行时身份**：`spring.application.name` 改为 `iboot-system`，注册中心中的服务名随之变更。**端口保持 8080**，因此按端口寻址的配置（file、ai 的 scope 地址）无需改动。
- **网关路由**：路由 id `admin` → `system`，占位配置 `IBOOT_ADMIN_URI` → `IBOOT_SYSTEM_URI`；`order: 100` 与 `Path=/api/**` 谓词不变，兜底语义不变。
- **依赖方寻址**：job 与 monitor 的 `scope-url` 默认值 `http://iboot-admin/api/auth/scope` → `http://iboot-system/...`；ai 的 `admin-base-url` 同样改为 `http://iboot-system`。
- **契约不变**：不新增、不删除、不改动任何 HTTP 路径与方法，57 条路径逐条保持不变。前端零改动。
- **数据边界不变**：`iboot_admin` schema 作为历史名称保留，本次不改名、不迁数据、不清理遗留表。schema 改名与遗留表清理另起变更。
- **跨 schema 只读改判**：五个服务对 `sys_user` / `sys_dept` / `sys_config` / `sys_dict_data` 的只读访问从「待收敛的临时债务」改为「接受的长期模式」，并把实际消费的表列语义固化为显式契约；dashboard 与 statistics 对 `iboot_monitor` 两张日志表的只读同样改判为长期接受。
- **缺陷修正**：`iboot-monitor` 的 `PermissionScopeClient` 绑定了 `${iboot.job.permission.scope-url}` 与 `${iboot.job.permission.cache-seconds}`，与其自身 `iboot.monitor.permission.*` 配置键不匹配，导致配置文件与环境变量从未生效；本次改为正确前缀。
- **BREAKING**：注册中心服务名从 `iboot-admin` 变为 `iboot-system`。任何以 `http://iboot-admin` 或 `lb://iboot-admin` 寻址的外部脚本、部署配置或运维手册必须同步更新，否则调用会变成服务发现失败。

## Capabilities

### New Capabilities

- `system-management`: 系统域作为独立可部署单元的所有权边界；数据权限范围的提供方式；其余服务消费系统域数据（跨 schema 只读、服务寻址、失败语义）的契约。

### Modified Capabilities

无。本次不改变任何既有能力的行为或需求：路径、方法、响应结构、权限语义、数据归属均保持不变，改动集中在服务的名称与寻址方式上。

## Impact

**代码**

- `iboot-system`（原 `iboot-admin`）：181 个 Java 文件、17 个测试类、10 个 Mapper XML 的包名与模块坐标迁移；不涉及业务逻辑改动。
- `iboot-gateway`：本地与 Nacos 两份路由配置中的 id、uri 与占位变量。
- `iboot-ai`：`AiProperties.adminBaseUrl` 默认值。
- `iboot-job`、`iboot-monitor`：`scope-url` 默认值；monitor 另修正配置键前缀。

**接口**

- 对外 HTTP 契约零变化：57 条 `/api/**` 路径、方法与响应结构全部保持原样。
- 对内的服务发现契约发生变化：服务名 `iboot-admin` 不再存在。

**数据**

- 不涉及数据迁移。`iboot_admin` 中的 12 张系统域表与 25 张前几次拆分遗留的表均原样保留。

**依赖关系**

- `iboot-auth` 直连 `iboot_admin` 读写用户、角色、菜单、参数与登录日志——不受影响（schema 未变）。
- `iboot-file`、`iboot-job`、`iboot-monitor`、`iboot-notify`、`iboot-workflow` 跨 schema 只读系统域表——不受影响（schema 未变），但消费关系在本变更中首次被写成契约。
- `iboot-ai` 经服务名调用系统服务——需要改寻址。

**脚本、配置与文档**

- `scripts/dev-init.sh`、`scripts/build.sh`、`docker/deploy.sh`、`scripts/regress/` 下的服务名与快照文件名。
- `ROADMAP.md`、`doc/23`、`doc/07`、`doc/11`、`README.md`。

**明确不改的**

- `jwt.secret` 的默认值 `iboot-admin-secret-key-...` 是密钥字面量，不是服务名，保持原样。
- 数据备份配置中的库名 `iboot-admin`（`BackupCommandFactoryTest` 等）指 schema，保持原样。
- 前端 `iboot-portal` 一行不改。
- `openspec/specs/knowledge-graph` 中以「admin」指称系统服务的措辞会过时；这属于术语同步，不构成需求变更，留待该能力下次变更时一并处理。
