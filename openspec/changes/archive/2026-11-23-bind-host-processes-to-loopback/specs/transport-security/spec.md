# transport-security 增量规格

## ADDED Requirements

### Requirement: 宿主机进程只监听回环地址

宿主机开发模式下由启动脚本直接运行的业务进程 SHALL 只监听回环地址，不得因 Spring Boot 默认绑定而暴露到宿主机全部网卡。

- 每个业务服务的 HTTP 端口 MUST 绑定 `127.0.0.1`，MUST NOT 绑定 `0.0.0.0`、`*` 或 `::`。
- 宿主机非回环 IPv4 地址 MUST NOT 能连接这些端口。
- 当 Docker 内的采集组件无法直接访问宿主机回环时，SHALL 提供一个 TCP relay；relay MUST 只绑定 Docker 网桥网关地址，MUST NOT 绑定全部网卡或 LAN 地址。
- relay MUST 只转发业务服务的 HTTPS 端口，MUST NOT 转发数据面或管理端口。
- relay MUST 随宿主机开发栈启动和停止，不残留孤儿进程。
- 回归 MUST 断言端口绑定地址、非回环不可达性以及 relay 的绑定范围；Docker Desktop 已代理宿主机回环时可以跳过 relay 断言。

#### Scenario: 业务端口只绑定回环

- **WHEN** `dev-init.sh` 启动宿主机后端
- **THEN** 每个业务服务的 HTTP 端口只出现在 `127.0.0.1`
- **AND** 从宿主机非回环地址连接同一端口失败

#### Scenario: 原生 Docker 仍可采集

- **WHEN** Docker 内的 Prometheus 无法通过 `host.docker.internal` 访问宿主机回环
- **THEN** 启动一个绑定 Docker 网桥网关的 relay
- **AND** Prometheus 的业务服务抓取目标仍为 `up=1`

#### Scenario: relay 不扩大暴露面

- **WHEN** 检查 relay 的监听地址
- **THEN** 只出现 Docker 网桥网关地址
- **AND** 不出现 `0.0.0.0`、`*` 或宿主机 LAN 地址

#### Scenario: 停止开发栈

- **WHEN** 执行 `dev-init.sh --stop`、`--down` 或 `--purge`
- **THEN** relay 进程被停止
- **AND** 不残留 PID 文件对应的运行进程
