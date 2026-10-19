# Proposal

## Why

ROADMAP 阶段 0.1 要求把工程预备成多模块结构。当前仓库没有根 `pom.xml`，`iboot-admin` 直接继承
`spring-boot-starter-parent`，全部第三方依赖与插件版本集中在 522 行的单模块 POM 里。阶段 1 的
`iboot-ai` 必须是独立 Maven 模块，阶段 2 还要把单体拆成多个服务；如果到那时才做构建结构改造，
等于在每个新模块上重复一遍版本声明，并在拆分成本最高的时候重写工程结构。现在做是纯构建改造，
可以做到零业务代码改动、零运行时行为变化。

## What Changes

- 新增根 `pom.xml`：`com.iboot:iboot-parent:1.0.0`，`packaging=pom`，parent 仍为
  `spring-boot-starter-parent:4.0.3`，`modules` 仅包含 `iboot-admin`。
- 在父 POM 中预留 `iboot-ai` 模块位置（仅注释，**不创建空模块**）。
- 把 Java 版本、第三方依赖版本、插件版本等共享属性上提到父 POM。
- 在父 POM 的 `dependencyManagement` 中集中管理自有版本依赖；Spring Boot 生态版本继续由
  `spring-boot-starter-parent` 托管。
- 在父 POM 的 `pluginManagement` 中集中管理编译器（Java 25 + MapStruct 注解处理器）、
  `spring-boot-maven-plugin`、CycloneDX、Javadoc 插件配置。
- `iboot-admin/pom.xml` 改为继承 `iboot-parent`（`relativePath` 指向 `../pom.xml`），
  删除已由父 POM 管理的版本号与插件配置，保留自身依赖清单、排除项与 javadoc profile。
- 不改任何 Java 代码、资源文件、Flyway 脚本、Docker 配置或前端；不升级任何依赖版本。

## Capabilities

### New Capabilities

无。本变更是纯构建结构重构，不改变任何可观察行为，按 `skip_specs: true` 处理。

### Modified Capabilities

无。现有 `portal-design-system` 能力不受影响。

## Impact

- 构建文件：新增仓库根 `pom.xml`；修改 `iboot-admin/pom.xml`。
- 构建入口：根目录 `mvn verify` 可用；原有 `cd iboot-admin && mvn ...` 命令保持有效。
- 产物：`iboot-admin/target/iboot-admin.jar` 名称与内容不变。
- 运行时、API、依赖版本、中间件环境与前端联调均无变化。
- 基线：变更前已在 `iboot-admin` 下执行 `mvn -s /home/lam/repo/settings.xml verify`，退出码 0。
