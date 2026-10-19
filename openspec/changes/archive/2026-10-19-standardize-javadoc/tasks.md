# Tasks

## 1. Foundation

- [x] 1.1 在 `iboot-admin/pom.xml` 增加 `maven-javadoc-plugin` 3.12.0（`release=25`、UTF-8、`doclint=all,-missing`、`show=public`）与按需 `javadoc` profile；验证：`mvn -s /home/lam/repo/settings.xml -q -DskipTests compile` 通过，`mvn -s /home/lam/repo/settings.xml javadoc:javadoc` 能生成 `target/reports/apidocs/index.html`
- [x] 1.2 跑一次全量基线 `javadoc:javadoc`，记录存量断链/HTML/引用错误并修复配置或注释；验证：基线条目清零或形成明确的逐批修复清单，命令无 error
- [x] 1.3 编写 `doc/19-JavaDoc 规范.md`，包含类/方法/字段模板、中文说明 + 英文标签规则、深度预算、生成成员排除规则、正反例；验证：文档中的模板可直接套用，且覆盖 `CLAUDE.md` 将要引用的所有规则
- [x] 1.4 在 `CLAUDE.md` 增加第 5 节「JavaDoc 注释规范」，限定适用于新增/修改代码，并链接 `doc/19-JavaDoc 规范.md`；验证：章节存在、不超过约 40 行、与第 3 节「Surgical Changes」不冲突
- [x] 1.5 完成黄金样例：`UserController.java` 与 `UserApplicationService.java` 的类级、方法级、字段级注释；验证：编译与 javadoc 通过，经你评审确认文风、术语和深度后才开始批量改写

## 2. Common Layer (24 files)

- [x] 2.1 补齐 `common/enums`（5）与 `common/annotation`（5）的类级、枚举常量、注解成员注释；验证：编译与 javadoc 通过，`git diff` 仅注释行
- [x] 2.2 补齐 `common/util`（3）、`common/cloudevent`（3）、`common/result`（2）、`common/i18n`（2）、`common/exception`（2）、`common/constant`（2）的类级、手写方法、字段注释；验证：编译与 javadoc 通过，`git diff` 仅注释行

## 3. Domain Layer (37 files)

- [x] 3.1 补齐 `domain/system/model`（16）的类级与字段注释，重点说明聚合根、实体关系、状态字段取值；验证：编译与 javadoc 通过，生成成员未改动
- [x] 3.2 补齐 `domain/job/model`（2）与 `domain/system/service`（1）的类级、字段、方法注释；验证：编译与 javadoc 通过，`git diff` 仅注释行
- [x] 3.3 补齐 `domain/system/repository`（16）与 `domain/job/repository`（2）的接口级与方法级注释，说明查询语义、空值/分页约定；验证：编译与 javadoc 通过，`git diff` 仅注释行
- [x] 3.4 统一领域层术语（用户、角色、岗位、部门、字典等）并抽查 5 个模型；验证：同一概念在不同文件中用词一致，评审通过

## 4. Infrastructure Layer (105 files)

- [x] 4.1 补齐 `infrastructure/persistence/po`（18）与 `persistence/mapper`（18）的类级、字段、方法注释，说明表映射与 SQL 语义；验证：编译与 javadoc 通过，生成成员未改动
- [x] 4.2 补齐 `infrastructure/repository`（18）的类级与方法注释，说明与领域仓储接口的对应关系、事务和异常转换；验证：编译与 javadoc 通过，`git diff` 仅注释行
- [x] 4.3 补齐 `infrastructure/config`（18）与 `config/cache`（3）的类级、`@Bean` 方法注释，说明配置项、默认值和生效条件；验证：编译与 javadoc 通过，`git diff` 仅注释行
- [x] 4.4 补齐 `infrastructure/security`（5）与 `push`（9）的类级、方法注释，说明认证/授权流程、SSE/Redis 推送契约；验证：编译与 javadoc 通过，`git diff` 仅注释行
- [x] 4.5 补齐 `infrastructure/job`（7）、`job/task`（1）、`storage`（4）、`interceptor`（2）、`filter`（1）、`aspect`（1）的类级与方法注释；验证：编译与 javadoc 通过，`git diff` 仅注释行

## 5. Application Layer (35 files)

- [x] 5.1 补齐 `application/service` 中的 `Auth`、`User`、`Role`、`Menu`、`Dept`、`Post`、`Config`、`Dict`、`File`、`Notice`、`Message`、`Job` 等 12 个服务的类级与方法注释；验证：编译与 javadoc 通过，事务/异常/权限语义描述准确
- [x] 5.2 补齐剩余 11 个应用服务（`Dashboard`、`Statistics`、`ServerMonitor`、`RedisMonitor`、`OnlineUser`、`OperateLog`、`LoginLog`、`RunLog`、`MinioAdmin`、`BusinessMetrics`、`MessageTemplate`）的注释；验证：编译与 javadoc 通过，`git diff` 仅注释行
- [x] 5.3 补齐 `application/mapper`（12）的接口级与方法级注释，说明 MapStruct 映射方向与字段转换规则；验证：编译与 javadoc 通过，生成实现未被改动

## 6. Interfaces Layer (86 files)

- [x] 6.1 补齐 `interfaces/controller` 前 11 个控制器（`Auth`、`Config`、`Dashboard`、`Dept`、`Dict`、`File`、`I18n`、`Job`、`Log`、`Menu`、`Message`）的类级与方法级注释，重点补全约 146 个缺失方法注释；验证：编译与 javadoc 通过，`@Operation` 与 Javadoc 语义一致
- [x] 6.2 补齐剩余 10 个控制器（`MessageTemplate`、`MinioMonitor`、`Notice`、`OnlineUser`、`Post`、`RedisMonitor`、`Role`、`ServerMonitor`、`Statistics`、`User`）的类级与方法级注释；验证：编译与 javadoc 通过，`git diff` 仅注释行
- [x] 6.3 补齐 `interfaces/dto/request`（19）的类级与字段注释，保留 `@Schema(description=...)` 并说明校验约束；验证：编译与 javadoc 通过，字段注释与校验注解一致
- [x] 6.4 补齐 `interfaces/dto/response` 前 14 个类的类级与字段注释，重点处理 `ServerInfoResponse`（114 字段）、`RedisMonitorResponse`（98 字段）、`StatisticsResponse`（84 字段）；验证：编译与 javadoc 通过，生成成员未改动
- [x] 6.5 补齐 `interfaces/dto/response` 后 14 个类与 `interfaces/dto/export`（16）的类级与字段注释，保留 `@Schema`；验证：编译与 javadoc 通过，`git diff` 仅注释行
- [x] 6.6 补齐 `IBootAdminApplication`（1）、`interfaces/advisor`（1）、`interfaces/config`（1）的类级与方法注释；验证：编译与 javadoc 通过，`git diff` 仅注释行

## 7. Integration Verification

- [x] 7.1 全量编译：`mvn -s /home/lam/repo/settings.xml -q -DskipTests compile` 通过，确认没有代码行为改动
- [x] 7.2 全量 javadoc：`mvn -s /home/lam/repo/settings.xml javadoc:javadoc` 无 error，生成页面中文显示正常
- [x] 7.3 变更范围核验：逐批检查 `git diff`，确认所有改动均为注释行，业务代码、注解、依赖零变化
- [x] 7.4 生成成员核验：确认 5,367 个 `@SuppressWarnings("all")` 生成成员未被添加 Javadoc 或改写（HEAD 与工作区数量一致）
- [x] 7.5 准确性抽样：从控制器、应用服务、DTO、领域模型中抽取 20 个类，核对注释与代码行为一致
- [x] 7.6 规范回写：若执行中发现术语或模板需要调整，同步更新 `doc/19-JavaDoc 规范.md` 与 `CLAUDE.md`；验证：两处规则一致
- [x] 7.7 OpenSpec 校验：`openspec validate standardize-javadoc --strict` 通过，变更状态与产物一致

## 8. Package Documentation (37 packages)

- [x] 8.1 为 `iboot-admin` 的 37 个 Java 包新增 `package-info.java`，包含包职责、边界和主要类型说明；验证：每个有源码的包都有 `package-info.java`
- [x] 8.2 验证包级文档：`mvn -s /home/lam/repo/settings.xml -f iboot-admin/pom.xml javadoc:javadoc` 无 error，包摘要和中文显示正常；验证：`target/reports/apidocs/index.html` 中出现包列表
