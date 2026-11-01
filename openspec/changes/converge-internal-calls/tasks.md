# Tasks

## 1. 文件服务改按服务名寻址

- [x] 1.1 在 `iboot-file/pom.xml` 增加 `spring-cloud-starter-loadbalancer`；执行 `mvn -pl iboot-file dependency:tree` 确认依赖就位
- [x] 1.2 新增负载均衡的 `RestClient.Builder` Bean，并让 `PermissionScopeClient` 注入它（与 `iboot-job`、`iboot-monitor` 的写法一致）；确认文件服务内没有其它按类型注入的 `RestClient.Builder` 会被误波及
- [x] 1.3 在 `iboot-file/application.yml` 补齐 `iboot.file.permission.scope-url`（默认 `http://iboot-system/api/auth/scope`）与 `cache-seconds`，代码中的默认值同步修改；确认默认配置下不再出现 `localhost:8080`
- [x] 1.4 启动注册中心与系统、文件服务，调用一次带数据范围的列表接口，确认文件服务的调用目标为 `iboot-system` 且数据权限过滤生效

## 2. AI 服务 scope 默认值收口

- [x] 2.1 把 `iboot-ai/application.yml` 与 `AiProperties` 中 `scope-url` 的默认值改为 `http://iboot-system/api/auth/scope`；确认启动日志与配置端点显示新默认值
- [x] 2.2 同步 `.env.example`、`doc/10-后端配置详解.md`、`doc/22-AI 工作台操作指南.md` 中的示例地址，并写明覆盖值必须是服务名形式；确认三处不再出现 `localhost:8080/api/auth/scope`
- [x] 2.3 启动 AI 服务并发起一次 RAG 问答，确认 scope 调用经过注册中心且权限过滤生效；当系统服务不可用时，问答以明确错误失败而不是返回未过滤结果

## 3. JWKS 例外登记

- [x] 3.1 在 `doc/23-微服务底座操作指南.md` 增加「内部调用清单」表：调用方、目标、寻址方式、是否例外；JWKS 一行写明用途、不可用服务发现的原因与 `JWT_JWK_SET_URI` 覆盖方式
- [x] 3.2 以非默认地址启动一个资源服务，确认 `JWT_JWK_SET_URI` 覆盖后验签正常；未覆盖时失败关闭并给出可定位日志

## 4. 端到端验证

- [x] 4.1 停掉网关后重跑一次文件列表与 RAG 问答，确认内部调用不受影响（不经网关）
- [x] 4.2 停掉系统服务，确认文件服务的带数据范围查询返回明确错误、不带数据范围的接口仍可用；恢复系统服务后自动恢复，无需重启
- [x] 4.3 执行 `bash scripts/regress/regress.sh`，确认契约快照、路由覆盖、冒烟与权限矩阵全部通过
