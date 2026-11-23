#!/usr/bin/env bash
# RocketMQ 服务端证书校验回归：SAN、兼容层打包、正确/错误信任库与实际投递。
#
# 需要 workflow/notify 已构建；集成投递需要 RocketMQ TLS 入口与 workflow 凭据。

set -uo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$HERE/../.." && pwd)"
. "$HERE/lib.sh"

if [ -z "${ROCKETMQ_WORKFLOW_SECRET_KEY:-}" ] && [ -f "$ROOT/.env" ]; then
    set -a
    # shellcheck disable=SC1091
    . "$ROOT/.env"
    set +a
fi

CA="${REG_CACERT:-$ROOT/docker/certs/ca/rootCA.pem}"
TRUSTSTORE="$ROOT/docker/certs/truststore.p12"
CERT="$ROOT/docker/certs/portal.pem"
ENDPOINT="${IBOOT_ROCKETMQ_ENDPOINTS:-localhost:18081}"
MAVEN_SETTINGS="${MAVEN_SETTINGS:-/home/lam/repo/settings.xml}"
MVN=(mvn)
[ -f "$MAVEN_SETTINGS" ] && MVN+=(-s "$MAVEN_SETTINGS")
JAVA_HOME_REAL="$(java -XshowSettings:properties -version 2>&1 \
    | awk -F'= ' '/java.home =/{print $2; exit}')"
JDK_CACERTS="${JAVA_HOME_REAL:-/usr/lib/jvm/default-java}/lib/security/cacerts"

reg_log "== 1. 证书 SAN =="
if [ -f "$CERT" ] && openssl x509 -in "$CERT" -noout -checkhost rocketmq-tls >/dev/null 2>&1; then
    reg_pass "RocketMQ 服务端证书 SAN 覆盖 rocketmq-tls"
else
    reg_fail "RocketMQ 服务端证书缺少 rocketmq-tls SAN（执行 scripts/tls-init.sh --force）"
fi

reg_log "== 2. 兼容层打包 =="
for module in iboot-workflow iboot-notify; do
    jar="$(find "$ROOT/$module/target" -maxdepth 1 -name "$module-*.jar" ! -name '*-sources.jar' ! -name '*-javadoc.jar' 2>/dev/null | head -1)"
    if [ -z "$jar" ]; then
        reg_fail "$module fat jar 不存在，先执行 mvn package"
        continue
    fi
    listing="$(unzip -l "$jar" 2>/dev/null || true)"
    if printf '%s\n' "$listing" \
        | grep -q 'BOOT-INF/classes/org/apache/rocketmq/shaded/io/grpc/netty/shaded/io/netty/handler/ssl/util/InsecureTrustManagerFactory.class'; then
        reg_pass "$module 的 BOOT-INF/classes 包含信任管理器兼容层"
    else
        reg_fail "$module 的兼容层未进入 BOOT-INF/classes"
    fi
done

reg_log "== 3. 信任管理器单元测试 =="
if "${MVN[@]}" -q -pl iboot-workflow -Dtest=RocketMqServerTrustTest \
    -Diboot.certs.dir="$ROOT/docker/certs" \
    -DargLine="-Djavax.net.ssl.trustStore=$TRUSTSTORE -Djavax.net.ssl.trustStorePassword=changeit" \
    test >/tmp/rocketmq-trust-unit.log 2>&1; then
    reg_pass "本地 CA 证书被接受、自签名证书被拒绝"
else
    reg_fail "信任管理器单元测试失败，详见 /tmp/rocketmq-trust-unit.log"
fi

reg_log "== 4. 实际投递：正确 CA =="
if IBOOT_ROCKETMQ_INTEGRATION=true IBOOT_ROCKETMQ_EXPECT_FAILURE=false \
    IBOOT_ROCKETMQ_ENDPOINTS="$ENDPOINT" \
    "${MVN[@]}" -q -pl iboot-workflow -Dtest='RocketMqServerTrustTest#sendsMessageWhenConfigured' \
    -Diboot.certs.dir="$ROOT/docker/certs" \
    -DargLine="-Djavax.net.ssl.trustStore=$TRUSTSTORE -Djavax.net.ssl.trustStorePassword=changeit" \
    test >/tmp/rocketmq-trust-positive.log 2>&1; then
    reg_pass "使用本地 CA 信任库可向 $ENDPOINT 投递"
else
    reg_fail "使用本地 CA 信任库投递失败，详见 /tmp/rocketmq-trust-positive.log"
fi

reg_log "== 5. 实际投递：错误 CA =="
if [ ! -f "$JDK_CACERTS" ]; then
    reg_log "  [SKIP] 找不到 JDK cacerts：$JDK_CACERTS"
elif IBOOT_ROCKETMQ_INTEGRATION=true IBOOT_ROCKETMQ_EXPECT_FAILURE=true \
    IBOOT_ROCKETMQ_ENDPOINTS="$ENDPOINT" \
    "${MVN[@]}" -q -pl iboot-workflow -Dtest='RocketMqServerTrustTest#sendsMessageWhenConfigured' \
    -Diboot.certs.dir="$ROOT/docker/certs" \
    -DargLine="-Djavax.net.ssl.trustStore=$JDK_CACERTS -Djavax.net.ssl.trustStorePassword=changeit" \
    test >/tmp/rocketmq-trust-negative.log 2>&1; then
    reg_pass "使用不含本地 CA 的信任库时投递失败"
else
    reg_fail "错误 CA 场景仍投递成功（服务端证书校验未生效），详见 /tmp/rocketmq-trust-negative.log"
fi

reg_summary
exit "$REG_FAILED"
