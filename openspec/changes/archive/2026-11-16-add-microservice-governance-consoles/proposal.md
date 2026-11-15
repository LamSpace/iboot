# Proposal

## Why

系统已经引入 Nacos 与 Sentinel：前者承担注册与配置中心，后者承担入口与调用治理，本地编排会常驻启动两者。但门户没有对应入口，运维人员必须自己记住 `8849` 与 `8858` 两个地址。Nacos 3.3 的控制台鉴权又是独立于客户端/管理端鉴权的第三个开关，当前开发编排只关闭了后两者，控制台仍停在登录页，且本地没有可用的默认控制台账号；生产环境则必须保持控制台鉴权。因此不能用一个无条件 iframe 同时满足两种环境，需要“开发内嵌、生产跳转”的双模式入口。

## What Changes

- 门户新增顶级菜单“微服务治理”，下挂“Nacos 控制台”与“Sentinel 控制台”两个页面。
- 两个页面复用现有 `IframePanel`：`embed=true` 时内嵌；`embed=false` 或控制台需要登录时显示说明卡与“在新窗口打开”，不渲染持续失败的 iframe。
- 新增四个配置键：`sys.nacos.console.url`、`sys.nacos.console.embed`、`sys.sentinel.dashboard.url`、`sys.sentinel.dashboard.embed`；开发默认内嵌，生产默认只跳转。
- 开发编排为 Nacos 增加 `NACOS_AUTH_CONSOLE_ENABLE=false`，与已有的客户端/管理端免鉴权配置对齐；Sentinel 开发环境继续使用 `auth.enabled=false`。生产编排不变，两者保持鉴权。
- Nacos 通过门户同源代理 `/nacos-console/` 内嵌：代理把 `/nacos-console/*` 转发到 Nacos 控制台，把 `/nacos-console/v3/*` 转发到控制台 API，并剥离 `X-Frame-Options`；该代理只存在于开发环境，生产仍只跳转。
- 门户不保存、不代填、不转发任何控制台账号密码。
- 新增菜单与按钮权限 `governance:nacos`、`governance:sentinel`，并授权管理员角色；更新运维文档说明两种访问模式。
- 不改变 Nacos/Sentinel 的部署拓扑、规则来源与网关暴露边界，也不新增后端服务接口。

## Capabilities

### New Capabilities

- `governance-console`: 门户中的微服务治理控制台入口，定义 Nacos/Sentinel 控制台的开发内嵌、生产跳转、鉴权边界与失败降级行为。

### Modified Capabilities

- `service-resilience`: 现有“治理控制台的常驻与访问边界”要求禁止控制台经门户暴露；需改为允许本地开发环境通过门户内嵌（控制台登录关闭），生产环境只提供内网入口跳转，禁止内嵌与公网代理。

## Impact

- 前端：`iboot-portal` 新增两个视图、路由与中英文文案；复用 `IframePanel`；无后端接口变更。
- 数据：`iboot-db-init` 新增迁移，插入顶级菜单 9600、两个菜单项与按钮权限，并写入四个配置键。
- 部署：`docker/docker-compose.yml` 的 Nacos 服务增加 `NACOS_AUTH_CONSOLE_ENABLE=false`；生产编排不变。
- 文档：`docs/23-微服务底座操作指南.md`、`docker/README.md` 补充控制台入口与生产访问边界。
- 权限：新增 `governance:nacos`、`governance:sentinel`。
- 测试：前端 type-check/build、菜单与路由手测、compose 配置校验；不改变业务 API 与网关路由。
