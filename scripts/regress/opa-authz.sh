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
OPA_IMAGE="openpolicyagent/opa:1.8.0"

opa_running() {
    docker ps --filter "ancestor=$OPA_IMAGE" --format '{{.Names}}'
}

opa_all() {
    docker ps -a --filter "ancestor=$OPA_IMAGE" --format '{{.Names}}'
}

wait_opa_lb() {
    for _ in $(seq 1 30); do
        curl -sf -m 2 "$OPA/health" >/dev/null 2>&1 && return 0
        sleep 1
    done
    return 1
}

if ! curl -sf -m 3 "$OPA/health" >/dev/null 2>&1; then
    reg_log "  [SKIP] OPA 未运行"
    reg_summary
    exit "$REG_FAILED"
fi
reg_pass "OPA 健康端点可用"

opa_container="$(opa_running | head -1)"
if [ -n "$opa_container" ] \
    && { docker exec "$opa_container" /opt/opa/opa test /policies >/tmp/opa-test.log 2>&1 \
        || docker exec "$opa_container" opa test /policies >/tmp/opa-test.log 2>&1; }; then
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
    service_pid="$(pgrep -f 'iboot-system-1\.0\.0\.jar' | head -1 || true)"
    if [ -n "$service_pid" ] \
        && tr '\0' '\n' < "/proc/$service_pid/environ" 2>/dev/null \
            | grep -q '^IBOOT_SERVICE_AUTHORIZATION_OPA_URL=..'; then
        service_opa="(host process)"
    fi
fi
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

    # 单副本故障：停一个副本，LB 仍应允许/拒绝。
    mapfile -t running_opa < <(opa_running)
    if [ "${#running_opa[@]}" -ge 2 ]; then
        docker stop "${running_opa[0]}" >/dev/null 2>&1 || true
        sleep 2
        allow_after="$(curl -sS -m 5 -X POST "$OPA/v1/data/iboot/authz/allow" \
            -H 'Content-Type: application/json' \
            -d '{"input":{"caller":"iboot-gateway","path":"/api/user/1","rules":[{"paths":["/api/**"],"callers":["iboot-gateway"]}]}}' \
            2>/dev/null || true)"
        if printf '%s' "$allow_after" | grep -q '"result":true'; then
            reg_pass "单个 OPA 副本故障时授权仍可用"
        else
            reg_fail "单个 OPA 副本故障时授权不可用（$allow_after）"
        fi
        docker start "${running_opa[0]}" >/dev/null 2>&1 || true
        wait_opa_lb || true
    else
        reg_log "  [SKIP] OPA 副本少于 2，跳过单副本故障用例"
    fi

    # 全部副本故障后必须失败关闭。
    mapfile -t all_opa < <(opa_all)
    if [ "${#all_opa[@]}" -gt 0 ]; then
        docker stop "${all_opa[@]}" >/dev/null 2>&1 || true
        sleep 1
        code="$(curl -sS -m 5 --cacert "$CA" --cert "$PORTAL_CERT" --key "$PORTAL_KEY" \
            -o /dev/null -w '%{http_code}' https://localhost:8080/actuator/health 2>/dev/null || true)"
        if [ "$code" = "403" ]; then
            reg_pass "全部 OPA 副本停止时请求失败关闭（403）"
        else
            reg_fail "全部 OPA 副本停止时请求未失败关闭（HTTP ${code:-无响应}）"
        fi
        docker start "${all_opa[@]}" >/dev/null 2>&1 || true
        wait_opa_lb || reg_fail "OPA 副本恢复后 LB 未就绪"
    fi
fi

reg_summary
exit "$REG_FAILED"
