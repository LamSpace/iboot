#!/bin/bash

# iBoot 依赖安装脚本
# 用于安装项目所需的命令行工具
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

# 安装 Node.js (如果尚未安装)
install_nodejs() {
    print_message "info" "检查 Node.js 安装状态..."
    
    if command_exists node; then
        NODE_VERSION=$(node --version | cut -d'v' -f2)
        print_message "info" "Node.js 已安装，版本: $NODE_VERSION"
        return 0
    fi
    
    print_message "info" "正在安装 Node.js..."
    
    # 检测操作系统
    if [[ "$OSTYPE" == "linux-gnu"* ]]; then
        # Linux 系统
        if command_exists apt-get; then
            # Ubuntu/Debian
            sudo apt-get update
            sudo apt-get install -y nodejs npm
        elif command_exists yum; then
            # CentOS/RHEL
            sudo yum install -y nodejs npm
        elif command_exists dnf; then
            # Fedora
            sudo dnf install -y nodejs npm
        else
            print_message "error" "无法检测到支持的包管理器，请手动安装 Node.js"
            exit 1
        fi
    elif [[ "$OSTYPE" == "darwin"* ]]; then
        # macOS
        if command_exists brew; then
            brew install node
        else
            print_message "error" "请先安装 Homebrew，然后运行此脚本"
            exit 1
        fi
    else
        print_message "error" "不支持的操作系统: $OSTYPE"
        exit 1
    fi
    
    # 验证安装
    if command_exists node; then
        NODE_VERSION=$(node --version | cut -d'v' -f2)
        print_message "success" "Node.js 安装成功，版本: $NODE_VERSION"
    else
        print_message "error" "Node.js 安装失败"
        exit 1
    fi
}

# 安装 cnpm (如果尚未安装)
install_cnpm() {
    print_message "info" "检查 cnpm 安装状态..."
    
    if command_exists cnpm; then
        print_message "info" "cnpm 已安装"
        return 0
    fi
    
    print_message "info" "正在安装 cnpm..."
    
    # 使用 npm 安装 cnpm
    npm install -g cnpm --registry=https://registry.npmmirror.com
    
    # 验证安装
    if command_exists cnpm; then
        print_message "success" "cnpm 安装成功"
    else
        print_message "error" "cnpm 安装失败"
        exit 1
    fi
}

# 安装 Maven (如果尚未安装)
install_maven() {
    print_message "info" "检查 Maven 安装状态..."
    
    if command_exists mvn; then
        MVN_VERSION=$(mvn --version | head -n 1)
        print_message "info" "Maven 已安装: $MVN_VERSION"
        return 0
    fi
    
    print_message "info" "正在安装 Maven..."
    
    # 检测操作系统
    if [[ "$OSTYPE" == "linux-gnu"* ]]; then
        # Linux 系统
        if command_exists apt-get; then
            # Ubuntu/Debian
            sudo apt-get install -y maven
        elif command_exists yum; then
            # CentOS/RHEL
            sudo yum install -y maven
        elif command_exists dnf; then
            # Fedora
            sudo dnf install -y maven
        else
            # 手动安装 Maven
            print_message "info" "手动安装 Maven..."
            MVN_VERSION="3.9.6"
            MVN_URL="https://archive.apache.org/dist/maven/maven-3/${MVN_VERSION}/binaries/apache-maven-${MVN_VERSION}-bin.tar.gz"
            
            cd /tmp
            wget $MVN_URL
            sudo tar -zxvf apache-maven-${MVN_VERSION}-bin.tar.gz -C /opt/
            sudo ln -sf /opt/apache-maven-${MVN_VERSION} /opt/maven
            
            # 设置环境变量
            echo 'export M2_HOME=/opt/maven' | sudo tee -a /etc/profile
            echo 'export PATH=${M2_HOME}/bin:${PATH}' | sudo tee -a /etc/profile
            
            # 加载环境变量
            source /etc/profile
        fi
    elif [[ "$OSTYPE" == "darwin"* ]]; then
        # macOS
        if command_exists brew; then
            brew install maven
        else
            print_message "error" "请先安装 Homebrew，然后运行此脚本"
            exit 1
        fi
    else
        print_message "error" "不支持的操作系统: $OSTYPE"
        exit 1
    fi
    
    # 验证安装
    if command_exists mvn; then
        MVN_VERSION=$(mvn --version | head -n 1)
        print_message "success" "Maven 安装成功: $MVN_VERSION"
    else
        print_message "error" "Maven 安装失败"
        exit 1
    fi
}

# 打印使用说明
show_usage() {
    echo "使用方法: $0 [选项]"
    echo "选项:"
    echo "  all         安装所有依赖 (默认)"
    echo "  node        只安装 Node.js 和 cnpm"
    echo "  maven       只安装 Maven"
    echo "  help        显示此帮助信息"
}

# 主函数
main() {
    print_message "info" "iBoot 依赖安装脚本开始执行"
    print_message "info" "当前时间: $(date)"
    
    # 检查参数
    case ${1:-all} in
        "node")
            install_nodejs
            install_cnpm
            ;;
        "maven")
            install_maven
            ;;
        "all")
            install_nodejs
            install_cnpm
            install_maven
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
    
    print_message "success" "所有依赖安装任务已完成！"
    print_message "info" "安装完成时间: $(date)"
    
    # 显示已安装的版本
    print_message "info" "已安装的工具版本:"
    if command_exists node; then
        print_message "info" "  Node.js: $(node --version)"
    fi
    if command_exists cnpm; then
        print_message "info" "  cnpm: $(cnpm --version 2>/dev/null || echo 'Installed')"
    fi
    if command_exists mvn; then
        print_message "info" "  Maven: $(mvn --version | head -n 1)"
    fi
}

# 调用主函数，传入所有参数
main "$@"