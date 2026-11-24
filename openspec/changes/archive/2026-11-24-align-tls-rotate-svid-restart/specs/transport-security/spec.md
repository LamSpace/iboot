# Spec Delta

## ADDED Requirements

### Requirement: 轮换重启保持 SVID 证书来源

在 Compose SVID 栈上执行证书轮换的 Docker 重启时，环境 MUST 保持服务加载 SVID，
MUST NOT 回退到 mkcert 证书路径。

- `--restart docker|rolling` MUST 检测 `docker-svid-*` 容器。
- 检测到 SVID 栈时 MUST 委托 `docker-svid-refresh.sh`，由其重新取回 SVID 并滚动重启。
- 未检测到 SVID 栈时 MAY 保持基础 compose 重建。
- CA 三阶段轮换的 `--restart docker|rolling` MUST 复用同一保护。
- 回归 MUST 断言 SVID 栈走委托路径，且非 SVID 栈不误触发。

#### Scenario: SVID 栈上的叶子轮换

- **WHEN** Compose SVID 栈运行中执行 `tls-rotate.sh --rotate --restart docker`
- **THEN** 重启委托 `docker-svid-refresh.sh`
- **AND** 服务继续使用 `/svid/svid.0.pem`
- **AND** 不执行基础 compose 的 `up --force-recreate`

#### Scenario: 非 SVID 栈

- **WHEN** 没有 `docker-svid-*` 容器时执行 Docker 重启
- **THEN** 仍按基础 compose 重建当前服务
- **AND** 不调用 SVID 刷新入口
