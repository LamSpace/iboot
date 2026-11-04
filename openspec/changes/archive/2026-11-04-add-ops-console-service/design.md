# Design

## Context

当前 Spring Boot Admin（SBA）4.0.2 的 server 与 client 依赖都在 `iboot-system` 内，`SpringBootAdminConfig` 仍带 `@EnableAdminServer`；但全仓没有配置 `spring.boot.admin.client.url`，因此控制台实际上只显示系统服务自身。门户的 `ServerMonitorView.vue` 通过 `/sba` iframe 打开它，Vite 把 `/sba` 代理到 `localhost:8080`，系统服务的白名单也放行了 `/sba/**`。

九个业务服务都已注册 Nacos，`iboot-kernel` 的默认白名单放行 `/actuator/**`，并且都已具备统一的 `run-java.sh`、Dockerfile 与 logback 配置（`service-runtime`）。SBA 4.0.2 的 starter 传递依赖 `spring-boot-admin-server-cloud`，其 `AdminServerDiscoveryAutoConfiguration` 在存在单个 Spring Cloud `DiscoveryClient` 时启用，`DefaultServiceInstanceConverter` 默认使用 `/actuator` 与 `health`，与当前服务的暴露方式一致。

约束：Boot 4.0.3、SCA 2025.1.0.0、Nacos 3.x；ROADMAP R4 的本机内存压力；生产环境不得把管理控制台放到公网入口。动机见 `proposal.md`，行为契约见 `specs/ops-console/spec.md` 与 `specs/service-runtime/spec.md`。

## Goals / Non-Goals

**Goals:**

- 用独立进程提供运维控制台，业务服务零 SBA client 配置。
- 通过注册中心自动跟随实例上下线。
- 提供单实例巡检与日志级别调整，作为 Grafana/ELK/Tempo 之外的“扳手”。
- 开发环境保持门户内嵌体验，生产环境只允许内网访问。
- 把 SBA 从系统服务中彻底移除，并让新服务遵守既有运行期模板。

**Non-Goals:**

- 不替代 Prometheus/Grafana 的指标聚合与告警。
- 不做控制台的高可用多副本；默认单副本。
- 不修改网关的业务路由，不为生产新增 `/sba` 公网路由。
- 不引入 SBA 的 client 推送模式。
- 不在本变更中实现阶段 3 的 Helm/Ingress/NetworkPolicy 清单；只保证部署形态与之兼容。

## Decisions

### D1：独立 `iboot-ops`，而不是并入 `iboot-monitor`

新增第十个可部署模块 `iboot-ops`，端口 8089；不连数据库，不依赖 `iboot-kernel`。

理由：控制面与数据面分离。`iboot-monitor` 拥有审计表、在线会话与日志查询，是业务可用的监控数据服务；把 SBA server 塞进去会把它的可用性与控制台绑定——监控服务挂掉时，最需要用来定位问题的控制台也一起消失。独立后 SBA 升级、认证与资源限制都可以单独调整。

备选：并入 `iboot-monitor`。被否，原因是依赖膨胀、可用性耦合，以及 monitor 的 JWT 安全链与 SBA 的 UI 认证会互相干扰。

代价是 R4 的第十个 JVM。缓解：ops 堆上限 256MB 左右、`dev-init.sh` 支持关闭、K8s 单副本。

### D2：注册中心拉取（pull），不做 client 推送

`iboot-ops` 引入 `spring-cloud-starter-alibaba-nacos-discovery`，由 SBA 的 `DiscoveryClient` 自动发现实例：

```yaml
spring.boot.admin.discovery:
  services: iboot-*
  ignored-services: iboot-ops
  converter:
    management-context-path: /actuator
```

理由：九个业务服务不需要新增依赖、不需要写 SBA 地址；实例上下线由注册中心驱动；与 `service-discovery` 的既有契约一致。忽略 `iboot-ops` 自身，避免控制台巡检自己并陷入认证配置。

备选：client 推送。被否，九处配置与地址漂移，且与“注册中心真实承载”的方向相反。

### D3：不依赖 `iboot-kernel`

`iboot-ops` 只有 Web、Actuator、SBA server、Nacos discovery 与 Prometheus registry。日志配置复制一份独立的 `logback-spring.xml`（与网关同样的先例），启动脚本与 Dockerfile 复用 `service-runtime` 模板。

理由：`iboot-kernel` 会带入 Spring Security、Redis、JDBC、jjwt、AspectJ 与业务安全链；ops 没有业务域，也不需要 JWT。硬套 DDD 四层同样没有收益，参照 `iboot-gateway` 作为基础设施服务的先例。

备选：依赖 kernel 只为复用 logback。被否，依赖过重且会激活不需要的安全自动配置。备选：抽一个 `iboot-runtime` 公共模块。当前只有两个非 kernel 服务，抽取属于过度设计。

### D4：统一的 Actuator 暴露面

九个业务服务统一暴露：

```
health, info, metrics, prometheus, loggers, threaddump,
env, configprops, beans, caches, scheduledtasks, mappings
```

- `heapdump` 默认不暴露，需要排障时显式开启；
- `shutdown` 永不暴露；
- `iboot-ai` 现在只放行 `/actuator/health` 与 `/actuator/prometheus`，改为与 kernel 服务一致的 `/actuator/**`，否则它在控制台里只有健康页；
- `iboot-gateway` 增加 `metrics,prometheus`，顺带补齐 Prometheus 对网关的采集缺口。

理由：SBA 的页面价值来自这些端点；只暴露 health 会让控制台退化成第二个健康检查页。生产环境通过内网与认证控制暴露面。

备选：只为 system 扩大暴露面。被否，其它实例的巡检价值不大。

### D5：HTTP Basic 认证，不用 form login

`iboot-ops` 引入 `spring-boot-starter-security`，安全链只覆盖控制台：

- `/sba/**` 需要认证，使用 HTTP Basic；
- `/actuator/health` 与 `/actuator/prometheus` 放行，便于探针与集中监控；
- CSRF 关闭（SBA UI 的写操作不带 CSRF token）；
- `frameOptions.sameOrigin`，配合开发环境的同源代理；
- 账号密码来自 `IBOOT_OPS_USER` / `IBOOT_OPS_PASSWORD`，密码无默认值，缺失时启动失败并给出明确提示；
- 本地开发可通过 `IBOOT_OPS_SECURITY_ENABLED=false` 关闭认证，`dev-init.sh` 默认注入该值，使门户 iframe 直接打开；生产不设置，保持默认开启。

理由：SBA server 本身没有内置认证；Spring Security 默认 form login 占 `/login`，而网关已把 `/login` 路由给 `iboot-auth`，会产生冲突。Basic 避开该冲突，也适合 iframe。

备选：form login。被否，`/login` 冲突且需要额外处理 CSRF 与登录页。备选：复用门户 JWT/会话做 SSO。被否，iframe 无法附加 `Authorization` 头，现有前端也没有 cookie 会话。

### D6：开发内嵌、生产内网

- 开发：Vite 的 `/sba` 代理从 `8080` 改为 `8089`，浏览器看到的是同源 iframe；`monitor.sba.url` 保持相对路径 `/sba`，由代理或反向代理决定最终落点，避免 iframe 跨源。
  代理 MUST 保留原始 Host（Vite 的 `changeOrigin: false`）：SBA 用请求 Host 生成页面的 `base href`，
  改写 Host 会让 SPA 的模块资源与接口变成跨源，iframe 内表现为空白页。
- 生产：网关不新增 `/sba` 路由；`iboot-ops` 只暴露 ClusterIP，通过 `kubectl port-forward` 或带来源白名单的内网 Ingress 访问。
- 门户新增 `monitor.sba.enabled`：为 `false` 时不渲染 iframe，显示“运维控制台仅限内网访问”的说明。

理由：满足“开发内嵌、生产内网”的访问模型，同时避免生产门户展示一个必然失败的 iframe。`monitor.sba.url` 已经是现成的配置解耦点，不需要改前端接口。

### D7：控制台显式设置上下文路径

`iboot-ops` 明确设置 `spring.boot.admin.context-path: /sba`。当前系统服务的 yml 中没有该配置，只在 `doc/10` 里出现过，实际值可能位于 Nacos 配置；实施时先核实系统服务当前的 `/sba` 是否真的可用，再迁移，避免把“文档写了但没生效”的状态带到新服务。

### D8：运行期与规范

- `iboot-ops` 提供 `logback-spring.xml`、`script/run-java.sh`、Dockerfile（非 root、`/app/logs`、`EXPOSE 8089`）；
- 父 POM 增加模块，SBA 版本继续使用已有的 `spring-boot-admin.version=4.0.2`；
- `service-runtime` 的“九个服务”措辞通过本次 delta 改为“所有可部署服务”；
- 归档时把 `openspec/specs/service-runtime/spec.md` 的 Purpose 中“九个服务”同步改掉（Purpose 不属于 delta 的可修改范围）。

## Risks / Trade-offs

- **R4 第十个 JVM** → ops 堆上限约 256MB；`dev-init.sh` 提供关闭开关；K8s requests 100m/256Mi、limits 500m/512Mi；单副本。
- **Actuator 暴露面扩大** → 仅内网 + Basic 认证；`env`/`configprops` 的值由 Boot 默认脱敏；`heapdump` 默认关闭；`shutdown` 不暴露。
- **SBA 4.0.2 × Boot 4.0.3 的运行时差异** → 先在开发环境对九个实例做一次发现与页面冒烟，重点检查 Nacos 实例地址、`/actuator` 索引与日志级别写操作；当前 system 内嵌已在跑，可降低风险。
- **`/login` 路由冲突** → 使用 Basic，不启用 form login。
- **iframe、认证与 frameOptions 的组合** → 开发环境坚持同源代理 + `sameOrigin`；不做跨源直连。
- **多副本视图不一致** → 控制台默认单副本；多副本需要引入共享存储，本变更不做。
- **与 Grafana 的定位混淆** → 在 `doc/11` 与 README 中明确控制台是“单实例巡检与运行期控制”，不承担指标聚合与告警。
- **Nacos 注册地址的可达性** → 本地开发同机可达；阶段 3 要求 ops 与业务服务在同一命名空间/网络内，NetworkPolicy 只放行控制台到业务服务管理端口的流量。
- **门户生产降级不完整** → `monitor.sba.enabled` 默认跟随环境配置；生产 values 显式设为 `false`，避免依赖 iframe 加载失败兜底。

## Migration Plan

1. 新增 `iboot-ops` 模块并启动，确认注册中心可见、控制台能列出九个实例。
2. 扩大九个业务服务的 Actuator 暴露面，逐个确认巡检页面可用。
3. 切换门户：Vite `/sba` 代理指向 8089，`monitor.sba.url` 保持 `/sba`，新增 `monitor.sba.enabled`。
4. 从 `iboot-system` 移除 SBA 依赖、配置类、白名单、iframe 放宽与测试断言；确认旧 `/sba` 不再提供页面。
5. 生产按“ClusterIP + 内网访问”部署，确认公网网关访问 `/sba` 不转发。
6. 回滚：把 Vite/反向代理的 `/sba` 目标改回系统服务并恢复系统侧 SBA（`git revert` 对应提交）；`monitor.sba.url` 保持 `/sba` 不变。控制台是旁路，回滚不影响业务数据。

## Open Questions

- 生产内网入口最终采用 `kubectl port-forward` 还是带白名单的内网 Ingress，留到阶段 3 的 Ingress/NetworkPolicy 设计中决定；本变更只要求“不经公网网关”。
- 是否把 `iboot-ops` 纳入 Prometheus 抓取与告警，作为可选项在实施时决定；不影响控制台的核心验收。
