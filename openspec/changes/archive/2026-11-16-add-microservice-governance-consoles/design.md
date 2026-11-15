# Design

## Context

动机见 `proposal.md`。影响实现方式的现状与约束：

- 门户是 Vue 3 + Element Plus，路由在 `iboot-portal/src/router/index.ts` 中静态声明；`sys_menu` 只通过 `stores/permission.ts` 控制菜单可见性与路由访问，不负责生成路由。
- 现有嵌入页（`GrafanaMonitorView`、`TempoMonitorView`、`KibanaMonitorView`、`ServerMonitorView`）都通过 `getConfigByKey` 读取 `sys_config` 地址，并复用 `components/IframePanel.vue` 的加载、失败和重试状态。
- Nacos 3.3.0 的控制台 SPA 在 `/next/`；根路径 `GET /` 返回 302，且该响应带 `X-Frame-Options: DENY`。注意 `/next/` 的 GET 响应同样带 `X-Frame-Options: DENY`，HEAD 响应却不带，直接用 HEAD 检查会产生误导；控制台 SPA 还会把路径导航到根 `/`。因此 Nacos 控制台不能直接 iframe，需要同源入口剥离该响应头。控制台鉴权由 `nacos.core.auth.console.enabled`（环境变量 `NACOS_AUTH_CONSOLE_ENABLE`）控制，与客户端鉴权 `NACOS_AUTH_ENABLE`、管理端鉴权 `NACOS_AUTH_ADMIN_ENABLE` 相互独立。当前开发编排只关闭了后两者，控制台仍要求登录，且本地控制台用户库没有可用默认账号。
- Sentinel Dashboard 1.8.10 不发送 `X-Frame-Options`；开发编排使用 `-Dauth.enabled=false`，生产覆盖文件 `docker-compose.prod.yml` 开启登录并要求注入账号密码。登录态基于 Session Cookie。
- `ops-console` 规格已经确立了同类模式：开发环境可在认证关闭时通过门户内嵌，生产环境不得经门户暴露，内嵌关闭时门户降级为说明性提示。

## Goals / Non-Goals

**Goals:**

- 在门户新增“微服务治理”顶级入口，提供 Nacos 与 Sentinel 两个控制台页面。
- 页面地址与内嵌开关运行期可配，不依赖重新构建前端。
- 本地开发环境免登录内嵌；非本地环境保持控制台鉴权并只提供跳转。
- 菜单、权限、路由、配置与迁移方式与现有监控页保持一致。

**Non-Goals:**

- 不做 SSO、凭据代理或自动登录。
- 生产环境不为控制台增加门户同源反向代理；开发环境只为 Nacos 增加仅本地生效的 Vite 代理。
- 不改变 Nacos/Sentinel 的部署拓扑、规则来源或网关暴露边界。
- 不做通用的“任意第三方控制台注册表”，只覆盖这两个控制台。

## Decisions

### 1. 两个独立页面，而不是一个带标签页的聚合页

分别新增 `NacosConsoleView.vue` 与 `SentinelDashboardView.vue`，各自拥有路由、权限和配置键。理由是现有监控页都是“一页一工具”，独立页面便于分别授权、分别降级，也便于未来单独替换某个控制台。聚合页会把两个不同的鉴权边界绑在一起，收益不大。

考虑过的替代方案：一个“中间件控制台”页面内部用标签切换。拒绝原因是权限粒度、菜单可见性和失败隔离都会变差。

### 2. 每控制台一个 `embed` 开关，双模式渲染

页面读取 URL 与 `embed` 两个配置：`embed=true` 时渲染 `IframePanel`；`embed=false` 时渲染说明卡和“在新窗口打开”按钮。这样开发环境与生产环境共用同一份前端代码，生产不需要关闭 iframe 逻辑，只需要改配置。

考虑过的替代方案：始终 iframe。拒绝原因是生产控制台需要登录，跨源 iframe 里的登录态不可靠，而且不应把控制台登录页塞进门户。

### 3. Nacos 通过开发环境同源代理内嵌

Nacos 控制台的 GET 文档响应（包括 `/next/`）带 `X-Frame-Options: DENY`，HEAD 响应不带，直接用 HEAD 检查会产生误导；控制台 SPA 还会把路径导航到根 `/`，根 302 同样带 DENY。因此直接 iframe 不可行。

开发环境在 Vite 增加 `/nacos-console` 代理：

- `/nacos-console/*` → `http://localhost:8849/next/*`
- `/nacos-console/v3/*` → `http://localhost:8849/v3/*`
- 在 `proxyRes` 中删除 `X-Frame-Options`

门户 iframe 使用相对地址 `/nacos-console/`，与门户同源。代理只存在于 `vite.config.ts`，生产 nginx 不配置该路径；生产仍通过 `embed=false` 只提供内网跳转。

考虑过的替代方案：直接 iframe `/next/`（实测被浏览器拒绝）；新增独立反代容器（可行，但比 Vite 代理多一个常驻组件）；生产同源代理（不做，生产只跳转）。

### 4. 开发环境只关闭 Nacos 控制台鉴权

在 `docker/docker-compose.yml` 的 Nacos 服务增加 `NACOS_AUTH_CONSOLE_ENABLE=false`，保留 `NACOS_AUTH_ENABLE=false` 与 `NACOS_AUTH_ADMIN_ENABLE=false` 的既有语义。Sentinel 开发环境继续使用 `auth.enabled=false`。生产 compose 不增加任何关闭鉴权的配置。

考虑过的替代方案：为本地 Nacos 初始化一个控制台管理员账号。拒绝原因是本地用户库当前没有可用账号，即使补账号，用户仍需在 iframe 内登录，体验和凭据管理都不如开发环境直接关闭控制台鉴权干净。

### 5. 生产环境只跳转，不内嵌

生产环境保持控制台登录开启，门户只提供内网入口链接。门户不读取、不保存、不代填任何控制台凭据。Nacos 使用 localStorage Token，Sentinel 使用 Session Cookie；两者的登录态都不适合由门户代为维护，跨站 iframe 还会受浏览器第三方存储/Cookie 策略影响。

考虑过的替代方案：生产环境门户同源反向代理控制台，使 Cookie 与 localStorage 变成第一方。拒绝原因是 Sentinel 前端大量使用 `/app`、`/v1`、`/auth` 绝对路径，Nacos 也有自己的路径与鉴权头，需要在生产 nginx 增加代理与重写规则，属于独立项目；Nacos 的 Vite 代理仅用于本地开发，不作为生产方案。

### 6. 配置通过现有 `getConfigByKey` 读取

页面挂载时读取 `sys.nacos.console.url`、`sys.nacos.console.embed`、`sys.sentinel.dashboard.url`、`sys.sentinel.dashboard.embed`，读取失败或缺失时使用代码内默认值。这样运维可以在“参数配置”页面调整地址和模式，不需要重新构建前端。

考虑过的替代方案：构建期环境变量。拒绝原因是控制台地址和访问模式属于运行期运维参数，不是构建产物的一部分。

### 7. 菜单、权限与静态路由

新增顶级菜单 `9600`“微服务治理”，下挂 `9601`“Nacos 控制台”和 `9602`“Sentinel 控制台”；权限码为 `governance:nacos` 与 `governance:sentinel`，授权管理员角色。路由仍按现有方式在 `router/index.ts` 静态声明为 `nacos` 与 `sentinel` 子路由（实际地址 `/dashboard/nacos` 与 `/dashboard/sentinel`），菜单逻辑路径为 `/governance/nacos` 与 `/governance/sentinel`；`sys_menu` 的 `component` 字段只作为登记信息。

考虑过的替代方案：根据菜单动态生成路由。拒绝原因是当前门户没有动态路由加载器，新增这套机制超出本变更范围。

### 8. 不新增后端接口

页面复用 `getConfigByKey` 和 iframe 的加载/错误事件判断控制台是否可用，不新增健康检查接口。控制台停机时 `IframePanel` 会显示失败与重试；`embed=false` 时页面本身不依赖控制台在线。

考虑过的替代方案：新增后端代理健康检查。拒绝原因是这会把门户变成控制台的运行时依赖，而控制台停机本来就不应影响业务或门户。

### 9. 国际化沿用 `monitor` 命名空间

新增 `iboot-portal/src/locales/zh-CN/monitor/nacos.json`、`sentinel.json` 及对应英文文件，视图内使用 `monitor.nacos.*`、`monitor.sentinel.*`。菜单名称来自 `sys_menu`，不进入 i18n。

### 10. 迁移中写入开发友好的默认值

Flyway 迁移插入 `embed=true`，使本地开发开箱即可内嵌；Nacos 地址写成开发代理地址 `/nacos-console/`，Sentinel 地址写成 `http://localhost:8858`。生产部署清单要求把两个 `embed` 改为 `false`，并把 Nacos 地址改为内网绝对地址（不再经过门户代理）。这与现有 `monitor.sba.enabled` 的做法一致（迁移写入开发默认值，生产在参数配置或部署流程中覆盖）。

考虑过的替代方案：迁移写入 `embed=false`，开发脚本再改回 `true`。拒绝原因是开发栈需要额外步骤，且本地新库会先显示跳转页，不符合“开箱即用”的既有约定。

## Risks / Trade-offs

- [Nacos 升级后代理路径、响应头或控制台鉴权开关变化] → 升级检查清单增加“门户内嵌 Nacos”冒烟项；代理规则集中在 `vite.config.ts`，若新版本禁止内嵌，把 `sys.nacos.console.embed` 改为 `false` 即可降级，不需要改代码。
- [Vite 代理只存在于开发环境，生产误设 `embed=true`] → 生产部署清单要求 `embed=false`；生产 nginx 不提供 `/nacos-console` 代理，页面会显示加载失败而不是把控制台暴露到门户。
- [生产库忘记把 `embed` 改为 `false`，iframe 内出现控制台登录页] → 部署清单明确列出两项配置；运维文档在“生产检查”中重复提示；页面不保存任何凭据，最坏情况是用户在内网控制台登录页看到登录框。
- [开发环境关闭 Nacos 控制台鉴权降低安全性] → 仅限本地 compose；生产覆盖文件不包含该开关；文档说明该值不得用于非本地环境。
- [开发环境 Nacos 控制台仍显示登录页] → 迁移和 compose 检查项要求 `NACOS_AUTH_CONSOLE_ENABLE=false` 生效后再验收；若容器未重启，先重启 Nacos。
- [跨源 iframe 受浏览器隐私策略影响] → 开发环境两个控制台都不要求登录，不依赖第三方 Cookie/localStorage；一旦开启登录，页面切到 `embed=false` 跳转模式。
- [菜单 ID 或权限码冲突] → 迁移前确认 `9600/9601/9602` 未被占用；权限码使用新的 `governance:` 前缀。
- [路由与菜单不一致] → 新增路由后在测试中直接访问 `/governance/nacos` 与 `/governance/sentinel`，确认菜单隐藏时路由也按权限拦截。

## Migration Plan

1. 新增 `iboot-db-init` 迁移：菜单 `9600/9601/9602`、管理员角色授权、四个 `sys_config` 配置项（`embed=true`，Nacos URL 为 `/nacos-console/`）。
2. `docker/docker-compose.yml` 的 Nacos 增加 `NACOS_AUTH_CONSOLE_ENABLE=false`，重启 Nacos。
3. 在 `iboot-portal/vite.config.ts` 增加开发环境 `/nacos-console` 代理并剥离 `X-Frame-Options`。
4. 新增两个视图、静态路由、i18n 文案；执行前端构建。
5. 开发环境验收：两个页面均可免登录内嵌；工具栏新窗口打开与刷新可用；控制台停机时显示失败与重试。
6. 生产部署：执行迁移后，在“参数配置”把两个 `embed` 改为 `false`，把 Nacos URL 改为内网绝对地址，保持控制台鉴权开启，验证页面只显示跳转说明；生产 nginx 不添加 `/nacos-console` 代理。
7. 回滚：把两个 `embed` 改为 `false` 或删除配置行；删除菜单与角色授权行；撤回 compose 的 Nacos 环境变量和 Vite 代理。以上都不涉及业务数据，回滚不影响 Nacos/Sentinel 本身。
