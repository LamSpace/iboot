# Tasks

## 1. 兼容性验证与依赖底座

- [x] 1.1 在 `iboot-gateway` 临时接入 SCA Sentinel Gateway 适配，启动后确认路由资源能被 Sentinel 识别：在 Dashboard 看到 `workflow`、`notify` 资源，且临时 QPS=1 规则能让请求返回阻塞响应；若 SCG 5 不兼容，记录现象并按 design D3 的备选方案调整，验证命令与结论写入本任务备注。
- [x] 1.2 新建非部署模块 `iboot-sentinel` 并加入父 POM `<modules>`，验证 `mvn -s /home/lam/repo/settings.xml -pl iboot-sentinel -am -DskipTests package` 成功。
- [x] 1.3 在 `iboot-gateway`、`iboot-workflow`、`iboot-notify` 增加 `spring-cloud-starter-alibaba-sentinel`（网关另加 `spring-cloud-alibaba-sentinel-gateway`），验证 `mvn -s /home/lam/repo/settings.xml -pl iboot-gateway,iboot-workflow,iboot-notify -am -DskipTests package` 成功，且 `mvn dependency:tree` 中 Sentinel 版本为 1.8.9。
- [x] 1.4 让 `iboot-kernel` 与 `iboot-gateway` 依赖 `iboot-sentinel`，`sentinel-core` 以 `provided` 方式声明；验证未接入 Sentinel 的服务启动日志中没有 Sentinel 自动配置，`/actuator/prometheus` 不出现 `sentinel_` 指标。

## 2. 治理指标桥接

- [x] 2.1 在 `iboot-sentinel` 实现 `MetricExtension`：pass/block/exception 映射为 Counter，RT 映射为 Timer，线程数映射为 Gauge，统一附加 `application` 标签；用 `SimpleMeterRegistry` 单测断言各指标名称、标签与计数。
- [x] 2.2 增加自动配置 `@ConditionalOnClass({MetricExtension, MeterRegistry})` 与重复注册保护；用 `ApplicationContextRunner` 验证未接入 Sentinel 时不激活、重复创建上下文不会重复注册。
- [x] 2.3 编写/更新模块类与方法的 JavaDoc（中文说明、英文标签），验证 `mvn -s /home/lam/repo/settings.xml -pl iboot-sentinel test` 通过。
- [x] 2.4 在 `docs/11-可观测性设计.md` 增加 Sentinel 指标清单、标签约定与「Dashboard vs Prometheus」职责边界；按文档中的 PromQL 查询能返回数据（接入服务启动后验证）。

## 3. 网关路由级限流

- [x] 3.1 在 `iboot-gateway` 配置 Sentinel transport 与 Nacos `flow` Datasource（data-id `iboot-gateway-flow-rules`，group `SENTINEL_GROUP`），验证启动日志显示规则源加载成功、Dashboard 能看到规则。
- [x] 3.2 实现自定义 `BlockRequestHandler`，限流响应为 `{"code":429,"message":"请求过于频繁","path":"..."}` 且 `Content-Type` 为 JSON；用网关集成测试或单元测试断言状态码与响应体，`mvn -s /home/lam/repo/settings.xml -pl iboot-gateway test` 通过。
- [x] 3.3 为 `workflow`、`notify` 两条路由写入保守基线规则（其它路由不限制）；验证正常流量不受影响、单路由超限不影响其它路由。
- [x] 3.4 在 `docs/23-微服务底座操作指南.md` §7.6 补充限流响应与路由级规则说明；按文档下调 Nacos 阈值后请求返回 429，且网关启动时间不变。

## 4. 工作流调用熔断降级

- [x] 4.1 在 `HttpNotificationPort` 用编程式 `SphU.entry("iboot-workflow:notify-enqueue")` 包住「重试直到耗尽」的全过程，重试耗尽后调用 `Tracer.trace` 记录异常并在外层降级；单测覆盖正常、重试耗尽、熔断阻塞三条路径，`mvn -s /home/lam/repo/settings.xml -pl iboot-workflow test` 通过。
- [x] 4.2 配置 `iboot-workflow` 的 Nacos `degrade` Datasource（data-id `iboot-workflow-degrade-rules`），写入异常比例基线规则；验证规则加载成功、熔断打开时不再调用下游且失败计数增加。
- [x] 4.3 验证熔断窗口结束后自动半开并恢复调用；记录一次「停 notify → 熔断 → 恢复 notify → 自动恢复」的日志证据。
- [x] 4.4 更新 `HttpNotificationPort` 与相关方法的 JavaDoc，说明熔断资源名、降级语义与「通知失败不阻断审批」约束；验证文档与实际行为一致。

## 5. 通知服务入口流控

- [x] 5.1 在 `iboot-notify` 配置 Nacos `flow` Datasource（data-id `iboot-notify-flow-rules`），为 `/internal/notifications` 写入 QPS 基线；验证 Dashboard 能看到该 URL 资源与规则。
- [x] 5.2 实现统一 `BlockExceptionHandler`，被限流时返回项目 `Result` JSON（`code=429`）而非默认纯文本；用 MockMvc 测试断言状态码、`code` 与 `message`。
- [x] 5.3 验证未超限请求行为不变、单端点限流不影响通知服务的其它接口；`mvn -s /home/lam/repo/settings.xml -pl iboot-notify test` 通过。
- [x] 5.4 复核通知按 `eventId` 幂等：在调用方重试与提供方限流组合下不产生重复投递；补充或调整测试并在 `docs/23` 通知服务章节登记结论。

## 6. Nacos 规则初始化

- [x] 6.1 新增 `docker/sentinel/rules/` 下的三个基线规则 JSON（网关 `flow`、工作流 `degrade`、通知 `flow`），验证 JSON 可被对应 rule-type 解析。
- [x] 6.2 新增 `scripts/sentinel-init.sh`，通过 Nacos v1 配置接口幂等写入 data-id；连续执行两次后 `GET` 内容一致，脚本退出码为 0。
- [x] 6.3 在 `scripts/dev-init.sh` 的 Nacos 就绪之后调用规则初始化脚本；全新环境执行 `dev-init.sh` 后三个服务的规则均能在日志/Dashboard 中看到。
- [x] 6.4 在 `docs/23` 记录规则维护流程：Nacos 为权威来源、Dashboard 修改仅运行期有效、如何回滚到空规则。

## 7. Dashboard 必选组件

- [x] 7.1 在 `docker/docker-compose.yml` 增加无 profile 的 `sentinel-dashboard` 服务（镜像 `bladex/sentinel-dashboard:1.8.9`、`8858:8858`、堆上限 256MB、本地关闭认证、`extra_hosts` 兜底）；`docker compose config` 校验通过，`docker compose up -d sentinel-dashboard` 后健康检查通过。
- [x] 7.2 `scripts/dev-init.sh` 在中间件阶段启动并等待 Dashboard 就绪，`--status` 显示它，就绪输出打印访问地址；全新执行 `dev-init.sh` 时若 Dashboard 未就绪则给出明确失败信息。
- [x] 7.3 在 `.env.example` 增加 `SENTINEL_DASHBOARD_USER` / `SENTINEL_DASHBOARD_PASSWORD` 占位，并在 compose 注释与文档中明确非本地环境必须开启认证、不得使用默认口令；用 `docker compose config` 确认本地默认值不会泄漏到生产模板。
- [x] 7.4 验证 Dashboard 能发现 `iboot-gateway`、`iboot-workflow`、`iboot-notify` 三个客户端；从宿主机进程场景确认规则推送可用，若不可达按 design D5 设置 `client-ip` 并复验。
- [x] 7.5 停止 Dashboard，确认业务接口、已加载规则与 Prometheus 指标均不受影响；重启后客户端自动重新出现在 Dashboard。

## 8. 监控接入

- [x] 8.1 在 `docker/scripts/prometheus/prometheus.yml` 增加 `iboot-gateway`、`iboot-workflow`、`iboot-notify` 的抓取 job；Prometheus 页面三个 target 均为 UP。
- [x] 8.2 在 Grafana 或 `docs/11` 中提供治理指标查询：按资源查看阻塞、异常与 RT；触发一次限流后查询结果非空。
- [x] 8.3 验证 Prometheus 抓取不依赖 Dashboard：停 Dashboard 后 target 仍为 UP，`sentinel_block_total` 仍可增长。

## 9. 文档与路线图

- [x] 9.1 `docs/23` 新增「Sentinel 治理」章节，覆盖接入范围、规则 data-id、Dashboard 地址与认证、故障演练与回滚；按文档命令逐条执行通过。
- [x] 9.2 `README.md` 增加 Dashboard 访问地址与「本地开发必选、生产内网 + 认证」说明；启动方式与 `dev-init.sh` 实际输出一致。
- [x] 9.3 `ROADMAP.md` 把「Sentinel 业务接入」从阶段 2 收尾未完成项移入能力索引，更新 R4 常驻进程数（+1）与相关验收描述；`grep -n "Sentinel" ROADMAP.md` 不再出现「尚未接入」的现状描述。
- [x] 9.4 同步 `docs/spike/microservice-2-0-spike-report.md` 的后续说明或 `docs/23` 的引用，注明「规则持久化到 Nacos 已在本次变更验证」，不重写历史结论。

## 10. 集成验证与回滚演练

- [x] 10.1 运行受影响模块的完整测试：`mvn -s /home/lam/repo/settings.xml -pl iboot-gateway,iboot-workflow,iboot-notify,iboot-sentinel -am test`，全部通过。
- [x] 10.2 全栈冒烟：`scripts/dev-init.sh` 拉起后登录、发起并审批请假、确认通知投递；网关、Dashboard、Prometheus 均可访问。
- [x] 10.3 熔断演练：停止 `iboot-notify` 后审批主流程仍成功且调用方快速降级；恢复后自动半开并恢复投递，日志中保留证据。
- [x] 10.4 限流演练：通过 Nacos 下调网关路由或通知端点阈值，验证 429 响应、单资源隔离与启动时间不变；恢复基线阈值。
- [x] 10.5 回滚演练：清空 Nacos 规则后服务秒级回到无规则、业务不受影响；重新写入基线后规则恢复，无需重启。
- [x] 10.6 前端零改动检查：`git diff --stat` 不包含 `iboot-portal` 业务代码，门户经网关的既有路径与响应结构保持不变。

## Workflow follow-up

- 按项目 Git 流程在功能分支提交、创建 PR 并合并；合并后归档本变更。
