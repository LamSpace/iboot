# Tasks

## 1. 模块与运行期骨架

- [x] 1.1 父 POM 增加 `iboot-ops` 模块并新建模块 `pom.xml`（web、actuator、SBA server、Nacos discovery、Prometheus registry、Security）；验证：`mvn -s /home/lam/repo/settings.xml -pl iboot-ops -am -DskipTests package` 成功。
- [x] 1.2 新建 `OpsApplication` 与 `application.yml`（8089、应用名、Nacos discovery、SBA `context-path=/sba`、`discovery.services=iboot-*`、忽略自身、暴露 health/info/prometheus）；验证：启动后 `/actuator/health` 为 UP，Nacos 中可见 `iboot-ops`。
- [x] 1.3 添加 `logback-spring.xml`、`script/run-java.sh`、Dockerfile（非 root、`/app/logs` 可写、`EXPOSE 8089`）；验证：镜像构建成功，容器内进程非 root，`logs/iboot-ops.log` 可写。
  - 说明：文件已按统一模板落地，jar 已本地启动验证；本机访问 Docker Hub 拉取 `openjdk:25-jdk-slim` 超时，镜像构建未执行。已确认接受该环境限制并在归档时保留说明，留待有镜像源的环境补验。
- [x] 1.4 实现 Basic 认证安全链（`/sba/**` 需认证、health/prometheus 放行、CSRF 关闭、`frameOptions.sameOrigin`、密码缺失时启动失败）；验证：MockMvc 测试覆盖未认证 401、认证后 200、缺失密码启动失败，`mvn -s /home/lam/repo/settings.xml -pl iboot-ops test` 通过。
- [x] 1.5 更新 `doc/09` 模块表与 README 服务表，加入 `iboot-ops` 的端口与职责；验证：文档中的端口与启动命令可执行。
- [x] 1.6 增加本地开发免认证开关 `iboot.ops.security.enabled`（默认开启，`dev-init.sh` 默认关闭）；验证：关闭时 `/sba` 无凭证返回 200，开启时未认证返回 401，`mvn -pl iboot-ops test` 通过。

## 2. 发现、暴露面与巡检

- [x] 2.1 统一九个业务服务的 `management.endpoints.web.exposure.include`（含 loggers/threaddump/env/configprops/beans/caches/scheduledtasks/mappings），`iboot-ai` 安全白名单扩到 `/actuator/**`，`iboot-gateway` 增加 metrics/prometheus；验证：逐个请求 `/actuator/loggers` 与 `/actuator/threaddump` 返回 200。
- [x] 2.2 启动全部服务，验证控制台自动列出九个实例；验证：`grep -r "spring.boot.admin.client" --include=application.yml .` 无命中。
- [x] 2.3 验证实例上下线跟随与注册中心故障恢复；验证：停一个服务后 60 秒内从控制台消失，重启后自动出现；Nacos 不可用时 ops 仍启动，恢复后列表自动恢复。
- [x] 2.4 验证运行期日志级别调整；验证：通过控制台把 `iboot-ai` 的某个 logger 调到 DEBUG，日志立即变化且进程启动时间不变，随后恢复原级别。
- [x] 2.5 更新 `doc/11` 可观测性章节，说明控制台是单实例巡检与运行期控制入口，不替代 Prometheus/Grafana、ELK 与 Tempo；验证：文档入口、端口与定位准确。
- [x] 2.6（可选）在 `docker/scripts/prometheus/prometheus.yml` 增加 `iboot-ops` 抓取 job；验证：Prometheus Targets 页面该 job 为 UP。

## 3. 门户与配置迁移

- [x] 3.1 把 `vite.config.ts` 中 `/sba` 代理目标从 8080 改为 8089；验证：开发环境门户的服务器监控页 iframe 能加载控制台。
- [x] 3.2 `ServerMonitorView.vue` 读取 `monitor.sba.enabled`，关闭时显示「运维控制台仅限内网访问」提示而不是 iframe；验证：开关两种状态下页面表现符合预期。
- [x] 3.3 新增 `iboot-db-init` 迁移 `V2__ops_console_config.sql`：更新 `monitor.sba.url` 并新增 `monitor.sba.enabled`；验证：重复运行 db-init 后 `sys_config` 值正确且不重复插入。
- [x] 3.4 更新 `doc/10` 的 Spring Boot Admin 配置章节，改为 `iboot-ops` 的地址、上下文路径与开关；验证：文档值与 `application.yml`、迁移 SQL 一致。
- [x] 3.5 「在新窗口中打开」使用 `iboot-ops` 的绝对地址（开发环境 8089），不再打开门户的 5173 地址；验证：开发环境点击后浏览器地址为 `http://localhost:8089/sba`。
- [x] 3.6 修正 `/sba` 代理的 `changeOrigin`，保留门户 Host，避免 SBA SPA 的模块与接口跨源导致 iframe 空白；验证：代理返回的 `base href` 为门户地址，页面资源与 `/sba/instances` 均返回 200。

## 4. 从系统服务移除

- [x] 4.1 删除 `iboot-system` 的 SBA server/client 依赖、`SpringBootAdminConfig`、`/sba/**` 等白名单、`WebConfig` 排除与 `frameOptions` 放宽；验证：`grep -r "spring-boot-admin" iboot-system` 无命中，系统服务启动正常且旧 `/sba` 返回 404。
  - 实测：未带令牌访问 `/sba` 为 401，`/actuator/mappings` 中无 `/sba` 映射，端点已不存在。
- [x] 4.2 更新 `SecurityWhitelistBindingTest` 等涉及 `/sba/**` 的测试断言；验证：`mvn -s /home/lam/repo/settings.xml -pl iboot-system test` 通过。
- [x] 4.3 核对 `spring.security.user` 的 actuator 账号是否仍被使用，未使用则移除；验证：系统服务启动与现有测试通过，无新增 401 回归。
- [x] 4.4 更新 `doc/23` §947 的决定记录，说明 SBA 已迁出系统服务；验证：文档不再声称 SBA 仍在系统服务进程内。

## 5. 本地编排与规范

- [x] 5.1 `scripts/dev-init.sh` 增加 `iboot-ops:8089` 与 `IBOOT_OPS_ENABLED` 开关；验证：默认启动时 ops 健康检查通过，关闭开关时跳过且其余服务全绿。
- [x] 5.2 更新 `ROADMAP.md` 能力索引与阶段说明，加入运维控制台及其验收判据；验证：条目与实现一致，不再保留「SBA 在系统服务内」的表述。
- [x] 5.3 把 `openspec/specs/service-runtime/spec.md` 的 Purpose 中「九个服务」与场景标题「九个服务都能构建镜像」改为「所有可部署服务」/「每个可部署服务都能构建镜像」；验证：`openspec validate add-ops-console-service --strict` 通过，且主 spec 中不再残留「九个服务」的适用范围表述。

## 6. 集成验收

- [x] 6.1 全量启动十个服务，验证 `scripts/dev-init.sh` 健康检查全绿，门户登录与主要业务接口正常。
  - 本地 `.env` 的 `IBOOT_AI_MYSQL_DATABASE` 原为 `iboot_ai`（单库合并后已不存在），已修正为 `iboot`；AI 健康恢复，`dev-init.sh` 退出码 0。
- [x] 6.2 故障隔离：停 `iboot-ops` 后业务接口不受影响；停任一业务实例后控制台在 60 秒内反映，重启后恢复。
- [x] 6.3 生产形态检查：网关配置中没有 `/sba` 路由，控制台只通过 ClusterIP/本机访问，公网入口不可达；验证：`grep -n "sba" iboot-gateway/src/main/resources/application-local.yml` 无命中。
- [x] 6.4 运行 `openspec validate add-ops-console-service --strict` 与受影响模块测试，确认全部通过。
