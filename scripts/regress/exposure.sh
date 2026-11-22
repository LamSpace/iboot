#!/usr/bin/env bash
# 暴露面回归：宿主机端口绑定、非回环可达性与容器网络连通矩阵。
#
# 三层检查：
#   1. 只有门户 80/443 绑定 0.0.0.0；其余已发布端口只绑定 127.0.0.1。
#   2. 宿主机非回环地址不能访问 MySQL/Redis/ES/Nacos/Kafka 的宿主端口。
#   3. Docker 全量部署下，门户不能访问数据面，业务服务可以访问其依赖。
#
# 前置：证书与容器已按当前模式启动。宿主机开发模式只运行部分容器，
# 容器网络矩阵会明确跳过，但端口绑定检查仍执行。
#
# 环境变量：REG_CACERT（与其它回归脚本一致，本脚本不发起 HTTPS 请求）。

set -uo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=lib.sh
. "$HERE/lib.sh"

command -v jq >/dev/null 2>&1 || reg_die "缺少 jq，无法解析容器端口绑定"

REMOVED_PORTS="5044 17128 17913 9100 9104 9114 9121 19191 19193 19194"
DATA_PORTS="3306 6379 9200 8848 29092"

have_docker=false
if command -v docker >/dev/null 2>&1 && docker info >/dev/null 2>&1; then
    have_docker=true
fi

reg_log "== 1. 宿主机端口绑定 =="
containers=""
if [ "$have_docker" = true ]; then
    containers="$(docker ps --filter 'name=iboot-' --format '{{.Names}}' 2>/dev/null || true)"
fi
if [ -z "$containers" ]; then
    reg_log "  没有运行中的 iboot 容器，跳过端口绑定检查"
else
    while IFS= read -r name; do
        [ -n "$name" ] || continue
        bindings="$(docker inspect -f '{{json .HostConfig.PortBindings}}' "$name" 2>/dev/null || true)"
        [ -n "$bindings" ] && [ "$bindings" != "null" ] || continue
        while IFS='|' read -r host_port host_ip; do
            [ -n "$host_port" ] || continue
            case " $REMOVED_PORTS " in
                *" $host_port "*)
                    reg_fail "$name 仍发布已移除的端口 $host_port"
                    continue
                    ;;
            esac
            if [ "$name" = "iboot-portal" ] && { [ "$host_port" = "80" ] || [ "$host_port" = "443" ]; }; then
                case "$host_ip" in
                    ""|"0.0.0.0"|"::") reg_pass "门户端口 $host_port 按预期绑定全部网卡" ;;
                    *) reg_fail "门户端口 $host_port 未绑定全部网卡（$host_ip）" ;;
                esac
            elif [ "$host_ip" = "127.0.0.1" ]; then
                reg_pass "$name 的端口 $host_port 只绑定回环地址"
            else
                reg_fail "$name 的端口 $host_port 绑定到 ${host_ip:-全部网卡}，应为 127.0.0.1"
            fi
        done < <(printf '%s' "$bindings" | jq -r 'to_entries[] | .value[]? | "\(.HostPort // "")|\(.HostIp // "")"')
    done <<< "$containers"
fi

reg_log "== 2. 非回环地址不可达 =="
# 优先取默认路由的源地址，避免把 docker0 之类的网桥地址当成宿主机 LAN 地址。
host_ip="$(ip -4 route get 1.1.1.1 2>/dev/null | awk '{for (i=1;i<=NF;i++) if ($i=="src") {print $(i+1); exit}}')"
[ -n "$host_ip" ] || host_ip="$(ip -4 addr show scope global 2>/dev/null | awk '/inet /{print $2; exit}' | cut -d/ -f1)"
[ -n "$host_ip" ] || host_ip="$(hostname -I 2>/dev/null | awk '{print $1}')"
if [ -z "$host_ip" ]; then
    reg_log "  找不到宿主机非回环 IPv4，跳过本组检查"
else
    reg_log "  使用宿主机地址：$host_ip"
    for port in $DATA_PORTS; do
        if ! timeout 3 bash -c "exec 3<>/dev/tcp/127.0.0.1/$port" 2>/dev/null; then
            reg_log "  端口 $port 未在回环地址监听，跳过"
            continue
        fi
        if timeout 3 bash -c "exec 3<>/dev/tcp/$host_ip/$port" 2>/dev/null; then
            reg_fail "端口 $port 可通过非回环地址 $host_ip 访问"
        else
            reg_pass "端口 $port 仅回环可达，$host_ip 被拒绝"
        fi
    done
fi

reg_log "== 3. 容器网络矩阵 =="
if [ "$have_docker" != true ] || ! docker inspect iboot-portal >/dev/null 2>&1; then
    reg_log "  iboot-portal 未运行（宿主机开发模式），跳过容器网络矩阵"
else
    for target in "iboot-gateway 8088" "iboot-system 8080"; do
        set -- $target
        if docker exec iboot-portal nc -z -w 3 "$1" "$2" >/dev/null 2>&1; then
            reg_pass "门户可访问 $1:$2"
        else
            reg_fail "门户无法访问 $1:$2"
        fi
    done
    if docker inspect iboot-ops >/dev/null 2>&1; then
        if docker exec iboot-portal nc -z -w 3 iboot-ops 8089 >/dev/null 2>&1; then
            reg_pass "门户可访问 iboot-ops:8089"
        else
            reg_fail "门户无法访问 iboot-ops:8089"
        fi
    fi

    for target in "mysql 3306" "redis 6379" "elasticsearch 9200" "nacos 8848"; do
        set -- $target
        if docker exec iboot-portal nc -z -w 3 "$1" "$2" >/dev/null 2>&1; then
            reg_fail "门户仍可访问数据面 $1:$2（网络分段失效）"
        else
            reg_pass "门户无法访问数据面 $1:$2"
        fi
    done

    if docker inspect iboot-gateway >/dev/null 2>&1; then
        for target in "mysql 3306" "nacos 8848"; do
            set -- $target
            if docker exec iboot-gateway bash -c "exec 3<>/dev/tcp/$1/$2" >/dev/null 2>&1; then
                reg_pass "业务服务可访问依赖 $1:$2"
            else
                reg_fail "业务服务无法访问依赖 $1:$2"
            fi
        done
    fi
fi

reg_summary
