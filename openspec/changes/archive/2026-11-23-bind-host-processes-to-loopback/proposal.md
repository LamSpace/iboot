# 宿主机进程回环绑定

## Why

宿主机开发模式由 `dev-init.sh` 直接启动 Java 进程，Spring Boot 默认监听所有网卡。业务端口虽然要求
客户端证书，但在同一局域网内仍可被扫描和发起连接，属于已登记的暴露面边界。Docker 全量部署已经
把发布端口收敛到 `127.0.0.1`，宿主机进程需要同等收敛。

## What Changes

- `dev-init.sh` 启动十个服务时追加 `--server.address=127.0.0.1`，业务 HTTP 端口只监听回环地址。
- Prometheus/blackbox 仍需要抓取宿主进程。Docker Desktop 的 `host.docker.internal` 已代理到宿主机回环，
  可直接使用；原生 Docker 无法访问宿主机回环时，启动一个只绑定 Docker 网桥网关地址的 TCP relay，
  把 `8080-8089` 转发到 `127.0.0.1` 同端口。
- relay 为独立 Python 脚本，支持多绑定地址、PID/日志文件与 SIGTERM 退出；`dev-init.sh --stop/down/purge`
  一并停止。
- 新增 `scripts/regress/host-loopback.sh`：验证 relay 转发、Java 进程只绑定回环、relay 不绑定全部网卡，
  并检查 `dev-init.sh` 的启动参数。
- 更新 `docs/27`、`docker/README.md`、`ROADMAP.md`。

## Capabilities

### New Capabilities

无。

### Modified Capabilities

- `transport-security`：新增“宿主机进程只监听回环地址”要求，并约束 relay 的绑定范围。

## Impact

- 修改：`scripts/dev-init.sh`、`scripts/regress/regress.sh`、文档与路线图。
- 新增：`scripts/host-loopback-relay.py`、`scripts/regress/host-loopback.sh`。
- 不改变 Docker 全量部署的端口发布方式，不改变服务间 TLS/mTLS。
- 非目标：Sentinel 客户端命令端口 8719（由 Sentinel 自身绑定策略决定），以及容器内服务端口。
