# Proposal

## Why

`iboot-admin` 的 Javadoc 覆盖不均、描述过短：288 个类中有 168 个类注释不足 30 字；851 个手写 public/protected 方法中 214 个缺失（控制器约 146 个）；1,659 个字段/常量中 933 个没有 Javadoc。现有注释无法支撑 API 阅读、代码评审和新成员上手，项目也没有 Javadoc 构建校验，无法防止质量回退。

## What Changes

- 制定并落地 iBoot 后端 JavaDoc 规范：**中文说明 + 英文标签**；类/接口/枚举/注解必须有类级文档；手写 public/protected 方法必须有方法级文档；DTO 字段、枚举常量、非显而易见的私有字段必须有字段级文档。
- 为 `iboot-admin` 的 288 个 Java 文件补齐或重写 Javadoc，约 2,798 个注释块。
- 明确排除 2,851 个 delombok 生成成员（getter/setter/equals/hashCode/toString/构造器），不为其添加注释。
- 在 `iboot-admin/pom.xml` 配置 `maven-javadoc-plugin` 3.12.0，使用 `doclint=all,-missing` 校验断链、HTML 和引用；通过 `javadoc` profile 按需产出 javadoc jar。
- 在 `CLAUDE.md` 增加「JavaDoc 注释规范」章节，并把详细规范写入 `doc/19-JavaDoc 规范.md`。
- 为 `iboot-admin` 的 37 个 Java 包新增 `package-info.java`，提供包级职责说明。
- 不改变任何业务代码、接口签名、运行时行为、数据库结构或 API 契约。

## Capabilities

### New Capabilities

无。本次是文档与构建工具变更，不改变运行时行为，`.openspec.yaml` 设置 `skip_specs: true`。

### Modified Capabilities

无。

## Impact

- **代码**：`iboot-admin/src/main/java` 下 288 个 Java 文件的注释；不触碰代码逻辑。
- **构建**：`iboot-admin/pom.xml` 新增插件与 profile；常规 `mvn package` 不受影响。
- **文档**：`CLAUDE.md`、新增 `doc/19-JavaDoc 规范.md`。
- **依赖**：无新增运行时依赖；仅使用 Maven 构建插件。
- **风险**：注释可能与实际行为不一致；通过分批评审、只改注释、编译与 javadoc 校验控制。
