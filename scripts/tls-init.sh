#!/usr/bin/env bash
# 生成本地 TLS CA、门户证书与 Java 信任库。
#
# 用法：
#   scripts/tls-init.sh              证书不存在或剩余有效期不足 30 天时生成/重签
#   scripts/tls-init.sh --force      强制重签
#   scripts/tls-init.sh --check      只检查证书，不生成
#
# 证书目录默认是 docker/certs（已在 .gitignore 中）。浏览器信任需要另外执行一次
# `CAROOT=<证书目录>/ca mkcert -install`，该步骤可能需要管理员权限，脚本不代为执行。
#
# 可用环境变量：
#   IBOOT_TLS_CERT_DIR   证书输出目录，默认 <repo>/docker/certs
#   IBOOT_TLS_CAROOT     mkcert 的 CAROOT，默认 <证书目录>/ca
#   MKCERT               mkcert 可执行文件，默认从 PATH 查找

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CERT_DIR="${IBOOT_TLS_CERT_DIR:-$ROOT/docker/certs}"
CAROOT="${IBOOT_TLS_CAROOT:-$CERT_DIR/ca}"
MKCERT="${MKCERT:-mkcert}"
CERT_FILE="$CERT_DIR/portal.pem"
KEY_FILE="$CERT_DIR/portal-key.pem"
TRUSTSTORE="$CERT_DIR/truststore.p12"
ROOT_CA="$CAROOT/rootCA.pem"
# 容器运行期证书副本：以容器用户可读权限提供，宿主私钥仍保持 0600
RUNTIME_DIR="$CERT_DIR/runtime"
# Kafka 需要同时包含私钥与证书链的 PKCS12 keystore；密码仅用于本地开发
KAFKA_KEYSTORE="$RUNTIME_DIR/kafka-keystore.p12"
KAFKA_KEY_PASSWORD_FILE="$RUNTIME_DIR/kafka-key-password"
KAFKA_KEYSTORE_PASSWORD_FILE="$RUNTIME_DIR/kafka-keystore-password"
KAFKA_STORE_PASS="changeit"

# 叶子证书覆盖宿主机、Docker 主机名与全部服务名；B2 可直接复用同一份信任链。
SAN_NAMES=(
    localhost 127.0.0.1 ::1 host.docker.internal iboot.local '*.iboot.local'
    iboot-system iboot-gateway iboot-auth iboot-file iboot-notify
    iboot-job iboot-workflow iboot-monitor iboot-ai iboot-ops
    mysql redis kafka elasticsearch silo nacos seata-server rocketmq-proxy tempo
)

FORCE=0
CHECK_ONLY=0

usage() {
    awk 'NR > 1 && /^#/ { sub(/^# ?/, ""); print; next } NR > 1 { exit }' "$0"
}

for arg in "$@"; do
    case "$arg" in
        --force) FORCE=1 ;;
        --check) CHECK_ONLY=1 ;;
        -h|--help) usage; exit 0 ;;
        *) printf '[ERROR] 未知参数：%s\n' "$arg" >&2; exit 2 ;;
    esac
done

log() { printf '%s\n' "$*"; }
fail() { printf '[ERROR] %s\n' "$*" >&2; exit 1; }

require_command() {
    command -v "$1" >/dev/null 2>&1 || fail "缺少命令：$1"
}

require_command openssl
require_command keytool

require_mkcert() {
    if ! command -v "$MKCERT" >/dev/null 2>&1; then
        fail "未找到 mkcert（$MKCERT）。安装方式：sudo apt-get install -y mkcert libnss3-tools，或 go install filippo.io/mkcert@v1.4.4 后把 \$(go env GOPATH)/bin 加入 PATH。"
    fi
}

mkdir -p "$CERT_DIR" "$CAROOT"
chmod 700 "$CAROOT"

cert_valid() {
    [ -f "$CERT_FILE" ] && [ -f "$KEY_FILE" ] && [ -f "$ROOT_CA" ] && [ -f "$TRUSTSTORE" ] || return 1
    openssl x509 -checkend 2592000 -noout -in "$CERT_FILE" >/dev/null 2>&1
}

# 生成容器运行期副本：容器内非 root 用户需要可读私钥；宿主私钥保持 0600。
ensure_runtime_copies() {
    mkdir -p "$RUNTIME_DIR"
    cp -f "$CERT_FILE" "$RUNTIME_DIR/portal.pem"
    cp -f "$KEY_FILE" "$RUNTIME_DIR/portal-key.pem"
    cp -f "$ROOT_CA" "$RUNTIME_DIR/rootCA.pem"
    # 与宿主 docker/certs/ca/rootCA.pem 保持同一相对结构，供 Spring SSL bundle 使用
    mkdir -p "$RUNTIME_DIR/ca"
    cp -f "$ROOT_CA" "$RUNTIME_DIR/ca/rootCA.pem"
    cp -f "$TRUSTSTORE" "$RUNTIME_DIR/truststore.p12"
    # Silo 的证书目录契约
    cp -f "$CERT_FILE" "$RUNTIME_DIR/public.crt"
    cp -f "$KEY_FILE" "$RUNTIME_DIR/private.key"
    # Kafka 的 PKCS12 keystore 与密码文件；密码文件供容器镜像的 credentials 机制读取
    rm -f "$KAFKA_KEYSTORE"
    openssl pkcs12 -export \
        -in "$CERT_FILE" \
        -inkey "$KEY_FILE" \
        -certfile "$ROOT_CA" \
        -name iboot \
        -out "$KAFKA_KEYSTORE" \
        -passout "pass:$KAFKA_STORE_PASS"
    printf '%s' "$KAFKA_STORE_PASS" > "$KAFKA_KEY_PASSWORD_FILE"
    printf '%s' "$KAFKA_STORE_PASS" > "$KAFKA_KEYSTORE_PASSWORD_FILE"
    chmod 644 "$RUNTIME_DIR/portal.pem" "$RUNTIME_DIR/portal-key.pem" \
        "$RUNTIME_DIR/rootCA.pem" "$RUNTIME_DIR/truststore.p12" \
        "$RUNTIME_DIR/public.crt" "$RUNTIME_DIR/private.key" \
        "$KAFKA_KEYSTORE" "$KAFKA_KEY_PASSWORD_FILE" "$KAFKA_KEYSTORE_PASSWORD_FILE"
    chmod 755 "$RUNTIME_DIR/ca"
    chmod 644 "$RUNTIME_DIR/ca/rootCA.pem"
}

check_expiry() {
    [ -f "$CERT_FILE" ] || fail "证书不存在：$CERT_FILE（先执行 scripts/tls-init.sh）"
    if openssl x509 -checkend 2592000 -noout -in "$CERT_FILE" >/dev/null 2>&1; then
        log "证书有效：$CERT_FILE"
        openssl x509 -noout -enddate -in "$CERT_FILE" | sed 's/^/  /'
        return 0
    fi
    fail "证书剩余有效期不足 30 天：$CERT_FILE（执行 scripts/tls-init.sh --force 重签）"
}

if [ "$CHECK_ONLY" -eq 1 ]; then
    check_expiry
    exit 0
fi

if [ "$FORCE" -eq 0 ] && cert_valid; then
    ensure_runtime_copies
    log "证书仍然有效，跳过生成：$CERT_FILE"
    log "需要重签时执行：scripts/tls-init.sh --force"
    exit 0
fi

require_mkcert
log "生成本地 CA 与门户证书：$CERT_DIR"
CAROOT="$CAROOT" "$MKCERT" \
    -cert-file "$CERT_FILE" \
    -key-file "$KEY_FILE" \
    "${SAN_NAMES[@]}"

chmod 600 "$KEY_FILE" "$CAROOT/rootCA-key.pem"
chmod 644 "$CERT_FILE" "$ROOT_CA"

log "生成 Java 信任库：$TRUSTSTORE"
rm -f "$TRUSTSTORE"
keytool -importcert -noprompt \
    -alias iboot-local-ca \
    -file "$ROOT_CA" \
    -keystore "$TRUSTSTORE" \
    -storetype PKCS12 \
    -storepass changeit >/dev/null
chmod 644 "$TRUSTSTORE"
ensure_runtime_copies

log "完成："
log "  叶子证书   $CERT_FILE"
log "  私钥       $KEY_FILE"
log "  PEM CA     $ROOT_CA"
log "  Java 信任库 $TRUSTSTORE"
log "  容器副本   $RUNTIME_DIR（私钥为容器用户可读的 0644）"
log "  Kafka keystore $KAFKA_KEYSTORE"
log ""
log "浏览器信任（一次性，可能需要管理员权限）："
log "  CAROOT=$CAROOT $MKCERT -install"
