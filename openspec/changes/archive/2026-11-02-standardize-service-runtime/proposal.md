# Proposal

## Why

九个可部署服务里只有 `iboot-system` 配了 `logback-spring.xml`：其余八个服务（含网关）只有 Spring Boot 默认的控制台输出，既不写文件，也不向 Logstash 投递，日志格式里也没有 TraceID。拆分之后最需要的两种排障手段——按 TraceID 把一次请求串起来、在 Kibana 里集中检索——因此只覆盖了九分之一；`doc/11` 描述的集中日志链路事实上只收到一个服务的日志。

运行期打包同样只有一处：`iboot-system` 有 `script/run-java.sh` 和 `Dockerfile`，其余八个服务两者都没有，无法用一致的方式构建和启动镜像。

## What Changes

- **共享日志配置下沉到 `iboot-kernel`**：内核新增 `logback-spring.xml`，8 个依赖它的服务自动继承；网关不依赖内核，单独放一份同样的配置。
- **日志格式统一**：控制台、文件、错误文件与 Logstash 四类输出；每条日志包含时间、线程、`traceId`/`spanId`、级别与 logger。
- **应用标识自动取自 `spring.application.name`**：不再为每个服务手改常量，文件名、Logstash 的 `app` 字段与注册名一致。
- **输出目标在所有环境一致**：控制台、主日志文件、错误文件与 Logstash 四类输出始终开启，与现在 `iboot-system` 的行为一致；容器内文件写在 `/app/logs`，需要持久化时挂载该目录。
- **`logstash-logback-encoder` 加入内核依赖**：由父 POM 统一管理版本（9.0），随内核传递给全部消费方。
- **`run-java.sh` 复制到 9 个可部署模块**：gateway、system、ai、auth、file、notify、job、workflow、monitor。
- **9 份 Dockerfile 采用统一模板**：`openjdk:25-jdk-slim`、非 root 用户、`/app/logs` 可写、拷贝预构建 jar、声明 `JAVA_APP_NAME` 与 `EXPOSE`；已有的 `iboot-system/Dockerfile` 一并改成同一模板。
- **不做**：`docker-compose.yml` 的应用编排块（保持注释状态）；镜像瘦身与 distroless（`run-java.sh` 需要 `/bin/sh`，两者不可共存；ROADMAP 3.1 已改为「非 root 运行」）。

## Capabilities

### New Capabilities

- `service-runtime`: 服务的运行期契约——日志输出的种类、格式与投递目标，以及容器内的启动方式（启动脚本、非 root、JVM 参数来源）。

### Modified Capabilities

无。现有能力的行为、接口与数据归属均不变。

## Impact

- `iboot-kernel`：新增 `logback-spring.xml` 与 `logstash-logback-encoder` 依赖。
- `iboot-gateway`：单独一份 `logback-spring.xml`。
- 8 个业务服务：启动即继承内核日志配置，无需改代码。
- 9 个可部署模块：新增 `script/run-java.sh` 与 `Dockerfile`；`iboot-system` 的两份文件被替换为统一模板。
- 运行期：所有服务的日志会同时出现在控制台、`logs/<app>.log`、`logs/<app>-error.log` 与 Logstash；Logstash 不可用时由 ring buffer 兜底，不阻塞业务。
- 文档：`doc/11` 的集中日志描述在本次首次成立；ROADMAP 阶段 3.1 的镜像判据保持「可构建、可启动」。
