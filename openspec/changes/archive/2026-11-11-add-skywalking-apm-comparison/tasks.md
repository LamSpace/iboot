# Tasks

## 1. 镜像与 Agent 资产

- [x] 1.1 在 `scripts/pull-docker-images.sh` 的 Tempo 段之后新增 BanyanDB `0.11.0`、OAP `11.0.0`、Horizon UI `horizon-1.0.0` 三条 `pull → tag → rmi`，全部使用 `swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/apache/...` 中转；验证：连续执行两次脚本均成功退出，`docker images` 中存在三个编排引用的固定标签
- [x] 1.2 新增 `scripts/apm/fetch-agent.sh`：从 Apache 归档站（优先国内镜像）下载 `apache-skywalking-java-agent-9.8.0.tgz`，校验 SHA-512 后解压到 `docker/skywalking/agent/`，目录幂等（已存在则跳过下载），并把该目录加入 `.gitignore`；同时把 `springmvc-annotation-6.x`、`spring-webflux-6.x`、`spring-cloud-gateway-4.x` 三个 optional 插件移入 `plugins/`，把不兼容的 `apm-lettuce-*` 移入 `expired-plugins/`；验证：脚本可重复执行，`plugins/` 中存在三个 Spring 6/7 插件，Agent 启动后 OAP 能看到业务入口链路
- [x] 1.3 在 `.env.example` 增加 `IBOOT_SKYWALKING_ENABLED=false`、`IBOOT_SKYWALKING_AGENT`、`IBOOT_SKYWALKING_OAP` 三个占位并注明默认值；验证：`grep` 三个变量均存在，且注释说明打开开关的前置条件（Agent 已下载、辅助栈已启动）

## 2. 辅助栈编排

- [x] 2.1 新增 `docker/skywalking/horizon.yaml`（取自官方模板，OAP query/admin 地址指向 `skywalking-oap:12800` / `skywalking-oap:17128`），标注默认账号仅限本机；验证：文件可被 YAML 解析，地址与 compose 服务名一致
- [x] 2.2 在 `docker/docker-compose.yml` 新增 `skywalking-banyandb`、`skywalking-oap`、`skywalking-ui` 三个服务，`profiles: ["apm"]`，端口 `17913`、`11800/12800/17128`、`18082:8081`，OAP 堆上限 `-Xms512m -Xmx1024m`，BanyanDB 命名卷，三者各配健康检查；验证：`docker compose --profile apm up -d --wait` 全部 healthy，`curl http://localhost:18082` 返回 UI 页面，`curl http://localhost:12800/graphql` 可响应
- [x] 2.3 确认三个服务未被任何业务服务加入 `depends_on`，且不带 `--profile apm` 时 `docker compose up -d` 不会启动它们；验证：默认启动后 `docker ps` 无 `iboot-skywalking-*`，业务服务健康

## 3. Agent 注入

- [x] 3.1 修改 `scripts/dev-init.sh`：当 `IBOOT_SKYWALKING_ENABLED=true` 时，在九个业务服务的 `java -jar` 前追加 `-javaagent` 与 `-Dskywalking.agent.service_name/-Dskywalking.collector.backend_service/-Dskywalking.agent.sample_n_per_3_secs=-1`；开关关闭时命令与改动前逐字一致；验证：`bash -x scripts/dev-init.sh` 或记录启动命令，两种开关下对比；打开开关后 OAP GraphQL `queryServices` 能查到九个服务名
- [x] 3.2 在 `docs/11-可观测性设计.md` 记录 `mvn spring-boot:run` 单服务调试时如何手动加 Agent（`-Dspring-boot.run.jvmArguments`），避免只有 `dev-init.sh` 路径可用；验证：按文档命令启动单个服务后，OAP 中能看到该实例

## 4. 双后端链路验收

- [x] 4.1 新增 `scripts/apm/verify.sh`：经网关发起一次已知跨服务请求，用 `X-Trace-Id` 查 Tempo；用 OAP GraphQL `findService → findEndpoint → queryTraces` 在固定时间窗口内取入口链路，断言两侧都包含网关与预期的下游服务；任一缺失以非零退出并指出缺失服务；验证：全栈开启 Agent 时一次跑通，人为停掉某服务的 Agent 后脚本能报出缺失
- [x] 4.2 验证辅助栈停机的隔离性：`docker compose --profile apm stop` 后发起请求，业务正常、Tempo 链路完整，`scripts/regress/trace-chain.sh` 原有用法与结果不变；验证：脚本退出码 0，Tempo 查询成功，日志无导出异常导致的请求失败
- [x] 4.3 验证既有追踪契约未变：对比实验前后的 OTLP 配置、采样率与 `X-Trace-Id` 行为；验证：`git diff` 中九个服务的 `application.yml` 与 `TraceIdResponseFilter` 无改动，任一请求仍返回 `X-Trace-Id`

## 5. 影响度量与选型结论

- [x] 5.1 新增 `scripts/apm/measure.sh`：以 `iboot-system` 为样本，分别在开关关闭/打开下各启动 3 次记录到 `/actuator/health` 成功的时间；服务稳定后记录 `ps -o rss=` 与 `jcmd GC.heap_info`；用固定并发（8）与固定请求数的 `curl` 循环统计完成请求数；输出可粘贴的原始数据表；验证：同一组参数连续执行两次，量级一致且脚本不依赖缺失的压测工具
- [x] 5.2 执行度量并把命令、样本数、原始数据写入 `docs/24-SkyWalking 与 Tempo 对比实验.md`；验证：文档中的命令可逐条复现，数据包含对照组与实验组
- [x] 5.3 撰写一页选型结论：分别说明 agent 零侵入埋点与 SDK 埋点适合的场景、启动/内存/吞吐代价、拓扑与方法级剖析能力差异，并明确「本次不替换 Tempo」；验证：结论能直接回答 ROADMAP 的四条验收标准，且不含未经测量的断言

## 6. 文档与路线图

- [x] 6.1 在 `docs/11-可观测性设计.md` 增加「辅助 APM 对比栈」小节：组件与端口、`apm` profile 启停、Agent 开关、与 Tempo 的主辅关系、整体下线步骤，并链接 docs/24；验证：按小节命令完成一次启停与下线演练
- [x] 6.2 更新 `README.md` 的本地中间件清单与 `ROADMAP.md`：§4 SkyWalking 行改为 OAP 11.0.0 + Horizon UI 1.0.0 + BanyanDB 0.11.0 + Agent 9.8.0；§3 阶段 2 收尾在实验完成后标记该辅助项完成；§5 R4 保持 +3 进程并补充内存上限；验证：文档版本号与 compose/拉取脚本完全一致，`grep` 不再出现 11.1.0 作为当前目标版本
- [x] 6.3 在 `docs/23-微服务底座操作指南.md` 增加排障入口：辅助栈未就绪、Agent 未加载、GraphQL 查询失败三类现象与定位命令；验证：文档命令可直接执行，覆盖 4.1/4.2 的失败场景

## 7. 集成验收

- [x] 7.1 全栈冒烟：门户登录 → 用户列表 → 发起一次审批，分别在辅助栈运行与停止两种状态下重复；两次都无 5xx，Tempo 链路完整，辅助栈运行时 SkyWalking 链路完整；验证：`scripts/apm/verify.sh` 与既有冒烟脚本均通过
- [x] 7.2 确认改动边界：`git status` 中不包含任何业务 Java 代码、`pom.xml`、数据库脚本或 `iboot-portal` 变更；`mvn -s /home/lam/repo/settings.xml test` 全量通过；验证：diff 范围与 proposal 的 Impact 一致，测试全绿
- [x] 7.3 验证默认开发路径未退化：不设置任何 SkyWalking 变量时 `scripts/dev-init.sh` 拉起全栈，启动耗时与内存占用与实验前基线同量级；验证：与改动前记录对比，无 Agent 连接尝试与额外进程

## Workflow follow-up

- 评审通过后归档本变更，核对归档后的 `apm-comparison` 主规格。
- 是否长期保留 SkyWalking 由选型结论决定：若保留，另起变更把辅助栈转正并更新能力索引；若下线，删除 `apm` profile 与拉取脚本条目，保留 docs/24 的对比记录。
- 镜像站同步 11.1.0 组合后，另起变更升级版本，不在本变更内追踪。

