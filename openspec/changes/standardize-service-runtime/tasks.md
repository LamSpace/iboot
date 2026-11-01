# Tasks

## 1. 共享日志配置

- [x] 1.1 在 `iboot-kernel/src/main/resources` 新增 `logback-spring.xml`：控制台、主文件、错误文件、Logstash 四类 appender，格式含 `traceId`/`spanId`，应用名用 `<springProperty>` 取自 `spring.application.name`；启动任一依赖内核的服务，确认 `logs/<应用名>.log` 与 `logs/<应用名>-error.log` 生成
- [x] 1.2 在 `iboot-kernel/pom.xml` 声明 `logstash-logback-encoder`，版本由父 POM 管理；执行 `mvn -pl iboot-kernel dependency:tree` 确认该依赖出现在编译范围
- [x] 1.3 在 `iboot-gateway/src/main/resources` 新增等价 `logback-spring.xml`（网关不依赖内核）；启动网关并请求一次接口，确认控制台日志含同一 `traceId`
- [x] 1.4 实现全部环境一致的四类输出与可覆盖的日志目录；分别以本地方式与容器方式启动同一服务，确认四类输出齐全，且设置目录覆盖变量后文件落到新目录

## 2. 启动脚本与镜像

- [x] 2.1 把 `iboot-system/script/run-java.sh` 复制到其余 8 个可部署模块的 `script/` 目录并保留可执行位，文件头标注来源与版本；确认 9 个模块的脚本内容一致（`sha256sum` 比对）
- [x] 2.2 为 9 个可部署模块编写统一模板 Dockerfile（应用名、端口、jar 名三处差异），并同步替换 `iboot-system/Dockerfile`；`docker build` 九个镜像全部成功
- [x] 2.3 以 `docker run --rm <image> id` 验证容器内进程为非 root；开启 `LOG_TO_FILE=true` 启动，确认能写入 `/app/logs` 且不因权限失败
- [x] 2.4 启动每个镜像并访问 `/actuator/health`（网关为 `/actuator/health`），确认九个服务全部返回 UP；记录各镜像大小，作为阶段 3 瘦身的基线

## 3. 文档同步

- [x] 3.1 更新 `doc/11-可观测性设计.md`：集中日志的覆盖范围由「仅系统服务」改为「九个服务」，写明 `app` 字段与 TraceID 串联方式；确认文档中不再出现只针对 `iboot-admin` 的描述
- [x] 3.2 更新 `README.md` 的「镜像与部署现状」：由「仅 `iboot-system` 与 `iboot-portal` 有 Dockerfile」改为「九个后端服务均有统一模板 Dockerfile」；确认 README 中的镜像章节与实际文件一致
- [x] 3.3 在 `doc/23-微服务底座操作指南.md` 补充日志排障一节：按 `app` 字段过滤、按 `traceId` 串联的具体命令；确认命令在本地 ELK 上可执行

## 4. 一致性与回归

- [x] 4.1 用 `scripts/dev-init.sh` 拉起九个服务，确认 `logs/` 下出现九个服务的 `.log` 与 `-error.log`；在 Kibana 中确认九个 `app` 字段都有数据
- [x] 4.2 停掉 Logstash 后重跑一次冒烟用例，确认业务请求不受影响；恢复 Logstash 后确认投递自动恢复，无需重启服务
- [x] 4.3 执行 `bash scripts/regress/regress.sh`，确认契约快照、路由覆盖、冒烟与权限矩阵全部通过
