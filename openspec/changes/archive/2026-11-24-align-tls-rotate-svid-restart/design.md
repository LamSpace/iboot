# Design

## Context

`tls-rotate.sh` 的 `--restart docker` 与 `rolling` 只构造
`docker compose -f docker/docker-compose.yml`，而 Compose SVID 模式需要额外加载
`docker-compose.svid.yml` 与 `--profile spire --profile svid`。直接重建会把服务环境变量改回
mkcert 证书路径，SVID 命名卷仍存在但不再被使用。

## Goals / Non-Goals

**Goals:**

- SVID 栈上的轮换重启保持 SVID 证书来源。
- 复用既有 `docker-svid-refresh.sh`，由其处理 sidecar 重建与滚动重启。
- 非 SVID 栈行为不变。

**Non-Goals:**

- 不在 `tls-rotate.sh` 内复制 SVID 覆盖层 compose 逻辑。
- 不改变 SVID 刷新调度（A1）或 CA 轮换阶段顺序（A3/A5）。

## Decisions

### 1. 以容器存在性识别 SVID 栈

sidecar 是一次性容器，退出后仍以 `docker-svid-<service>-1` 存在。用
`docker ps -a --format '{{.Names}}'` 匹配 `^docker-svid-.*-1$`，比读取 `.env`
更可靠（`IBOOT_SVID_ENABLED` 未必写入 `.env`）。

### 2. 委托而不是拼接 compose 参数

`docker-svid-refresh.sh` 已封装：重新注册条目、重建 sidecar 写入卷、等待完成、按序重建十个服务。
委托可避免两处覆盖层参数漂移；SVID 刷新本身也保证轮换后的信任 bundle 已合并 SPIRE CA。

### 3. CA 轮换复用同一入口

`tls-ca-rotate.sh` 的 `restart()` 调用 `tls-rotate.sh --restart-only`，因此三个阶段的
Docker 重启自动获得相同保护；`finalize` 后 SVID 侧也会重新合并 SPIRE CA。
