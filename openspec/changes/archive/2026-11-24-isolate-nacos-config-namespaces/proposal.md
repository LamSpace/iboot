# Proposal

## Why

Nacos 服务账号此前统一绑定 `iboot_role`，权限为 `resource=*`，任一服务都能读写 public 下的全部配置。
服务发现必须共用 public 命名空间，但配置没有理由共享。把配置迁到每服务命名空间并收紧角色，
可以在不改变发现模型的前提下实现配置资源级隔离。

## What Changes

- `nacos-init.sh` 为每个服务创建配置命名空间 `iboot-<service>`，并创建独立角色
  `iboot_<service>_role`；只授予 `public:*:*`（服务发现）与 `iboot-<service>:*:*`（配置读写）。
- 从每个服务账号上摘除旧的共享角色 `iboot_role`；运维账号 `iboot` 改用 `iboot_ops_role`。
- 九个使用 Nacos 配置的服务在 `spring.cloud.nacos.config.namespace` 指向 `iboot-<service>`。
- 新增 `scripts/regress/nacos-namespace-isolation.sh`：自己的命名空间可读、其他服务命名空间 403、public 可读。
- 更新 README、ROADMAP、docs/23、docs/27。

## Capabilities

### New Capabilities

无。

### Modified Capabilities

无。发现仍在 public；配置授权收紧，不改变服务 API 语义。

## Impact

- 脚本：`scripts/nacos-init.sh`。
- 配置：九个服务的 `application.yml`。
- 回归：`scripts/regress/regress.sh` 新增 12f。
- 文档：README、ROADMAP、docs/23、docs/27。

## 说明

Nacos HTTP API 仍只支持令牌认证，客户端证书 mTLS 保留为残余边界。
