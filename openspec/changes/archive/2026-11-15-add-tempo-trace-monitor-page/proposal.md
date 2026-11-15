# Proposal

## Why

系统已经把一次请求的链路写入 Tempo，Grafana 也通过 provisioning 把 Tempo 配成数据源，但门户里没有直接入口：用户必须自己打开 Grafana、手动切换数据源并进入 Explore。Tempo 自身在 3200 端口只提供 HTTP 查询 API 和状态页，没有独立的浏览器查询界面，因此“像 Grafana 那样集成”意味着在门户中嵌入一个预置 Tempo 数据源的 Grafana Explore 页面，而不是 iframe Tempo 本身。

## What Changes

- 为 Grafana 的 Tempo 数据源固定 `uid`，使深链不依赖运行时生成的随机标识。
- 门户新增“Tempo 链路追踪”页面：读取现有 Grafana 地址配置，以 kiosk 模式嵌入 Explore，数据源固定为 Tempo，支持在工具栏新窗口打开与刷新。
- 新增门户路由 `tempo-monitor`，并在“可观测平台”菜单下新增菜单项与查询权限，仅授权管理员角色。
- 补齐中英文文案与图标映射。
- 更新可观测性文档，说明 Tempo 查询入口位于门户，底层界面由 Grafana Explore 提供。
- 不改变 Tempo 接收与存储链路的方式，也不改变现有 Grafana 监控页。

## Capabilities

### New Capabilities

无。

### Modified Capabilities

- `distributed-tracing`: 新增“门户提供追踪查询入口”的要求：可从系统菜单直接进入按 traceId 或 TraceQL 查询链路的界面，入口受权限控制，查询后端不可用时给出明确错误。

## Impact

- 前端：`iboot-portal` 新增监控页面、路由、i18n 文案与图标映射。
- 数据：`iboot-db-init` 新增迁移，插入 Tempo 菜单与按钮权限并授权管理员角色。
- 部署：`docker/scripts/grafana/provisioning/datasources/tempo.yaml` 固定数据源 `uid`。
- 文档：可观测性与快速开始文档补充 Tempo 查询入口。
- 权限：新增 `monitor:tempo` 与 `tempo:query` 权限码。
