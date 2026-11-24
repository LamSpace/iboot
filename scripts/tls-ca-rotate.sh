#!/usr/bin/env bash
# 本地 CA 轮换入口。
#
# 三阶段滚动，避免新旧 CA 切换时互相拒绝：
#   --prepare   生成下一张 CA，rootCA.pem 变为“旧+新”信任 bundle；叶子证书仍由旧 CA 签发
#   --activate  切换签名 CA 到新 CA，重签全部叶子证书；信任 bundle 同时包含新旧 CA
#   --finalize  信任 bundle 只保留新 CA，旧 CA 退役
#
# 用法：
#   scripts/tls-ca-rotate.sh --prepare [--restart none|docker|rolling|api|host]
#   scripts/tls-ca-rotate.sh --activate [--restart ...]
#   scripts/tls-ca-rotate.sh --finalize [--restart ...]
#   scripts/tls-ca-rotate.sh --status
#   scripts/tls-ca-rotate.sh --prepare --dry-run
#
# 每个阶段结束后需重启运行时，使服务重新加载信任 bundle / 叶子证书；
# 默认 --restart none，仅修改文件。

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CERT_DIR="${IBOOT_TLS_CERT_DIR:-$ROOT/docker/certs}"
CAROOT="${IBOOT_TLS_CAROOT:-$CERT_DIR/ca}"
ROOT_CA="$CAROOT/rootCA.pem"
ROOT_KEY="$CAROOT/rootCA-key.pem"
ACTIVE_CA="$CAROOT/active-ca.pem"
ACTIVE_KEY="$CAROOT/active-ca-key.pem"
ROT="$CAROOT/rotation"
NEXT_CA="$ROT/next-ca.pem"
NEXT_KEY="$ROT/next-ca-key.pem"
PREV_CA="$ROT/previous-ca.pem"
PREV_KEY="$ROT/previous-ca-key.pem"
STATE="$ROT/state"
LOG_FILE="${IBOOT_LOG_DIR:-$ROOT/logs}/tls-ca-rotate.log"

MODE=""
RESTART="none"
DRY_RUN=0

usage() {
    awk 'NR > 1 && /^#/ { sub(/^# ?/, ""); print; next } NR > 1 { exit }' "$0"
}

say() { printf '[tls-ca-rotate] %s\n' "$*"; }
die() { say "$*" >&2; exit 2; }

while [ "$#" -gt 0 ]; do
    case "$1" in
        --prepare) MODE="prepare" ;;
        --activate) MODE="activate" ;;
        --finalize) MODE="finalize" ;;
        --status) MODE="status" ;;
        --dry-run) DRY_RUN=1 ;;
        --restart)
            [ "$#" -ge 2 ] || die "--restart 缺少取值（none|docker|rolling|api|host）"
            RESTART="$2"
            shift
            ;;
        --restart=none) RESTART="none" ;;
        --restart=docker) RESTART="docker" ;;
        --restart=rolling) RESTART="rolling" ;;
        --restart=api) RESTART="api" ;;
        --restart=host) RESTART="host" ;;
        -h|--help) usage; exit 0 ;;
        *) die "未知参数：$1" ;;
    esac
    shift
done

[ -n "$MODE" ] || { usage >&2; exit 2; }
case "$RESTART" in
    none|docker|rolling|api|host) ;;
    *) die "未知重启范围：$RESTART（none|docker|rolling|api|host）" ;;
esac

log() {
    mkdir -p "$(dirname "$LOG_FILE")"
    printf '%s %s\n' "$(date -Iseconds)" "$*" >> "$LOG_FILE"
}

state() { cat "$STATE" 2>/dev/null || printf 'initial\n'; }

current_ca() {
    if [ -f "$ACTIVE_CA" ]; then printf '%s' "$ACTIVE_CA"; else printf '%s' "$ROOT_CA"; fi
}

current_key() {
    if [ -f "$ACTIVE_KEY" ]; then printf '%s' "$ACTIVE_KEY"; else printf '%s' "$ROOT_KEY"; fi
}

bundle_count() {
    awk '/BEGIN CERTIFICATE/{n++} END{print n+0}' "$ROOT_CA" 2>/dev/null || printf '0'
}

restart() {
    if [ "$RESTART" = "none" ]; then
        say "未重启运行时；需要时执行 scripts/tls-rotate.sh --restart-only --restart=$RESTART"
        return 0
    fi
    "$ROOT/scripts/tls-rotate.sh" --restart-only --restart="$RESTART"
}

require_files() {
    [ -f "$ROOT_CA" ] || die "信任 CA 不存在：$ROOT_CA（先执行 scripts/tls-init.sh）"
    [ -f "$(current_key)" ] || die "签名 CA 私钥不存在：$(current_key)"
}

write_bundle() {
    local tmp="$ROOT_CA.tmp"
    cat "$1" "$2" > "$tmp"
    chmod 644 "$tmp"
    mv -f "$tmp" "$ROOT_CA"
    log "trust bundle: $1 + $2"
}

sync_runtime() {
    IBOOT_TLS_CERT_DIR="$CERT_DIR" IBOOT_TLS_CAROOT="$CAROOT" \
        IBOOT_TLS_SIGN_CA="$ACTIVE_CA" IBOOT_TLS_SIGN_CA_KEY="$ACTIVE_KEY" \
        "$ROOT/scripts/tls-init.sh" --sync-runtime
}

prepare() {
    require_files
    [ "$(state)" = "initial" ] || [ "$(state)" = "finalized" ] \
        || die "当前状态为 $(state)，不能重复 prepare；先 activate/finalize 或人工清理 $ROT"
    if [ "$(state)" = "finalized" ]; then
        # 上一轮已收敛；清理 next 以便开始下一轮，previous 会被当前 active 覆盖。
        rm -f "$NEXT_CA" "$NEXT_KEY"
    fi
    [ ! -f "$NEXT_CA" ] || die "下一张 CA 已存在：$NEXT_CA"
    if [ "$DRY_RUN" = "1" ]; then
        say "[dry-run] 将生成新 CA，rootCA.pem 变为“旧+新”，叶子证书保持旧 CA"
        return 0
    fi
    mkdir -p "$ROT"
    chmod 700 "$ROT"
    cp -f "$(current_ca)" "$PREV_CA"
    cp -f "$(current_key)" "$PREV_KEY"
    chmod 600 "$PREV_KEY"
    say "生成下一张 CA：$NEXT_CA"
    openssl req -x509 -newkey rsa:4096 -nodes -sha256 -days 3650 \
        -keyout "$NEXT_KEY" -out "$NEXT_CA" \
        -subj "/CN=iBoot Local CA Rotation/O=iBoot" \
        -addext "basicConstraints=critical,CA:TRUE" \
        -addext "keyUsage=critical,keyCertSign,cRLSign" >/dev/null 2>&1
    chmod 600 "$NEXT_KEY"
    chmod 644 "$NEXT_CA"
    # 签名 CA 保持旧 CA；rootCA.pem 作为信任 bundle 同时包含旧+新。
    cp -f "$PREV_CA" "$ACTIVE_CA"
    cp -f "$PREV_KEY" "$ACTIVE_KEY"
    chmod 600 "$ACTIVE_KEY"
    write_bundle "$PREV_CA" "$NEXT_CA"
    printf 'prepared\n' > "$STATE"
    sync_runtime
    say "prepare 完成：信任 bundle 已包含新旧 CA，叶子证书仍由旧 CA 签发"
    log "prepare complete"
    restart
}

activate() {
    require_files
    [ "$(state)" = "prepared" ] || die "当前状态为 $(state)，需要先 prepare"
    [ -f "$NEXT_CA" ] || die "下一张 CA 不存在：$NEXT_CA"
    if [ "$DRY_RUN" = "1" ]; then
        say "[dry-run] 将切换签名 CA 到新 CA，重签全部叶子证书"
        return 0
    fi
    cp -f "$NEXT_CA" "$ACTIVE_CA"
    cp -f "$NEXT_KEY" "$ACTIVE_KEY"
    chmod 600 "$ACTIVE_KEY"
    write_bundle "$NEXT_CA" "$PREV_CA"
    printf 'activated\n' > "$STATE"
    say "切换签名 CA 并重签全部叶子证书"
    IBOOT_TLS_CERT_DIR="$CERT_DIR" IBOOT_TLS_CAROOT="$CAROOT" \
        IBOOT_TLS_SIGN_CA="$ACTIVE_CA" IBOOT_TLS_SIGN_CA_KEY="$ACTIVE_KEY" \
        "$ROOT/scripts/tls-init.sh" --force
    say "activate 完成：叶子证书已由新 CA 签发，bundle 同时信任新旧 CA"
    log "activate complete"
    restart
}

finalize() {
    require_files
    [ "$(state)" = "activated" ] || die "当前状态为 $(state)，需要先 activate"
    if [ "$DRY_RUN" = "1" ]; then
        say "[dry-run] 将信任 bundle 收敛为新 CA，旧 CA 退役"
        return 0
    fi
    cp -f "$ACTIVE_CA" "$ROOT_CA"
    cp -f "$ACTIVE_KEY" "$ROOT_KEY"
    chmod 600 "$ROOT_KEY"
    chmod 644 "$ROOT_CA"
    printf 'finalized\n' > "$STATE"
    sync_runtime
    say "finalize 完成：信任 bundle 只保留新 CA"
    log "finalize complete"
    restart
}

show_status() {
    printf 'state=%s\n' "$(state)"
    printf 'cert_dir=%s\n' "$CERT_DIR"
    printf 'bundle=%s certs=%s\n' "$ROOT_CA" "$(bundle_count)"
    if [ -f "$ACTIVE_CA" ]; then
        printf 'signing_ca=%s\n' "$ACTIVE_CA"
        openssl x509 -noout -subject -issuer -enddate -in "$ACTIVE_CA" | sed 's/^/  /'
    else
        printf 'signing_ca=%s（初始状态）\n' "$ROOT_CA"
    fi
    if [ -f "$NEXT_CA" ]; then
        printf 'next_ca=%s\n' "$NEXT_CA"
        openssl x509 -noout -subject -enddate -in "$NEXT_CA" | sed 's/^/  /'
    fi
    local leaf="$CERT_DIR/services/iboot-system.pem"
    if [ -f "$leaf" ]; then
        printf 'leaf_issuer=%s\n' "$(openssl x509 -noout -issuer -in "$leaf" | sed 's/^issuer=//')"
    fi
}

case "$MODE" in
    prepare) prepare ;;
    activate) activate ;;
    finalize) finalize ;;
    status) show_status ;;
esac
