# 宿主机进程回环绑定 — 设计

## 1. 绑定地址

`dev-init.sh` 的 Java 启动命令在 `-jar` 之后追加 `--server.address=127.0.0.1`。这是 Spring Boot 的
命令行属性源，优先级高于 `application.yml`，且不改动任何服务的配置文件。

- 覆盖 10 个服务（含 gateway/ops）的 HTTP 端口 `8080-8089`。
- `iboot-db-init` 是短生命周期迁移进程，不监听端口。
- `service_pid` 仍按 `-jar <module>/target/<jar>` 匹配，追加参数不影响进程识别。

## 2. 采集兼容

Prometheus 与 blackbox 在 Docker 内，需要访问宿主进程。分两种情况：

| 环境 | `host.docker.internal` 解析 | 处理 |
|---|---|---|
| Docker Desktop | 指向宿主机代理地址（非默认网桥网关） | 该代理可直达宿主机 `127.0.0.1`，不需要 relay |
| 原生 Docker | 解析为默认网桥网关（如 `172.17.0.1`） | 启动 relay，绑定 Docker 网桥网关并转发到 `127.0.0.1` |

检测方式：读取 Prometheus 容器 `/etc/hosts` 中 `host.docker.internal` 的 IPv4，与
`docker network inspect bridge` 的网关比较。两者相同即原生 Docker。

relay 绑定地址来自所有 Docker 网络的网关（`docker network inspect` 的 `IPAM.Config.Gateway`），
去重后传入 `scripts/host-loopback-relay.py`。这样无论 `host-gateway` 解析到默认网桥还是业务网络网关，
都能命中。

## 3. relay 实现

`scripts/host-loopback-relay.py`：

- `--bind` 逗号分隔的监听地址，`--ports` 支持 `8080-8089` 与逗号列表。
- 每个 `(bind, port)` 启动一个 `asyncio.start_server`；收到连接后连接 `127.0.0.1:<port>` 并双向转发。
- `--pid-file`、`--log-file`；SIGTERM/SIGINT 关闭所有 listener 后退出。
- 任一监听地址绑定失败时记录错误并以非零退出，避免静默降级。
- 只做 TCP 透传，不终止 TLS；服务端仍看到原始客户端证书与 SPIFFE 身份。

## 4. 生命周期

- `start_host_loopback_relay` 在 `start_backend` 之后、`wait_backend` 之前调用；此时端口已开始监听。
- `stop_host_loopback_relay` 由 `stop_all_services` 调用，`--stop`、`--down`、`--purge` 都会执行。
- PID 文件 `logs/host-loopback-relay.pid`，日志 `logs/host-loopback-relay.log`，均被 `.gitignore` 覆盖。
- 已运行时不重复启动；`--status` 输出 relay 状态。
- `IBOOT_HOST_LOOPBACK_RELAY=false` 可显式关闭 relay，仅用于排障；关闭后 Prometheus 在原生 Docker 上会抓取失败。

## 5. 回归

`scripts/regress/host-loopback.sh`：

1. 启动临时 HTTP 服务在 `127.0.0.1:18080`，relay 绑定 `127.0.0.2` 转发同端口，验证字节透传；
2. 若宿主 Java 进程在运行，用 `ss -ltn` 断言每个端口只有 `127.0.0.1`；
3. 用宿主机非回环 IPv4 连接 `8080-8089`，必须失败；
4. 若 relay 在运行，断言监听地址不是 `0.0.0.0`/`*`；
5. 静态检查 `dev-init.sh` 包含 `--server.address=127.0.0.1` 和 relay 生命周期调用。

宿主后端未运行时，回归只执行 relay 单元测试与静态检查；Docker 全量部署下自动跳过端口绑定断言。
