# Design

## Context

- Tempo 在 `docker/docker-compose.yml` 中常驻，OTLP 接收端口为 4317/4318，HTTP 查询端口为 3200；Grafana 通过 `docker/scripts/grafana/provisioning/datasources/tempo.yaml` 把它配置成数据源。
- Grafana 已启用 `allow_embedding = true`，并允许匿名管理员访问，因此现有 Grafana 监控页可以直接 iframe。
- 门户的监控页遵循同一模式：`GrafanaMonitorView.vue`、`PrometheusMonitorView.vue` 等从 `sys_config` 读取地址，用 `IframePanel` 嵌入并复用工具栏。
- 菜单来自 `sys_menu`，侧边栏只取 `path` 的最后一段映射到 `/dashboard/<segment>`（`DashboardView.vue` 的 `convertMenuTree`），因此新菜单的 `path` 最后一段必须与路由子路径一致。
- Tempo 2.x 没有自带的浏览器查询界面，3200 根路径只有状态页；查询 UI 由 Grafana Explore 的 Tempo 数据源提供。

## Goals / Non-Goals

**Goals:**

- 在“可观测平台”下提供一键进入 Tempo 链路查询的菜单，默认选中 Tempo 数据源。
- 复用现有监控页的嵌入、刷新、新窗口打开和错误反馈模式。
- 菜单与权限遵循现有 `sys_menu` 与角色授权机制。

**Non-Goals:**

- 不实现原生 Vue 链路列表/水位图，也不直接调用 Tempo 的 `/api/search`。
- 不改造 Grafana 的登录、匿名访问或反向代理。
- 不改变 Tempo 的接收、采样和存储配置。
- 不新增 Tempo 专属的系统参数页。

## Decisions

### 决策 1：嵌入 Grafana Explore，而不是 Tempo 自身

Tempo 没有独立 UI，自建原生页面需要自行实现 TraceQL 输入、分页、trace 详情和水位图，工作量与维护成本远高于复用 Grafana。选择 iframe Grafana Explore，并在 URL 中固定 Tempo 数据源与 kiosk 模式。

**替代方案**：

- 自建 Vue 页面调用 Tempo HTTP API：UI 更统一，但重复 Grafana 已有能力，本次不做。
- 只做菜单跳转新窗口打开 Grafana：实现最小，但不满足“在系统页面单击查看”。

### 决策 2：固定数据源 `uid`，页面按配置拼装 URL

在 `tempo.yaml` 中为数据源写入稳定的 `uid: tempo`。页面读取 `sys.grafana.monitor.url` 作为 Grafana 基地址，拼出 `/explore?orgId=1&kiosk&schemaVersion=1&panes=<encoded JSON>`，其中 pane 根数据源为 `tempo`，查询对象的数据源为 `{type:"tempo", uid:"tempo"}`，默认查询类型为 `traceqlSearch`。

**替代方案**：

- 新增 `sys.tempo.monitor.url` 存完整 Explore URL：URL 很长且经过编码，配置页难以维护，数据源变更时容易失效。
- 复用数据源名称而非 uid：Grafana 升级或重建数据源时名称可能变化，uid 更稳定。

### 决策 3：复用现有监控页组件

新增 `TempoMonitorView.vue`，结构与 `GrafanaMonitorView.vue` 一致：工具栏、`IframePanel`、`getConfigByKey` 读取地址、新窗口打开与刷新。错误态沿用 `IframePanel` 的超时与重试逻辑。

**替代方案**：

- 抽一个通用 iframe 监控页组件：现有多个页面都是复制结构，本次不扩大重构范围。

### 决策 4：菜单与权限走数据库迁移

新增迁移插入 `sys_menu` 712（父级 9700，`C` 类型，路径 `/observability/tempo-monitor`，组件 `monitor/tempo-monitor/index`，权限 `monitor:tempo`，图标 `connection`，排序 5）和按钮权限 7121（`tempo:query`），并给管理员角色 1 授权。前端新增路由子项 `tempo-monitor`。

**替代方案**：

- 只改前端静态路由不写菜单：侧边栏不会出现入口，不符合需求。
- 沿用已存在的其他菜单 ID：会覆盖现有菜单。

### 决策 5：迁移版本排在存储变更之后

本 change 的迁移使用 `V9`，前提是 `default-object-storage-in-docker` 的 `V8` 已应用。Flyway 默认不接受乱序迁移，因此两个 change 按“存储 → Tempo 菜单”的顺序落地；若单独实施本 change，可把版本改为当时的下一个可用整数。

## Risks / Trade-offs

- [Grafana Explore URL 结构随版本变化] → 只依赖 `datasource`、`queryType`、`range` 等稳定参数；升级 Grafana 时执行一次手工验收。
- [Grafana 匿名管理员不适合公网] → 这是现有 Grafana 页面的既有风险；文档标注上云前需接入统一登录或反向代理，本次不改变访问模型。
- [iframe 被安全头阻止] → 现有 `allow_embedding = true` 已满足；新增页面验收时确认 `X-Frame-Options`/CSP 行为。
- [迁移版本冲突] → 明确 V9 依赖 V8；两个 change 按固定顺序应用。
- [Tempo 或 Grafana 不可用导致空白页] → 复用 `IframePanel` 的超时与错误态，显示重试入口。

## Migration Plan

1. 先应用 `default-object-storage-in-docker` 的 V8 迁移。
2. 应用本 change 的 V9 迁移，插入菜单与权限。
3. 重新构建并启动门户与数据库初始化。
4. 验证：管理员能看到菜单，点击后进入带 Tempo 数据源的 Explore；无权限角色不可见。
5. 回滚：把菜单 `status` 置为 0 或删除菜单行，并回退前端路由与页面；迁移本身不回滚。

## Open Questions

无。
