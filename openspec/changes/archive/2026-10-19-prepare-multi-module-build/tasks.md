# Tasks

## 1. 基线与回滚锚点

- [x] 1.1 在 `iboot-admin` 目录记录改造前基线：执行
  `mvn -s /home/lam/repo/settings.xml verify` 并把 `mvn -s /home/lam/repo/settings.xml dependency:tree`
  输出保存到 `/tmp/iboot-dep-tree-before.txt`；确认 `verify` 退出码 0 且依赖树文件非空。
- [x] 1.2 记录改造前产物与有效 POM：执行
  `mvn -s /home/lam/repo/settings.xml help:effective-pom -Doutput=/tmp/iboot-effective-before.xml`
  并确认 `iboot-admin/target/iboot-admin-1.0.0.jar` 存在，且 `jar tf` 输出包含 `BOOT-INF/`。

## 2. 父 POM 骨架与配置上提

- [x] 2.1 新建仓库根 `pom.xml`：parent 为 `spring-boot-starter-parent:4.0.3` 且
  `<relativePath/>` 置空，坐标 `com.iboot:iboot-parent:1.0.0`，`packaging=pom`，
  `modules` 仅含 `iboot-admin`，并用注释预留 `iboot-ai`；验证：根目录
  `mvn -s /home/lam/repo/settings.xml -N validate` 退出码 0，随后
  `mvn -s /home/lam/repo/settings.xml validate` 仍为绿（admin 尚未切换继承）。
- [x] 2.2 把 Java 版本、第三方依赖版本、插件版本属性迁入父 POM，并在
  `dependencyManagement` 中声明所有自有版本依赖（Spring Boot 生态依赖不写版本）；验证：
  `mvn -s /home/lam/repo/settings.xml -N validate` 退出码 0，且用 `grep` 确认父 POM 中
  每个 `<xxx.version>` 属性至少被引用一次，不存在孤立属性。
- [x] 2.3 把 `maven-compiler-plugin`（`release=25` + MapStruct 注解处理器）、
  `spring-boot-maven-plugin`（`--enable-native-access=ALL-UNNAMED`）、CycloneDX、Javadoc
  的共享配置迁入父 POM `pluginManagement`；验证：
  `mvn -s /home/lam/repo/settings.xml -N help:effective-pom` 输出中
  `pluginManagement` 同时包含这 4 个插件，且 Boot 父 POM 提供的插件版本未被覆盖。

## 3. 切换 iboot-admin 继承

- [x] 3.1 把 `iboot-admin/pom.xml` 的 parent 改为 `com.iboot:iboot-parent:1.0.0`，
  `<relativePath>../pom.xml</relativePath>`；删除已上提的 `<properties>`、依赖上的显式
  `<version>`、插件共享 `<configuration>` 与版本；保留 `<dependencies>` 清单、
  `<exclusions>`、`<scope>`、`<executions>`、`<resources>` 和 `javadoc` profile；验证：
  在 `iboot-admin` 下执行 `mvn -s /home/lam/repo/settings.xml verify` 退出码 0。
- [x] 3.2 比对依赖树：在 `iboot-admin` 下执行
  `mvn -s /home/lam/repo/settings.xml dependency:tree -DoutputFile=/tmp/iboot-dep-tree-after.txt`，
  与 `/tmp/iboot-dep-tree-before.txt` 做 `diff`；验证：差异为空（依赖坐标、版本、scope、
  optional 完全一致）。
- [x] 3.3 校验产物与有效构建配置：确认 `iboot-admin/target/iboot-admin-1.0.0.jar` 存在、
  `jar tf` 含 `BOOT-INF/`，并在 `help:effective-pom` 中确认 `release=25`、MapStruct
  `annotationProcessorPaths`、CycloneDX `makeAggregateBom` 执行与 `javadoc` profile
  均仍生效。
- [x] 3.4 在 README 项目结构树中补一行根 `pom.xml`（父 POM），不改任何构建命令；验证：
  `git diff --stat` 仅包含两个 POM 与 README，且 README 中的 `cd iboot-admin` 命令保持原样。

## 4. 集成验证

- [x] 4.1 在仓库根执行 `mvn -s /home/lam/repo/settings.xml clean verify`；验证：退出码 0，
  Reactor 构建顺序列出 `iboot-parent` 与 `iboot-admin`。
- [x] 4.2 验证既有文档命令仍可用：在 `iboot-admin` 下执行
  `mvn -s /home/lam/repo/settings.xml clean package -DskipTests`；验证：退出码 0 且
  `target/iboot-admin-1.0.0.jar` 重新生成，文件名未变。
- [x] 4.3 条件式运行时冒烟：若 `docker compose -f docker/docker-compose.yml ps` 显示
  MySQL 与 Redis 正在运行，则用 `mvn spring-boot:run -Dspring-boot.run.profiles=dev`
  启动应用并请求 `http://localhost:8080/actuator/health`，验证返回 `UP`；若中间件未运行，
  记录“未执行运行时冒烟”，并以 3.2 的依赖树一致性与 4.2 的产物不变作为等价证据。
  > 记录（2026-10-19）：`docker compose ps` 无运行中容器，未执行运行时冒烟。
- [x] 4.4 确认变更范围：执行 `git status --short` 与 `git diff --stat`；验证：只改动
  根 `pom.xml`、`iboot-admin/pom.xml`、README 结构树及本变更目录，无 Java、资源、Flyway、
  Docker 或前端文件变更。
