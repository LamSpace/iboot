# Design

## Context

前端统一经网关已经完成；内部调用的现状分三类：

| 调用方 → 目标 | 现状 | 判定 |
|---|---|---|
| job / monitor → system（scope） | `@LoadBalanced` + `http://iboot-system/...` | 符合 |
| workflow → notify（内部通知） | `@LoadBalanced` + `http://iboot-notify` | 符合 |
| ai → system / job（智能体工具） | `@LoadBalanced` + 服务名 | 符合 |
| **file → system（scope）** | 普通 `RestClient` + `http://localhost:8080`，模块无负载均衡依赖 | 不符合 |
| **ai → system（scope）** | `@LoadBalanced` 构建器 + `http://localhost:8080` 默认值 | 语义冲突 |
| **kernel → auth（JWKS）** | 普通 `RestClient` + `http://localhost:8083` | 启动期例外 |

## Goals / Non-Goals

**Goals:**

- 业务调用一律按注册中心服务名寻址；固定地址只剩可解释、可审计的启动期例外。
- 容器化或换环境时，数据权限调用不会静默退化。

**Non-Goals:**

- 不引入 RPC 框架（Dubbo 仍等待 Boot 4 兼容版本）。
- 不改动网关路由或前端。
- 不为 JWKS 引入服务发现（原因见 D3）。

## Decisions

### D1 `iboot-file` 复用 job / monitor 的既有模式

`iboot-job` 与 `iboot-monitor` 已有可参照的实现：一个 `@LoadBalanced` 的 `RestClient.Builder` Bean，配一个默认值为服务名的 `scope-url`。文件服务按同一模式补齐，不发明新写法。

注意一个副作用：Boot 的 `RestClient.Builder` 自动配置是「缺失才创建」，定义了带 `@LoadBalanced` 的构建器后，容器内所有按类型注入的 `RestClient.Builder` 都会变成负载均衡版本。文件服务目前只有 scope 客户端使用它，因此影响可控；若将来新增使用绝对地址的客户端，必须显式使用非负载均衡构建器。

替代方案「保留固定地址、只改配置默认值」被否决：模块没有负载均衡依赖，改成服务名会在运行期解析失败。

### D2 `iboot-ai` 的 scope 默认值改为服务名

AI 的 scope 客户端注入的构建器本身带 `@LoadBalanced`，因此它只接受服务名（或 `lb://` 前缀）形式的地址。默认值改为 `http://iboot-system/api/auth/scope` 后，默认行为与客户端能力终于一致。

覆盖方式随之收紧：`IBOOT_AI_SCOPE_URL` 若被设置为绝对地址（如 `http://localhost:8080/...`），负载均衡拦截器会把 `localhost` 当作服务名解析并失败。文档必须写明该变量的取值形式，`.env.example` 与 `doc/10`、`doc/22` 同步修改。

### D3 JWKS 保持绝对地址，登记为启动期例外

内核的 JWKS 获取发生在服务启动或首次验签时，此时调用方可能还没有完整的服务发现上下文；更重要的是，负载均衡客户端会把 URI 的任意主机名当作服务名，一旦内核改用带 LB 的构建器，运维设置的绝对地址覆盖会全部失效。内核也不应为了一个引导调用引入 Spring Cloud 依赖，把耦合扩散到所有资源服务。

因此保持现状：默认 `http://localhost:8083/oauth2/jwks`，部署时必须通过 `JWT_JWK_SET_URI` 覆盖为可达地址。这一条按能力要求登记为例外，写明用途、不可用服务发现的原因与覆盖方式。

替代方案「内核引入 loadbalancer 并改用 `http://iboot-auth`」被否决：收益只是省掉一个环境变量，代价是把 Spring Cloud 依赖下沉到不依赖它的服务，并使绝对地址覆盖失效。

### D4 配置项与文档同步

- `iboot-file` 的 `application.yml` 补齐 `iboot.file.permission.scope-url` 与 `cache-seconds`，与 job / monitor 的键命名保持一致。
- `doc/23` 增加「内部调用清单」表：调用方、目标、寻址方式、是否例外。
- `.env.example` 中 job 的地址在上一轮已修正；本次同步 AI 的地址。

## Risks / Trade-offs

- **`@LoadBalanced` 构建器影响同上下文其它客户端** → 文件服务目前只有 scope 客户端使用；新增客户端时在评审中检查。
- **AI 的绝对地址覆盖失效** → 文档写明覆盖值必须是服务名；若确需固定地址，另起变更引入独立构建器。
- **JWKS 例外在容器化时被遗忘** → 部署清单（阶段 3）必须显式设置 `JWT_JWK_SET_URI`；本次在 `doc/23` 与例外登记中写明。
- **注册中心短暂不可用时广播** → 调用方按失败关闭处理，与现有数据权限语义一致。

## Migration Plan

纯配置改动，无数据迁移。按 file → ai 的顺序合入，逐服务重启验证；回滚为 `git revert`。

## Open Questions

无。
