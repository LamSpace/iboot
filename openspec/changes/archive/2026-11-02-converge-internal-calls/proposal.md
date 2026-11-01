# Proposal

## Why

前端已经统一经网关访问，但服务之间的调用并未全部走注册中心：`iboot-file` 的数据权限客户端使用固定的 `http://localhost:8080`，模块里既没有负载均衡依赖，也没有对应的配置项；`iboot-ai` 的 scope 默认值同样是 `localhost:8080`，而它注入的 `RestClient.Builder` 带 `@LoadBalanced`，地址语义与客户端能力互相矛盾；内核的 JWKS 获取固定指向 `localhost:8083`。

这些默认值在宿主机开发时恰好能工作，一旦换环境或容器化就会失败关闭——而且是静默地退化成「数据权限不可用」，不是启动时就报错。

## What Changes

- **`iboot-file` 改为按服务名寻址**：补 `spring-cloud-starter-loadbalancer` 依赖与 `@LoadBalanced` 的 `RestClient.Builder`；`scope-url` 默认值改为 `http://iboot-system/api/auth/scope`，并补齐 `iboot.file.permission.*` 配置项。
- **`iboot-ai` 的 scope 默认值改为服务名**：`http://iboot-system/api/auth/scope`；`.env.example` 与 `doc/22` 的示例同步。
- **内核 JWKS 登记为显式例外**：保持绝对地址 `http://localhost:8083/oauth2/jwks`，要求部署时通过 `JWT_JWK_SET_URI` 覆盖；不改为服务发现，原因是负载均衡客户端会把任意主机名当作服务名，绝对地址与 `lb://` 混用会互相破坏。
- **固化一条原则**：服务间业务调用 MUST 按注册中心服务名寻址；只有启动期引导类依赖可以作为登记例外，且必须在文档中逐条列出。
- 不改变任何接口路径、响应结构与权限语义。

## Capabilities

### New Capabilities

无。

### Modified Capabilities

- `service-discovery`: 新增「内部服务调用的寻址方式」要求——业务调用按服务名寻址并由调用方负载均衡，固定地址只允许作为已登记的启动期例外。

## Impact

- `iboot-file`：`pom.xml`、`PermissionScopeClient` 的构造、`application.yml` 新增配置项。
- `iboot-ai`：`application.yml` 与 `AiProperties` 的默认值；`.env.example`、`doc/10`、`doc/22` 的示例地址。
- `iboot-kernel`：仅在文档与 `doc/23` 中登记 JWKS 例外，代码不变。
- 运行期：`iboot-file` 与 `iboot-ai` 的数据权限调用改为经注册中心负载均衡；系统服务不可用时仍为失败关闭。
- 验证：停掉网关不影响任何内部调用；停掉系统服务时文件服务的带数据范围查询返回明确错误。
