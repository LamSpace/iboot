# Design

## Context

当前只有 `iboot-system` 具备完整的日志配置与镜像文件。其余八个服务使用 Spring Boot 默认日志：只有控制台，没有文件，没有集中投递，格式中也没有 TraceID。拆分之后，跨服务排障依赖 TraceID 串联与集中检索，这两件事现在都只对一个服务成立。

约束：

- 内核 `iboot-kernel` 已被 8 个服务依赖，承载统一响应、鉴权与审计写入，是横切能力的既有归处；网关不依赖内核。
- 各服务的 `spring.application.name` 已与注册名一致，可直接作为日志文件与来源字段。
- 需求明确要求 `run-java.sh` 覆盖全部后端模块，而该脚本依赖 `/bin/sh`。

## Goals / Non-Goals

**Goals:**

- 九个服务的日志输出种类、格式、落盘命名与投递目标一致。
- 日志文件与集中日志能按服务区分，且能被 TraceID 串联。
- 九个可部署服务用同一套启动脚本与镜像模板构建，不再需要逐服务特判。

**Non-Goals:**

- 不引入新的日志聚合组件（沿用现有 Logstash / Elasticsearch / Kibana）。
- 不做镜像瘦身、不做 distroless、不做多阶段构建（留给阶段 3 的 CI）。
- 不修改 `docker-compose.yml` 的应用编排块。
- 不改变任何 HTTP 接口、审计日志语义或业务行为。

## Decisions

### D1 共享配置放在 `iboot-kernel`，网关单独一份

内核是横切库的既有归处；把 `logback-spring.xml` 放在内核的资源目录，8 个依赖它的服务在类路径上直接获得同一份配置，不需要各自复制。被依赖 jar 中的 `logback-spring.xml` 会被 Spring Boot 的日志系统发现；若某个服务将来需要覆盖，只需在自身资源目录放同名文件即可（模块类路径优先于依赖 jar）。

网关不依赖内核（WebFlux 与 Servlet 依赖不应互相牵连），因此单独放一份等价配置。这是本设计里唯一接受的一处重复。

替代方案「每个模块各放一份」被否决：9 份配置必然漂移，且新增服务时容易漏掉。

### D2 应用名来自 `spring.application.name`

配置中用 `<springProperty name="APP_NAME" source="spring.application.name" defaultValue="iboot"/>` 取应用名，用于文件名、Logstash 的 `app` 字段。这样新增服务不需要改日志配置，也保证文件名、注册名、指标标签三者一致。

替代方案「每个模块写死常量」被否决：改名时（如 `iboot-admin → iboot-system`）会留下与注册名不一致的文件名。

### D3 输出目标在所有环境一致

控制台、主日志文件、错误文件与 Logstash 四类输出在全部 profile 下始终开启，与 `iboot-system` 当前的行为一致——这正是本次变更要解决的问题：其余服务缺失文件输出。

容器内同样写文件，目录固定为 `/app/logs`（镜像内已创建并授权给运行用户）。容器日志的持久化由两条路径共同保证：需要保留时挂载该目录，集中日志系统提供跨服务的长期检索。日志目录可由环境变量覆盖，便于本地把日志写到仓库根目录的 `logs/`。

### D4 `logstash-logback-encoder` 放在内核依赖

父 POM 已统一管理版本（9.0）。把它加为内核的编译依赖，8 个消费方通过传递依赖获得；网关显式声明同一版本。这样日志配置与它依赖的 encoder 始终同处一地，不会出现「配置引用了 appender 但类不存在导致日志系统启动失败」。

### D5 启动脚本复制而非共享

Dockerfile 的构建上下文是模块目录，`COPY` 不能跨出上下文。要让各模块继续用 `cd iboot-<module> && docker build .`，脚本必须出现在模块目录内。共享脚本需要把构建上下文改成仓库根，会改变所有人的构建习惯，收益不抵成本。

脚本是第三方 vendor 文件（fabric8 `run-java-sh`），复制后在文件头标注来源与版本，避免后续升级时各处改法不一。

### D6 镜像模板与基础镜像

统一模板的差异只有三处：jar 文件名、应用名、监听端口。模板要点：

```dockerfile
FROM openjdk:25-jdk-slim
ARG JAR_FILE=target/iboot-<name>-1.0.0.jar
WORKDIR /app
COPY script/run-java.sh /app/run-java.sh
COPY ${JAR_FILE} /app/app.jar
RUN useradd -r -u 1000 app && mkdir -p /app/logs && chown -R app:app /app
USER app
EXPOSE <port>
ENV JAVA_APP_JAR=/app/app.jar JAVA_APP_NAME=iboot-<name> \
    JAVA_OPTIONS="--enable-native-access=ALL-UNNAMED"
ENTRYPOINT ["/app/run-java.sh"]
```

- **基础镜像保持 `openjdk:25-jdk-slim`**：与现有 `iboot-system` 镜像一致，不引入新变量。代价是镜像明显大于 200MB，阶段 3.1 的镜像判据已相应改为「可构建、可启动」，瘦身留给 CI 阶段。
- **distroless 被排除**：它没有 `/bin/sh`，`run-java.sh` 无法执行。两者只能二选一，本次选脚本。
- **单阶段、拷贝预构建 jar**：构建由 `scripts/build.sh` 或 CI 完成；镜像内跑 Maven 需要额外的 `settings.xml` 与网络，慢一个数量级。
- **非 root 与 `/app/logs`**：脚本会把日志写到 `JAVA_APP_DIR/logs`，目录必须存在且对运行用户可写，否则开启文件输出时启动即失败。

### D7 现有 system 镜像一并替换

保留两套模板会让「统一」名不副实。`iboot-system/Dockerfile` 改为同一模板，仅保留其应用名与端口差异。

## Risks / Trade-offs

- **内核新增依赖影响全部消费方** → encoder 是轻量库；版本由父 POM 统一管理，升级只改一处。
- **服务自己放同名 `logback-spring.xml` 会覆盖内核配置** → 这是有意的逃生通道；若发生，需要在设计文档登记原因。
- **容器内写文件占用可写层** → 文件随容器销毁；需要留存时挂载 `/app/logs`，长期检索仍以集中日志为准。部署文档需写明这一点。
- **脚本复制后漂移** → 文件头标注来源与版本；升级时按插件方式整体替换。
- **非 root 用户写入宿主挂载目录时权限不足** → 仅当外部挂载 `logs/` 时出现；文档注明挂载前需匹配 UID。

## Migration Plan

无数据迁移。按模块合入：先内核与网关（配置生效），再逐模块补脚本与 Dockerfile，最后替换 system 的既有两份文件。

回滚：`git revert` 即可；已构建的镜像与新日志文件不影响回滚。

## Open Questions

无。
