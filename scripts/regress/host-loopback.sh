#!/usr/bin/env bash
# 宿主进程回环绑定回归：relay 转发、Java 端口绑定、非回环不可达与启动参数。
#
# 宿主后端未运行时只执行 relay 单元测试与静态检查；Docker 全量部署下同样适用。

set -uo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$HERE/../.." && pwd)"
. "$HERE/lib.sh"

TMP="$(mktemp -d)"
HTTP_PID=""
RELAY_PID=""
cleanup() {
    [ -n "$RELAY_PID" ] && kill "$RELAY_PID" 2>/dev/null || true
    [ -n "$HTTP_PID" ] && kill "$HTTP_PID" 2>/dev/null || true
    rm -rf "$TMP"
}
trap cleanup EXIT

reg_log "== 1. relay 转发 =="
python3 -m http.server 18080 --bind 127.0.0.1 >"$TMP/http.log" 2>&1 &
HTTP_PID=$!
sleep 1
python3 "$ROOT/scripts/host-loopback-relay.py" \
    --bind 127.0.0.2 --ports 18080 \
    --pid-file "$TMP/relay.pid" --log-file "$TMP/relay.log" \
    >/dev/null 2>&1 &
RELAY_PID=$!
sleep 1
if curl -sS -m 3 http://127.0.0.2:18080/ >/dev/null 2>&1; then
    reg_pass "relay 可把 127.0.0.2:18080 转发到 127.0.0.1:18080"
else
    reg_fail "relay 转发失败"
fi
if grep -Fq '转发 127.0.0.2:18080' "$TMP/relay.log"; then
    reg_pass "relay 记录监听地址"
else
    reg_fail "relay 未记录监听地址"
fi
if python3 "$ROOT/scripts/host-loopback-relay.py" --bind 0.0.0.0 --ports 18080 >/dev/null 2>&1; then
    reg_fail "relay 接受 0.0.0.0 绑定"
else
    reg_pass "relay 拒绝 0.0.0.0 绑定"
fi
kill "$RELAY_PID" 2>/dev/null || true
wait "$RELAY_PID" 2>/dev/null || true
RELAY_PID=""
sleep 1
if [ ! -f "$TMP/relay.pid" ]; then
    reg_pass "relay 退出时清理 PID 文件"
else
    reg_fail "relay 退出后 PID 文件仍存在"
fi

reg_log "== 2. 启动参数与生命周期 =="
if grep -Fq -- '--server.address=127.0.0.1' "$ROOT/scripts/dev-init.sh"; then
    reg_pass "dev-init 为宿主进程追加回环绑定"
else
    reg_fail "dev-init 缺少 --server.address=127.0.0.1"
fi
if grep -Fq 'start_host_loopback_relay' "$ROOT/scripts/dev-init.sh" \
    && grep -Fq 'stop_host_loopback_relay' "$ROOT/scripts/dev-init.sh"; then
    reg_pass "dev-init 管理 relay 生命周期"
else
    reg_fail "dev-init 未管理 relay 生命周期"
fi

reg_log "== 3. 宿主 Java 端口绑定 =="
if ! pgrep -f 'iboot-.*\.jar' >/dev/null 2>&1; then
    reg_log "  [SKIP] 未发现宿主机 Java 进程（Docker 全量部署）"
else
    host_ip="$(ip -4 route get 1.1.1.1 2>/dev/null | awk '{for (i=1;i<=NF;i++) if ($i=="src") {print $(i+1); exit}}')"
    [ -n "$host_ip" ] || host_ip="$(ip -4 addr show scope global 2>/dev/null | awk '/inet /{print $2; exit}' | cut -d/ -f1)"
    relay_pid="$(cat "$ROOT/logs/host-loopback-relay.pid" 2>/dev/null || true)"
    for port in 8080 8081 8082 8083 8084 8085 8086 8087 8088 8089; do
        lines="$(ss -ltnpH "sport = :$port" 2>/dev/null)"
        if [ -z "$lines" ]; then
            reg_log "  端口 $port 未监听，跳过"
            continue
        fi
        bad=""
        while IFS= read -r line; do
            [ -n "$line" ] || continue
            addr="$(printf '%s\n' "$line" | awk '{print $4}')"
            case "$addr" in
                "127.0.0.1:$port"|"[::1]:$port"|"[::ffff:127.0.0.1]:$port") continue ;;
            esac
            # relay 绑定 Docker 网桥网关是预期行为；按 PID 排除，避免把转发器当成服务端口。
            if [ -n "$relay_pid" ] && printf '%s\n' "$line" | grep -q "pid=$relay_pid,"; then
                continue
            fi
            bad="$bad $addr"
        done <<< "$lines"
        if [ -z "$bad" ]; then
            reg_pass "端口 $port 只绑定回环地址（relay 网关监听已排除）"
        else
            reg_fail "端口 $port 绑定到非回环地址：$bad"
        fi
        if [ -n "$host_ip" ]; then
            if timeout 2 bash -c "exec 3<>/dev/tcp/$host_ip/$port" 2>/dev/null; then
                reg_fail "端口 $port 可通过宿主机非回环地址 $host_ip 访问"
            else
                reg_pass "端口 $port 拒绝宿主机非回环地址 $host_ip"
            fi
        fi
    done
fi

reg_log "== 4. relay 绑定范围 =="
if [ -f "$ROOT/logs/host-loopback-relay.pid" ] \
    && kill -0 "$(cat "$ROOT/logs/host-loopback-relay.pid")" 2>/dev/null; then
    relay_addrs="$(ss -ltnH 2>/dev/null | awk '$4 ~ /:(808[0-9]|8089)$/ {print $4}' | sort -u \
        | grep -vE '^(127\.0\.0\.1|\[::1\]|\[::ffff:127\.0\.0\.1\]):' || true)"
    if [ -z "$relay_addrs" ]; then
        reg_log "  [SKIP] relay 运行但未发现非回环监听（可能尚未转发）"
    elif printf '%s\n' "$relay_addrs" | grep -qE '^(0\.0\.0\.0|\*|\[::\]):'; then
        reg_fail "relay 绑定到全部网卡：$relay_addrs"
    else
        reg_pass "relay 只绑定 Docker 网桥网关：$(printf '%s' "$relay_addrs" | tr '\n' ' ')"
    fi
else
    reg_log "  [SKIP] relay 未运行（Docker Desktop 代理或未启用可观测平台）"
fi

reg_log "== 5. Prometheus 采集 =="
CA="${REG_CACERT:-$ROOT/docker/certs/ca/rootCA.pem}"
if curl -sf --cacert "$CA" -m 3 https://127.0.0.1:9090/-/ready >/dev/null 2>&1; then
    up_count="$(curl -sS --cacert "$CA" --get https://127.0.0.1:9090/api/v1/query \
        --data-urlencode 'query=count(up{job=~"iboot-.*"} == 1)' 2>/dev/null \
        | python3 -c 'import json,sys; r=json.load(sys.stdin)["data"]["result"]; print(r[0]["value"][1] if r else "0")' 2>/dev/null || echo 0)"
    if [ "${up_count:-0}" -gt 0 ] 2>/dev/null; then
        reg_pass "Prometheus 有 $up_count 个宿主机服务抓取目标为 up"
    else
        reg_fail "Prometheus 没有可达的宿主机服务抓取目标"
    fi
else
    reg_log "  [SKIP] Prometheus 未运行"
fi

reg_summary
exit "$REG_FAILED"
