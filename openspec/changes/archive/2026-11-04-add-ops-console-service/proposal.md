# Proposal

## Why

Spring Boot Admin 4.0.2 已经在依赖里，但它被嵌在 `iboot-system` 内，全仓又没有配置任何 `spring.boot.admin.client.url`，实际只监控系统服务自己；另外八个服务没有统一的实例视图。微服务拆分完成后，排障需要在同一处看到每个实例的健康、JVM、线程、环境与日志级别，而 Prometheus/Grafana、ELK、Tempo 各自只覆盖指标、日志、链路中的一段，缺少一个「单实例实时体检与控制」的入口。

现在适合动手：SBA 的 `server-cloud` 已支持通过 Spring Cloud `DiscoveryClient` 从 Nacos 拉取实例，项目九个服务又都已注册 Nacos；把控制面从业务服务里剥离出来，不需要给任何业务服务加 SBA client 依赖。

## What Changes

- 新增独立可部署服务 `iboot-ops`（Spring Boot Admin Server，端口 8089），不连数据库、不依赖 `iboot-kernel`。
- 通过 Nacos `DiscoveryClient` 自动发现全部 `iboot-*` 实例；业务服务不新增 SBA client 依赖、不配置 SBA 地址。
- 统一九个业务服务的 Actuator 暴露面（health/info/metrics/prometheus/loggers/threaddump/env/configprops/beans/caches/scheduledtasks/mappings）；`heapdump` 默认关闭，`shutdown` 永不暴露。
- 控制台自带 Basic 认证；开发环境默认关闭认证并经 Vite 代理以同源 iframe 嵌入门户，生产环境保持认证开启且只开 ClusterIP / 内网入口，公网网关不新增 `/sba` 路由。
- 门户「服务器监控」页新增 `monitor.sba.enabled` 开关：关闭时显示内网访问提示，不再渲染会失败的 iframe。
- 从 `iboot-system` 移除 SBA server/client 依赖、`@EnableAdminServer`、`/sba/**` 等白名单、iframe 放宽配置与相关测试断言。
- **BREAKING**（运维入口）：SBA 控制台地址从 `http://<系统服务>:8080/sba` 迁移到 `http://<iboot-ops>:8089/sba`；收藏或脚本里写死旧地址的入口需要更新。

## Capabilities

### New Capabilities

- `ops-console`: 独立运维控制台的实例发现、单实例巡检与控制、访问边界、认证与降级契约。

### Modified Capabilities

- `service-runtime`: 运行期契约的适用范围从「九个服务」扩展为「所有可部署服务」，新增的 `iboot-ops` 必须遵守同一套日志、镜像与启动模板。

## Impact

- 新增 `iboot-ops` 模块：`pom.xml`、`application.yml`、`logback-spring.xml`、`script/run-java.sh`、`Dockerfile`。
- 父 POM 的 `<modules>` 增加 `iboot-ops`。
- 九个业务服务的 `management.endpoints.web.exposure.include` 配置调整（仅 yml）。
- `iboot-system`：移除 SBA 依赖、配置类、安全白名单与测试断言。
- `iboot-portal`：`ServerMonitorView.vue` 增降级分支，`vite.config.ts` 的 `/sba` 代理目标改为 8089，`sys_config` 增加 `monitor.sba.enabled`。
- `scripts/dev-init.sh`：增加 `iboot-ops:8089` 并支持关闭开关。
- 可选：`docker/scripts/prometheus/prometheus.yml` 增加 `iboot-ops` 抓取 job。
- 文档：`doc/10`、`doc/11`、`doc/23`、`README`、`ROADMAP`。
- 规范：新增 `ops-console`，修改 `service-runtime`。
