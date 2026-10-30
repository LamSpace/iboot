# 阶段 2.0 门禁 Spike 报告

> 对应变更：`openspec/changes/add-microservice-foundation`，任务组 2（2.0-A）与任务组 3（2.0-B）
>
> 门禁不过则不进入 2.1。本报告只记录实测结论，实施细节见 tasks.md。

## 1. 目的

在改动任何业务代码之前，先回答三个问题：

1. Spring Cloud Alibaba `2025.1.0.0` 在 Boot `4.0.3` 上能否真正跑起来（而不只是依赖能解析）？
2. Nacos 3.x 的注册发现、配置热更新、Sentinel 限流三项能力是否都可用？
3. 跨进程的 TraceID 能否贯穿？

## 2. 环境与版本（实测）

| 组件 | 版本 | 来源 |
|---|---|---|
| JDK | 25.0.3 | 本机 |
| Spring Boot | 4.0.3 | 父 POM |
| Spring Cloud | 2025.1.0（`spring-cloud-commons 5.0.0`） | 依赖树 |
| Spring Cloud Alibaba | 2025.1.0.0 | 依赖树 |
| nacos-client | 3.1.1 | 依赖树 |
| Nacos Server | 3.1.1 | `nacos/nacos-server:v3.1.1` |
| sentinel-core | 1.8.9 | 依赖树 |

结论：客户端与服务端版本严格对齐，不存在跨大版本组合。

## 3. 门禁项与结果（2.0-A）

### S1 两服务互相发现 ✅

- `spike-b` 启动后注册：`nacos registry, DEFAULT_GROUP spike-b 192.168.0.107:18081 register finished`
- `spike-a` 注册成功，并通过服务名调用 B：

  ```
  GET http://localhost:18080/call-b
  → {"service":"spike-b","message":"local-default","startedAt":...}
  ```

- **踩坑**：`@LoadBalanced` 的 `RestClient` 需要显式引入 `spring-cloud-starter-loadbalancer`。Nacos 发现 starter 不再传递带入负载均衡实现，缺失时报 `UnknownHostException: spike-b`（服务名被当成主机名）。

### S2 配置热更新 ✅

- 配置经 `spring.config.import: optional:nacos:spike-b.yml` 引入，未使用 `bootstrap.yml`（R9）。
- 发布前：`message=hello-from-nacos`；发布后：`message=hot-reload-v2`。
- **JVM 启动时间保持一致**（`1793371432948`），证明是热更新而非重启。
- **踩坑**：观测点不能放在 `@RefreshScope` 的 Bean 实例字段上——刷新会重建 Bean，实例字段随之重置，看起来像重启。必须取 `ManagementFactory.getRuntimeMXBean().getStartTime()`。

### S3 Sentinel 限流 ✅

- 为资源 `/call-b` 加载 QPS=2 的规则。
- 连续 20 次请求：`200` 2 次、`429` 18 次，响应体 `Blocked by Sentinel (flow limiting)`。
- 说明：门禁阶段用代码加载规则即可；**规则持久化到 Nacos 属于 2.4 的工作，本次未验证**。

### S4 配置中心不可用时的降级 ⚠️ 部分验证

- `spring.config.import` 使用 `optional:` 前缀，配置不存在时服务正常启动（本次首次启动即在该状态下完成）。
- Nacos 完全停机时的启动行为留到 2.1 接入真实服务时验证，门禁阶段不重复。

## 4. 门禁项与结果（2.0-B）

### S5 跨两跳同一条 TraceID ✅

- Tempo 单机启动，应用经 OTLP HTTP（4318）直连导出，采样 100%。
- 一次 `GET /call-b` 在 Tempo 中得到三个 span，父子关系正确：

  ```
  [spike-a] http get /call-b   SERVER   （根）
    └─ [spike-a] http get      CLIENT
         └─ [spike-b] http get /hello   SERVER
  ```

- 涉及服务：`spike-a`、`spike-b`；TraceID 唯一。

### 踩坑（按发现顺序）

| 现象 | 原因 | 处置 |
|---|---|---|
| 应用完全没有追踪日志 | Boot 4 把追踪自动配置拆到 `spring-boot-starter-opentelemetry`，只加 `micrometer-tracing-bridge-otel` 不够 | 改用一个官方 starter |
| 配置写了但不生效 | 属性名在 Boot 4 改变：`management.opentelemetry.tracing.export.otlp.endpoint`（旧的 `management.otlp.tracing.endpoint` 不再识别） | 按 `spring-boot-opentelemetry` 的配置元数据修正 |
| 下游另起一条链路，无客户端 span | Boot 4 同样把 `RestClient` 自动配置拆到 `spring-boot-starter-restclient`；缺它时要么没有 Builder，要么拿到无观测能力的实例 | 引入该 starter，使用自动配置的 Builder |
| 手写 `RestClient.builder()` 不传递上下文 | 手工构建绕过了 Boot 的观测定制 | 改用自动配置的 Builder + `DiscoveryClient` 显式解析实例 |

## 5. 环境层面的发现（与业务无关，但影响后续）

| 发现 | 影响 | 处置 |
|---|---|---|
| Nacos 3.x 控制台与主端口分离，路径不再带 `/nacos` 前缀 | 照搬 2.x 文档会访问不到 | 控制台容器内 `8080` 映射到宿主机 `8849`，避开 admin 的 `8080` |
| Nacos 3.x 移除了 `/nacos/v1/console/health/readiness` | 健康检查恒失败，容器卡在 `health: starting` | 改用控制台的 `/v3/console/health/readiness` |
| Nacos 3.x 镜像**即使关闭鉴权也要求提供 `NACOS_AUTH_TOKEN`** | 不设置则容器以 255 退出 | compose 中提供开发用令牌 |
| 配置写入接口 `/nacos/v1/cs/configs` 仍可用，`/v3/admin/cs/config` 需要登录态 | 自动化脚本的选择 | 回归/门禁脚本使用 v1 写配置 |

## 6. 结论

**2.0-A 通过**：注册发现、配置热更新、Sentinel 限流三项均在本机实测成立，版本矩阵成立。

**2.0-B 通过**：跨两跳 TraceID 贯穿成立，父子关系正确。

**放行进入 2.1**。

### 进入 2.1 时必须沿用的结论

1. 追踪依赖用 `spring-boot-starter-opentelemetry`，不要只加 bridge。
2. 出站 HTTP 用 `spring-boot-starter-restclient` 自动配置的 `RestClient`，不要手写 `RestClient.builder()`，否则上下文不传递。
3. OTLP 端点属性为 `management.opentelemetry.tracing.export.otlp.endpoint`。
4. Sentinel 规则持久化到 Nacos 尚未验证，列入 2.4 的范围。

## 7. 复现步骤

```bash
# 1. 启动 Nacos
docker compose -f docker/docker-compose.yml --profile platform up -d nacos

# 2. 构建并启动门禁服务（独立聚合，不进根构建）
cd spike/microservice-gate && mvn -s /home/lam/repo/settings.xml -DskipTests package
java -jar gate-b/target/gate-b-1.0.0.jar &
java -jar gate-a/target/gate-a-1.0.0.jar &

# 3. 验证发现与调用
curl http://localhost:18080/call-b

# 4. 验证配置热更新
curl -X POST "http://localhost:8848/nacos/v1/cs/configs" \
  --data-urlencode "dataId=spike-b.yml" --data-urlencode "group=DEFAULT_GROUP" \
  --data-urlencode "content=spike:
  message: hot-reload-v2" --data-urlencode "type=yaml"
curl http://localhost:18081/hello

# 5. 验证限流
for i in $(seq 1 20); do curl -s -o /dev/null -w "%{http_code}\n" http://localhost:18080/call-b; done | sort | uniq -c
```
