#!/usr/bin/env bash
# 启动 SPIRE Server/Agent 与 demo 工作负载，验证 Workload API 能签发 SVID。
#
# 该脚本只操作 `spire` profile；不改变默认 compose 启动路径或现有 mTLS 证书。
# 依赖：docker compose、openssl、python3。
#
# 用法：
#   scripts/spire-up.sh            启动 Server/Agent 并运行 demo SVID
#   scripts/spire-up.sh --no-demo  只启动 Server/Agent，供服务 SVID 同步使用

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
COMPOSE=(docker compose -f "$ROOT/docker/docker-compose.yml" --profile spire)
CA_DIR="$ROOT/docker/spire/ca"
BUNDLE_DIR="$ROOT/docker/spire/bundle"
SERVER_SOCKET="/run/spire/server/api.sock"
AGENT_SOCKET="/run/spire/sockets/agent.sock"

log() { printf '[spire-up] %s\n' "$*"; }
fail() { printf '[spire-up] ERROR: %s\n' "$*" >&2; exit 1; }

NO_DEMO=0
for arg in "$@"; do
    case "$arg" in
        --no-demo) NO_DEMO=1 ;;
        -h|--help) awk 'NR > 1 && /^#/ { sub(/^# ?/, ""); print; next } NR > 1 { exit }' "$0"; exit 0 ;;
        *) fail "未知参数：$arg" ;;
    esac
done

[ -f "$ROOT/.env" ] && set -a && . "$ROOT/.env" && set +a

mkdir -p "$CA_DIR" "$BUNDLE_DIR"
if [ ! -f "$CA_DIR/ca.crt" ] || [ ! -f "$CA_DIR/ca.key" ]; then
    log '生成本地 SPIRE upstream CA'
    openssl req -x509 -newkey rsa:2048 -nodes -days 3650 \
        -subj '/CN=iBoot SPIRE Upstream CA/O=iBoot' \
        -keyout "$CA_DIR/ca.key" -out "$CA_DIR/ca.crt" >/dev/null 2>&1
    chmod 600 "$CA_DIR/ca.key"
fi

log '启动 SPIRE Server'
"${COMPOSE[@]}" up -d spire-server >/dev/null
for _ in $(seq 1 60); do
    if docker exec iboot-spire-server /opt/spire/bin/spire-server healthcheck \
        -socketPath "$SERVER_SOCKET" >/dev/null 2>&1; then
        break
    fi
    sleep 1
done
docker exec iboot-spire-server /opt/spire/bin/spire-server healthcheck \
    -socketPath "$SERVER_SOCKET" >/dev/null 2>&1 || fail 'SPIRE Server 未就绪'

log '生成 join token 与 trust bundle'
token_json="$(docker exec iboot-spire-server /opt/spire/bin/spire-server token generate \
    -spiffeID spiffe://iboot.local/agent/iboot -output json -socketPath "$SERVER_SOCKET")"
SPIRE_JOIN_TOKEN="$(printf '%s' "$token_json" | python3 -c 'import json,sys; print(json.load(sys.stdin)["value"])')"
[ -n "$SPIRE_JOIN_TOKEN" ] || fail 'join token 为空'
docker exec iboot-spire-server /opt/spire/bin/spire-server bundle show \
    -format pem -socketPath "$SERVER_SOCKET" > "$BUNDLE_DIR/bundle.crt"
[ -s "$BUNDLE_DIR/bundle.crt" ] || fail 'trust bundle 为空'

log '启动 SPIRE Agent'
SPIRE_JOIN_TOKEN="$SPIRE_JOIN_TOKEN" "${COMPOSE[@]}" up -d spire-agent >/dev/null
for _ in $(seq 1 60); do
    if docker exec iboot-spire-agent /opt/spire/bin/spire-agent healthcheck \
        -socketPath "$AGENT_SOCKET" >/dev/null 2>&1; then
        break
    fi
    sleep 1
done
docker exec iboot-spire-agent /opt/spire/bin/spire-agent healthcheck \
    -socketPath "$AGENT_SOCKET" >/dev/null 2>&1 || fail 'SPIRE Agent 未就绪'

if [ "$NO_DEMO" = "1" ]; then
    log 'SPIRE Server/Agent 已就绪（跳过 demo）'
    exit 0
fi

log '注册 demo 工作负载'
docker exec iboot-spire-server /opt/spire/bin/spire-server entry create \
    -entryID iboot-demo \
    -parentID spiffe://iboot.local/agent/iboot \
    -spiffeID spiffe://iboot.local/service/iboot-demo \
    -selector docker:label:com.docker.compose.service:spire-demo \
    -dns iboot-demo \
    -socketPath "$SERVER_SOCKET" >/dev/null 2>&1 || true

log '运行 demo 工作负载并获取 SVID'
"${COMPOSE[@]}" up -d --no-deps --force-recreate spire-demo >/dev/null

demo_output=''
for attempt in $(seq 1 5); do
    for _ in $(seq 1 30); do
        status="$(docker inspect -f '{{.State.Status}}' iboot-spire-demo 2>/dev/null || true)"
        [ "$status" = "exited" ] && break
        sleep 1
    done
    demo_output="$(docker logs iboot-spire-demo 2>&1 || true)"
    if printf '%s' "$demo_output" | grep -q 'spiffe://iboot.local/service/iboot-demo'; then
        break
    fi
    log "demo 第 ${attempt} 次未取得 SVID，重试"
    docker restart iboot-spire-demo >/dev/null 2>&1 || true
    sleep 2
done

printf '%s\n' "$demo_output"
printf '%s' "$demo_output" | grep -q 'spiffe://iboot.local/service/iboot-demo' \
    || fail 'demo 未取得 SPIFFE SVID'
log 'SVID 获取成功'
