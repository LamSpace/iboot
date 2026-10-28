# Tasks

## 1. Spike 与依赖接入

- [x] 1.1 在根 `pom.xml` 引入 `io.agentscope:agentscope-bom:2.0.1` 依赖管理，`iboot-ai/pom.xml` 引入 `agentscope-openai-spring-boot-starter`，验证：`mvn -s /home/lam/repo/settings.xml -pl iboot-ai dependency:tree` 能解析且无版本冲突告警
- [x] 1.2 完成 Spike S1/S2：JDK 25 + Boot 4.0.3 上下文启动，Jackson 2.21 与 Jackson 3 共存且现有 ES/序列化测试全绿，验证：`mvn -s /home/lam/repo/settings.xml -pl iboot-ai test` 通过
- [x] 1.3 完成 Spike S3–S8：工具调用往返、`ASK` 能中止而非挂起、`RuntimeContext` 传递 token、模型 usage 可取、流式接 `SseEmitter`、关闭 thinking 后工具循环正常，验证：Spike 临时工程逐项记录证据
- [x] 1.4 产出 `doc/spike/ai-1.3-spike-report.md` 并据此确认运行时选择（AgentScope 通过则采用，S1/S2 失败则切 Spring AI 自管循环），验证：报告含每项判据、结论与复现要点

## 2. 数据与权限

- [x] 2.1 新增 `iboot-admin` Flyway `V26`：`ai:agent:use` / `ai:agent:manage` 菜单与权限，并给内置角色授权，验证：启动后菜单与权限查询结果符合预期
- [x] 2.2 为 `StatisticsController` 的两个接口补 `@PreAuthorize`（复用既有 `report:view` / `usage:view`，无需新增权限），验证：`StatisticsControllerTest` 覆盖有权限 200 / 无权限 403
- [x] 2.3 新增 `iboot-ai` Flyway `V2__create_agent.sql`：`ai_agent`、`ai_agent_run`，验证：新增迁移测试在同一 schema 上重复执行幂等
- [x] 2.4 更新 `doc/07-数据库设计.md` 的两张新表说明，验证：字段与迁移脚本逐列一致

## 3. 工具层

- [x] 3.1 定义 `AgentTool`、`AgentToolRegistry` 与只读/可写分级，验证：单元测试覆盖"未登记工具不执行""只读工具不得走写接口"
- [x] 3.2 实现 `AgentContext` 与 `AdminApiClient`，身份只取自登录态；模型传入的 `userId` / `deptId` / `token` 参数被丢弃，验证：单元测试断言伪造身份参数不影响下游请求头与参数
- [x] 3.3 实现三个只读工具（查用户、查登录日志、查备份状态）与统一结果裁剪（条数、字符数、敏感字段），验证：单元测试覆盖裁剪上限与密码/令牌字段剔除
- [x] 3.4 实现写工具"触发备份"，执行前必须命中授权记录，验证：单元测试断言无授权时下游零请求
- [x] 3.5 用假 admin 做工具契约测试，断言路径、查询参数与响应形态；更新 `doc/06-API 集成.md` 的工具对照表，验证：契约测试通过且文档路径与实现一致

## 4. 编排运行时

- [x] 4.1 实现 `AgentRuntime` 适配层（模型配置复用既有 `DASHSCOPE_BASE_URL` / 模型标识 / Key；工具只用 `AgentTool` 注册，禁用 `@Tool` 与结构化输出重载），验证：单测用假模型完成一次工具往返
- [x] 4.2 实现 `AgentRunService` 状态机（RUNNING / PENDING_APPROVAL / EXECUTING / COMPLETED / REJECTED / EXPIRED / FAILED）与运行持久化，验证：状态迁移单测覆盖全部合法与非法转换
- [x] 4.3 把配额与指标接到模型调用边界（从 `MODEL_CALL_END` 事件累加用量），验证：单测断言一次 N 步运行计入 N 次调用与 N 次用量
- [x] 4.4 实现两段式写确认与 `/runs/{runId}/confirm`：一次性授权、绑定运行/用户/工具/参数摘要、过期与重放拒绝，验证：单测覆盖正常确认、拒绝、过期、重放、他人确认、参数篡改六种情形
- [x] 4.5 实现降级与错误映射（模型不可用、下游不可用、配额不足、步数超限），验证：单测断言错误码可区分且不出现"正常完成"
- [x] 4.6 运行审计只落摘要、不落敏感原值，验证：单测断言轨迹与模型上下文中均不含密码/令牌/密钥原值

## 5. 接口与文档

- [x] 5.1 新增 `/api/ai/agent/chat/stream`，实现 `meta → step → tool_result → (pending_approval) → token* → done | error` 事件序列，验证：集成测试按序断言事件名与状态
- [x] 5.2 新增 Agent 配置 CRUD 接口并落实 `ai:agent:manage` 权限，验证：控制器测试覆盖 403 与进行中运行不受配置变更影响
- [x] 5.3 新增 `iboot.ai.agent.enabled` 开关（默认 false），验证：关闭时新端点不可用、既有对话与 RAG 测试全部通过
- [x] 5.4 在 `doc/22-AI 工作台操作指南.md` 增加智能体编排章节（工具清单、权限、事件、确认流程、排障），验证：文档中的 curl 示例可原样执行

## 6. 前端

- [x] 6.1 扩展 `iboot-portal/src/utils/aiStream.ts` 的事件类型并保持既有对话行为不变，验证：`cnpm run build` 通过且原有对话视图无回归
- [x] 6.2 新增智能体对话视图（工具调用展示、确认卡片、拒绝入口），验证：`cnpm run build` 通过并手工跑通一次只读提问与一次写确认
- [x] 6.3 新增 Agent 配置视图（列表、编辑、启停、工具白名单），验证：手工验证无管理权限用户看不到入口
- [x] 6.4 补充中英文语言包与菜单路由，验证：切换语言后新视图文案完整，无硬编码中文

## 7. 回测

- [x] 7.1 新增 `ScriptedOpenAiServer`（按调用序列返回 tool_call 与最终答复）与假 admin 服务，验证：自测用例可在无网络、无云凭据下运行
- [x] 7.2 建立 `test/resources/agent/eval-cases.json` 与确定性回放执行器，产出 `target/agent-eval-report.json` 并提交基线文件，验证：回放模式下工具选择与事件序列断言全部通过
- [x] 7.3 增加 `@Tag("cloud")` 的真实模型评测，统计工具选择准确率、参数准确率与用量，验证：`-Dsurefire.excludedGroups=disabled` 下产出报告且与基线可对比
- [x] 7.4 补齐安全红队用例（写入不确认、注入调未登记工具、跨用户确认、身份参数伪造），设为硬门禁，验证：任一用例失败时构建失败
- [x] 7.5 在 `doc/22` 补充回测运行方式与门禁说明，验证：文档命令在本地可复现报告

## 8. 可观测与配置文档

- [x] 8.1 新增 Agent 指标与告警规则，并在 Grafana `iBoot AI 指标` 面板增加运行数、工具调用、确认分布、绕过拦截图表，验证：本地 Prometheus 抓取到指标且面板有数据
- [x] 8.2 更新 `doc/11-可观测性设计.md` 与 `doc/10-后端配置详解.md`（`iboot.ai.agent.*`），验证：配置项与 `application.yml` 逐项一致

## 9. 集成验收与发布文档

- [x] 9.1 端到端验收：三个只读工具正确回答、写操作必须确认且确认后生效、拒绝后无数据变更，验证：按 `doc/22` 步骤在本地完整走通
- [x] 9.2 更新 `ROADMAP.md`：1.3 状态、Nacos Agent Card 挪至 2.1、版本矩阵补 AgentScope 2.0.1 与 Spike 结论，验证：`openspec validate add-agent-orchestration --strict` 仍通过
- [x] 9.3 更新 `README.md` 能力表、目录树与启动说明，验证：README 中的启动命令可原样执行
- [x] 9.4 归档变更并同步主 spec，验证：`openspec archive add-agent-orchestration` 后 `openspec/specs/ai-agent/spec.md` 存在且 `openspec list --specs` 可查
