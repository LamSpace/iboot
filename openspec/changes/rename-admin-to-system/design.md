# Design

## Context

动机见 `proposal.md` — Why。这里只记录决定实现方式的事实约束。

`iboot-admin` 当前是阶段 2 最后一次拆分前剩下的全部内容：

- 181 个 Java 文件、17 个测试类、10 个 Mapper XML，包根 `com.iboot.admin`。
- 11 个控制器、57 条 `/api/**` 路径，全部属于系统域或其聚合视图。
- 数据源 schema `iboot_admin`，Flyway 已推进到 `V28`，其中 12 张系统域表仍在服务，25 张前五次拆分保留的遗留表（`sys_file`、`sys_job*`、`sys_message*`、`sys_notice*`、`sys_notification_delivery`、`sys_login_log`、`sys_operate_log`、`biz_leave_application`、`db_backup_record`、`db_restore_record`、`QRTZ_*`）已无写入方。
- 依赖方向：

```
              iboot-auth   ──直连 schema 读写──┐
              iboot-ai     ──服务名 HTTP─────┤
              iboot-file   ──跨 schema 只读──┤
              iboot-job    ──跨 schema 只读──┼──> iboot_admin (8080)
              iboot-notify ──跨 schema 只读──┤
              iboot-workflow ──跨 schema 只读┤
              iboot-monitor  ──跨 schema 只读┘
```

这个依赖形状决定了本次改动最重要的性质：**服务是根，但数据不搬家**。

## Goals / Non-Goals

**Goals**

- 让服务的名称、包名与注册中心身份与其实际承载的领域一致。
- 改动只触及「身份」，不触及「行为」：路径、方法、响应、权限语义、数据归属全部不变。
- 把六个服务的依赖关系从隐式耦合转成可检查的契约。
- 保持回滚成本为一次 `git revert`。

**Non-Goals**

- schema 改名（`iboot_admin` → `iboot_system`）。
- 遗留表清理。
- 跨 schema 只读收敛为内部 API。
- 任何业务逻辑、接口签名或前端改动。

## Decisions

### D1 · 原地改名，不做「新建 + 摘除」

`git mv iboot-admin iboot-system`，包名 `com.iboot.admin` → `com.iboot.system`，全仓同步。

**为什么不走 `add-system-service` + `remove-admin` 两步**：绞杀者模式的前提是「切流量时源服务仍可回退」。这里路径与服务是一对一，没有可切的流量；两步的中间态是两个模块跑同一批路径，既不是有效回退点，也让路径守恒检查无法区分「搬家」与「改名」。工作量还会翻倍。

**代价**：约 181 个文件的机械 diff，评审噪声大。缓解办法是把改名放在独立提交里（仅包名与坐标），后续提交再做配置与文档，评审时可分别查看。

### D2 · 端口保持 8080

`server.port` 不变。

**理由**：

- 依赖方有两条按端口寻址的默认值（`iboot-file`、`iboot-ai` 的 `http://localhost:8080/api/auth/scope`），改端口等于无谓地把它们一起动。
- 回归装置的直连基址（`scripts/regress/lib.sh` 的 `REG_BASE`）指向 8080，冒烟、权限矩阵、等价性基线都依赖它。
- 运维手册、Grafana 里已有的直连链接不需要修改。

**备选**：改用 8090 让端口与服务一一对应。否决：收益只是编号整齐，代价是三处配置与全部直连脚本。

### D3 · schema 保持 `iboot_admin`，本次不改

**理由**（这是本设计里最重要的取舍）：

1. 依赖方有六个服务直连该 schema，任何一处漏改都是**运行期静默错误**，而不是编译错误。
2. MySQL 没有 `RENAME DATABASE`，只能建库 → 逐表复制 → 校验 → 删旧库。
3. `flyway_schema_history` 必须随数据一起搬。漏掉它，改名后的服务会在空 schema 上重跑 `V1__initial_schema.sql`——该脚本第一条就是 `DROP TABLE IF EXISTS sys_user`——启动失败且可能毁数据。
4. schema 里还躺着 25 张遗留表，清理它们与改名的窗口无法共用（一个是破坏性删除，一个是等价迁移）。

因此本次把 schema 名定义为**历史名称**，与模块名解耦，并在 `doc/07` 与 `doc/23` 中写明。schema 改名与遗留表清理另起变更，届时没有业务代码改动与之交织，回滚面也更小。

**代价**：服务叫 `iboot-system`、库叫 `iboot_admin`，需要文档解释。这是可以接受的命名债——它比一次高风险的停机迁移便宜。

### D4 · 跨 schema 只读：接受为长期模式，补契约而不是补接口

**不收敛为内部 API 的理由**：

- 四个服务的 `DataScopeSqlBuilder` 把 `EXISTS (SELECT 1 FROM iboot_admin.sys_user ...)` 直接拼进 SQL 片段。改成远程调用不是替换一个客户端，而是重写数据权限的 SQL 合成方式。
- 改成远程调用会让**可用性倒退**：现在系统服务挂掉，监控的日志列表仍然可用；改完之后，系统服务一挂，五个服务的用户/配置读取全部失败。
- 「每服务独立 schema」约束的是**写入所有权**，不是禁止读取。系统域的写方已经唯一。

**代价**：schema 事实上成了没有契约的公共 API。缓解：把消费面写成显式清单，并用单测锁定列名（`DataScopeSqlBuilderTest` 已是先例）。契约清单落在 `doc/23`，逐服务列出表名、列名、用途与失败语义。

同样的判断适用于 dashboard / statistics 对 `iboot_monitor` 日志表的只读：原退出条件是「等 system 拆分后改调监控服务」，但实测停 monitor 时仪表盘仍返回 200，改成远程调用会让仪表盘一起挂。这条债务以「改判」结束，而不是以「实施」结束。

### D5 · 包名一并改，不留 `com.iboot.admin`

**备选**：模块改名但保留包名，diff 更小。否决：模块名、artifactId、`spring.application.name`、包名四者不一致是最坏的一种状态——搜索 `com.iboot.admin` 会指向一个不存在的模块，新人无从判断哪个是事实。改名一次做干净，后续不再碰。

**例外**：`jwt.secret` 的默认值 `iboot-admin-secret-key-...` 是**密钥字面量**，不是服务名，保持原样。改动它会让所有服务与既有令牌失配。备份配置里的库名 `iboot-admin` 指 schema，同样保持。

### D6 · 网关路由换名，优先级不变

路由 `id: admin` → `id: system`，`uri` 从 `IBOOT_ADMIN_URI` 改为 `IBOOT_SYSTEM_URI`，Nacos 配置下从 `lb://iboot-admin` 改为 `lb://iboot-system`。`order: 100` 与 `Path=/api/**` 保持不变——它是兜底路由，优先级语义与名字无关。

不保留 `IBOOT_ADMIN_URI` 作为兼容别名：项目尚未对外发布，保留别名只会让两套配置并存。

### D7 · 回归装置按「改名」而非「搬家」处理

- `golden/admin.json` → `golden/system.json`，内容（57 条路径）逐条不变。
- `scripts/regress/routes.txt` 中 `/api` 的目标服务改为 `iboot-system`。
- `equivalence.py` 的采集函数与基线文件名同步改名。
- **`migrations.txt` 不新增行**：该文件登记的是「接口从一个服务搬到另一个服务」，本次没有搬。新增登记行反而会让 `path-conservation.sh` 去找一个已经不存在的 `admin` 服务。

风险在于 `path-conservation.sh` 与 `route-coverage.sh` 都按快照文件名识别服务；改名的顺序必须是「先改快照名与路由表，再删旧快照」，且要在跑回归前确认目录里没有残留的 `admin.json`。

### D8 · 顺带修正 monitor 的配置键前缀

`iboot-monitor` 的 `PermissionScopeClient` 绑定 `${iboot.job.permission.scope-url}` / `${iboot.job.permission.cache-seconds}`，而它的 `application.yml` 写的是 `iboot.monitor.permission.*`。结果是该服务的 scope 地址与缓存时间**从未被配置或环境变量覆盖过**，一直使用代码默认值。改名正好要动这两行，一并改为正确前缀。

这不是顺手重构：不改的话，改名后 `IBOOT_PERMISSION_SCOPE_URL` 仍然无效，配置里写着新地址而程序跑旧地址。

## Risks / Trade-offs

- **漏改的硬编码服务名** → 编译期不报错、启动期不报错，只有跨服务调用时表现为服务发现失败。缓解：改完后全仓 `grep -rn "iboot-admin\|iboot_admin"` 逐条判定，作为独立检查项；并实际跑一次「系统服务停止」的故障演练。
- **包改名后 MyBatis 与组件扫描路径失配** → 启动失败或 Mapper 未注册。缓解：`mybatis.type-aliases-package`、`mapper-locations`、`@MapperScan`、`scanBasePackages`、日志级别包名逐一核对；启动冒烟覆盖。
- **Nacos 残留注册** → 改名前注册的旧实例在短时间内仍可被解析，故障演练会出现「停服务后仍 200」的假象（monitor 拆分时已踩过）。缓解：演练前重启注册中心或等待实例过期，确认注册列表里只剩新服务名。
- **`flyway_schema_history` 陷阱** → 本次不动 schema，因此不触发；但它会在后续的 schema 改名变更中成为首要风险，已写入 D3 备查。
- **评审噪声** → 181 个文件的机械 diff 会掩盖真正的配置改动。缓解：按提交切分（改名 / 配置 / 回归装置 / 文档）。
- **术语漂移** → `openspec/specs/knowledge-graph` 中以「admin」指称系统服务的措辞会过时。缓解：已在 proposal 中登记，留待该能力下次变更时同步，不在本次引入一个只为改词的需求增量。

## Migration Plan

无数据迁移，无停机窗口要求；部署顺序仍然是从依赖方到被依赖方之外，因为系统服务是被依赖的一侧，可以先起它再起其余服务。

```
1. 构建 iboot-system           → mvn verify 全绿
2. 启动 system                 → /actuator/health UP，注册中心可见 iboot-system
3. 启动其余服务                → 每个服务启动日志无绑定的 scope 地址错误
4. 跑全量回归                  → 契约、路径守恒、权限矩阵、等价性、冒烟全绿
5. 故障演练                    → 停 system，确认失败有界、已登录会话不被踢出
```

**回滚**：`git revert` 后重启全部服务即可。没有数据变更，没有 schema 变更，回滚不存在补偿动作——这是 D3 选择不动 schema 的直接收益。

## Open Questions

无。三项待定事项（D1 形态、D2 schema、D3 跨库读）已在提案阶段由需求方确认。
