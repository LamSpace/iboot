#!/usr/bin/env bash
# 生成本地 TLS CA、门户/服务证书、探针客户端证书与 Java 信任库。
#
# 用法：
#   scripts/tls-init.sh              证书不存在、用途不足或剩余有效期不足 30 天时生成/重签
#   scripts/tls-init.sh --force      强制重签
#   scripts/tls-init.sh --check      只检查证书，不生成
#
# 证书目录默认是 docker/certs（已在 .gitignore 中）。浏览器信任需要另外执行一次
# `CAROOT=<证书目录>/ca mkcert -install`，该步骤可能需要管理员权限，脚本不代为执行。
#
# 叶子证书由 mkcert 生成的本地 CA 通过 openssl 签发，EKU 同时包含 serverAuth 与
# clientAuth，服务证书因此可以直接作为服务间 mTLS 的客户端身份。
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
CA_KEY="$CAROOT/rootCA-key.pem"
CA_SERIAL="$CAROOT/rootCA.srl"
# 容器运行期证书副本：以容器用户可读权限提供，宿主私钥仍保持 0600
RUNTIME_DIR="$CERT_DIR/runtime"
SERVICE_DIR="$CERT_DIR/services"
CLIENT_DIR="$CERT_DIR/clients"
RUNTIME_SERVICE_DIR="$RUNTIME_DIR/services"
RUNTIME_CLIENT_DIR="$RUNTIME_DIR/clients"
# Kafka 需要同时包含私钥与证书链的 PKCS12 keystore；密码仅用于本地开发
KAFKA_KEYSTORE="$RUNTIME_DIR/kafka-keystore.p12"
KAFKA_KEY_PASSWORD_FILE="$RUNTIME_DIR/kafka-key-password"
KAFKA_KEYSTORE_PASSWORD_FILE="$RUNTIME_DIR/kafka-keystore-password"
NACOS_KEYSTORE="$RUNTIME_DIR/nacos-keystore.p12"
KAFKA_STORE_PASS="changeit"
CERT_DAYS=825
# SPIFFE 信任域；证书 URI SAN 与授权解析器共用该值。
TRUST_DOMAIN="iboot.local"

# 叶子证书覆盖宿主机、Docker 主机名与全部服务名；B2 可直接复用同一份信任链。
SAN_NAMES=(
    localhost 127.0.0.1 ::1 host.docker.internal iboot.local '*.iboot.local'
    iboot-system iboot-gateway iboot-auth iboot-file iboot-notify
    iboot-job iboot-workflow iboot-monitor iboot-ai iboot-ops
    mysql redis kafka elasticsearch silo nacos seata-server rocketmq-proxy tempo
    prometheus alertmanager grafana thanos-query thanos-sidecar thanos-store thanos-compact kibana
)

# 可部署服务各自使用独立证书；同一张证书同时承担服务端与客户端身份。
SERVICE_NAMES=(
    iboot-system iboot-gateway iboot-auth iboot-file iboot-notify
    iboot-job iboot-workflow iboot-monitor iboot-ai iboot-ops
)

# 监控 exporter 只承担服务端身份；证书只看 serverAuth。
EXPORTER_NAMES=(
    mysql-exporter redis-exporter node-exporter elasticsearch-exporter
)

# 不提供业务端口的调用方使用独立客户端证书，避免借用门户或某个服务的身份。
CLIENT_NAMES=(
    prometheus blackbox
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

# 证书用途检查：服务证书要能同时做服务端和客户端，探针证书只要能做客户端。
cert_has_purpose() {
    local cert="$1" purpose="$2"
    openssl x509 -in "$cert" -noout -purpose 2>/dev/null \
        | grep -Eq "^${purpose}[[:space:]]*:[[:space:]]*Yes$"
}

# 证书必须带匹配的 SPIFFE URI SAN，例如 service/iboot-system 或 client/prometheus。
cert_has_spiffe() {
    local cert="$1" path="$2"
    openssl x509 -in "$cert" -noout -text 2>/dev/null \
        | grep -q "URI:spiffe://${TRUST_DOMAIN}/${path}"
}

# URI SAN 与 Subject CN 必须指向同一个工作负载名。
cert_identity_matches() {
    local cert="$1" expected="$2" cn
    cert_has_spiffe "$cert" "$3" || return 1
    cn="$(openssl x509 -in "$cert" -noout -subject -nameopt RFC2253 2>/dev/null \
        | sed 's/^subject=//' | tr ',' '\n' | sed -n 's/^CN=//p' | head -1)"
    [ "$cn" = "$expected" ]
}

cert_valid() {
    [ -f "$CERT_FILE" ] && [ -f "$KEY_FILE" ] && [ -f "$ROOT_CA" ] && [ -f "$TRUSTSTORE" ] || return 1
    cert_has_purpose "$CERT_FILE" "SSL server" || return 1
    cert_has_purpose "$CERT_FILE" "SSL client" || return 1
    cert_identity_matches "$CERT_FILE" "iboot-portal" "service/iboot-portal" || return 1
    openssl x509 -checkend 2592000 -noout -in "$CERT_FILE" >/dev/null 2>&1 || return 1
    local svc client
    for svc in "${SERVICE_NAMES[@]}"; do
        [ -f "$SERVICE_DIR/$svc.pem" ] && [ -f "$SERVICE_DIR/$svc-key.pem" ] || return 1
        cert_has_purpose "$SERVICE_DIR/$svc.pem" "SSL server" || return 1
        cert_has_purpose "$SERVICE_DIR/$svc.pem" "SSL client" || return 1
        cert_identity_matches "$SERVICE_DIR/$svc.pem" "$svc" "service/$svc" || return 1
        openssl x509 -checkend 2592000 -noout -in "$SERVICE_DIR/$svc.pem" >/dev/null 2>&1 || return 1
    done
    for client in "${CLIENT_NAMES[@]}"; do
        [ -f "$CLIENT_DIR/$client.pem" ] && [ -f "$CLIENT_DIR/$client-key.pem" ] || return 1
        cert_has_purpose "$CLIENT_DIR/$client.pem" "SSL client" || return 1
        cert_identity_matches "$CLIENT_DIR/$client.pem" "$client" "client/$client" || return 1
        openssl x509 -checkend 2592000 -noout -in "$CLIENT_DIR/$client.pem" >/dev/null 2>&1 || return 1
    done
    for exporter in "${EXPORTER_NAMES[@]}"; do
        [ -f "$SERVICE_DIR/$exporter.pem" ] && [ -f "$SERVICE_DIR/$exporter-key.pem" ] || return 1
        cert_has_purpose "$SERVICE_DIR/$exporter.pem" "SSL server" || return 1
        cert_identity_matches "$SERVICE_DIR/$exporter.pem" "$exporter" "service/$exporter" || return 1
        openssl x509 -checkend 2592000 -noout -in "$SERVICE_DIR/$exporter.pem" >/dev/null 2>&1 || return 1
    done
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
    # Nacos 使用 Spring Boot server.ssl，需要 PKCS12 keystore
    rm -f "$NACOS_KEYSTORE"
    openssl pkcs12 -export \
        -in "$CERT_FILE" \
        -inkey "$KEY_FILE" \
        -certfile "$ROOT_CA" \
        -name nacos \
        -out "$NACOS_KEYSTORE" \
        -passout "pass:$KAFKA_STORE_PASS"
    chmod 644 "$RUNTIME_DIR/portal.pem" "$RUNTIME_DIR/portal-key.pem" \
        "$RUNTIME_DIR/rootCA.pem" "$RUNTIME_DIR/truststore.p12" \
        "$RUNTIME_DIR/public.crt" "$RUNTIME_DIR/private.key" \
        "$KAFKA_KEYSTORE" "$KAFKA_KEY_PASSWORD_FILE" "$KAFKA_KEYSTORE_PASSWORD_FILE" \
        "$NACOS_KEYSTORE"
    # 各服务的独立证书副本；目录整体随 /certs 挂载进容器。
    mkdir -p "$RUNTIME_SERVICE_DIR"
    local svc client
    for svc in "${SERVICE_NAMES[@]}"; do
        cp -f "$SERVICE_DIR/$svc.pem" "$RUNTIME_SERVICE_DIR/$svc.pem"
        cp -f "$SERVICE_DIR/$svc-key.pem" "$RUNTIME_SERVICE_DIR/$svc-key.pem"
        chmod 644 "$RUNTIME_SERVICE_DIR/$svc.pem" "$RUNTIME_SERVICE_DIR/$svc-key.pem"
    done
    chmod 755 "$RUNTIME_SERVICE_DIR"
    mkdir -p "$RUNTIME_CLIENT_DIR"
    for client in "${CLIENT_NAMES[@]}"; do
        cp -f "$CLIENT_DIR/$client.pem" "$RUNTIME_CLIENT_DIR/$client.pem"
        cp -f "$CLIENT_DIR/$client-key.pem" "$RUNTIME_CLIENT_DIR/$client-key.pem"
        chmod 644 "$RUNTIME_CLIENT_DIR/$client.pem" "$RUNTIME_CLIENT_DIR/$client-key.pem"
    done
    for exporter in "${EXPORTER_NAMES[@]}"; do
        cp -f "$SERVICE_DIR/$exporter.pem" "$RUNTIME_SERVICE_DIR/$exporter.pem"
        cp -f "$SERVICE_DIR/$exporter-key.pem" "$RUNTIME_SERVICE_DIR/$exporter-key.pem"
        chmod 644 "$RUNTIME_SERVICE_DIR/$exporter.pem" "$RUNTIME_SERVICE_DIR/$exporter-key.pem"
    done
    chmod 755 "$RUNTIME_CLIENT_DIR"
    chmod 755 "$RUNTIME_DIR/ca"
    chmod 644 "$RUNTIME_DIR/ca/rootCA.pem"
}

check_expiry() {
    local svc client
    [ -f "$CERT_FILE" ] || fail "证书不存在：$CERT_FILE（先执行 scripts/tls-init.sh）"
    cert_has_purpose "$CERT_FILE" "SSL server" || fail "门户证书缺少 serverAuth 用途：$CERT_FILE"
    cert_has_purpose "$CERT_FILE" "SSL client" || fail "门户证书缺少 clientAuth 用途：$CERT_FILE（执行 scripts/tls-init.sh --force 重签）"
    cert_identity_matches "$CERT_FILE" "iboot-portal" "service/iboot-portal" || fail "门户证书 SPIFFE ID 与 CN 不一致：$CERT_FILE"
    if openssl x509 -checkend 2592000 -noout -in "$CERT_FILE" >/dev/null 2>&1; then
        log "证书有效：$CERT_FILE"
        openssl x509 -noout -enddate -in "$CERT_FILE" | sed 's/^/  /'
    else
        fail "证书剩余有效期不足 30 天：$CERT_FILE（执行 scripts/tls-init.sh --force 重签）"
    fi
    for svc in "${SERVICE_NAMES[@]}"; do
        [ -f "$SERVICE_DIR/$svc.pem" ] || fail "服务证书不存在：$SERVICE_DIR/$svc.pem（执行 scripts/tls-init.sh --force 重签）"
        cert_has_purpose "$SERVICE_DIR/$svc.pem" "SSL server" || fail "服务证书缺少 serverAuth 用途：$SERVICE_DIR/$svc.pem"
        cert_has_purpose "$SERVICE_DIR/$svc.pem" "SSL client" || fail "服务证书缺少 clientAuth 用途：$SERVICE_DIR/$svc.pem（执行 scripts/tls-init.sh --force 重签）"
        cert_identity_matches "$SERVICE_DIR/$svc.pem" "$svc" "service/$svc" || fail "服务证书 SPIFFE ID 与 CN 不一致：$SERVICE_DIR/$svc.pem"
        openssl x509 -checkend 2592000 -noout -in "$SERVICE_DIR/$svc.pem" >/dev/null 2>&1 \
            || fail "服务证书剩余有效期不足 30 天：$SERVICE_DIR/$svc.pem"
    done
    for client in "${CLIENT_NAMES[@]}"; do
        [ -f "$CLIENT_DIR/$client.pem" ] || fail "探针客户端证书不存在：$CLIENT_DIR/$client.pem（执行 scripts/tls-init.sh --force 重签）"
        cert_has_purpose "$CLIENT_DIR/$client.pem" "SSL client" || fail "探针客户端证书缺少 clientAuth 用途：$CLIENT_DIR/$client.pem"
        cert_identity_matches "$CLIENT_DIR/$client.pem" "$client" "client/$client" || fail "探针证书 SPIFFE ID 与 CN 不一致：$CLIENT_DIR/$client.pem"
        openssl x509 -checkend 2592000 -noout -in "$CLIENT_DIR/$client.pem" >/dev/null 2>&1 \
            || fail "探针客户端证书剩余有效期不足 30 天：$CLIENT_DIR/$client.pem"
    done
    for exporter in "${EXPORTER_NAMES[@]}"; do
        [ -f "$SERVICE_DIR/$exporter.pem" ] || fail "exporter 证书不存在：$SERVICE_DIR/$exporter.pem（执行 scripts/tls-init.sh --force 重签）"
        cert_has_purpose "$SERVICE_DIR/$exporter.pem" "SSL server" || fail "exporter 证书缺少 serverAuth 用途：$SERVICE_DIR/$exporter.pem"
        cert_identity_matches "$SERVICE_DIR/$exporter.pem" "$exporter" "service/$exporter" || fail "exporter 证书 SPIFFE ID 与 CN 不一致：$SERVICE_DIR/$exporter.pem"
        openssl x509 -checkend 2592000 -noout -in "$SERVICE_DIR/$exporter.pem" >/dev/null 2>&1 \
            || fail "exporter 证书剩余有效期不足 30 天：$SERVICE_DIR/$exporter.pem"
    done
    log "服务证书有效：${#SERVICE_NAMES[@]} 张（$SERVICE_DIR）"
    log "探针客户端证书有效：${#CLIENT_NAMES[@]} 张（$CLIENT_DIR）"
    log "exporter 证书有效：${#EXPORTER_NAMES[@]} 张（$SERVICE_DIR）"
}

if [ "$CHECK_ONLY" -eq 1 ]; then
    check_expiry
    exit 0
fi

# 首次生成时由 mkcert 创建本地 CA；之后叶子证书由 openssl 使用同一 CA 签发。
ensure_ca() {
    if [ -f "$ROOT_CA" ] && [ -f "$CA_KEY" ]; then
        return 0
    fi
    require_mkcert
    local tmp
    tmp="$(mktemp -d)"
    log "初始化本地 CA：$CAROOT"
    CAROOT="$CAROOT" "$MKCERT" \
        -cert-file "$tmp/bootstrap.pem" \
        -key-file "$tmp/bootstrap-key.pem" \
        localhost >/dev/null
    rm -rf "$tmp"
    [ -f "$ROOT_CA" ] && [ -f "$CA_KEY" ] || fail "本地 CA 初始化失败：$CAROOT"
}

# 用本地 CA 签发叶子证书；SAN 中的 IP 与 DNS 按字面值区分。
issue_leaf() {
    local name="$1" cert="$2" key="$3" eku="$4"
    shift 4
    local tmp entry san
    local -a entries=()
    for entry in "$@"; do
        case "$entry" in
            URI:*) entries+=("$entry") ;;
            *:*) entries+=("IP:$entry") ;;
            127.*) entries+=("IP:$entry") ;;
            *) entries+=("DNS:$entry") ;;
        esac
    done
    san="$(IFS=,; printf '%s' "${entries[*]}")"
    tmp="$(mktemp -d)"
    cat > "$tmp/ext.cnf" <<EOF
basicConstraints=CA:FALSE
keyUsage=critical,digitalSignature,keyEncipherment
extendedKeyUsage=$eku
subjectAltName=$san
subjectKeyIdentifier=hash
authorityKeyIdentifier=keyid,issuer
EOF
    openssl req -new -newkey rsa:2048 -nodes \
        -keyout "$key" -subj "/CN=$name" -out "$tmp/leaf.csr" >/dev/null 2>&1
    openssl x509 -req -in "$tmp/leaf.csr" \
        -CA "$ROOT_CA" -CAkey "$CA_KEY" \
        -CAcreateserial -CAserial "$CA_SERIAL" \
        -days "$CERT_DAYS" -sha256 -extfile "$tmp/ext.cnf" \
        -out "$cert" >/dev/null 2>&1
    rm -rf "$tmp"
    chmod 600 "$key"
    chmod 644 "$cert"
}

# 已有环境补齐 exporter 证书时只签发缺失项，避免无谓地重签全部服务证书。
ensure_ca
mkdir -p "$SERVICE_DIR"
for exporter in "${EXPORTER_NAMES[@]}"; do
    if [ ! -f "$SERVICE_DIR/$exporter.pem" ] || [ ! -f "$SERVICE_DIR/$exporter-key.pem" ]; then
        issue_leaf "$exporter" "$SERVICE_DIR/$exporter.pem" "$SERVICE_DIR/$exporter-key.pem" \
            "serverAuth" "URI:spiffe://$TRUST_DOMAIN/service/$exporter" \
            localhost 127.0.0.1 ::1 "$exporter"
    fi
done

if [ "$FORCE" -eq 0 ] && cert_valid; then
    ensure_runtime_copies
    log "证书仍然有效，跳过生成：$CERT_FILE"
    log "需要重签时执行：scripts/tls-init.sh --force"
    exit 0
fi

log "生成本地 CA 与门户证书：$CERT_DIR"
issue_leaf "iboot-portal" "$CERT_FILE" "$KEY_FILE" "serverAuth,clientAuth" \
    "URI:spiffe://$TRUST_DOMAIN/service/iboot-portal" "${SAN_NAMES[@]}"

mkdir -p "$SERVICE_DIR"
log "生成服务独立证书：$SERVICE_DIR"
for svc in "${SERVICE_NAMES[@]}"; do
    issue_leaf "$svc" "$SERVICE_DIR/$svc.pem" "$SERVICE_DIR/$svc-key.pem" \
        "serverAuth,clientAuth" "URI:spiffe://$TRUST_DOMAIN/service/$svc" \
        localhost 127.0.0.1 ::1 host.docker.internal "$svc"
done

log "生成监控 exporter 服务端证书：$SERVICE_DIR"
for exporter in "${EXPORTER_NAMES[@]}"; do
    issue_leaf "$exporter" "$SERVICE_DIR/$exporter.pem" "$SERVICE_DIR/$exporter-key.pem" \
        "serverAuth" "URI:spiffe://$TRUST_DOMAIN/service/$exporter" \
        localhost 127.0.0.1 ::1 "$exporter"
done

mkdir -p "$CLIENT_DIR"
log "生成探针客户端证书：$CLIENT_DIR"
for client in "${CLIENT_NAMES[@]}"; do
    issue_leaf "$client" "$CLIENT_DIR/$client.pem" "$CLIENT_DIR/$client-key.pem" \
        "clientAuth" "URI:spiffe://$TRUST_DOMAIN/client/$client" \
        localhost 127.0.0.1 ::1 "$client"
done

chmod 600 "$KEY_FILE" "$CA_KEY"
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
log "  服务证书   $SERVICE_DIR（每服务一张，serverAuth + clientAuth）"
log "  探针证书   $CLIENT_DIR（${CLIENT_NAMES[*]}）"
log "  Kafka keystore $KAFKA_KEYSTORE"
log "  Nacos keystore $NACOS_KEYSTORE"
log ""
log "浏览器信任（一次性，可能需要管理员权限）："
log "  CAROOT=$CAROOT $MKCERT -install"
