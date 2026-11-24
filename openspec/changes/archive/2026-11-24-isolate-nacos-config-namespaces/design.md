# Design

## Context

Nacos 的命名空间同时影响服务发现与配置读取。若把每服务放到独立命名空间，服务之间将互相不可见，
网关也无法按服务名路由。因此保留 public 作为发现平面，只把配置迁到每服务命名空间。

## Decisions

- 命名空间 ID 使用 `iboot-<service>`，与账号 `iboot_<service>` 一一对应。
- 每服务角色 `iboot_<service>_role` 的权限：
  - `public:*:*` action `rw`：注册、订阅与公共配置；
  - `iboot-<service>:*:*` action `rw`：仅本服务配置命名空间。
- 旧共享角色 `iboot_role` 从每个服务账号上 `DELETE` 摘除；运维账号改用 `iboot_ops_role`（`*`）。
- 服务配置 `spring.cloud.nacos.config.namespace` 指向 `iboot-<service>`；`discovery` 不设置 namespace，
  保持 public。网关只用发现，不增加配置命名空间。
- 现有 public 下没有业务配置（验证为空），无需迁移；新配置写入各服务命名空间。

## Risks / Trade-offs

- [服务配置仍在 public 部署] → 服务会读不到；本仓库 public 下无业务配置，且 import 为 optional。
- [旧共享角色残留] → 初始化脚本对每个账号显式摘除；角色本身无用户后不再生效。
- [发现权限过宽] → public 仍为共享发现平面，不存放业务配置；这是与发现模型的必要折中。
- [Nacos HTTP 客户端证书] → 上游只支持令牌，继续登记为残余边界。

## Migration Plan

1. 合并后重启 `nacos-init`，创建命名空间与角色并摘除旧角色。
2. 重启九个服务，使配置 namespace 生效；回归验证健康与权限。
3. 回滚：把服务 namespace 改回 public，并恢复 `iboot_role` 授权。

## Open Questions

无。
