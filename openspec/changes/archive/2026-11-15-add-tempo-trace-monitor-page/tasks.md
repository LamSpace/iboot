# Tasks

## 1. Grafana 数据源

- [x] 1.1 在 `docker/scripts/grafana/provisioning/datasources/tempo.yaml` 为 Tempo 数据源设置 `uid: tempo`；验证：重启 Grafana 后，`GET /api/datasources` 返回的 Tempo 数据源 uid 为 `tempo`，且现有仪表板不受影响。

## 2. 门户页面与路由

- [x] 2.1 新增 `iboot-portal/src/views/monitor/TempoMonitorView.vue`：读取 `sys.grafana.monitor.url`，拼接 kiosk 模式的 Explore URL，固定 Tempo 数据源与 `traceqlSearch` 查询类型，复用 `IframePanel` 与工具栏；验证：`cnpm run type-check` 与构建通过，页面能在浏览器加载 Grafana Explore。
- [x] 2.2 在 `iboot-portal/src/router/index.ts` 的 Dashboard 子路由中新增 `tempo-monitor`；验证：直接访问 `/dashboard/tempo-monitor` 能渲染新页面。
- [x] 2.3 新增 `locales/zh-CN/monitor/tempo.json` 与 `locales/en-US/monitor/tempo.json`，在对应 `index.ts` 注册，并在 `icon-map.ts` 增加 `connection` 映射；验证：中英文切换后工具栏标题、按钮提示与菜单图标均正常，无缺失键。

## 3. 菜单与权限

- [x] 3.1 新增 `iboot-db-init/src/main/resources/db/migration/V9__add_tempo_monitor_menu.sql`，插入菜单 712（父级 9700、路径 `/observability/tempo-monitor`、权限 `monitor:tempo`）和按钮权限 7121（`tempo:query`），并授权角色 1；验证：迁移执行后 `sys_menu` 与 `sys_role_menu` 查询结果符合预期。
- [x] 3.2 更新 `SchemaMigratorTest` 的迁移数量为 9，并断言菜单 712/7121 与管理员授权存在；验证：`mvn -pl iboot-db-init test -Dtest=SchemaMigratorTest` 通过。
- [x] 3.3 验证权限过滤：没有 `monitor:tempo` 的角色登录后侧边栏不出现该菜单，管理员可见；验证：分别用两个角色登录门户检查。

## 4. 文档

- [x] 4.1 更新 `docs/11-可观测性设计.md` 的追踪章节与 `docs/05-快速开始.md`，写明门户“Tempo 链路追踪”入口、Grafana Explore 依赖与 kiosk 模式；验证：按文档步骤能打开并查询链路。

## 5. 集成验证

- [x] 5.1 门户全链路验证：点击菜单进入 Explore，按 traceId 查到一次经网关请求的完整链路，各段耗时可读；“新窗口打开”与“刷新”可用；验证：三项操作全部成功。
- [x] 5.2 故障态验证：停止 Grafana 或 Tempo 后打开页面，显示 `IframePanel` 的加载失败与重试入口，而不是空白页；验证：恢复服务后点击重试能重新加载。

## Workflow follow-up

- 本 change 的 V9 迁移需在 `default-object-storage-in-docker` 的 V8 之后应用。
- 完成后按项目评审要求归档该 change。
- 归档后确认 `openspec/specs/distributed-tracing/spec.md` 已包含“门户提供追踪查询入口”要求。
