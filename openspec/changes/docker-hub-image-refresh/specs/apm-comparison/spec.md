# Spec Delta

## MODIFIED Requirements

### Requirement: 镜像与版本来源可复现

辅助栈镜像 SHALL 从 Docker Hub 官方仓库获取并固定版本。

- OAP、查询界面与存储的版本 MUST 相互兼容；存储的 server API 版本 MUST 与 OAP 要求精确匹配。
- OAP、BanyanDB 与 Horizon UI MUST 同批升级，版本组合 MUST 来自同一次官方发布。
- 镜像拉取 MUST 可通过项目现有拉取脚本重复执行，重复执行 MUST NOT 失败。
- 本地缺失镜像时，MUST 能按文档从 Docker Hub 重新获取，并还原到编排引用的标签。

#### Scenario: 干净环境重建辅助栈

- **WHEN** 在未缓存辅助栈镜像的机器上执行项目镜像拉取脚本
- **THEN** 三个镜像均从 Docker Hub 获取成功
- **AND** 编排可直接使用固定标签启动
- **AND** 重复执行脚本不报错
