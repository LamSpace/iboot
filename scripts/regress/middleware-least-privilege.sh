#!/usr/bin/env bash
# 中间件表级/键级最小权限回归。
#
# 验证：MySQL 无库级 DML、跨服务写被拒绝、本服务写成功、DDL 被拒绝；
# Redis 只能访问登记前缀、共享会话前缀可用、monitor 宽权限例外已登记。

set -uo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$HERE/../.." && pwd)"
. "$HERE/lib.sh"

if [ -z "${MYSQL_FILE_PASSWORD:-}" ] && [ -f "$ROOT/.env" ]; then
    set -a
    # shellcheck disable=SC1091
    . "$ROOT/.env"
    set +a
fi

mysql_as() {
    local user="$1" password="$2" sql="$3" svc="${1#iboot_}"
    local -a cert_opts=()
    if [ -f "$ROOT/docker/certs/runtime/services/iboot-$svc.pem" ]; then
        cert_opts=(--ssl-cert="/certs/services/iboot-$svc.pem" --ssl-key="/certs/services/iboot-$svc-key.pem")
    fi
    docker exec -e MYSQL_PWD="$password" iboot-mysql \
        mysql -u"$user" -h 127.0.0.1 --ssl-mode=REQUIRED "${cert_opts[@]}" -N -B -D iboot -e "$sql" 2>&1
}

redis_as() {
    local user="$1" password="$2"
    shift 2
    docker exec -e REDISCLI_AUTH="$password" iboot-redis \
        redis-cli --tls --cacert /certs/ca/rootCA.pem \
        --cert /certs/clients/redis-exporter.pem --key /certs/clients/redis-exporter-key.pem \
        --no-auth-warning --user "$user" "$@" 2>&1
}

mysql_password_of() {
    local service="$1" var
    var="MYSQL_$(printf '%s' "$service" | tr '[:lower:]' '[:upper:]')_PASSWORD"
    printf '%s' "${!var:-}"
}

redis_password_of() {
    local service="$1" var
    var="REDIS_$(printf '%s' "$service" | tr '[:lower:]' '[:upper:]')_PASSWORD"
    printf '%s' "${!var:-}"
}

expect_mysql_ok() {
    local desc="$1" user="$2" sql="$3" out
    out="$(mysql_as "$user" "$(mysql_password_of "${user#iboot_}")" "$sql")"
    if printf '%s' "$out" | grep -qi '^ERROR'; then
        reg_fail "$desc（$out）"
    else
        reg_pass "$desc"
    fi
}

expect_mysql_denied() {
    local desc="$1" user="$2" sql="$3" out
    out="$(mysql_as "$user" "$(mysql_password_of "${user#iboot_}")" "$sql")"
    if printf '%s' "$out" | grep -qiE '^ERROR (1142|1044)|command denied|access denied'; then
        reg_pass "$desc"
    else
        reg_fail "$desc（未被拒绝：$out）"
    fi
}

reg_log "== 1. MySQL 库级 DML 已撤销 =="
for svc in system workflow file notify job monitor auth ai; do
    user="iboot_$svc"
    grants="$(mysql_as "$user" "$(mysql_password_of "$svc")" 'SHOW GRANTS FOR CURRENT_USER()')"
    if printf '%s' "$grants" | grep -Eq 'GRANT .*(INSERT|UPDATE|DELETE).* ON `iboot`\.\*'; then
        reg_fail "$user 仍拥有 iboot 库级 DML"
    else
        reg_pass "$user 没有 iboot 库级 DML"
    fi
done

reg_log "== 2. MySQL 跨服务写被拒绝 =="
expect_mysql_denied "file 不能写 sys_user" iboot_file "DELETE FROM sys_user WHERE 1=0"
expect_mysql_denied "ai 不能写 sys_file" iboot_ai "DELETE FROM sys_file WHERE 1=0"
expect_mysql_denied "workflow 不能写 sys_user" iboot_workflow "DELETE FROM sys_user WHERE 1=0"
expect_mysql_denied "monitor 不能写 sys_user" iboot_monitor "DELETE FROM sys_user WHERE 1=0"
expect_mysql_denied "auth 不能写 sys_role" iboot_auth "DELETE FROM sys_role WHERE 1=0"
expect_mysql_denied "file 不能删除审计记录" iboot_file "DELETE FROM sys_operate_log WHERE 1=0"

reg_log "== 3. MySQL 本服务写与共享读 =="
expect_mysql_ok "file 可写 sys_file" iboot_file "DELETE FROM sys_file WHERE 1=0"
expect_mysql_ok "workflow 可写 biz_leave_application" iboot_workflow "DELETE FROM biz_leave_application WHERE 1=0"
expect_mysql_ok "job 可写 sys_job" iboot_job "DELETE FROM sys_job WHERE 1=0"
expect_mysql_ok "notify 可写 sys_message" iboot_notify "DELETE FROM sys_message WHERE 1=0"
expect_mysql_ok "monitor 可清理审计表" iboot_monitor "DELETE FROM sys_operate_log WHERE 1=0"
expect_mysql_ok "auth 可写 sys_user_pat" iboot_auth "DELETE FROM sys_user_pat WHERE 1=0"
expect_mysql_ok "system 可写 sys_user" iboot_system "DELETE FROM sys_user WHERE 1=0"
expect_mysql_ok "file 可读共享 sys_user" iboot_file "SELECT COUNT(*) FROM sys_user"
expect_mysql_denied "file 不能执行 DDL" iboot_file "CREATE TABLE _least_privilege_deny (id INT)"

reg_log "== 4. Redis 键前缀 =="
ai_pw="$(redis_password_of ai)"
file_pw="$(redis_password_of file)"
notify_pw="$(redis_password_of notify)"
monitor_pw="$(redis_password_of monitor)"

out="$(redis_as iboot_ai "$ai_pw" SET iboot:ai:least-privilege-test 1)"
if [ "$out" = "OK" ]; then reg_pass "ai 可写 iboot:ai:*"; else reg_fail "ai 写本前缀失败（$out）"; fi
redis_as iboot_ai "$ai_pw" DEL iboot:ai:least-privilege-test >/dev/null 2>&1 || true

out="$(redis_as iboot_notify "$notify_pw" SET iboot:notify:least-privilege-test 1)"
if [ "$out" = "OK" ]; then reg_pass "notify 可写 iboot:notify:*"; else reg_fail "notify 写本前缀失败（$out）"; fi
redis_as iboot_notify "$notify_pw" DEL iboot:notify:least-privilege-test >/dev/null 2>&1 || true

out="$(redis_as iboot_ai "$ai_pw" SET iboot:notify:least-privilege-test 1)"
if printf '%s' "$out" | grep -q 'NOPERM'; then reg_pass "ai 不能写 iboot:notify:*"; else reg_fail "ai 越界写未被拒绝（$out）"; fi

out="$(redis_as iboot_file "$file_pw" SET pat_tokens:least-privilege-test 1)"
if printf '%s' "$out" | grep -q 'NOPERM'; then reg_pass "file 不能写 pat_tokens:*"; else reg_fail "file 可写 pat_tokens:*（$out）"; fi

out="$(redis_as iboot_file "$file_pw" GET login_tokens:least-privilege-test)"
if printf '%s' "$out" | grep -q 'NOPERM'; then reg_fail "file 不能读 shared login_tokens:*（$out）"; else reg_pass "file 可读共享 login_tokens:*"; fi

out="$(redis_as iboot_file "$file_pw" GET pat_tokens:least-privilege-test)"
if printf '%s' "$out" | grep -q 'NOPERM'; then reg_fail "file 不能读 shared pat_tokens:*（$out）"; else reg_pass "file 可读共享 pat_tokens:*"; fi

out="$(redis_as iboot_file "$file_pw" EXPIRE online_token:least-privilege-test 60)"
if printf '%s' "$out" | grep -q 'NOPERM'; then reg_fail "file 不能续期 shared online_token:*（$out）"; else reg_pass "file 可续期共享 online_token:*"; fi

out="$(redis_as iboot_file "$file_pw" SET login_retry:least-privilege-test 1)"
if printf '%s' "$out" | grep -q 'NOPERM'; then reg_pass "file 不能写 auth 专属 login_retry:*"; else reg_fail "file 可写 login_retry:*（$out）"; fi

out="$(redis_as iboot_monitor "$monitor_pw" SET least-privilege:arbitrary 1)"
if [ "$out" = "OK" ]; then reg_pass "monitor 保留键空间浏览例外"; else reg_fail "monitor 例外未生效（$out）"; fi
redis_as iboot_monitor "$monitor_pw" DEL least-privilege:arbitrary >/dev/null 2>&1 || true

reg_summary
exit "$REG_FAILED"
