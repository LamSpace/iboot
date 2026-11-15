# Tasks

## 1. 数据与开发编排

- [x] 1.1 新增 `iboot-db-init` Flyway `V11__add_governance_consoles.sql`：插入顶级菜单 9600“微服务治理”、子菜单 9601“Nacos 控制台”/9602“Sentinel 控制台”、管理员角色授权，以及 `sys.nacos.console.url`、`sys.nacos.console.embed`、`sys.sentinel.dashboard.url`、`sys.sentinel.dashboard.embed` 四个配置；图标使用已注册的 Element Plus 图标名；验证：在 Testcontainers MySQL 或本地库执行 Flyway 后查询 `sys_menu`/`sys_role_menu`/`sys_config`，确认行数与默认值符合预期，且迁移可在空库顺序执行。
- [x] 1.2 在 `docker/docker-compose.yml` 的 `nacos` 服务增加 `NACOS_AUTH_CONSOLE_ENABLE=false`；验证：`docker compose -f docker/docker-compose.yml config` 通过，重启 Nacos 后 `curl -s http://localhost:8849/v3/console/server/state` 中 `login_page_enabled` 为 `false`，控制台不再要求登录。
- [x] 1.3 验证 Nacos 内嵌限制：`curl -sS -D - -o /dev/null http://localhost:8849/next/`（GET）返回 `X-Frame-Options: DENY`，HEAD 不返回该头；验证：浏览器 iframe 直连 `/next/` 被拒绝，门户同源代理剥离该头后可正常加载。
- [x] 1.4 更新 `docs/23-微服务底座操作指南.md` 与 `docker/README.md`：写清“微服务治理”入口、Nacos 同源代理 `/nacos-console/`、开发内嵌/生产跳转、`NACOS_AUTH_CONSOLE_ENABLE` 仅限本地；验证：按文档命令能打开两个控制台，生产检查项与实际 compose 配置一致。
- [x] 1.5 在 `iboot-portal/vite.config.ts` 增加仅开发环境生效的 `/nacos-console` 代理：`/nacos-console/*` → `http://localhost:8849/next/*`，`/nacos-console/v3/*` → `http://localhost:8849/v3/*`，并在 `proxyRes` 中删除 `X-Frame-Options`；验证：重启 Vite 后 `curl -D - http://localhost:5173/nacos-console/` 不返回 `X-Frame-Options`，浏览器可加载控制台。

## 2. 门户页面

- [x] 2.1 新增 `iboot-portal/src/views/monitor/NacosConsoleView.vue`：读取 `sys.nacos.console.url`（开发默认 `/nacos-console/`）与 `sys.nacos.console.embed`，`embed=true` 时用 `IframePanel` 渲染，`embed=false` 时显示说明卡和“在新窗口打开”；工具栏提供刷新与新窗口打开；验证：`cnpm run build` 通过，开发环境页面免登录显示 Nacos 控制台，刷新按钮可重新加载 iframe。
- [x] 2.2 新增 `iboot-portal/src/views/monitor/SentinelDashboardView.vue`：读取 `sys.sentinel.dashboard.url` 与 `sys.sentinel.dashboard.embed`，复用同一双模式与工具栏；验证：开发环境页面免登录显示 Sentinel Dashboard，停掉 Dashboard 后显示失败与重试。
- [x] 2.3 在 `iboot-portal/src/router/index.ts` 增加 `/governance/nacos` 与 `/governance/sentinel` 子路由；验证：登录后直接访问两个路径能渲染页面，未授权用户按现有路由守卫被拦截。
- [x] 2.4 新增 `iboot-portal/src/locales/zh-CN/monitor/nacos.json`、`sentinel.json` 及英文对应文件，并在 `locales/*/index.ts` 的 `monitor` 命名空间注册；验证：页面标题、按钮、失败提示在中英文切换下均能解析，无缺失 key 警告。
- [x] 2.5 更新 `docs/04-功能模块.md` 或相关门户文档，说明“微服务治理”菜单与两种访问模式；验证：文档描述与页面实际行为一致。

## 3. 集成验证

- [x] 3.1 开发环境端到端：启动 Nacos、Sentinel、db-init 与门户，确认两个页面均内嵌且无登录页；验证：手工记录两个页面加载成功，菜单显示“微服务治理”。
- [x] 3.2 降级路径：分别停止 Nacos 与 Sentinel，刷新页面确认显示失败与重试，门户其它页面不受影响；验证：恢复控制台后点击重试能重新加载。
- [x] 3.3 生产模式模拟：把两个 `embed` 配置改为 `false`，确认页面只显示说明卡和跳转按钮，不渲染 iframe；同时确认 `iboot-portal/nginx.conf` 没有新增 `/nacos-console` 代理；验证：浏览器网络面板中没有对应控制台的 iframe 请求。
- [x] 3.4 权限验证：用无 `governance:nacos` / `governance:sentinel` 权限的角色登录，确认菜单不可见且直接访问路由被拦截；验证：管理员角色仍可看到两个入口。

## Workflow follow-up

- Archive the change after review and merge.
- If implementation order changes, swap V11/V12 with `group-system-config-params` and update both migrations before applying.
