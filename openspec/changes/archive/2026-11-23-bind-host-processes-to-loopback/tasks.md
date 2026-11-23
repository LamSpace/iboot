# 任务

## 1. relay 与启动参数

- [x] 1.1 新增 `scripts/host-loopback-relay.py`：多绑定地址、端口范围、PID/日志、SIGTERM 退出。
- [x] 1.2 `dev-init.sh` 为十个服务追加 `--server.address=127.0.0.1`。
- [x] 1.3 `dev-init.sh` 增加 relay 检测、启动、停止与状态；`--stop/down/purge` 清理 relay。

## 2. 回归

- [x] 2.1 新增 `scripts/regress/host-loopback.sh`：relay 转发、端口绑定、非回环不可达与静态检查。
- [x] 2.2 `regress.sh` 接入新的宿主回环检查。

## 3. 文档

- [x] 3.1 更新 `docs/27` §12.4/§23.3，移除“宿主机进程绑定所有网卡”的登记边界。
- [x] 3.2 更新 `docker/README.md` 与 `ROADMAP.md`。

## 4. 验证

- [x] 4.1 `bash scripts/regress/host-loopback.sh` 通过。
- [x] 4.2 完整本地回归通过；仅剩 Docker 备份关闭导致的已知冒烟用例失败。
- [x] 4.3 在临时宿主进程中验证 `ss` 只出现 `127.0.0.1`，并验证 relay 转发。
