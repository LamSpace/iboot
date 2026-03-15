#!/bin/bash

# iBoot 一键构建脚本
# 用于编译前端和后端应用
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

set -e  # 遇到错误立即退出

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 打印带颜色的消息
print_message() {
    case $1 in
        "info") echo -e "${BLUE}[INFO]${NC} $2" ;;
        "success") echo -e "${GREEN}[SUCCESS]${NC} $2" ;;
        "warning") echo -e "${YELLOW}[WARNING]${NC} $2" ;;
        "error") echo -e "${RED}[ERROR]${NC} $2" ;;
        *) echo "$2" ;;
    esac
}

# 检查命令是否存在
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# 检查环境依赖
check_dependencies() {
    print_message "info" "检查环境依赖..."
    
    if ! command_exists node; then
        print_message "error" "Node.js 未安装，请先安装 Node.js"
        exit 1
    fi
    
    if ! command_exists cnpm; then
        print_message "error" "cnpm 未安装，请先安装 cnpm"
        exit 1
    fi
    
    if ! command_exists mvn; then
        print_message "error" "Maven 未安装，请先安装 Maven"
        exit 1
    fi
    
    NODE_VERSION=$(node --version | cut -d'v' -f2)
    MIN_NODE_VERSION="16.0.0"
    
    if [[ $(printf '%s\n' "$MIN_NODE_VERSION" "$NODE_VERSION" | sort -V | head -n1) != "$MIN_NODE_VERSION" ]]; then
        print_message "error" "Node.js 版本过低，需要 $MIN_NODE_VERSION 或更高版本，当前版本: $NODE_VERSION"
        exit 1
    fi
    
    print_message "success" "环境依赖检查通过"
}

# 构建前端应用
build_frontend() {
    print_message "info" "开始构建前端应用..."
    
    cd ./iboot-portal
    
    # 检查 package.json 是否存在
    if [[ ! -f "package.json" ]]; then
        print_message "error" "package.json 不存在，请确保在正确的目录下"
        cd ..
        exit 1
    fi
    
    # 安装依赖
    print_message "info" "安装前端依赖..."
    cnpm install
    
    # 构建前端应用
    print_message "info" "构建前端应用..."
    cnpm run build
    
    print_message "success" "前端应用构建完成"
    cd ..
}

# 构建后端应用
build_backend() {
    print_message "info" "开始构建后端应用..."
    
    cd ./iboot-admin
    
    # 检查 pom.xml 是否存在
    if [[ ! -f "pom.xml" ]]; then
        print_message "error" "pom.xml 不存在，请确保在正确的目录下"
        cd ..
        exit 1
    fi
    
    # 清理并构建后端应用
    print_message "info" "构建后端应用..."
    mvn clean package -DskipTests
    
    print_message "success" "后端应用构建完成"
    cd ..
}

# 打印使用说明
show_usage() {
    echo "使用方法: $0 [选项]"
    echo "选项:"
    echo "  frontend    只构建前端应用"
    echo "  backend     只构建后端应用"
    echo "  all         构建前端和后端应用 (默认)"
    echo "  help        显示此帮助信息"
}

# 主函数
main() {
    print_message "info" "iBoot 一键构建脚本开始执行"
    print_message "info" "当前时间: $(date)"
    
    # 检查参数
    case ${1:-all} in
        "frontend")
            check_dependencies
            build_frontend
            ;;
        "backend")
            check_dependencies
            build_backend
            ;;
        "all")
            check_dependencies
            build_frontend
            build_backend
            ;;
        "help"|"-h"|"--help")
            show_usage
            exit 0
            ;;
        *)
            print_message "error" "未知参数: $1"
            show_usage
            exit 1
            ;;
    esac
    
    print_message "success" "所有构建任务已完成！"
    print_message "info" "构建完成时间: $(date)"
}

# 调用主函数，传入所有参数
main "$@"