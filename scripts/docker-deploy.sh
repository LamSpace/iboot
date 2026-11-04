#!/usr/bin/env bash
#
# iBoot 全栈 Docker 部署入口。
#
# 该脚本把多阶段构建与 compose 启动串成一条可重复的链路：
#   1. 停止宿主机的开发进程，避免端口与 fat jar 冲突；
#   2. 确保构建/运行期基础镜像已从镜像站拉取；
#   3. 构建共享 Maven builder 镜像（整个 reactor 只构建一次）；
#   4. 构建全部运行期镜像；
#   5. 启动 compose 并等待健康。
#
# 用法：
#   docker-deploy.sh up                 启动核心 + 可观测性组件（默认）
#   docker-deploy.sh up core            只启动核心组件
#   docker-deploy.sh up graph           核心 + Neo4j
#   docker-deploy.sh up full            核心 + 可观测性 + Neo4j
#   docker-deploy.sh status             查看状态
#   docker-deploy.sh logs [service]     查看日志
#   docker-deploy.sh down               停止并移除容器，保留数据卷
#   docker-deploy.sh purge --yes        停止并删除数据卷（不可恢复）
#
# 说明：
# - 构建与运行期基础镜像来自 scripts/pull-docker-images.sh 配置的 SWR 镜像站；
# - 根目录 .env 会被加载，用于 compose 插值（如 NEO4J_PASSWORD）；
# - 应用容器的环境变量由 docker-compose.yml 的 env_file 注入。

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

COMPOSE=(docker compose -f docker/docker-compose.yml)
BASE_IMAGES=(
  "maven:3.9.14-eclipse-temurin-25"
  "eclipse-temurin:25-jre"
  "node:22-bookworm-slim"
  "nginx:stable-alpine3.23"
)

log()  { printf '[docker-deploy] %s\n' "$*"; }
fail() { printf '[docker-deploy] ERROR: %s\n' "$*" >&2; exit 1; }

# 加载根目录 .env，供 compose 插值与脚本本身使用。
if [ -f "$ROOT/.env" ]; then
  set -a
  # shellcheck disable=SC1091
  . "$ROOT/.env"
  set +a
fi

require_docker() {
  command -v docker >/dev/null 2>&1 || fail "未找到 docker"
  docker compose version >/dev/null 2>&1 || fail "未找到 docker compose 插件"
}

stop_host_services() {
  if [ -x "$ROOT/scripts/dev-init.sh" ]; then
    log "停止宿主机开发进程（如果存在）..."
    "$ROOT/scripts/dev-init.sh" --stop >/dev/null 2>&1 || true
  fi
}

ensure_base_images() {
  local missing=0 image
  for image in "${BASE_IMAGES[@]}"; do
    if ! docker image inspect "$image" >/dev/null 2>&1; then
      log "缺少基础镜像：$image"
      missing=1
    fi
  done
  if [ "$missing" -eq 1 ]; then
    log "执行基础镜像拉取脚本..."
    "$ROOT/scripts/pull-docker-images.sh" || fail "基础镜像拉取失败"
  fi
}

build_images() {
  log "构建共享后端 builder 镜像（一次 Maven reactor）..."
  "${COMPOSE[@]}" --profile build build iboot-backend-builder \
    || fail "builder 镜像构建失败"

  log "构建全部运行期镜像..."
  "${COMPOSE[@]}" build || fail "运行期镜像构建失败"
}

profiles_for() {
  case "${1:-core}" in
    core)          PROFILE_ARGS=() ;;
    observability) PROFILE_ARGS=(--profile observability) ;;
    graph)         PROFILE_ARGS=(--profile graph) ;;
    full)          PROFILE_ARGS=(--profile observability --profile graph) ;;
    *) fail "未知启动范围：$1（可选 core|observability|graph|full）" ;;
  esac
}

compose_ps_lines() {
  "${COMPOSE[@]}" "${PROFILE_ARGS[@]}" ps -a \
    --format '{{.Name}}|{{.Service}}|{{.State}}|{{.Health}}' 2>/dev/null || true
}

report_unhealthy() {
  printf '[docker-deploy] 等待服务健康超时，当前状态：\n' >&2
  "${COMPOSE[@]}" "${PROFILE_ARGS[@]}" ps -a >&2 || true
  local name service state health
  while IFS='|' read -r name service state health; do
    [ -n "$service" ] || continue
    case "$service" in
      db-init|minio-init) continue ;;
    esac
    if [ "$state" != "running" ] || { [ -n "$health" ] && [ "$health" != "healthy" ]; }; then
      printf '\n--- %s（%s，health=%s）最近日志 ---\n' "$name" "$state" "${health:-none}" >&2
      docker logs --tail=60 "$name" >&2 2>/dev/null || true
    fi
  done < <(compose_ps_lines)
}

# 等待常驻服务健康，并确认一次性服务以 0 退出。
# 不使用 compose up --wait：它会把 db-init / minio-init 这类正常退出的一次性服务判为失败。
wait_ready() {
  local timeout="${1:-600}"
  local deadline=$((SECONDS + timeout))
  local name service state health exitcode all_ok
  while [ "$SECONDS" -lt "$deadline" ]; do
    all_ok=1
    while IFS='|' read -r name service state health; do
      [ -n "$service" ] || continue
      case "$service" in
        db-init|minio-init)
          if [ "$state" = "exited" ]; then
            exitcode=$(docker inspect -f '{{.State.ExitCode}}' "$name" 2>/dev/null || echo 1)
            if [ "$exitcode" != "0" ]; then
              printf '[docker-deploy] 一次性服务 %s 失败（exit %s）\n' "$name" "$exitcode" >&2
              docker logs --tail=80 "$name" >&2 2>/dev/null || true
              return 1
            fi
          elif [ "$state" != "running" ]; then
            all_ok=0
          fi
          ;;
        *)
          if [ "$state" != "running" ]; then
            all_ok=0
          elif [ -n "$health" ] && [ "$health" != "healthy" ]; then
            all_ok=0
          fi
          ;;
      esac
    done < <(compose_ps_lines)
    if [ "$all_ok" -eq 1 ]; then
      return 0
    fi
    sleep 5
  done
  report_unhealthy
  return 1
}

up() {
  local scope="${1:-observability}"
  profiles_for "$scope"
  require_docker
  [ -f "$ROOT/.env" ] || fail "缺少根目录 .env，请从 .env.example 复制并填写后重试"
  stop_host_services
  ensure_base_images
  build_images

  log "启动服务（范围：$scope）..."
  "${COMPOSE[@]}" "${PROFILE_ARGS[@]}" up -d \
    || fail "容器创建/启动失败，请先查看：${COMPOSE[*]} ps"
  log "等待服务健康（最多 600 秒）..."
  wait_ready 600 || fail "服务未在预期时间内达到健康"
  log "服务已就绪。"
  "${COMPOSE[@]}" "${PROFILE_ARGS[@]}" ps
}

status() {
  require_docker
  "${COMPOSE[@]}" ps
}

logs() {
  require_docker
  if [ -n "${1:-}" ]; then
    "${COMPOSE[@]}" logs -f --tail=200 "$1"
  else
    "${COMPOSE[@]}" logs -f --tail=200
  fi
}

down() {
  require_docker
  log "停止并移除容器，数据卷保留..."
  "${COMPOSE[@]}" --profile observability --profile graph down --remove-orphans
}

purge() {
  require_docker
  if [ "${1:-}" != "--yes" ]; then
    fail "purge 会删除所有数据卷，确认请执行：$0 purge --yes"
  fi
  log "停止并删除容器与数据卷（不可恢复）..."
  "${COMPOSE[@]}" --profile observability --profile graph down -v --remove-orphans
}

usage() {
  awk 'NR>1 && /^#/ { sub(/^# ?/, ""); print; next } NR>1 { exit }' "$0"
}

case "${1:-up}" in
  up)      shift || true; up "${1:-core}" ;;
  status)  status ;;
  logs)    shift || true; logs "${1:-}" ;;
  down)    down ;;
  purge)   shift || true; purge "${1:-}" ;;
  -h|--help|help) usage ;;
  *) usage; exit 2 ;;
esac
