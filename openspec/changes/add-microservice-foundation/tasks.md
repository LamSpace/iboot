# Tasks

## 1. 回归基线（必须在任何改造之前完成）

- [x] 1.1 新增 `scripts/regress/api-snapshot.sh`：从 admin 的 `/v3/api-docs` 导出全部接口，归一化后写入 `scripts/regress/golden/admin.json`；归一化规则固定为「去掉 `servers`、路径排序、剔除示例值、剔除时间戳类字段」；验证：在未改代码的情况下连续执行两次，两次输出 `diff` 为空
- [x] 1.2 对 `iboot-ai`（`http://localhost:8081/v3/api-docs`）执行同样导出，写入 `scripts/regress/golden/ai.json`；验证：两次执行结果一致，且基线中全部路径都带 `/api/ai` 前缀
- [x] 1.3 新增 `scripts/regress/route-coverage.sh`：读取两份基线，校验每条路径只归属一个前缀（`/api/ai/**` 或 `/api/**`），并输出未覆盖与重复覆盖的路径；验证：对当前代码退出码为 0；手工把某条路径改成不匹配任何前缀后退出码非 0 且打印该路径
- [x] 1.4 新增 `scripts/regress/smoke.sh`：串起登录 → 用户信息变更 → 文件上传与下载 → 实时推送收到消息 → 数据备份触发 → 审批流转六条链路，每条断言状态码与关键字段；验证：在本地全栈（compose 中间件 + 8080/8081）上执行退出码为 0，且人为停掉 admin 后对应用例失败
- [x] 1.5 新增 `scripts/regress/permission-matrix.sh`：用固定用例表覆盖「角色 × 端点 × 期望结果」，同时包含允许与拒绝用例，每条标注验证的是功能权限还是数据范围；验证：矩阵在本地全栈上全部符合预期，且把某个低权限角色的期望同步改为「允许」后脚本会失败
- [x] 1.6 新增 `scripts/regress/regress.sh` 作为唯一入口，按「快照 → 路由覆盖 → 冒烟 → 权限矩阵」顺序执行并在任一失败时非零退出；新增 `doc/23-微服务底座操作指南.md` 记录四个脚本的用法、前置条件与「允许差异登记」规则；验证：`bash scripts/regress/regress.sh` 在本地全栈上退出码为 0
- [x] 1.7 记录改造前基线：在 `doc/23` 中写明本次捕获的接口总数、用例数与执行耗时；验证：文档中的数字与 `regress.sh` 实际输出一致

## 2. 2.0-A 门禁：Nacos 与 Sentinel 冒烟

- [x] 2.1 `docker/docker-compose.yml` 新增 `nacos` 服务，置于 `platform` profile，映射 `8848`、`9848`、`9849`，控制台 `8080` 映射到宿主机 `8849`，并设置内存上限与注释说明换端口原因；验证：`docker compose --profile platform up -d nacos` 后 `http://localhost:8849` 控制台可打开
- [x] 2.2 在 `spike/microservice-gate/` 下建两个最小 Boot 4.0.3 应用（独立聚合 pom，**不加入根 `pom.xml` 的 modules**）：B 启动后注册到 Nacos，A 通过服务发现调用 B；验证：控制台可见两个实例，A 调用 B 返回预期结果
- [x] 2.3 验证配置热更新：B 的一个可调参数由 Nacos 下发，修改后观察 A 侧行为变化且 B 进程启动时间未变；验证：改动前后 B 的启动时间一致、行为按新值变化
- [x] 2.4 验证 Sentinel 限流：A 接入 Sentinel starter，配置某资源 QPS 阈值，压测超出阈值后返回限流状态码；验证：阈值内请求成功、超出部分被限流，控制台可见规则
- [x] 2.5 撰写 `doc/spike/microservice-2-0-spike-report.md`，记录版本矩阵（Boot / Spring Cloud / SCA / Nacos client 与 server / Sentinel）、每项验证的实测结果、失败项与结论；验证：报告中的版本号与实际依赖、镜像标签逐一对得上

## 3. 2.0-B 门禁：链路追踪打通

- [x] 3.1 `docker/docker-compose.yml` 新增 `tempo` 服务（`platform` profile，映射 `4317`、`4318`、`3200`），并在 Grafana 数据源中登记 Tempo；验证：Grafana 中能看到 Tempo 数据源且健康检查通过
- [x] 3.2 两个 spike 应用接入 Micrometer Tracing + OTLP 导出，采样设为全量；验证：调用 A 后 Tempo 中出现链路
- [x] 3.3 断言跨两跳的追踪标识一致：A 调 B 后查询该标识，结果包含两个服务的段且父子关系正确；验证：`curl http://localhost:3200/api/traces/<traceId>` 返回两个服务的 span
- [x] 3.4 在 `doc/spike/microservice-2-0-spike-report.md` 追加追踪部分与最终结论，明确写出「是否放行进入 2.1」；验证：结论明确，失败项有处置意见

## 4. 网关模块

- [x] 4.1 根 `pom.xml` 的 `dependencyManagement` 引入 `spring-cloud-dependencies`（2025.1.x）与 `spring-cloud-alibaba-dependencies:2025.1.0.0`，Boot 保持 `4.0.x`；验证：`mvn -s /home/lam/repo/settings.xml validate` 通过，既有模块依赖无版本冲突
- [x] 4.2 新建 `iboot-gateway` 模块（WebFlux，端口 `8088`），先只提供 `local` profile 的静态路由：`/api/ai/**` → `http://localhost:8081`、`/api/**` → `http://localhost:8080`；同时实测控制器上 `version = "1"` 的版本解析来源（默认策略 / header / query），并在网关如实透传；验证：两条路径经 `8088` 访问的响应体与直连后端完全一致，带版本信息的请求经网关后仍命中同一接口
- [x] 4.3 在网关统一配置 CORS（预检直接由网关应答、不转发）；验证：`curl -X OPTIONS` 预检请求返回允许头且后端日志无该请求
- [x] 4.4 为推送通道单独配置路由：不缓冲响应、保留查询参数中的凭证、不设短于心跳周期的空闲超时；验证：`curl -N` 长连接能持续收到心跳与事件，且后端能读到凭证参数
- [x] 4.5 配置上游不可用与超时的失败响应：连接拒绝返回服务不可用状态码、超时返回明确超时、响应体可区分于业务错误；验证：停掉 `8080` 后 `/api/**` 返回降级响应，同时 `/api/ai/**` 仍正常返回
- [x] 4.6 为网关补测试：用 `WebTestClient` 覆盖路由匹配优先级、预处理由网关应答、上游超时降级；验证：`mvn -s /home/lam/repo/settings.xml -pl iboot-gateway test` 通过
- [x] 4.7 在 `doc/23-微服务底座操作指南.md` 增网关章节：路由表、端口、profile 切换方式、失败响应约定、回滚方式；验证：文档中的端口与路由与实际配置逐条一致

## 5. 切流量到网关

- [x] 5.1 `iboot-portal/vite.config.ts` 的 `/iboot/api` 与 `/iboot/ai` 两条代理目标改为 `http://localhost:8088`，保留原目标作为注释；验证：`cnpm run dev` 后登录与 AI 对话均正常
- [x] 5.2 `iboot-portal/nginx.conf` 的代理目标改为网关；验证：本地以生产配置启动后仍能访问接口
- [x] 5.3 以网关入口重跑冒烟：`scripts/regress/smoke.sh` 指向 `8088` 后六条链路全通过；验证：退出码为 0
- [x] 5.4 回滚演练：把两条代理目标改回 `8080`/`8081`，确认六条链路仍全通过，并把演练步骤写入 `doc/23`；验证：回滚后冒烟全绿，文档可直接照做

## 6. 注册中心接入

- [x] 6.1 `iboot-admin` 引入 Nacos 注册发现与配置依赖，配置一律通过 `spring.config.import`（带 `optional:` 前缀），不引入 `bootstrap.yml`；验证：启动后控制台能看到 `iboot-admin` 实例
- [x] 6.2 `iboot-ai` 同样接入；验证：控制台能看到 `iboot-ai` 实例，且服务名与 `spring.application.name` 一致
- [x] 6.3 网关在 `nacos` profile 下改为按服务名负载转发；验证：经 `8088` 访问管理端与 AI 接口均正常
- [x] 6.4 验证降级：停掉 Nacos 后 admin 与 ai 仍能启动，网关切回 `local` profile 后接口仍可用；验证：两次验证均成功，且日志中有注册失败的可见记录
- [x] 6.5 验证配置热更新：经 Nacos 修改一个可调参数，确认生效且进程启动时间未变；验证：行为按新值变化，进程未重启
- [x] 6.6 在 `doc/23` 增注册与配置章节：服务名清单、`spring.config.import` 写法、`local` / `nacos` 两个 profile 的切换与适用场景；验证：按文档操作可从 `local` 切到 `nacos` 再切回

## 7. 链路追踪接入业务服务

- [x] 7.1 `iboot-admin` 接入 Micrometer Tracing + OTLP，配置采样比例与总开关；验证：Tempo 中能查到 admin 的链路
- [x] 7.2 `iboot-ai` 接入，且其调用 admin 的请求沿用同一追踪标识；验证：一次 AI 工具调用在 Tempo 中表现为 admin 段挂在 ai 段之下
- [x] 7.3 让日志输出当前追踪标识；验证：同一请求在 admin 与 ai 的日志中标识相同，定时任务线程的日志不含标识
- [x] 7.4 验证追踪后端不可用时的行为：停掉 Tempo 后业务请求仍正常返回，导出失败在日志中可见；验证：业务响应不变，日志出现导出失败记录
- [x] 7.5 在 `doc/23` 增追踪章节：采样配置、关闭方式、在 Grafana 中查链路的步骤；验证：按文档步骤能查出一次完整链路

## 8. 集成验收

- [x] 8.1 全量单元与集成测试：`TESTCONTAINERS_RYUK_CONTAINER_IMAGE=testcontainers/ryuk:0.12.0 mvn -s /home/lam/repo/settings.xml verify`，admin 与 ai 既有 119 个测试文件零改动全绿，网关测试通过
- [x] 8.2 契约零漂移：重新导出并比对 `scripts/regress/golden/`，差异只允许出现「新增网关本身」这一项且已登记；验证：`bash scripts/regress/regress.sh` 退出码为 0
- [x] 8.3 故障演练：停掉 admin 后经网关访问 admin 接口返回明确降级、AI 接口不受影响；停掉 ai 后反之；验证：两种情形均符合预期，无需重启网关
- [x] 8.4 追踪贯穿验收：一次真实操作在 Tempo 中形成包含三个服务的完整链路；验证：经网关调用 `/api/ai/knowledge-documents/{id}/download`（AI 再调用管理端 `/api/auth/scope`）得到 `gateway → ai → admin` 同 TraceID、父子关系正确，用 `scripts/regress/trace-chain.sh <traceId> --expect iboot-gateway,iboot-ai,iboot-admin` 可复现。**结论订正**：原先判定的"未达成"是查询时间窗边界（请求时间戳落在 `end` 之外）导致的误判，网关一直在为转发请求创建并导出服务端 span；原设想的 `gateway → admin → ai` 方向也不成立——代码中不存在 admin 调用 ai 的路径。详见 `add-gateway-tracing`
- [ ] 8.5 把快检查接入持续集成：新增 `.github/workflows/regress.yml`，在 PR 上执行契约快照与路由覆盖（不依赖全栈），冒烟与权限矩阵按夜间调度在全栈上执行；验证：工作流在测试 PR 上成功运行，且人为制造一条路径漂移时该工作流失败
- [x] 8.6 同步文档：更新 `ROADMAP.md` 阶段 2 的实施状态、`doc/README.md` 索引加入 doc 23；验证：文档链接可达，状态描述与本变更有实际交付一致
