#!/usr/bin/env bash
# SPIRE SVID 同步入口。
#
# 为十个服务注册 Workload API 条目并取回 X.509-SVID，写入 docker/certs/svid/<service>/。
# SVID 的 DNS SAN 同时包含服务名与 localhost：宿主机模式的 Nacos 发现地址使用 localhost，
# 容器模式使用服务名，两种客户端都能通过主机名校验。
#
# 用法：
#   scripts/spire-svid-sync.sh --setup            启动 SPIRE、注册条目、取回 SVID、合并信任 bundle
#   scripts/spire-svid-sync.sh --refresh [--restart none|rolling|api|host]
#   scripts/spire-svid-sync.sh --status
#   scripts/spire-svid-sync.sh --check [--threshold-days 7]
#
# 默认不重启服务；刷新 SVID 后需重启才能让服务加载新证书。
# --check 供调度使用：所有 SVID 剩余有效期超过阈值时退出 0，否则退出 1。

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CERT_DIR="${IBOOT_TLS_CERT_DIR:-$ROOT/docker/certs}"
CA_BUNDLE="$CERT_DIR/ca/rootCA.pem"
SPIRE_CA="$ROOT/docker/spire/ca/ca.crt"
SVID_DIR="${IBOOT_SVID_DIR:-$CERT_DIR/svid}"
AGENT_IMAGE="ghcr.io/spiffe/spire-agent:1.12.4"
SERVER_CONTAINER="iboot-spire-server"
AGENT_CONTAINER="iboot-spire-agent"
SERVER_SOCKET="/run/spire/server/api.sock"
AGENT_SOCKET="/run/spire/sockets/agent.sock"
PARENT_ID="spiffe://iboot.local/agent/iboot"
TRUST_DOMAIN="iboot.local"
SERVICES=(iboot-system iboot-gateway iboot-auth iboot-file iboot-notify iboot-job iboot-workflow iboot-monitor iboot-ai iboot-ops)

MODE=""
RESTART="none"
THRESHOLD_DAYS="${IBOOT_SVID_REFRESH_THRESHOLD_DAYS:-7}"

usage() {
    awk 'NR > 1 && /^#/ { sub(/^# ?/, ""); print; next } NR > 1 { exit }' "$0"
}
say() { printf '[spire-svid] %s\n' "$*"; }
die() { say "$*" >&2; exit 2; }

while [ "$#" -gt 0 ]; do
    case "$1" in
        --setup) MODE="setup" ;;
        --refresh) MODE="refresh" ;;
        --status) MODE="status" ;;
        --check) MODE="check" ;;
        --threshold-days)
            [ "$#" -ge 2 ] || die "--threshold-days 缺少取值（正整数）"
            THRESHOLD_DAYS="$2"
            shift
            ;;
        --threshold-days=*) THRESHOLD_DAYS="${1#*=}" ;;
        --restart)
            [ "$#" -ge 2 ] || die "--restart 缺少取值（none|rolling|api|host）"
            RESTART="$2"
            shift
            ;;
        --restart=*) RESTART="${1#*=}" ;;
        -h|--help) usage; exit 0 ;;
        *) die "未知参数：$1" ;;
    esac
    shift
done
[ -n "$MODE" ] || { usage >&2; exit 2; }
case "$RESTART" in
    none|rolling|api|host) ;;
    *) die "未知重启范围：$RESTART（none|rolling|api|host）" ;;
esac
case "$THRESHOLD_DAYS" in
    ''|*[!0-9]*) die "阈值必须是正整数：$THRESHOLD_DAYS" ;;
esac

agent_volume() {
    docker volume ls --format '{{.Name}}' | grep -E 'spire_agent_socket$' | head -1
}

spire_ready() {
    docker exec "$SERVER_CONTAINER" /opt/spire/bin/spire-server healthcheck \
        -socketPath "$SERVER_SOCKET" >/dev/null 2>&1 \
        && docker exec "$AGENT_CONTAINER" /opt/spire/bin/spire-agent healthcheck \
            -socketPath "$AGENT_SOCKET" >/dev/null 2>&1
}

register_entries() {
    local svc ids id
    for svc in "${SERVICES[@]}"; do
        # 先按 SPIFFE ID 删除旧条目（可能由测试或上一轮创建，entryID 不一定相同）。
        ids="$(docker exec "$SERVER_CONTAINER" /opt/spire/bin/spire-server entry show \
            -spiffeID "spiffe://$TRUST_DOMAIN/service/$svc" -socketPath "$SERVER_SOCKET" 2>/dev/null \
            | awk '/Entry ID/{print $4}')"
        for id in $ids; do
            docker exec "$SERVER_CONTAINER" /opt/spire/bin/spire-server entry delete \
                -entryID "$id" -socketPath "$SERVER_SOCKET" >/dev/null 2>&1 || true
        done
        docker exec "$SERVER_CONTAINER" /opt/spire/bin/spire-server entry create \
            -entryID "iboot-svid-$svc" \
            -parentID "$PARENT_ID" \
            -spiffeID "spiffe://$TRUST_DOMAIN/service/$svc" \
            -selector "docker:label:com.iboot.svid.service:$svc" \
            -dns "$svc" \
            -dns localhost \
            -dns host.docker.internal \
            -x509SVIDTTL 2592000 \
            -socketPath "$SERVER_SOCKET" >/dev/null \
            || die "注册 SVID 条目失败：$svc"
    done
}

fetch_svid() {
    local svc="$1" volume="$2" attempt out name
    mkdir -p "$SVID_DIR/$svc"
    chmod 755 "$SVID_DIR/$svc"
    for attempt in $(seq 1 10); do
        name="svid-$svc-$$-$attempt"
        docker rm -f "$name" >/dev/null 2>&1 || true
        out="$(docker run --name "$name" --user "$(id -u):$(id -g)" \
            --label "com.iboot.svid.service=$svc" \
            -v "$volume:/run/spire/sockets" \
            -v "$SVID_DIR/$svc:/svid" \
            --entrypoint /opt/spire/bin/spire-agent "$AGENT_IMAGE" \
            api fetch x509 -socketPath "$AGENT_SOCKET" -write /svid 2>&1 || true)"
        if [ -f "$SVID_DIR/$svc/svid.0.pem" ] && [ -f "$SVID_DIR/$svc/svid.0.key" ]; then
            chmod 600 "$SVID_DIR/$svc/svid.0.key"
            chmod 644 "$SVID_DIR/$svc/svid.0.pem" "$SVID_DIR/$svc/bundle.0.pem"
            docker rm -f "$name" >/dev/null 2>&1 || true
            return 0
        fi
        if [ "$attempt" -eq 10 ]; then
            say "SVID 取回诊断（$svc）：$(docker inspect -f '{{json .Config.Labels}}' "$name" 2>/dev/null || true)"
            docker exec "$SERVER_CONTAINER" /opt/spire/bin/spire-server entry show \
                -spiffeID "spiffe://$TRUST_DOMAIN/service/$svc" -socketPath "$SERVER_SOCKET" 2>&1 \
                | sed 's/^/[spire-svid]   /' || true
            docker logs "$AGENT_CONTAINER" 2>&1 | tail -10 | sed 's/^/[spire-svid]   agent: /' || true
        fi
        docker rm -f "$name" >/dev/null 2>&1 || true
        sleep 2
    done
    die "取回 SVID 失败：$svc（${out:-无输出}）"
}

fetch_all() {
    local volume svc
    volume="$(agent_volume)"
    [ -n "$volume" ] || die "未找到 SPIRE agent socket 卷"
    for svc in "${SERVICES[@]}"; do
        fetch_svid "$svc" "$volume"
    done
}

merge_trust_bundle() {
    local body
    [ -f "$SPIRE_CA" ] || die "SPIRE upstream CA 不存在：$SPIRE_CA"
    body="$(openssl x509 -in "$SPIRE_CA" -outform PEM | sed -n '2p')"
    if ! grep -qF "$body" "$CA_BUNDLE"; then
        say "合并 SPIRE CA 到信任 bundle：$CA_BUNDLE"
        cat "$SPIRE_CA" >> "$CA_BUNDLE"
    fi
    IBOOT_TLS_CERT_DIR="$CERT_DIR" "$ROOT/scripts/tls-init.sh" --sync-runtime
}

restart_tls_dependents() {
    local -a compose=(docker compose -f "$ROOT/docker/docker-compose.yml")
    local -a running=()
    local svc i
    mapfile -t running < <("${compose[@]}" ps --services 2>/dev/null || true)
    # Redis 的客户端证书复用 IBOOT_TLS_CERT_FILE（SVID）；服务端只在启动时加载
    # tls-ca-cert-file，必须在合并 SPIRE CA 后重启，否则 SVID 客户端会 unknown_ca。
    if printf '%s\n' "${running[@]}" | grep -qx redis; then
        "${compose[@]}" restart redis >/dev/null 2>&1 || true
        for i in $(seq 1 30); do
            [ "$(docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' iboot-redis 2>/dev/null || true)" = "healthy" ] && break
            sleep 1
        done
    fi
    for svc in prometheus blackbox-exporter; do
        if printf '%s\n' "${running[@]}" | grep -qx "$svc"; then
            "${compose[@]}" restart "$svc" >/dev/null 2>&1 || true
        fi
    done
}

setup() {
    local up_out
    say '启动 SPIRE Server/Agent'
    if ! up_out="$("$ROOT/scripts/spire-up.sh" --no-demo 2>&1)"; then
        printf '%s\n' "$up_out" | tail -10 | sed 's/^/[spire-svid]   /' >&2
        die 'SPIRE 启动失败'
    fi
    spire_ready || die 'SPIRE Server/Agent 未就绪'
    # 先重启 Agent 清空旧 SVID 缓存，再注册条目；避免“条目已更新但 Agent 仍返回旧证书”。
    say '重启 SPIRE Agent 清理 SVID 缓存'
    docker restart "$AGENT_CONTAINER" >/dev/null
    local i
    for i in $(seq 1 30); do
        docker exec "$AGENT_CONTAINER" /opt/spire/bin/spire-agent healthcheck \
            -socketPath "$AGENT_SOCKET" >/dev/null 2>&1 && break
        sleep 1
    done
    say '注册十个服务的 SVID 条目'
    register_entries
    # 给 Docker attestor 留出稳定时间。
    sleep 2
    say "取回 SVID：$SVID_DIR"
    fetch_all
    merge_trust_bundle
    restart_tls_dependents
    say 'SPIRE SVID 初始化完成'
}

refresh() {
    spire_ready || die 'SPIRE 未运行；先执行 --setup'
    fetch_all
    merge_trust_bundle
    restart_tls_dependents
    case "$RESTART" in
        none) say 'SVID 已刷新；未重启服务（新证书在重启后生效）' ;;
        *) "$ROOT/scripts/tls-rotate.sh" --restart-only --restart="$RESTART" ;;
    esac
}

cert_end_epoch() {
    local end
    end="$(openssl x509 -in "$1" -noout -enddate 2>/dev/null | sed 's/^notAfter=//')"
    [ -n "$end" ] || return 1
    date -d "$end" +%s 2>/dev/null
}

# 汇总 SVID 剩余有效期。
#
# 输出 min_remaining_days 与 refresh_needed；任一 SVID 缺失、无法解析或剩余有效期
# 低于阈值时返回 1，供调度器决定是否刷新。
svid_remaining_summary() {
    local now threshold_secs svc cert end remain min="" needs=0
    now="$(date +%s)"
    threshold_secs=$((THRESHOLD_DAYS * 86400))
    for svc in "${SERVICES[@]}"; do
        cert="$SVID_DIR/$svc/svid.0.pem"
        if [ ! -f "$cert" ]; then
            needs=1
            continue
        fi
        if ! end="$(cert_end_epoch "$cert")"; then
            needs=1
            continue
        fi
        remain=$((end - now))
        if [ -z "$min" ] || [ "$remain" -lt "$min" ]; then
            min="$remain"
        fi
        if [ "$remain" -lt "$threshold_secs" ]; then
            needs=1
        fi
    done
    if [ -n "$min" ]; then
        printf 'min_remaining_seconds=%s\n' "$min"
        printf 'min_remaining_days=%s\n' "$((min / 86400))"
    else
        printf 'min_remaining_seconds=unknown\n'
        printf 'min_remaining_days=unknown\n'
    fi
    if [ "$needs" -eq 0 ]; then
        printf 'refresh_needed=no\n'
        return 0
    fi
    printf 'refresh_needed=yes\n'
    return 1
}

status() {
    local svc cert
    printf 'cert_dir=%s\nsvid_dir=%s\n' "$CERT_DIR" "$SVID_DIR"
    if spire_ready; then
        printf 'spire=ready\n'
    else
        printf 'spire=unavailable\n'
    fi
    for svc in "${SERVICES[@]}"; do
        cert="$SVID_DIR/$svc/svid.0.pem"
        if [ -f "$cert" ]; then
            printf '%s: %s\n' "$svc" "$(openssl x509 -in "$cert" -noout -enddate | cut -d= -f2)"
            openssl x509 -in "$cert" -noout -ext subjectAltName 2>/dev/null \
                | tail -1 | sed 's/^/  /'
        else
            printf '%s: missing\n' "$svc"
        fi
    done
    if [ -f "$CA_BUNDLE" ] && [ -f "$SVID_DIR/iboot-system/svid.0.pem" ]; then
        if openssl verify -CAfile "$CA_BUNDLE" -untrusted "$SVID_DIR/iboot-system/svid.0.pem" \
                "$SVID_DIR/iboot-system/svid.0.pem" >/dev/null 2>&1; then
            printf 'bundle=trusts-spire-ca\n'
        else
            printf 'bundle=missing-spire-ca\n'
        fi
    fi
    printf 'threshold_days=%s\n' "$THRESHOLD_DAYS"
    svid_remaining_summary || true
}

case "$MODE" in
    setup) setup ;;
    refresh) refresh ;;
    status) status ;;
    check) svid_remaining_summary ;;
esac
