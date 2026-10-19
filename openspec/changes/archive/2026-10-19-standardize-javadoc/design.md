# Design

## Context

- `iboot-admin` 是单模块 Maven 工程，没有根 `pom.xml`；构建插件只能放在 `iboot-admin/pom.xml`。
- 构建环境为 JDK 25 + Maven 3.9.16；`maven-javadoc-plugin` 3.12.0 已在本地仓库，也是 Central 最新版。
- 288 个 Java 文件中有 189 个是 `delombok` 提交（`1cf7ef9`）的产物，包含 2,851 个自动生成成员；这些成员不应补注释。
- 现有注释已有一定基础（897 处 `{@code}`、2,139 个 `@param`），但覆盖不均：控制器方法几乎无注释，DTO 字段大多只有 `@Schema`，168 个类注释不足 30 字。
- 注释只影响源码可读性和 javadoc 产物，不改变运行时行为，因此可以用“编译 + javadoc + diff 评审”验证，不依赖单元测试。

## Goals / Non-Goals

**Goals:**

- 建立一套可执行、可评审的 JavaDoc 规范，并让 Claude Code 在后续 Java 改动中持续遵守。
- 让 `iboot-admin` 的每个类型都有准确的类级文档；每个手写 public/protected 方法都有方法级文档；DTO 字段、枚举常量、非显而易见私有字段都有字段级文档。
- 让 `mvn javadoc:javadoc` 成为可重复执行的质量门禁，能发现断链、非法 HTML 和错误引用。
- 保持改动可分批、可回滚、可逐批评审。

**Non-Goals:**

- 不修改任何业务逻辑、方法签名、注解、依赖或数据库结构。
- 不为 delombok 生成成员编写 Javadoc。
- 不引入 Checkstyle/Spotless 等新的代码风格门禁（留待后续）。
- 不把完整规范塞进 `CLAUDE.md`；详细规范放在 `doc/`。

## Decisions

### D1. 中文说明 + 英文标签

说明文字使用中文，`@param` / `@return` / `@throws` / `@author` / `@since` 等标签名保持英文。

- 理由：团队评审与维护以中文为主；Javadoc 标签名是工具识别的固定语法，不能本地化。
- 备选：全英文（与 JDK `Lock` 一致）。放弃原因：评审成本和术语误译风险更高，且用户明确允许中文。
- 备选：中英双语。放弃原因：维护成本翻倍，内容容易不同步。

### D2. 范围：类级全量 + 手写成员 + 字段，排除生成成员

- 288 个类/接口/枚举/注解全部有类级文档。
- 851 个手写 public/protected 方法全部有方法级文档。
- 1,659 个字段/常量全部有字段级文档（DTO 字段同时保留 `@Schema(description=...)`）。
- 2,851 个 delombok 生成成员不写 Javadoc。
- 理由：生成成员的语义由字段和类型文档承载，逐成员注释只增加噪音和维护负担。
- 备选：所有成员都写。放弃原因：注释块从约 2,798 增至约 5,600，收益极低。
- 备选：只写类级。放弃原因：控制器和 DTO 的核心信息在方法和字段上，类级无法覆盖。

### D3. 深度预算：按类型分级，不照搬 `Lock` 的篇幅

| 类型 | 类注释 | 方法注释 |
|------|--------|----------|
| 核心领域/安全/并发/应用服务 | 8～20 行 | 3～10 行 |
| Controller / Mapper / Repository | 4～10 行 | 2～5 行 |
| DTO / PO / Config / Enum | 2～6 行 | 生成成员不写；字段 1～3 行 |

- 理由：`Lock` 的 150 行类文档适合定义公共契约的基础库；业务系统按类分级更可持续。
- 备选：统一长篇。放弃原因：维护成本高，读者会跳过。

### D4. Maven：`maven-javadoc-plugin` + 按需 profile

- 插件版本 3.12.0，`release=25`，UTF-8，`doclint=all,-missing`，`show=public`。
- 默认构建不绑定 `javadoc:jar`；通过 `-Pjavadoc` 按需产出。
- 理由：常规 `mvn package` 不应为文档生成付出时间；`missing` 会把 2,851 个生成成员报成警告，必须排除。
- 备选：把 `javadoc:jar` 绑定到 `package`。放弃原因：拖慢每次构建，且文档问题会阻断业务发布。
- 备选：启用 `doclint=all`（含 `missing`）。放弃原因：与 D2 的排除策略冲突。

### D5. 校验策略：doclint 先行为准，Checkstyle 暂不引入

- 本变更用 `doclint=all,-missing` 作为门禁；覆盖率靠分批任务和评审保证。
- 理由：Checkstyle 的 `MissingJavadocMethod` 难以排除 delombok 生成成员，需要额外 suppressions 配置。
- 备选：立即引入 Checkstyle。放弃原因：增加配置面，超出本次范围。

### D6. 规范落点：`CLAUDE.md` 短章节 + `doc/19-JavaDoc 规范.md` 详解

- `CLAUDE.md` 只放硬规则、模板、正例，控制在 30～40 行。
- 详细规范、反例、按类型示例放在 `doc/19-JavaDoc 规范.md`。
- 在 `CLAUDE.md` 中明确：规范约束“新增或修改的代码”，批量重写存量注释需单独立项。
- 理由：`CLAUDE.md` 每次会话加载，必须短；同时要避免与现有「Surgical Changes」条款冲突。
- 备选：全部写入 `CLAUDE.md`。放弃原因：上下文膨胀，规则容易被忽略。
- 备选：只写 `doc/`。放弃原因：Claude Code 不会自动读取，规则无法持续生效。

### D7. 执行顺序：黄金样例 → 自底向上分层

1. Phase 0：规范、Maven、`CLAUDE.md`、两个黄金样例（一个 Controller、一个 ApplicationService）。
2. Phase 1：`common` → `domain` → `infrastructure` → `application` → `interfaces`。
3. 每批只改注释，跑 `mvn compile` 与 `javadoc:javadoc`，按批提交。

- 理由：先用两个文件锁定文风、术语和深度，避免 288 个文件返工；自底向上先统一基础类型词汇，再写上层。
- 备选：按文件名字母序。放弃原因：跨层跳跃，术语不一致。
- 备选：先写 Controller。放弃原因：Controller 依赖下层 DTO/服务的术语，先写容易反复。

### D8. `package-info.java`：为 37 个有源码的包补充包级文档

- 每个包含至少一个 Java 文件的包新增 `package-info.java`，说明包的职责、边界和主要类型。
- 不为没有直接类型的父包单独创建（例如 `com.iboot.admin.interfaces.dto` 由三个子包的文档覆盖）。
- 理由：包级文档是 Javadoc 导航的第一层，成本低，能帮助新成员理解分层。
- 备选：不做。放弃原因：用户明确要求补充，且与本次规范一致。

### D9. 验证：编译 + javadoc + diff 评审

- `mvn -q -DskipTests compile` 保证没有语法/签名变化。
- `mvn javadoc:javadoc` 保证 doclint 无 error。
- `git diff` 逐批评审，确认只有注释行变化；发现代码行变化立即回退。
- 理由：注释不改变行为，单元测试无法验证“注释是否准确”；人工评审是必要环节。

## Risks / Trade-offs

- **注释说错业务语义** → 只根据代码、注解、调用方和已有文档归纳；不臆测外部系统行为；每批由你或模块负责人评审；不确定的语义不写。
- **2,798 个注释块导致评审疲劳** → 每批控制在 10～20 个文件；先黄金样例定调；按层提交，便于抽查。
- **`@since 1.0.0` 与真实引入版本不符** → 统一以项目基线 `1.0.0` 标注；如后续发现某类确为后续版本引入，可在该批修正。
- **doclint 暴露存量问题** → Phase 0 先跑一次基线，记录并修复断链/HTML 问题，再开始批量改写。
- **中文编码问题** → pom 中统一 UTF-8；生成 HTML 后抽查中文显示。
- **与进行中的代码改动冲突** → 注释改动集中在独立分支/变更中，按层提交；合并前与当前工作协调。
- **生成成员被误改** → 以 `@SuppressWarnings("all")` 和 delombok 特征识别；任务清单中明确排除规则。

## Migration Plan

1. 在 `iboot-admin/pom.xml` 增加插件与 `javadoc` profile；跑基线 `javadoc:javadoc`。
2. 编写 `doc/19-JavaDoc 规范.md`，并在 `CLAUDE.md` 增加第 5 节。
3. 完成两个黄金样例并评审通过。
4. 按层分批改写；每批编译、javadoc、diff 评审后提交。
5. 最终跑一次全量 javadoc，抽查 20 个类，更新变更状态。

回滚：按批 `git revert` 即可；不涉及数据迁移或运行时配置。

## Open Questions

- 是否在后续变更中增加 Checkstyle 类级注释门禁？本次不阻塞，留待规范稳定后评估。
