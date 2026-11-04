# Spec Delta

## MODIFIED Requirements

### Requirement: 容器启动契约

每个可部署服务 SHALL 以一致的脚本与镜像方式启动，使镜像构建不依赖对单个服务的特例处理。

- 每个可部署服务 MUST 提供启动脚本；镜像构建 MUST 使用统一模板，可以是一个共享的、按模块参数化的 Dockerfile，差异 MUST 仅限应用名、端口与 jar 文件名。
- 镜像 MUST 采用多阶段构建：构建阶段 MAY 执行完整的 Maven 构建，运行期镜像 MUST NOT 包含 Maven、JDK 编译器、构建缓存或项目源码。
- 运行期镜像 MUST 使用与构建阶段一致的 JDK 主版本。
- 镜像 MUST 以非 root 用户运行。
- 镜像内 MUST 存在一个可写的日志目录，使开启文件输出时启动不会失败。
- JVM 参数 MUST 可由环境变量覆盖，未覆盖时按容器可用内存推导。
- 镜像 MUST 声明服务监听的端口。

#### Scenario: 每个可部署服务都能构建镜像
- **WHEN** 在没有宿主机预构建 jar 的干净检出上构建任一可部署模块
- **THEN** 构建成功
- **AND** 镜像启动后健康检查返回 UP

#### Scenario: 运行期镜像不包含构建工具链
- **WHEN** 检查任一运行期镜像的内容
- **THEN** 镜像内不存在 Maven、`javac`、构建缓存或项目源码
- **AND** 镜像仅包含 JRE、该服务的 jar 与启动脚本

#### Scenario: 容器以非 root 运行
- **WHEN** 检查运行中的容器进程
- **THEN** 进程用户不是 root
- **AND** 开启文件输出时服务仍能写入日志目录

#### Scenario: JVM 参数可覆盖
- **WHEN** 启动容器时设置堆内存上限相关的环境变量
- **THEN** 进程的实际堆上限与设置一致
