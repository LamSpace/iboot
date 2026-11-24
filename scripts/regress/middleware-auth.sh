#!/usr/bin/env bash
# 中间件账号鉴权回归：每服务身份矩阵、最小权限、Redis ACL、Silo 桶级账号、Nacos 鉴权。
#
# 前置：中间件与服务已启动，根目录 .env 或环境变量提供受管凭据。
# 环境变量：MYSQL_ROOT_PASSWORD、MYSQL_MIGRATOR_PASSWORD、MYSQL_PROVISIONER_PASSWORD、
#          MYSQL_EXPORTER_PASSWORD、
#          MYSQL_/REDIS_/NACOS_<SERVICE>_PASSWORD（按组件独立）、
#          REDIS_EXPORTER_PASSWORD、KAFKA_*_PASSWORD、
#          SILO_ROOT_USER、SILO_ROOT_PASSWORD、SILO_FILE_*、SILO_AI_*、
#          NACOS_USERNAME、NACOS_PASSWORD、REG_CACERT。

set -uo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
. "$HERE/lib.sh"

ROOT="$(cd "$HERE/../.." && pwd)"
if [ -f "$ROOT/.env" ]; then
    set -a
    # shellcheck disable=SC1091
    . "$ROOT/.env"
    set +a
fi

CA="${REG_CACERT:-$ROOT/docker/certs/ca/rootCA.pem}"
DATA_SERVICES=(system workflow file notify job monitor auth ai)
ALL_SERVICES=(system workflow file notify job monitor auth ai gateway ops)
MYSQL_EXPORTER_USER="${MYSQL_EXPORTER_USER:-iboot_exporter}"
MYSQL_EXPORTER_PASSWORD="${MYSQL_EXPORTER_PASSWORD:-}"
MYSQL_MIGRATOR_PASSWORD="${MYSQL_MIGRATOR_PASSWORD:-}"
MYSQL_PROVISIONER_PASSWORD="${MYSQL_PROVISIONER_PASSWORD:-}"
REDIS_EXPORTER_PASSWORD="${REDIS_EXPORTER_PASSWORD:-}"
KAFKA_LOG_PASSWORD="${KAFKA_LOG_PASSWORD:-}"
KAFKA_LOGSTASH_PASSWORD="${KAFKA_LOGSTASH_PASSWORD:-}"
KAFKA_TOOLS_PASSWORD="${KAFKA_TOOLS_PASSWORD:-}"
SILO_ROOT_USER="${SILO_ROOT_USER:-}"
SILO_ROOT_PASSWORD="${SILO_ROOT_PASSWORD:-}"
SILO_FILE_ACCESS_KEY="${SILO_FILE_ACCESS_KEY:-iboot_file}"
SILO_FILE_SECRET_KEY="${SILO_FILE_SECRET_KEY:-}"
SILO_JOB_ACCESS_KEY="${SILO_JOB_ACCESS_KEY:-iboot_job}"
SILO_JOB_SECRET_KEY="${SILO_JOB_SECRET_KEY:-}"
SILO_AI_ACCESS_KEY="${SILO_AI_ACCESS_KEY:-iboot_ai}"
SILO_AI_SECRET_KEY="${SILO_AI_SECRET_KEY:-}"
NACOS_USERNAME="${NACOS_USERNAME:-}"
NACOS_PASSWORD="${NACOS_PASSWORD:-}"
NEO4J_PASSWORD="${NEO4J_PASSWORD:-}"
ES_AI_PASSWORD="${ES_AI_PASSWORD:-}"
ES_NOTIFY_PASSWORD="${ES_NOTIFY_PASSWORD:-}"
ES_MONITOR_PASSWORD="${ES_MONITOR_PASSWORD:-}"
ES_LOGSTASH_PASSWORD="${ES_LOGSTASH_PASSWORD:-}"
ES_GRAFANA_PASSWORD="${ES_GRAFANA_PASSWORD:-}"
ES_EXPORTER_PASSWORD="${ES_EXPORTER_PASSWORD:-}"

mysql_exec() {
    local user="$1" password="$2" sql="$3" svc="${1#iboot_}"
    local -a cert_opts=()
    if [ -f "$ROOT/docker/certs/runtime/services/iboot-$svc.pem" ]; then
        cert_opts=(--ssl-cert="/certs/services/iboot-$svc.pem" --ssl-key="/certs/services/iboot-$svc-key.pem")
    fi
    docker exec -e MYSQL_PWD="$password" iboot-mysql mysql \
        --protocol=TCP --ssl-mode=VERIFY_IDENTITY --ssl-ca=/certs/rootCA.pem \
        "${cert_opts[@]}" -h127.0.0.1 -u"$user" -e "$sql" 2>/dev/null
}

mysql_root_exec() {
    local sql="$1"
    docker exec -e MYSQL_PWD="$MYSQL_ROOT_PASSWORD" iboot-mysql mysql \
        --protocol=TCP --ssl-mode=VERIFY_IDENTITY --ssl-ca=/certs/rootCA.pem \
        -h127.0.0.1 -uroot -N -e "$sql" 2>/dev/null
}

service_account() {
    printf 'iboot_%s' "$1"
}

service_mysql_password() {
    local var="MYSQL_$(printf '%s' "$1" | tr '[:lower:]' '[:upper:]')_PASSWORD"
    printf '%s' "${!var:-}"
}

service_redis_password() {
    local var="REDIS_$(printf '%s' "$1" | tr '[:lower:]' '[:upper:]')_PASSWORD"
    printf '%s' "${!var:-}"
}

service_nacos_password() {
    local var="NACOS_$(printf '%s' "$1" | tr '[:lower:]' '[:upper:]')_PASSWORD"
    printf '%s' "${!var:-}"
}

# 读取服务进程实际收到的环境变量；compose 模式看容器，宿主机模式看 /proc。
service_env() {
    local name="iboot-$1"
    if docker inspect "$name" >/dev/null 2>&1; then
        docker inspect -f '{{range .Config.Env}}{{println .}}{{end}}' "$name" 2>/dev/null || true
        return
    fi
    local pid
    pid="$(pgrep -f "^java .*$name-1\\.0\\.0\\.jar" | head -1 || true)"
    [ -n "$pid" ] && tr '\0' '\n' < "/proc/$pid/environ" 2>/dev/null || true
}

ES_CA=/usr/share/elasticsearch/config/certs/rootCA.pem
es_status() {
    local user="$1" password="$2" method="$3" path="$4"
    docker exec iboot-elasticsearch \
        curl -sS --cacert "$ES_CA" -u "$user:$password" -X "$method" \
        -o /dev/null -w '%{http_code}' "https://localhost:9200$path" 2>/dev/null
}

reg_log "== 1. MySQL 每服务身份与最小权限 =="
for service in "${DATA_SERVICES[@]}"; do
    acct="$(service_account "$service")"
    password="$(service_mysql_password "$service")"
    if mysql_exec "$acct" "$password" "SELECT 1" >/dev/null; then
        reg_pass "MySQL 账号 $acct 可以连接并查询"
    else
        reg_fail "MySQL 账号 $acct 无法连接"
    fi
done
for service in "${DATA_SERVICES[@]}"; do
    mysql_pw="$(service_mysql_password "$service")"
    redis_pw="$(service_redis_password "$service")"
    nacos_pw="$(service_nacos_password "$service")"
    if [ -n "$mysql_pw" ] && [ -n "$redis_pw" ] && [ -n "$nacos_pw" ] \
        && [ "$mysql_pw" != "$redis_pw" ] && [ "$mysql_pw" != "$nacos_pw" ] \
        && [ "$redis_pw" != "$nacos_pw" ]; then
        reg_pass "$service 的 MySQL/Redis/Nacos 口令互不相同"
    else
        reg_fail "$service 的 MySQL/Redis/Nacos 口令未独立"
    fi
done

first_acct="$(service_account "${DATA_SERVICES[0]}")"
first_password="$(service_mysql_password "${DATA_SERVICES[0]}")"
if mysql_exec "$first_acct" "$first_password" \
    "CREATE TABLE iboot.__auth_probe__ (id INT)" >/dev/null; then
    reg_fail "业务账号可以执行 DDL"
    mysql_exec "$first_acct" "$first_password" "DROP TABLE IF EXISTS iboot.__auth_probe__" >/dev/null || true
else
    reg_pass "业务账号执行 DDL 被拒绝"
fi
if mysql_exec "$MYSQL_EXPORTER_USER" "$MYSQL_EXPORTER_PASSWORD" \
    "CREATE TABLE iboot.__exporter_probe__ (id INT)" >/dev/null; then
    reg_fail "exporter 账号可以写入业务库"
    mysql_exec "$MYSQL_EXPORTER_USER" "$MYSQL_EXPORTER_PASSWORD" "DROP TABLE IF EXISTS iboot.__exporter_probe__" >/dev/null || true
else
    reg_pass "exporter 账号不能写入业务库"
fi
if mysql_exec "$first_acct" "wrong-password" "SELECT 1" >/dev/null; then
    reg_fail "MySQL 错误口令仍可连接"
else
    reg_pass "MySQL 错误口令被拒绝"
fi
if mysql_exec "$first_acct" "$(service_redis_password "${DATA_SERVICES[0]}")" "SELECT 1" >/dev/null; then
    reg_fail "MySQL 接受 Redis 组件口令"
else
    reg_pass "MySQL 拒绝 Redis 组件口令"
fi

# 从中间件侧取证：连接列表必须出现八个服务账号，且不能回退到共享账号。
mysql_users="$(mysql_root_exec "SELECT DISTINCT user FROM information_schema.processlist WHERE user LIKE 'iboot\\_%' ORDER BY user" | tr -d '\r')"
for service in "${DATA_SERVICES[@]}"; do
    acct="$(service_account "$service")"
    if printf '%s\n' "$mysql_users" | grep -qx "$acct"; then
        reg_pass "MySQL 连接列表包含 $acct"
    else
        reg_fail "MySQL 连接列表缺少 $acct"
    fi
done
if printf '%s\n' "$mysql_users" | grep -qx "iboot_app"; then
    reg_fail "服务仍以共享账号 iboot_app 连接 MySQL"
else
    reg_pass "MySQL 无共享账号 iboot_app 连接"
fi
if [ "$(mysql_root_exec "SELECT COUNT(*) FROM mysql.user WHERE user='iboot_app'")" = "0" ]; then
    reg_pass "共享 MySQL 账号 iboot_app 已删除"
else
    reg_fail "共享 MySQL 账号 iboot_app 仍存在"
fi

reg_log "== 1b. MySQL 管理面 =="
if mysql_root_exec "SELECT 1" >/dev/null; then
    reg_pass "容器内 localhost root 可用于首次引导"
else
    reg_fail "容器内 localhost root 不可用"
fi

mysql_container_ip="$(docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}} {{end}}' iboot-mysql 2>/dev/null | awk '{print $1}')"
if [ -n "$mysql_container_ip" ] && docker exec -e MYSQL_PWD="$MYSQL_ROOT_PASSWORD" iboot-mysql mysql \
    --protocol=TCP --connect-timeout=5 --ssl-mode=VERIFY_CA --ssl-ca=/certs/rootCA.pem \
    -h "$mysql_container_ip" -uroot -e "SELECT 1" >/dev/null 2>&1; then
    reg_fail "root 可从非回环地址登录 MySQL"
else
    reg_pass "root 从非回环地址登录被拒绝"
fi

if mysql_exec iboot_migrator "$MYSQL_MIGRATOR_PASSWORD" \
    "CREATE TABLE iboot.__migrator_probe__ (id INT)" >/dev/null; then
    reg_pass "迁移账号可以执行 DDL"
    mysql_exec iboot_migrator "$MYSQL_MIGRATOR_PASSWORD" \
        "DROP TABLE iboot.__migrator_probe__" >/dev/null || true
else
    reg_fail "迁移账号无法执行 DDL"
fi
if mysql_exec iboot_migrator "$MYSQL_MIGRATOR_PASSWORD" \
    "CREATE USER 'iboot_migrator_probe'@'%' IDENTIFIED BY 'probe'" >/dev/null; then
    reg_fail "迁移账号可以创建账号"
else
    reg_pass "迁移账号创建账号被拒绝"
fi

if mysql_exec iboot_provisioner "$MYSQL_PROVISIONER_PASSWORD" \
    "CREATE USER 'iboot_provisioner_probe'@'%' IDENTIFIED BY 'probe'; GRANT SELECT ON iboot.sys_user TO 'iboot_provisioner_probe'@'%'" >/dev/null; then
    reg_pass "管理账号可以创建并授权服务账号"
else
    reg_fail "管理账号无法创建并授权服务账号"
fi
if mysql_exec iboot_provisioner "$MYSQL_PROVISIONER_PASSWORD" \
    "GRANT ALL PRIVILEGES ON *.* TO 'iboot_provisioner_probe'@'%'" >/dev/null 2>&1; then
    reg_fail "管理账号可以授予全局管理权限"
else
    reg_pass "管理账号授予全局管理权限被拒绝"
fi
if mysql_exec iboot_provisioner "$MYSQL_PROVISIONER_PASSWORD" \
    "GRANT FILE ON *.* TO 'iboot_provisioner_probe'@'%'" >/dev/null 2>&1; then
    reg_fail "管理账号可以授予 FILE 权限"
else
    reg_pass "管理账号授予 FILE 权限被拒绝"
fi
if mysql_exec iboot_provisioner "$MYSQL_PROVISIONER_PASSWORD" \
    "CREATE TABLE iboot.__provisioner_probe__ (id INT)" >/dev/null 2>&1; then
    reg_fail "管理账号可以执行 DDL"
else
    reg_pass "管理账号执行 DDL 被拒绝"
fi
mysql_root_exec "DROP USER IF EXISTS 'iboot_migrator_probe'@'%'; DROP USER IF EXISTS 'iboot_provisioner_probe'@'%'; DROP TABLE IF EXISTS iboot.__migrator_probe__; DROP TABLE IF EXISTS iboot.__provisioner_probe__" >/dev/null 2>&1 || true

if docker compose -f "$ROOT/docker/docker-compose.yml" config --format json 2>/dev/null | python3 -c '
import json, sys
services = json.load(sys.stdin)["services"]
def env(name):
    return services.get(name, {}).get("environment") or {}
assert env("mysql").get("MYSQL_ROOT_HOST") == "localhost", "mysql MYSQL_ROOT_HOST"
for name in ("db-init", "mysql-users-init", "mysql-grants-init"):
    assert "MYSQL_ROOT_PASSWORD" not in env(name), name + " 仍注入 root 口令"
assert env("db-init").get("MYSQL_USERNAME") == "iboot_migrator", "db-init 未使用迁移账号"
for name in ("mysql-users-init", "mysql-grants-init"):
    assert env(name).get("MYSQL_ADMIN_USER") == "iboot_provisioner", name + " 未使用管理账号"
'; then
    reg_pass "初始化容器不再注入 root，且分别使用迁移/管理账号"
else
    reg_fail "初始化容器凭据注入不符合最小权限"
fi

redis_exec() {
    local user="$1" password="$2"
    shift 2
    docker exec iboot-redis redis-cli --tls --cacert /certs/rootCA.pem -p 6379 \
        --cert /certs/clients/redis-exporter.pem --key /certs/clients/redis-exporter-key.pem \
        --user "$user" --pass "$password" "$@" 2>/dev/null
}

reg_log "== 2. Redis 每服务 ACL 身份 =="
for service in "${DATA_SERVICES[@]}"; do
    acct="$(service_account "$service")"
    password="$(service_redis_password "$service")"
    if redis_exec "$acct" "$password" ping | grep -q PONG; then
        reg_pass "Redis 账号 $acct PING 成功"
    else
        reg_fail "Redis 账号 $acct PING 失败"
    fi
done
if docker exec iboot-redis redis-cli --tls --cacert /certs/rootCA.pem -p 6379 ping 2>/dev/null | grep -q PONG; then
    reg_fail "Redis 未认证仍可执行命令"
else
    reg_pass "Redis 未认证被拒绝"
fi
if redis_exec default "$(service_redis_password "${DATA_SERVICES[0]}")" ping | grep -q PONG; then
    reg_fail "Redis default 用户仍可登录"
else
    reg_pass "Redis default 用户被禁用"
fi
if redis_exec "$first_acct" "wrong-password" ping | grep -q PONG; then
    reg_fail "Redis 错误口令仍可执行命令"
else
    reg_pass "Redis 错误口令被拒绝"
fi
if redis_exec "$first_acct" "$(service_mysql_password "${DATA_SERVICES[0]}")" ping | grep -q PONG; then
    reg_fail "Redis 接受 MySQL 组件口令"
else
    reg_pass "Redis 拒绝 MySQL 组件口令"
fi
# 触发各服务的健康检查，让惰性连接池建立 Redis 连接后再取证。
for entry in "system:8080" "ai:8081" "file:8082" "auth:8083" "notify:8084" \
    "job:8085" "workflow:8086" "monitor:8087" "gateway:8088" "ops:8089"; do
    port="${entry##*:}"
    reg_curl -o /dev/null "https://localhost:$port/actuator/health" >/dev/null 2>&1 || true
done
sleep 1
redis_clients="$(redis_exec iboot_exporter "$REDIS_EXPORTER_PASSWORD" client list || true)"
for service in "${DATA_SERVICES[@]}"; do
    acct="$(service_account "$service")"
    if printf '%s\n' "$redis_clients" | grep -q "user=$acct "; then
        reg_pass "Redis 连接列表包含 $acct"
    else
        reg_fail "Redis 连接列表缺少 $acct"
    fi
done
if printf '%s\n' "$redis_clients" | grep -q "user=iboot_app "; then
    reg_fail "服务仍以共享账号 iboot_app 连接 Redis"
else
    reg_pass "Redis 无共享账号 iboot_app 连接"
fi
if redis_exec iboot_exporter "$REDIS_EXPORTER_PASSWORD" info server | grep -qi redis_version; then
    reg_pass "Redis exporter 账号可读取 INFO"
else
    reg_fail "Redis exporter 账号无法读取 INFO"
fi
if redis_exec iboot_exporter "$REDIS_EXPORTER_PASSWORD" set __exporter_probe__ 1 | grep -q OK; then
    redis_exec iboot_exporter "$REDIS_EXPORTER_PASSWORD" del __exporter_probe__ >/dev/null || true
    reg_fail "Redis exporter 账号可以写入"
else
    reg_pass "Redis exporter 账号不能写入"
fi

reg_log "== 3. Silo 凭据与桶级身份 =="
if docker exec -e SILO_USER="$SILO_ROOT_USER" -e SILO_PASS="$SILO_ROOT_PASSWORD" iboot-silo \
    sh -c 'mcli alias set good https://127.0.0.1:9000 "$SILO_USER" "$SILO_PASS" >/dev/null && mcli ls good >/dev/null' 2>/dev/null; then
    reg_pass "Silo 根凭据可列举 bucket"
else
    reg_fail "Silo 根凭据无法访问"
fi
if docker exec iboot-silo sh -c \
    'mcli alias set bad https://127.0.0.1:9000 minioadmin minioadmin >/dev/null 2>&1 && mcli ls bad >/dev/null 2>&1'; then
    reg_fail "Silo 默认凭据 minioadmin 仍可访问"
else
    reg_pass "Silo 默认凭据被拒绝"
fi
silo_users="$(docker exec -e SILO_USER="$SILO_ROOT_USER" -e SILO_PASS="$SILO_ROOT_PASSWORD" iboot-silo \
    sh -c 'mcli alias set local https://127.0.0.1:9000 "$SILO_USER" "$SILO_PASS" >/dev/null && mcli admin user list local' 2>/dev/null || true)"
file_key="${SILO_FILE_ACCESS_KEY:-iboot_file}"
job_key="${SILO_JOB_ACCESS_KEY:-iboot_job}"
ai_key="${SILO_AI_ACCESS_KEY:-iboot_ai}"
for key in "$file_key" "$job_key" "$ai_key"; do
    if printf '%s\n' "$silo_users" | grep -qw "$key"; then
        reg_pass "Silo 服务账号 $key 存在"
    else
        reg_fail "Silo 服务账号 $key 不存在"
    fi
done
file_bucket="${IBOOT_FILE_SILO_BUCKET:-iboot-files}"
ai_bucket="${IBOOT_AI_SILO_BUCKET:-iboot-ai}"
if docker exec -e KEY="$file_key" -e SECRET="$SILO_FILE_SECRET_KEY" -e BUCKET="$file_bucket" iboot-silo \
    sh -c 'mcli alias set svc https://127.0.0.1:9000 "$KEY" "$SECRET" >/dev/null && mcli ls "svc/$BUCKET" >/dev/null' 2>/dev/null; then
    reg_pass "文件服务账号可访问自己的桶 $file_bucket"
else
    reg_fail "文件服务账号无法访问自己的桶 $file_bucket"
fi
if docker exec -e KEY="$file_key" -e SECRET="$SILO_FILE_SECRET_KEY" -e BUCKET="$ai_bucket" iboot-silo \
    sh -c 'mcli alias set svc https://127.0.0.1:9000 "$KEY" "$SECRET" >/dev/null && mcli ls "svc/$BUCKET" >/dev/null' 2>/dev/null; then
    reg_fail "文件服务账号可以读取 AI 桶 $ai_bucket"
else
    reg_pass "文件服务账号不能读取 AI 桶"
fi
if docker exec -e KEY="$ai_key" -e SECRET="$SILO_AI_SECRET_KEY" -e BUCKET="$file_bucket" iboot-silo \
    sh -c 'mcli alias set svc https://127.0.0.1:9000 "$KEY" "$SECRET" >/dev/null && mcli ls "svc/$BUCKET" >/dev/null' 2>/dev/null; then
    reg_fail "AI 服务账号可以读取文件桶 $file_bucket"
else
    reg_pass "AI 服务账号不能读取文件桶"
fi
if docker exec -e KEY="$job_key" -e SECRET="$SILO_JOB_SECRET_KEY" -e BUCKET="$file_bucket" iboot-silo \
    sh -c 'mcli alias set svc https://127.0.0.1:9000 "$KEY" "$SECRET" >/dev/null && mcli ls "svc/$BUCKET" >/dev/null' 2>/dev/null; then
    reg_pass "备份任务账号可访问文件桶 $file_bucket"
else
    reg_fail "备份任务账号无法访问文件桶 $file_bucket"
fi
if docker exec -e KEY="$job_key" -e SECRET="$SILO_JOB_SECRET_KEY" -e BUCKET="$ai_bucket" iboot-silo \
    sh -c 'mcli alias set svc https://127.0.0.1:9000 "$KEY" "$SECRET" >/dev/null && mcli ls "svc/$BUCKET" >/dev/null' 2>/dev/null; then
    reg_fail "备份任务账号可以读取 AI 桶 $ai_bucket"
else
    reg_pass "备份任务账号不能读取 AI 桶"
fi

reg_log "== 4. Nacos 每服务身份与鉴权 =="
for service in "${ALL_SERVICES[@]}"; do
    acct="$(service_account "$service")"
    password="$(service_nacos_password "$service")"
    login_resp="$(curl -sS --cacert "$CA" -X POST \
        "https://localhost:8848/nacos/v3/auth/user/login" \
        --data-urlencode "username=$acct" \
        --data-urlencode "password=$password" 2>/dev/null || true)"
    token="$(printf '%s' "$login_resp" | python3 -c 'import json,sys; print(json.load(sys.stdin).get("accessToken",""))' 2>/dev/null || true)"
    if [ -n "$token" ]; then
        status="$(curl -sS --cacert "$CA" -o /dev/null -w '%{http_code}' \
            -H "Authorization: Bearer $token" \
            "https://localhost:8848/nacos/v3/client/cs/config?dataId=__iboot_probe__&groupName=DEFAULT_GROUP&namespaceId=public" 2>/dev/null || true)"
        case "$status" in
            200|404) reg_pass "Nacos 账号 $acct 可登录并读取配置" ;;
            *) reg_fail "Nacos 账号 $acct 无法读取配置（HTTP $status）" ;;
        esac
    else
        reg_fail "Nacos 账号 $acct 登录失败"
    fi
done
login_resp="$(curl -sS --cacert "$CA" -X POST \
    "https://localhost:8848/nacos/v3/auth/user/login" \
    --data-urlencode "username=$NACOS_USERNAME" \
    --data-urlencode "password=$NACOS_PASSWORD" 2>/dev/null || true)"
if printf '%s' "$login_resp" | grep -q accessToken; then
    reg_pass "Nacos 运维账号登录成功"
else
    reg_fail "Nacos 运维账号登录失败"
fi
if curl -sS --cacert "$CA" -X POST \
    "https://localhost:8848/nacos/v3/auth/user/login" \
    --data-urlencode "username=iboot_system" \
    --data-urlencode "password=wrong-password" 2>/dev/null | grep -q accessToken; then
    reg_fail "Nacos 错误口令仍可登录"
else
    reg_pass "Nacos 错误口令被拒绝"
fi
if curl -sS --cacert "$CA" -X POST \
    "https://localhost:8848/nacos/v3/auth/user/login" \
    --data-urlencode "username=iboot_system" \
    --data-urlencode "password=$(service_redis_password system)" 2>/dev/null | grep -q accessToken; then
    reg_fail "Nacos 接受 Redis 组件口令"
else
    reg_pass "Nacos 拒绝 Redis 组件口令"
fi
anon_status="$(curl -sS --cacert "$CA" -o /dev/null -w '%{http_code}' -X POST \
    "https://localhost:8848/nacos/v3/admin/cs/config" \
    --data-urlencode "dataId=__auth_probe__" \
    --data-urlencode "groupName=DEFAULT_GROUP" \
    --data-urlencode "namespaceId=public" \
    --data-urlencode "content=x" --data-urlencode "type=json" 2>/dev/null || true)"
if [ "$anon_status" = "401" ] || [ "$anon_status" = "403" ]; then
    reg_pass "Nacos 匿名管理请求被拒绝（HTTP $anon_status）"
else
    reg_fail "Nacos 匿名管理请求未被拒绝（HTTP ${anon_status:-无响应}）"
fi
if env -u NACOS_USERNAME -u NACOS_PASSWORD "$ROOT/scripts/sentinel-init.sh" >/dev/null 2>&1; then
    reg_fail "sentinel-init 缺少凭据仍成功"
else
    reg_pass "sentinel-init 缺少凭据时失败"
fi

reg_log "== 4b. 服务进程环境隔离 =="
for service in "${ALL_SERVICES[@]}"; do
    env_dump="$(service_env "$service")"
    if [ -z "$env_dump" ]; then
        reg_log "  [SKIP] iboot-$service 未运行（不参与身份矩阵）"
        continue
    fi
    acct="$(service_account "$service")"
    if printf '%s\n' "$env_dump" | grep -qx "NACOS_USERNAME=$acct"; then
        reg_pass "iboot-$service 使用 Nacos 身份 $acct"
    else
        reg_fail "iboot-$service 未使用自己的 Nacos 身份"
    fi
    if printf '%s\n' "$env_dump" | grep -qx "NACOS_PASSWORD=$(service_nacos_password "$service")"; then
        reg_pass "iboot-$service 使用自己的 Nacos 组件口令"
    else
        reg_fail "iboot-$service 的 Nacos 组件口令不正确"
    fi
    case " ${DATA_SERVICES[*]} " in
        *" $service "*)
            if printf '%s\n' "$env_dump" | grep -qx "MYSQL_USERNAME=$acct" \
                && printf '%s\n' "$env_dump" | grep -qx "REDIS_USERNAME=$acct"; then
                reg_pass "iboot-$service 使用自己的 MySQL/Redis 身份 $acct"
            else
                reg_fail "iboot-$service 未使用自己的 MySQL/Redis 身份"
            fi
            if printf '%s\n' "$env_dump" | grep -qx "MYSQL_PASSWORD=$(service_mysql_password "$service")" \
                && printf '%s\n' "$env_dump" | grep -qx "REDIS_PASSWORD=$(service_redis_password "$service")"; then
                reg_pass "iboot-$service 使用独立的 MySQL/Redis 组件口令"
            else
                reg_fail "iboot-$service 的 MySQL/Redis 组件口令不正确"
            fi
            ;;
    esac
    if printf '%s\n' "$env_dump" | grep -qE '^MIDDLEWARE_[A-Z]+_PASSWORD=..'; then
        reg_fail "iboot-$service 仍持有其它服务的中间件口令"
    else
        reg_pass "iboot-$service 只持有自己的中间件口令"
    fi
    if printf '%s\n' "$env_dump" | grep -qE '^(SILO_ROOT_PASSWORD|MYSQL_ROOT_PASSWORD|NACOS_ADMIN_PASSWORD)=..'; then
        reg_fail "iboot-$service 仍持有根或管理口令"
    else
        reg_pass "iboot-$service 不持有根或管理口令"
    fi
done

reg_log "== 5. Elasticsearch 最小权限 =="
if [ "$(es_status iboot_ai "$ES_AI_PASSWORD" GET '/iboot-kb-chunk-*/_search')" = "200" ]; then
    reg_pass "AI 账号可读取知识库索引"
else
    reg_fail "AI 账号无法读取知识库索引"
fi
if [ "$(es_status iboot_ai "$ES_AI_PASSWORD" GET '/_security/user')" = "403" ]; then
    reg_pass "AI 账号不能访问安全接口"
else
    reg_fail "AI 账号越权访问安全接口"
fi
if [ "$(es_status iboot_ai "$ES_AI_PASSWORD" PUT '/iboot-kb-chunk-auth-probe')" = "200" ]; then
    reg_pass "AI 账号可创建知识库索引"
    es_status elastic "$ELASTIC_PASSWORD" DELETE '/iboot-kb-chunk-auth-probe' >/dev/null
else
    reg_fail "AI 账号无法创建知识库索引"
fi
if [ "$(es_status iboot_notify "$ES_NOTIFY_PASSWORD" PUT '/iboot-content-auth-probe')" = "200" ]; then
    reg_pass "通知账号可创建内容索引"
    es_status elastic "$ELASTIC_PASSWORD" DELETE '/iboot-content-auth-probe' >/dev/null
else
    reg_fail "通知账号无法创建内容索引"
fi
if [ "$(es_status iboot_monitor "$ES_MONITOR_PASSWORD" GET '/iboot-logs-*/_search')" = "200" ]; then
    reg_pass "监控账号可读取日志索引"
else
    reg_fail "监控账号无法读取日志索引"
fi
if [ "$(es_status iboot_monitor "$ES_MONITOR_PASSWORD" PUT '/iboot-logs-auth-probe')" = "403" ]; then
    reg_pass "监控账号不能写入日志索引"
else
    reg_fail "监控账号越权写入日志索引"
fi
if [ "$(es_status iboot_grafana "$ES_GRAFANA_PASSWORD" GET '/iboot-logs-*/_search')" = "200" ]; then
    reg_pass "Grafana 账号可读取日志索引"
else
    reg_fail "Grafana 账号无法读取日志索引"
fi
if [ "$(es_status iboot_logstash "$ES_LOGSTASH_PASSWORD" PUT '/iboot-logs-auth-probe')" = "200" ]; then
    reg_pass "Logstash 账号可写入日志索引"
    es_status elastic "$ELASTIC_PASSWORD" DELETE '/iboot-logs-auth-probe' >/dev/null
else
    reg_fail "Logstash 账号无法写入日志索引"
fi
if [ "$(es_status iboot_exporter "$ES_EXPORTER_PASSWORD" GET '/_cluster/health')" = "200" ]; then
    reg_pass "ES exporter 账号可读取集群健康"
else
    reg_fail "ES exporter 账号无法读取集群健康"
fi
if [ "$(es_status iboot_exporter "$ES_EXPORTER_PASSWORD" PUT '/iboot-exporter-auth-probe')" = "403" ]; then
    reg_pass "ES exporter 账号不能创建索引"
else
    reg_fail "ES exporter 账号越权创建索引"
fi
if [ "$(es_status iboot_monitor wrong-password GET '/_cluster/health')" = "401" ]; then
    reg_pass "ES 错误口令被拒绝"
else
    reg_fail "ES 错误口令未被拒绝"
fi
ai_env=""
if docker inspect iboot-ai >/dev/null 2>&1; then
    ai_env="$(docker inspect -f '{{range .Config.Env}}{{println .}}{{end}}' iboot-ai 2>/dev/null || true)"
else
    ai_pid="$(pgrep -f '^java .*iboot-ai-1\.0\.0\.jar' | head -1 || true)"
    [ -n "$ai_pid" ] && ai_env="$(tr '\0' '\n' < "/proc/$ai_pid/environ" 2>/dev/null || true)"
fi
if printf '%s' "$ai_env" | grep -q '^ELASTIC_PASSWORD=..'; then
    reg_fail "业务服务仍注入了 elastic 超级用户口令"
else
    reg_pass "业务服务没有 elastic 超级用户口令"
fi

reg_log "== 6. Neo4j 凭据 =="
neo4j_running="$(docker inspect -f '{{.State.Running}}' iboot-neo4j 2>/dev/null || true)"
if [ "$neo4j_running" = "true" ]; then
    # cypher-shell 没有 CA 参数；+ssc 表示加密并接受自签证书，CA 校验由内部 TLS 回归覆盖。
    if docker exec iboot-neo4j /var/lib/neo4j/bin/cypher-shell \
        -a bolt+ssc://localhost:7687 -u neo4j -p "$NEO4J_PASSWORD" "RETURN 1" >/dev/null 2>&1; then
        reg_pass "Neo4j 受管口令可连接"
    else
        reg_fail "Neo4j 受管口令无法连接"
    fi
    if docker exec iboot-neo4j /var/lib/neo4j/bin/cypher-shell \
        -a bolt+ssc://localhost:7687 -u neo4j -p iboot123456 "RETURN 1" >/dev/null 2>&1; then
        reg_fail "Neo4j 旧默认口令仍可连接"
    else
        reg_pass "Neo4j 旧默认口令被拒绝"
    fi
else
    reg_log "  [SKIP] Neo4j 未运行（graph profile 未启用）"
fi

reg_log "== 7. Kafka SASL 与授权 =="
kafka_client_config() {
    local user="$1" password="$2"
    {
        printf 'security.protocol=SASL_SSL\n'
        printf 'sasl.mechanism=PLAIN\n'
        printf 'sasl.jaas.config=org.apache.kafka.common.security.plain.PlainLoginModule required username="%s" password="%s";\n' "$user" "$password"
        printf 'ssl.truststore.location=/etc/kafka/secrets/truststore.p12\n'
        printf 'ssl.truststore.password=changeit\n'
        printf 'ssl.truststore.type=PKCS12\n'
        printf 'ssl.endpoint.identification.algorithm=HTTPS\n'
    } | docker exec -i iboot-kafka sh -c 'cat > /tmp/iboot-regress-client.properties'
}

kafka_topics() {
    timeout -k 3 40 docker exec iboot-kafka /opt/kafka/bin/kafka-topics.sh \
        --bootstrap-server localhost:9092 \
        --command-config /tmp/iboot-regress-client.properties "$@" 2>/dev/null
}

kafka_broker_versions() {
    timeout -k 3 15 docker exec iboot-kafka /opt/kafka/bin/kafka-broker-api-versions.sh \
        --bootstrap-server localhost:9092 \
        --command-config /tmp/iboot-regress-client.properties 2>/dev/null
}

kafka_client_config iboot_tools "$KAFKA_TOOLS_PASSWORD"
if kafka_topics --list | grep -qx iboot-logs; then
    reg_pass "Kafka 运维账号可列出日志 topic"
else
    reg_fail "Kafka 运维账号无法列出日志 topic"
fi

kafka_client_config iboot_log "$KAFKA_LOG_PASSWORD"
if kafka_topics --create --if-not-exists --topic __auth_probe__ --partitions 1 --replication-factor 1 >/dev/null 2>&1; then
    reg_fail "Kafka 业务日志账号可以创建 topic"
    kafka_client_config iboot_tools "$KAFKA_TOOLS_PASSWORD"
    kafka_topics --delete --topic __auth_probe__ >/dev/null 2>&1 || true
else
    reg_pass "Kafka 业务日志账号不能创建 topic"
fi

kafka_client_config iboot_logstash "$KAFKA_LOGSTASH_PASSWORD"
group_out="$(docker exec iboot-kafka /opt/kafka/bin/kafka-consumer-groups.sh \
    --bootstrap-server localhost:9092 \
    --command-config /tmp/iboot-regress-client.properties \
    --describe --group iboot-logstash 2>&1 || true)"
if printf '%s' "$group_out" | grep -q "iboot-logs"; then
    reg_pass "Kafka 消费账号可查看日志消费组"
else
    reg_fail "Kafka 消费账号无法查看日志消费组：$(printf '%s' "$group_out" | head -1)"
fi

kafka_client_config iboot_log "$KAFKA_LOG_PASSWORD"
consumer_out="$(docker exec iboot-kafka timeout 15 /opt/kafka/bin/kafka-console-consumer.sh \
    --bootstrap-server localhost:9092 \
    --command-config /tmp/iboot-regress-client.properties \
    --topic iboot-logs --from-beginning --max-messages 1 2>&1 || true)"
if printf '%s' "$consumer_out" | grep -qE "TopicAuthorizationException|GroupAuthorizationException"; then
    reg_pass "Kafka 业务日志账号不能消费日志 topic"
else
    reg_fail "Kafka 业务日志账号可以消费日志 topic"
fi

{
    printf 'security.protocol=SSL\n'
    printf 'ssl.truststore.location=/etc/kafka/secrets/truststore.p12\n'
    printf 'ssl.truststore.password=changeit\n'
    printf 'ssl.truststore.type=PKCS12\n'
    printf 'ssl.endpoint.identification.algorithm=HTTPS\n'
} | docker exec -i iboot-kafka sh -c 'cat > /tmp/iboot-regress-client.properties'
if kafka_broker_versions | grep -q .; then
    reg_fail "Kafka 无 SASL 凭据仍可访问"
else
    reg_pass "Kafka 无 SASL 凭据被拒绝"
fi

kafka_client_config iboot_log "wrong-password"
if kafka_broker_versions | grep -q .; then
    reg_fail "Kafka 错误 SASL 口令仍可访问"
else
    reg_pass "Kafka 错误 SASL 口令被拒绝"
fi

reg_summary
