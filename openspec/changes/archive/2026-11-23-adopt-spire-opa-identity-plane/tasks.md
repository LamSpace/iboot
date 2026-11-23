# 任务

## 1. OPA 策略与客户端

- [x] 1.1 新增 `docker/opa/policies/service_authz.rego` 与 OPA 单元测试。
- [x] 1.2 `ServiceAuthorizationProperties` 增加 `opa-url`/`opa-timeout`；三份 `iboot-tls.yml` 绑定。
- [x] 1.3 新增 `OpaAuthorizationClient` 与 `ServiceAuthorizationPolicy` 同步/异步决策；失败关闭。
- [x] 1.4 WebFlux 过滤器改用异步决策；Servlet 过滤器保持同步封装。
- [x] 1.5 compose 增加 `opa` 服务并在 `x-app-env`/`dev-init.sh` 启用。

## 2. SPIRE

- [x] 2.1 新增 `docker/spire/**`：server/agent 配置、CA 与 join token 初始化、demo 工作负载。
- [x] 2.2 compose 增加 `spire` profile 与健康检查；不进入默认启动路径。
- [x] 2.3 demo 通过 Workload API 获取 SVID，并校验 URI SAN。

## 3. 回归与文档

- [x] 3.1 新增 `scripts/regress/opa-authz.sh` 与 `scripts/regress/spire-svid.sh`，接入 `regress.sh`。
- [x] 3.2 更新 `docs/27`、`docs/23`、`docker/README.md`、`ROADMAP.md`。
- [x] 3.3 在 OPA 启用下运行 `service-authz.sh`，确认允许/拒绝链路不变。

## 4. 验证

- [x] 4.1 `mvn -pl iboot-service-authz test` 通过。
- [x] 4.2 `opa-authz.sh` 与 `spire-svid.sh` 通过。
- [x] 4.3 完整本地回归通过（OPA/SPIRE 段全过；仅剩 Docker 备份关闭的已知冒烟用例失败）。
- [x] 4.4 GitHub 全栈回归通过。
