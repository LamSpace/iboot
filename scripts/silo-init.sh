#!/usr/bin/env sh
# 初始化 Silo 桶、文件/AI 服务账号与桶级策略。
#
# 由 compose 的一次性服务 silo-init 执行，也可在已启动的 Silo 容器内手动复用。
# 根账号只在本脚本与运维工具中使用，不注入业务服务。
#
# 环境变量：SILO_ROOT_USER、SILO_ROOT_PASSWORD、
#          SILO_FILE_ACCESS_KEY/SECRET_KEY、SILO_AI_ACCESS_KEY/SECRET_KEY

set -eu

SILO_ROOT_USER="${SILO_ROOT_USER:?SILO_ROOT_USER 必须在 .env 中设置}"
SILO_ROOT_PASSWORD="${SILO_ROOT_PASSWORD:?SILO_ROOT_PASSWORD 必须在 .env 中设置}"
SILO_FILE_ACCESS_KEY="${SILO_FILE_ACCESS_KEY:-iboot_file}"
SILO_FILE_SECRET_KEY="${SILO_FILE_SECRET_KEY:?SILO_FILE_SECRET_KEY 必须在 .env 中设置}"
SILO_JOB_ACCESS_KEY="${SILO_JOB_ACCESS_KEY:-iboot_job}"
SILO_JOB_SECRET_KEY="${SILO_JOB_SECRET_KEY:?SILO_JOB_SECRET_KEY 必须在 .env 中设置}"
SILO_AI_ACCESS_KEY="${SILO_AI_ACCESS_KEY:-iboot_ai}"
SILO_AI_SECRET_KEY="${SILO_AI_SECRET_KEY:?SILO_AI_SECRET_KEY 必须在 .env 中设置}"
FILE_BUCKET="${SILO_FILE_BUCKET:-iboot-files}"
AI_BUCKET="${SILO_AI_BUCKET:-iboot-ai}"

fail() {
    printf '[silo-init] ERROR: %s\n' "$*" >&2
    exit 1
}

mcli alias set local https://silo:9000 "$SILO_ROOT_USER" "$SILO_ROOT_PASSWORD" >/dev/null \
    || fail '根账号别名设置失败'

for bucket in thanos "$FILE_BUCKET" "$AI_BUCKET"; do
    mcli mb --ignore-existing "local/$bucket" >/dev/null || fail "创建桶失败：$bucket"
done

write_policy() {
    local policy="$1" bucket="$2"
    cat > "/tmp/$policy.json" <<EOF
{"Version":"2012-10-17","Statement":[{"Effect":"Allow","Action":["s3:GetBucketLocation","s3:ListBucket","s3:ListBucketMultipartUploads","s3:GetObject","s3:PutObject","s3:DeleteObject","s3:AbortMultipartUpload","s3:ListMultipartUploadParts"],"Resource":["arn:aws:s3:::$bucket","arn:aws:s3:::$bucket/*"]}]}
EOF
}

create_service_account() {
    local user="$1" secret="$2" policy="$3"
    write_policy "$policy" "$4"
    mcli admin user remove local "$user" >/dev/null 2>&1 || true
    mcli admin user add local "$user" "$secret" >/dev/null || fail "创建账号失败：$user"
    mcli admin policy remove local "$policy" >/dev/null 2>&1 || true
    mcli admin policy create local "$policy" "/tmp/$policy.json" >/dev/null \
        || fail "创建策略失败：$policy"
    mcli admin policy attach local "$policy" --user "$user" >/dev/null \
        || fail "附加策略失败：$user"
}

create_service_account "$SILO_FILE_ACCESS_KEY" "$SILO_FILE_SECRET_KEY" iboot-file-rw "$FILE_BUCKET"
create_service_account "$SILO_JOB_ACCESS_KEY" "$SILO_JOB_SECRET_KEY" iboot-job-rw "$FILE_BUCKET"
create_service_account "$SILO_AI_ACCESS_KEY" "$SILO_AI_SECRET_KEY" iboot-ai-rw "$AI_BUCKET"

printf '[silo-init] 桶与服务账号已就绪：%s、%s、%s\n' \
    "$SILO_FILE_ACCESS_KEY" "$SILO_JOB_ACCESS_KEY" "$SILO_AI_ACCESS_KEY"
