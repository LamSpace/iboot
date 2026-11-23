# 自动调度证书轮换 — 设计

## 1. 入口与状态

新增 `scripts/tls-rotate-schedule.sh`，与 `tls-rotate.sh` 分离：后者保持“检查/重签/重启”的原子能力，
前者只负责“何时调用”。这样可以单独测试调度内容，也不会让普通启动路径获得重签副作用。

支持的模式：

| 模式 | 行为 |
|---|---|
| `--install` | 安装或更新本项目调度；幂等 |
| `--uninstall` | 只移除本项目创建的单元/标记块 |
| `--status` | 报告调度器类型、任务位置与下次运行时间 |
| `--run-now` | 执行一次检查，失败时重签并按配置重启 |
| `--print` | 只输出将安装的内容，不写系统 |

安装参数：`--restart none|docker|host`（默认 `none`）、`--scheduler auto|systemd|cron`（默认 `auto`）、
`--at HH:MM`（默认 `03:30`）。`--restart` 同时接受空格与等号形式。

## 2. 调度器选择

`auto` 的判定顺序：

1. `systemctl --user show-environment` 成功且 `XDG_RUNTIME_DIR` 非空；
2. 写入 `$XDG_CONFIG_HOME/systemd/user/iboot-tls-rotate.{service,timer}`，执行
   `systemctl --user daemon-reload` 与 `enable --now iboot-tls-rotate.timer`；
3. 任一步失败时回退 cron，并在 `--status` 中报告实际类型。

cron 使用带标记的块，安装时先删除旧块再追加，避免重复：

```text
# >>> iboot-tls-rotate >>>
30 3 * * * cd <root> && <root>/scripts/tls-rotate-schedule.sh --run-now --restart=docker >> <log> 2>&1
# <<< iboot-tls-rotate <<<
```

卸载只删除这个块或本项目的两个 unit 文件，不重写其它 crontab 行。

## 3. 运行语义

`--run-now`：

1. 用 `$XDG_STATE_HOME/iboot/tls-rotate.lock.d` 做目录锁；锁已存在时记录并退出非零，避免两个重签并发；
2. 记录开始时间；
3. 执行 `tls-rotate.sh --check`；
4. 检查通过：记录 `check ok`，退出 0，不修改证书、不重启；
5. 检查失败：记录失败原因，执行 `tls-rotate.sh --rotate --restart=<scope>`；
6. 重签或重启失败：记录并以非零退出；成功则记录 `rotate ok`。

`tls-rotate.sh --rotate` 自身会再次检查并在重启前完成校验，因此调度器不重复实现证书逻辑。
检查失败也可能来自缺少 `mkcert`/`openssl` 或证书文件损坏；此时重签会失败并原样记录，不会静默降级。

日志固定写入 `logs/tls-rotate.log`，同时保留 systemd journal；日志目录被 `.gitignore` 忽略。

## 4. 可测试性

以下环境变量只用于回归注入，不改变默认行为：

| 变量 | 默认值 |
|---|---|
| `IBOOT_TLS_ROTATE_BIN` | `<root>/scripts/tls-rotate.sh` |
| `IBOOT_CRONTAB_BIN` | `crontab` |
| `IBOOT_SYSTEMCTL_BIN` | `systemctl` |
| `IBOOT_SYSTEMD_USER_DIR` | `${XDG_CONFIG_HOME:-$HOME/.config}/systemd/user` |
| `IBOOT_LOG_DIR` | `<root>/logs` |
| `IBOOT_STATE_DIR` | `${XDG_STATE_HOME:-$HOME/.local/state}/iboot` |

回归用临时 `HOME` 与假 `crontab`/`systemctl`，验证安装幂等、卸载隔离、`--print` 内容以及
检查通过/失败两条运行路径；不触碰开发者真实的 crontab 或 systemd。

## 5. 安全与边界

- 默认 `--restart none`：安装调度不会在无人值守时重建整栈；需要自动生效时必须显式选择 `docker` 或 `host`。
- 不自动轮换 CA；CA 变化仍需人工执行并分发信任库。
- 不跨主机调度；容器化生产环境应由平台 CronJob 调用同一 `--run-now` 入口。
- 普通 `dev-init.sh` 与 `tls-init.sh` 的启动路径不调用调度器，保持“普通启动不重签”。
