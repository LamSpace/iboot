# Design

## Context

参见 `proposal.md` 的 Why。当前构建事实（2026-10-19 核查）：

- 仓库根目录无 `pom.xml`；`iboot-admin/pom.xml` 直接继承 `spring-boot-starter-parent:4.0.3`。
- 单模块 POM 内含 25 个属性（Java 25、依赖版本、插件版本）、30+ 依赖、4 个构建插件和 1 个
  `javadoc` profile。
- 无测试源码（`src/test` 下 0 个 Java 文件），基线 `mvn verify` 退出码 0。
- 构建入口分散在 `scripts/build.sh`（`cd iboot-admin && mvn clean package -DskipTests`）、
  `README.md`、`doc/05-快速开始.md`、`doc/08-部署指南.md`，均以 `iboot-admin` 为工作目录。

## Goals / Non-Goals

**Goals:**

- 建立可聚合、可被未来模块继承的 `iboot-parent`，且 `iboot-admin` 的构建产物、运行时行为和
  既有构建命令完全不变。
- 版本与插件配置只有一个事实来源，后续新增 `iboot-ai` 或拆分服务时只需声明依赖。
- 用可重复的命令验证结构改造没有引入依赖漂移。

**Non-Goals:**

- 不创建 `iboot-ai` 模块（仅注释预留），不引入任何新依赖。
- 不调整依赖版本、不升级 Spring Boot、不修改 `spring-boot-starter-parent` 的继承关系。
- 不重构 Java 包结构、不迁移资源文件、不改 Docker/前端/Flyway。
- 不新增 CI、不新增 Dockerfile、不改文档中的现有构建命令。
- 不把 admin 的依赖清单提升为父 POM 的 `<dependencies>`（只做 `dependencyManagement`）。

## Decisions

### D1：父 POM 继续继承 `spring-boot-starter-parent`

`iboot-parent` 的 `<parent>` 指向 `spring-boot-starter-parent:4.0.3`，`<relativePath/>` 置空。

- 理由：完整保留 Boot 父 POM 提供的依赖版本托管、插件默认配置、资源过滤与编码设置，改造面
  最小；未来模块只需继承 `iboot-parent` 即可同时获得 Boot 托管。
- 备选：`iboot-parent` 不继承 Boot，改为 `dependencyManagement` 导入
  `spring-boot-dependencies` BOM。该方案需要重新声明 compiler/repackage 等插件管理，
  容易造成行为漂移，且对只有两个模块的现状没有收益。
- 备选：保持 admin 继承 Boot，父 POM 仅做聚合（`modules`）。这样版本仍分散在 admin，
  无法达到“集中版本”的目标。

### D2：聚合与父 POM 合一，坐标 `com.iboot:iboot-parent:1.0.0`

根 `pom.xml` 同时承担 `modules` 聚合与父 POM 职责，`packaging=pom`。

- 理由：当前只有 `iboot-admin` 一个子模块，拆成“聚合 POM + 构建父 POM”两个文件是纯样板；
  Maven 允许一个 POM 兼任两者。
- 备选：`iboot-build`（父）+ 根聚合 POM。等模块数量增长、或父 POM 需要独立发布时再拆，
  现在做属于 YAGNI。
- `iboot-admin` 的坐标 `com.iboot:iboot-admin:1.0.0` 保持不变，确保产物名
  `target/iboot-admin-1.0.0.jar`（Maven 默认 `finalName`，改造前实测）不变。

### D3：父 POM 只做 `dependencyManagement`，不声明公共 `<dependencies>`

- 理由：阶段 2 拆出的服务不会都需要 Web/MyBatis/ES/Quartz/MinIO 全量依赖。把依赖写进父
  `<dependencies>` 会让每个新模块被动继承整条 admin 技术栈，增加裁剪成本，与“拆分”方向相反。
- 具体做法：第三方依赖在父 POM `dependencyManagement` 中声明版本；Spring Boot 生态依赖
  不声明版本（由 Boot 父 POM 托管）。子模块保留自己的 `<dependencies>`、`<scope>` 与
  `<exclusions>`。
- 备选：用 `import` 方式引入自建 BOM。当前规模下属于过度设计。

### D4：属性与插件配置上提，执行绑定留在使用方

- 属性：Java 25、全部第三方依赖版本、插件版本迁到父 POM。Boot 已托管且值相同的
  `flyway.version` 与未被任何占位符引用的 `spring-boot.version` 不再保留，避免出现双份版本源。
- `pluginManagement`：编译器（`release=25` + MapStruct 注解处理器）、
  `spring-boot-maven-plugin`（`--enable-native-access=ALL-UNNAMED`）、CycloneDX、Javadoc。
- 子模块只保留 `<plugin>` 声明与执行绑定（如 CycloneDX `package` 阶段、Javadoc profile），
  不再写版本与共享 `<configuration>`。
- 理由：未来模块按需“声明即继承”，不会因为继承父 POM 而被迫执行 CycloneDX 或 Javadoc。
- 备选：把整个 `<build><plugins>` 原样上提。会让所有未来模块无条件生成 SBOM，属行为扩大。

### D5：`iboot-ai` 只留注释位

在 `modules` 中写注释说明“阶段 1 在此加入 `iboot-ai`”，不创建目录和 POM。

- 理由：空模块无法产出有效构件，且会在 `mvn verify` 中引入无意义模块；ROADMAP 的约束是
  “预留 `iboot-ai`”，不是“提前搭空壳”。

### D6：迁移分两步，先加不删

1. 新增根 POM（继承 Boot、聚合 admin），此时 admin 仍继承 Boot，构建应保持绿。
2. admin 改继承 `iboot-parent`，逐步删除已上提的版本与配置。

- 理由：每步都能独立验证，出问题时能定位是“聚合引入”还是“继承切换/配置上提”导致。

## Risks / Trade-offs

- [版本上提遗漏导致依赖解析失败] → 以 `mvn verify` 和 `mvn dependency:tree` 改造前后
  对比为准；依赖树必须完全一致（除版本声明位置外无差异）。
- [插件配置继承改变构建行为，例如 MapStruct 处理器或 jar 命名] → 对比
  `mvn help:effective-pom` 的 `build/plugins` 与最终 `target/iboot-admin.jar` 名称；
  必要时做一次 `java -jar` 启动冒烟。
- [从根目录执行 `mvn install` 会额外安装父 POM] → 属预期行为，父 POM 版本与 admin 一致；
  本地仓库多一个 `com.iboot:iboot-parent` 不影响部署。
- [IDE 仍缓存单模块项目模型] → 迁移步骤中要求重新导入/刷新 Maven 项目；CI 与命令行不受影响。
- [既有文档命令以 `iboot-admin` 为工作目录] → 由于 `relativePath` 指向 `../pom.xml`，这些
  命令继续有效；本变更不改文档，根目录入口作为新增能力记录在 tasks 的验证项中。

## Migration Plan

1. 记录改造前基线：`mvn -s /home/lam/repo/settings.xml verify`、
   `mvn -s /home/lam/repo/settings.xml dependency:tree` 输出。
2. 新增根 `pom.xml`（D1/D2/D5），仅聚合，不改 admin。
3. 把属性、`dependencyManagement`、`pluginManagement` 迁入父 POM；admin 改为继承
   `iboot-parent` 并删除重复版本与共享配置（D3/D4）。
4. 从根目录与 `iboot-admin` 目录分别执行 `mvn verify`；对比依赖树；确认
   `iboot-admin/target/iboot-admin-1.0.0.jar` 存在且名称不变。
5. 回滚策略：本次只改两个 POM，`git revert` 单个提交即可恢复；无数据库、配置或运行时状态
   需要回滚。

## Open Questions

无。文档是否补充“从根目录构建”的说明留待后续文档类变更，不影响本次结构改造。
