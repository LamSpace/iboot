# Spec Delta

## MODIFIED Requirements

### Requirement: 干净检出可完成全栈构建

部署 SHALL 只依赖容器运行时、编排工具与 Docker Hub（MinIO 除外），从干净检出完成全部镜像构建与启动。

- 构建 MUST NOT 要求宿主机预先生成 jar、`dist` 或安装 Maven、Node.js、cnpm。
- 除 MinIO 外，基础镜像与运行期镜像 MUST 从 Docker Hub 官方仓库获取，并使用固定的版本标签；MUST NOT 使用浮动标签。
- MinIO MUST 从 `https://docker.aityp.com/` 展示的 SWR 镜像路径获取，并重打为编排引用的标签；MUST NOT 依赖已不可用的 Docker Hub `minio/minio` 仓库。
- Maven 与 npm 依赖 MUST 通过项目登记的内部镜像地址获取；重复构建 SHALL 复用构建缓存，不重复下载未变化的依赖。
- 构建失败时 MUST 明确指出失败的阶段与镜像，不得静默回退到宿主机构建或其它镜像源。

#### Scenario: 干净检出完成构建并启动

- **WHEN** 在一个没有 `target/`、`dist/`、`node_modules/` 的干净检出上执行部署命令
- **THEN** 全部镜像构建成功
- **AND** 核心服务启动并通过健康检查

#### Scenario: 基础镜像不可用时明确失败

- **WHEN** Docker Hub 或 MinIO 的 aityp/SWR 镜像源不可达，且本地没有所需镜像
- **THEN** 构建在拉取阶段失败并指出缺失的镜像
- **AND** 不会改去其它镜像源拉取后继续

### Requirement: 基础镜像拉取覆盖全部构建与运行期镜像

镜像拉取 SHALL 覆盖编排与 Dockerfile 使用的全部基础镜像，并使用固定标签。

- 拉取清单 MUST 包含构建阶段与运行期阶段使用的全部基础镜像。
- 除 MinIO 外，镜像 MUST 从 Docker Hub 官方仓库拉取，标签 MUST 固定到明确版本，MUST NOT 使用 `latest` 或浮动标签。
- MinIO MUST 从 `https://docker.aityp.com/` 展示的 SWR 镜像路径拉取，并重打为编排引用的标签。
- 拉取后的本地标签 MUST 与 Dockerfile 及编排中引用的镜像名一致。
- 重复执行拉取 MUST 是幂等的，已存在的镜像不得导致失败。

#### Scenario: 新机器完成基础镜像准备

- **WHEN** 在一台没有相关基础镜像的机器上执行拉取
- **THEN** 全部构建与运行期基础镜像就绪
- **AND** 后续镜像构建直接使用本地基础镜像，不再访问网络拉取

#### Scenario: 重复拉取成功

- **WHEN** 在已有基础镜像的机器上再次执行拉取
- **THEN** 命令成功结束
- **AND** 不删除正在被使用的镜像

## ADDED Requirements

### Requirement: MinIO 镜像来源例外

MinIO SHALL 通过 aityp/SWR 镜像路径获取，因为上游已停止发布官方容器镜像。

- 拉取脚本 MUST 使用 `https://docker.aityp.com/` 展示的 `minio/minio` SWR 路径，并在拉取后重打为 `docker.io/minio/minio:<tag>`。
- 版本 MUST 固定为 aityp 上可用的最后一个官方发布标签，MUST NOT 回退到 Docker Hub 或 Quay 的不可访问仓库。
- 若 aityp/SWR 缺失目标标签，拉取 MUST 明确失败，MUST NOT 静默使用其它 MinIO 镜像替代。

#### Scenario: 拉取 MinIO 并重打标签

- **WHEN** 执行项目镜像拉取脚本
- **THEN** MinIO 从 aityp/SWR 路径拉取成功
- **AND** 本地存在与编排引用一致的 `docker.io/minio/minio:<tag>` 标签

#### Scenario: MinIO 标签不可用

- **WHEN** aityp/SWR 上不存在脚本指定的 MinIO 标签
- **THEN** 拉取以非零状态失败并输出缺失的镜像
- **AND** 不会改用其它来源或版本继续
