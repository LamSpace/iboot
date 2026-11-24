# Design

## Context

SPIRE Server/Agent 都已在配置中启用 `health_checks` HTTP 端点（容器内 8080、`/live` 与 `/ready`），
但只在 `data` 网络，Prometheus/blackbox 在 `app`/`obs`，因此此前无法探测。SVID 刷新调度（A1）
已把状态写入 `docker/certs/runtime/svid-refresh.prom`；node-exporter 已在 `obs` 网络且挂载
`./certs/runtime:/certs:ro`，只差启用文本文件采集。

## Goals / Non-Goals

**Goals:**

- SPIRE 控制面不可用、SVID 刷新失败或停滞、SVID 临期都能触发告警。
- 告警带统一 `team="security"` 标签并被单独路由。
- 回归可做故障注入且不破坏运行中 SPIRE/SVID。

**Non-Goals:**

- 不引入 SPIRE 原生 Prometheus telemetry；健康端点足够覆盖可用性。
- 不新增外部通知渠道；本地默认复用门户 webhook。
- 不改变 SVID TTL、刷新阈值或调度安装方式。

## Decisions

### 1. 用 blackbox 探测健康端点

SPIRE 已提供 HTTP 健康检查，避免启用 telemetry 或自建 exporter。SPIRE 容器加入 `obs` 网络，
blackbox 用 `http_2xx` 模块探测 `/ready`；`/live` 不单独告警，避免进程存活但未就绪被误判正常。

### 2. 文本文件指标

调度写 `.prom` 文件，node-exporter 文本文件采集复用现有 `node-exporter` job，无需改 Prometheus
抓取目标。指标文件与运行期证书副本同目录，已在宿主机与容器间挂载。

### 3. 故障注入不改运行中 SPIRE

回归不停止 SPIRE 容器，而是直接改写指标文件注入 `exit_code=1` 与 4 天前的成功时间，
等待 Prometheus 抓取并断言 `SvidRefreshFailed`/`SvidRefreshStale` 进入 pending/firing；
结束后恢复原文件并重跑调度写入正常指标。

### 4. 路由按 team 标签

Alertmanager 顶层 receiver 仍是 webhook；新增 `team="security"` 子路由与 `security-webhook`
receiver。Alertmanager v0.35 不支持 `--config.expand-env`，因此本地 URL 固定复用门户 webhook，
生产接入安全值班渠道时替换该 URL，文档明确说明。
