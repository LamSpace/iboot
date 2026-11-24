# Tasks

## 1. Java 客户端韧性

- [x] 1.1 `ServiceAuthorizationProperties` 增加 `opaRetryAttempts` 与 `opaCacheTtl`，补 JavaDoc；verification: 配置绑定测试通过。
- [x] 1.2 `OpaAuthorizationClient` 实现传输错误有界重试、可选短 TTL/容量受限缓存（默认关闭）与规则指纹；verification: 新增单元测试覆盖重试、缓存命中/过期、失败不缓存。
- [x] 1.3 保持失败关闭与现有调用方语义；verification: `ServiceAuthorizationOpaTest` 与全量单元测试通过。

## 2. OPA 双副本与负载均衡

- [x] 2.1 `docker/docker-compose.yml` 的 `opa` 改为双副本并增加健康检查；新增 `opa-lb`（nginx）与 `docker/opa/nginx.conf`；verification: `docker compose up -d opa opa-lb` 后两个副本健康。
- [x] 2.2 `x-app-env` 与 `dev-init.sh` 的 OPA 地址改为 `opa-lb`/`localhost:8181`；verification: `docker compose config` 渲染正确，宿主机 `localhost:8181` 可用。
- [x] 2.3 重建受影响服务；verification: 服务间授权链路正常。

## 3. 回归与文档

- [x] 3.1 改造 `scripts/regress/opa-authz.sh`：LB 允许/拒绝、单副本故障仍可用、全副本故障 403、恢复；verification: 脚本逐项 PASS。
- [x] 3.2 更新 `docs/27` §27/§29、`docs/23`、`docker/README.md` 与 `ROADMAP.md` 的 OPA HA/缓存/重试说明；verification: 文档与 compose/配置一致。
- [x] 3.3 运行全量回归；verification: `scripts/regress/regress.sh` 通过。

## 4. Workflow follow-up

- [x] 归档 OpenSpec 变更并同步 `service-authorization` 主规格。
- [x] 新建 `feat/harden-opa-resilience-and-ha` 分支，提交、推送、创建 PR；PR CI 通过后合并到 `master`。
- [x] 在 GitHub 触发 `full-regress` 并确认通过（或记录不可抗因素）。
