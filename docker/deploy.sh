#!/bin/bash

# iBoot 一体化部署脚本
# 集成应用 + 监控环境一键启动

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "${SCRIPT_DIR}"

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

echo "========================================"
echo "       iBoot 一体化部署脚本"
echo "========================================"
echo ""

# 检查 Docker 和 Docker Compose
check_dependencies() {
    log_info "检查依赖..."

    if ! command -v docker &> /dev/null; then
        log_error "Docker 未安装，请先安装 Docker"
        exit 1
    fi

    if ! command -v docker-compose &> /dev/null; then
        log_error "Docker Compose 未安装，请先安装 Docker Compose"
        exit 1
    fi

    log_info "依赖检查通过"
}

# 创建必要的目录结构
create_directories() {
    log_info "创建目录结构..."
    mkdir -p ../observability/prometheus/alerts
    mkdir -p ../observability/grafana/provisioning/datasources
    mkdir -p ../observability/grafana/provisioning/dashboards
    mkdir -p ../observability/grafana/dashboards
    mkdir -p ../observability/alertmanager
    mkdir -p ../observability/elk/elasticsearch
    mkdir -p ../observability/elk/logstash
    mkdir -p ../iboot-admin/logs
}

# 构建后端
build_admin() {
    log_info "检查后端 JAR 文件..."
    if [ ! -f "../iboot-admin/target/iboot-admin-1.0.0.jar" ]; then
        log_warn "JAR 文件不存在，开始构建后端项目..."
        cd ../iboot-admin

        if command -v mvn &> /dev/null; then
            mvn clean package -DskipTests
        else
            log_error "Maven 未安装，无法构建后端"
            log_info "请手动运行：cd iboot-admin && mvn clean package -DskipTests"
            exit 1
        fi
        cd ../docker
    fi
    log_info "后端构建完成"
}

# 构建前端
build_portal() {
    log_info "检查前端 dist 目录..."
    if [ ! -d "../iboot-portal/dist" ]; then
        log_warn "dist 目录不存在，开始构建前端项目..."
        cd ../iboot-portal

        if command -v cnpm &> /dev/null; then
            cnpm install
            cnpm run build
        elif command -v npm &> /dev/null; then
            npm install
            npm run build
        else
            log_error "Node.js 未安装，无法构建前端"
            log_info "请手动运行：cd iboot-portal && npm run build"
            exit 1
        fi
        cd ../docker
    fi
    log_info "前端构建完成"
}

# 启动服务
start_services() {
    log_info "开始启动服务..."
    docker-compose up -d

    log_info "服务启动完成"
}

# 等待服务启动
wait_for_services() {
    log_info "等待服务启动..."
    sleep 30
}

# 查看服务状态
show_status() {
    log_info "服务状态:"
    docker-compose ps
}

# 主函数
main() {
    check_dependencies
    create_directories

    case "${1:-all}" in
        build-admin)
            build_admin
            ;;
        build-portal)
            build_portal
            ;;
        build)
            build_admin
            build_portal
            ;;
        start)
            start_services
            show_status
            ;;
        stop)
            log_info "停止服务..."
            docker-compose down
            ;;
        restart)
            log_info "重启服务..."
            docker-compose restart
            ;;
        status)
            show_status
            ;;
        logs)
            docker-compose logs -f "${2:-}"
            ;;
        all)
            build_admin
            build_portal
            start_services
            wait_for_services
            show_status
            echo ""
            echo "========================================"
            echo "  ✅ 部署完成！"
            echo "========================================"
            echo ""
            echo "  访问地址："
            echo "  =============================="
            echo "  应用前端：http://localhost:80"
            echo "  后端 API:  http://localhost:8080"
            echo "  Swagger:   http://localhost:8080/swagger-ui.html"
            echo "  Grafana:   http://localhost:3000 (admin/admin123)"
            echo "  Prometheus:http://localhost:9090"
            echo "  Kibana:    http://localhost:5601"
            echo "  MinIO:     http://localhost:9000 (minioadmin/minioadmin)"
            echo ""
            echo "  常用命令："
            echo "  =============================="
            echo "  查看日志：docker-compose logs -f [服务名]"
            echo "  停止服务：docker-compose down"
            echo "  重启服务：docker-compose restart"
            echo "  查看状态：docker-compose ps"
            echo ""
            ;;
        *)
            echo "用法：$0 {all|build-admin|build-portal|build|start|stop|restart|status|logs}"
            echo ""
            echo "命令说明:"
            echo "  all         - 构建并启动所有服务（默认）"
            echo "  build-admin - 仅构建后端"
            echo "  build-portal- 仅构建前端"
            echo "  build       - 构建前后端"
            echo "  start       - 启动服务"
            echo "  stop        - 停止服务"
            echo "  restart     - 重启服务"
            echo "  status      - 查看服务状态"
            echo "  logs        - 查看日志 [服务名]"
            exit 1
            ;;
    esac
}

main "$@"
