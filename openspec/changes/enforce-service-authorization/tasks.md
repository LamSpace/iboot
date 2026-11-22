# Tasks

## 1. 共享授权模块

- [x] 1.1 新增 `iboot-service-authz` 模块与父 POM/Dockerfile builder 引用；验证 `mvn -pl iboot-service-authz test` 通过
- [x] 1.2 实现 `ClientCertificateIdentity`（Servlet/WebFlux 证书读取、CN 解析）与 `ServiceAuthorizationProperties`（`enabled`、`rules`、`extra-rules`）；单元测试覆盖 CN 解析与无法识别场景
- [x] 1.3 实现 `ServiceAuthorizationPolicy`（Ant 匹配、extra 优先、首个匹配生效、未命中放行）与 `ServiceAuthorizationFilter`/`ServiceAuthorizationWebFilter`；单元测试覆盖专属规则先于通配、允许/拒绝与 fail-closed

## 2. 规则与开关

- [x] 2.1 在三份 `iboot-tls.yml` 增加公共规则（actuator、JWKS、OAuth/login、SBA、Swagger/Druid）与默认关闭开关；验证 YAML 绑定与单元测试
- [x] 2.2 为 system、notify、job、file、monitor、workflow、auth、ai、gateway 增加 `extra-rules`，按设计矩阵声明 `/internal/**`、`/api/auth/scope`、AI 管理路径、`/api/**` 与门户入口；验证规则顺序
- [x] 2.3 在 compose 公共环境与 `dev-init.sh` 注入 `IBOOT_SERVICE_AUTHORIZATION_ENABLED=true`；验证 Docker 与宿主机模式均启用、测试上下文默认关闭

## 3. 运行时注册

- [x] 3.1 `iboot-kernel` 依赖新模块，注册最高优先级 Servlet 过滤器与 `ServiceAuthorizationProperties`；验证 8 个 Servlet 服务生效
- [x] 3.2 `iboot-gateway` 依赖新模块，注册 WebFlux 过滤器，在路由前只允许门户证书；验证网关 `/api/**` 越权返回 403
- [x] 3.3 `iboot-ops` 依赖新模块，注册 Servlet 过滤器；验证门户可访问 `/sba`、`/actuator`，其它服务证书被拒

## 4. 回归与文档

- [x] 4.1 新增 `scripts/regress/service-authz.sh`：允许（门户→网关、portal/prometheus→actuator、ai→system 用户接口、file→scope、workflow→internal）与拒绝（file→system `/api/user/**`、file→actuator、file→gateway API、file→internal）双向用例；验证脚本通过
- [x] 4.2 把 `service-authz.sh` 接入 `regress.sh` 并更新回归说明；验证全量回归包含该步骤
- [x] 4.3 更新 `docs/27`（新增 B5.4 小节与调用矩阵）、`docs/23`、`docker/README.md`、`ROADMAP.md`；验证文档不再写“未按调用方授权”

## 5. 集成验证

- [x] 5.1 重建 8 个服务、网关与 ops，执行 `service-authz.sh`、`internal-tls.sh`、冒烟与全量回归；验证门户、探针、SBA、AI、scope、internal 链路全部通过
- [ ] 5.2 在 CI 分支运行全栈回归，确认 host 模式与 Docker 模式均通过；记录已知备份冒烟例外

## Workflow follow-up

- 同步 delta specs 到主 specs 并归档变更。
- 新建功能分支、提交、创建 PR、合并到 master。
