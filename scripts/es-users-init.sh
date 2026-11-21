#!/usr/bin/env bash
# 幂等创建 Elasticsearch 最小权限角色与账号。
#
# 由 compose 的一次性服务 es-users-init 执行；也可在宿主机开发时通过
# `docker compose run --rm es-users-init` 复用同一份脚本。
#
# 环境变量：ELASTIC_PASSWORD、ES_AI_PASSWORD、ES_NOTIFY_PASSWORD、
#          ES_MONITOR_PASSWORD、ES_LOGSTASH_PASSWORD、ES_GRAFANA_PASSWORD、
#          ES_EXPORTER_PASSWORD
#          ES_HOST / ES_PORT / ES_SCHEME

set -euo pipefail

ES_HOST="${ES_HOST:-elasticsearch}"
ES_PORT="${ES_PORT:-9200}"
ES_SCHEME="${ES_SCHEME:-https}"
ES_CACERT="${ES_CACERT:-/certs/rootCA.pem}"

: "${ELASTIC_PASSWORD:?ELASTIC_PASSWORD 必须在 .env 中设置}"
: "${ES_AI_PASSWORD:?ES_AI_PASSWORD 必须在 .env 中设置}"
: "${ES_NOTIFY_PASSWORD:?ES_NOTIFY_PASSWORD 必须在 .env 中设置}"
: "${ES_MONITOR_PASSWORD:?ES_MONITOR_PASSWORD 必须在 .env 中设置}"
: "${ES_LOGSTASH_PASSWORD:?ES_LOGSTASH_PASSWORD 必须在 .env 中设置}"
: "${ES_GRAFANA_PASSWORD:?ES_GRAFANA_PASSWORD 必须在 .env 中设置}"
: "${ES_EXPORTER_PASSWORD:?ES_EXPORTER_PASSWORD 必须在 .env 中设置}"

BASE="${ES_SCHEME}://${ES_HOST}:${ES_PORT}"
CURL=(curl -sS --cacert "$ES_CACERT" -u "elastic:${ELASTIC_PASSWORD}")

fail() {
    printf '[es-users-init] ERROR: %s\n' "$*" >&2
    exit 1
}

json_escape() {
    printf '%s' "$1" | sed 's/\\/\\\\/g; s/"/\\"/g'
}

put_role() {
    local role="$1" body="$2" code
    code="$("${CURL[@]}" -o /tmp/role.out -w '%{http_code}' -X PUT \
        -H 'Content-Type: application/json' -d "$body" "$BASE/_security/role/$role")"
    case "$code" in
        200|201) ;;
        *) fail "创建角色 $role 失败（HTTP $code）：$(cat /tmp/role.out)" ;;
    esac
}

put_user() {
    local user="$1" password="$2" role="$3" body code
    body="{\"password\":\"$(json_escape "$password")\",\"roles\":[\"$role\"]}"
    code="$("${CURL[@]}" -o /tmp/user.out -w '%{http_code}' -X PUT \
        -H 'Content-Type: application/json' -d "$body" "$BASE/_security/user/$user")"
    case "$code" in
        200|201) ;;
        *) fail "创建用户 $user 失败（HTTP $code）：$(cat /tmp/user.out)" ;;
    esac
}

# ES 健康检查通过后仍留一点余量，避免安全 API 尚未就绪。
for _ in $(seq 1 60); do
    if "${CURL[@]}" -o /dev/null "$BASE/_cluster/health" 2>/dev/null; then
        break
    fi
    sleep 2
done
"${CURL[@]}" -o /dev/null "$BASE/_cluster/health" 2>/dev/null \
    || fail "Elasticsearch 未就绪：$BASE"

put_role iboot_kb_rw '{
  "cluster": ["monitor"],
  "indices": [{
    "names": ["iboot-kb-chunk-*"],
    "privileges": ["read", "write", "create_index", "delete", "view_index_metadata"]
  }]
}'
put_role iboot_content_rw '{
  "cluster": ["monitor"],
  "indices": [{
    "names": ["iboot-content-*"],
    "privileges": ["read", "write", "create_index", "delete", "view_index_metadata"]
  }]
}'
put_role iboot_logs_reader '{
  "cluster": ["monitor"],
  "indices": [{
    "names": ["iboot-logs-*"],
    "privileges": ["read", "view_index_metadata"]
  }]
}'
put_role iboot_logs_writer '{
  "cluster": ["monitor", "manage_index_templates"],
  "indices": [{
    "names": ["iboot-logs-*"],
    "privileges": ["create_index", "index", "write", "view_index_metadata"]
  }]
}'
put_role iboot_metrics_reader '{
  "cluster": ["monitor"],
  "indices": [{
    "names": ["*"],
    "privileges": ["monitor", "view_index_metadata"]
  }]
}'

put_user iboot_ai "$ES_AI_PASSWORD" iboot_kb_rw
put_user iboot_notify "$ES_NOTIFY_PASSWORD" iboot_content_rw
put_user iboot_monitor "$ES_MONITOR_PASSWORD" iboot_logs_reader
put_user iboot_logstash "$ES_LOGSTASH_PASSWORD" iboot_logs_writer
put_user iboot_grafana "$ES_GRAFANA_PASSWORD" iboot_logs_reader
put_user iboot_exporter "$ES_EXPORTER_PASSWORD" iboot_metrics_reader

printf '[es-users-init] 账号已就绪：iboot_ai、iboot_notify、iboot_monitor、iboot_logstash、iboot_grafana、iboot_exporter\n'
