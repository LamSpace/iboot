#!/usr/bin/env bash
# OPA 服务间授权回归：策略测试、HTTP 允许/拒绝、服务链路与 OPA 不可用时失败关闭。

set -uo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$HERE/../.." && pwd)"
. "$HERE/lib.sh"

CA="${REG_CACERT:-$ROOT/docker/certs/ca/rootCA.pem}"
PORTAL_CERT="$ROOT/docker/certs/portal.pem"
PORTAL_KEY="$ROOT/docker/certs/portal-key.pem"
OPA="http://localhost:8181"

if ! curl -sf -m 3 "$OPA/health" >/dev/null 2>&1; then
    reg_log "  [SKIP] OPA 未运行"
    reg_summary
    exit "$REG_FAILED"
fi
reg_pass "OPA 健康端点可用"

if docker exec iboot-opa /opt/opa/opa test /policies >/tmp/opa-test.log 2>&1 \
    || docker exec iboot-opa opa test /policies >/tmp/opa-test.log 2>&1; then
    reg_pass "OPA 策略单元测试通过"
else
    reg_fail "OPA 策略单元测试失败，详见 /tmp/opa-test.log"
fi

MAVEN_SETTINGS="${MAVEN_SETTINGS:-/home/lam/repo/settings.xml}"
MVN=(mvn)
[ -f "$MAVEN_SETTINGS" ] && MVN+=(-s "$MAVEN_SETTINGS")
if "${MVN[@]}" -q -pl iboot-service-authz -Dtest=ServiceAuthorizationOpaTest test >/tmp/opa-java-test.log 2>&1; then
    reg_pass "Java OPA 决策客户端测试通过"
else
    reg_fail "Java OPA 决策客户端测试失败，详见 /tmp/opa-java-test.log"
fi

allow="$(curl -sS -m 3 -X POST "$OPA/v1/data/iboot/authz/allow" \
    -H 'Content-Type: application/json' \
    -d '{"input":{"caller":"iboot-gateway","path":"/api/user/1","rules":[{"paths":["/api/**"],"callers":["iboot-gateway"]}]}}')"
if printf '%s' "$allow" | grep -q '"result":true'; then
    reg_pass "OPA 允许网关访问业务 API"
else
    reg_fail "OPA 未允许网关访问业务 API（$allow）"
fi

deny="$(curl -sS -m 3 -X POST "$OPA/v1/data/iboot/authz/allow" \
    -H 'Content-Type: application/json' \
    -d '{"input":{"caller":"iboot-file","path":"/api/user/1","rules":[{"paths":["/api/**"],"callers":["iboot-gateway"]}]}}')"
if printf '%s' "$deny" | grep -q '"result":false'; then
    reg_pass "OPA 拒绝文件服务访问业务 API"
else
    reg_fail "OPA 未拒绝文件服务访问业务 API（$deny）"
fi

service_opa="$(docker inspect -f '{{range .Config.Env}}{{println .}}{{end}}' iboot-system 2>/dev/null \
    | grep '^IBOOT_SERVICE_AUTHORIZATION_OPA_URL=' | cut -d= -f2- || true)"
if [ -z "$service_opa" ]; then
    service_opa="${IBOOT_SERVICE_AUTHORIZATION_OPA_URL:-}"
fi
if [ -z "$service_opa" ]; then
    reg_log "  [SKIP] iboot-system 未启用 OPA（本地镜像可能未重建）"
else
    # 服务实际链路：门户证书访问系统健康端点应允许，文件服务证书访问用户列表应 403。
    if curl -sf -m 5 --cacert "$CA" --cert "$PORTAL_CERT" --key "$PORTAL_KEY" \
        https://localhost:8080/actuator/health >/dev/null 2>&1; then
        reg_pass "OPA 启用时门户访问系统健康端点成功"
    else
        reg_fail "OPA 启用时门户访问系统健康端点失败"
    fi
    file_cert="$ROOT/docker/certs/services/iboot-file.pem"
    file_key="$ROOT/docker/certs/services/iboot-file-key.pem"
    code="$(curl -sS -m 5 --cacert "$CA" --cert "$file_cert" --key "$file_key" \
        -o /dev/null -w '%{http_code}' https://localhost:8080/api/user/list 2>/dev/null || true)"
    if [ "$code" = "403" ]; then
        reg_pass "OPA 启用时文件服务访问用户列表返回 403"
    else
        reg_fail "OPA 启用时文件服务访问用户列表未返回 403（HTTP ${code:-无响应}）"
    fi

    # OPA 停止后必须失败关闭。
    docker stop iboot-opa >/dev/null 2>&1 || true
    sleep 1
    code="$(curl -sS -m 5 --cacert "$CA" --cert "$PORTAL_CERT" --key "$PORTAL_KEY" \
        -o /dev/null -w '%{http_code}' https://localhost:8080/actuator/health 2>/dev/null || true)"
    if [ "$code" = "403" ]; then
        reg_pass "OPA 停止时请求失败关闭（403）"
    else
        reg_fail "OPA 停止时请求未失败关闭（HTTP ${code:-无响应}）"
    fi
    docker start iboot-opa >/dev/null 2>&1 || true
    for _ in $(seq 1 30); do
        curl -sf -m 2 "$OPA/health" >/dev/null 2>&1 && break
        sleep 1
    done
fi

reg_summary
exit "$REG_FAILED"
