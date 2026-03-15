<div align="center">

# iBoot - 基于Vue 3和Spring Boot的通用管理系统

<p align="center">
  <a href="https://github.com/vuejs/vue">
    <img src="https://img.shields.io/badge/Vue.js-3.x-brightgreen?style=flat-square&logo=vue.js&logoColor=white" alt="Vue.js">
  </a>&nbsp;&nbsp;
  <a href="https://github.com/spring-projects/spring-boot">
    <img src="https://img.shields.io/badge/Spring%20Boot-4.x-blue?style=flat-square&logo=spring-boot&logoColor=white" alt="Spring Boot">
  </a>&nbsp;&nbsp;
  <a href="https://www.typescriptlang.org/">
    <img src="https://img.shields.io/badge/TypeScript-5.x-blue?style=flat-square&logo=typescript&logoColor=white" alt="TypeScript">
  </a>&nbsp;&nbsp;
  <a href="https://nodejs.org/">
    <img src="https://img.shields.io/badge/Node.js-20.x-green?style=flat-square&logo=node.js&logoColor=white" alt="Node.js">
  </a>&nbsp;&nbsp;
  <a href="https://www.mysql.com/">
    <img src="https://img.shields.io/badge/MySQL-8.x-red?style=flat-square&logo=mysql&logoColor=white" alt="MySQL">
  </a>&nbsp;&nbsp;
  <a href="https://redis.io/">
    <img src="https://img.shields.io/badge/Redis-7.x-red?style=flat-square&logo=redis&logoColor=white" alt="Redis">
  </a>
</p>

<p align="center">
  <strong>iBoot 是一个现代化的前后端分离管理系统，采用 Vue 3 和 Spring Boot 技术栈构建。</strong><br>
  项目遵循 DDD（领域驱动设计）架构模式，采用分层架构设计，旨在提供一个可扩展、可维护的企业级解决方案。
</p>

<p align="center">
  <a href="#项目概述">项目概述</a> •
  <a href="#技术栈">技术栈</a> •
  <a href="#项目特点">项目特点</a> •
  <a href="#系统功能模块">功能模块</a> •
  <a href="#快速开始">快速开始</a> •
  <a href="#许可证">许可证</a>
</p>

</div>

## 📋 项目概述

iBoot 是一个现代化的前后端分离管理系统，采用 Vue 3 和 Spring Boot 技术栈构建。项目遵循 DDD（领域驱动设计）架构模式，采用分层架构设计，旨在提供一个可扩展、可维护的企业级解决方案。

## 🛠️ 技术栈

### 后端技术栈
- **Spring Boot 4.x**: 快速开发框架
- **MyBatis-Plus**: ORM框架，简化数据库操作
- **Redis**: 缓存和会话管理
- **Spring Security**: 安全框架，提供认证和授权
- **JWT**: 无状态身份验证
- **MySQL/PostgreSQL**: 关系型数据库
- **Flyway**: 数据库版本管理工具
- **Druid**: 阿里巴巴数据库连接池及监控工具
- **MapStruct 1.6.x**: 对象映射框架（[查看使用指南](doc/14-MapStruct 集成.md)）
- **SSE 推送**: 实时消息推送（[查看使用指南](doc/15-SSE 推送集成.md)）
- **Docker**: 容器化部署

### 前端技术栈
- **Vue 3**: 前端框架
- **TypeScript**: 类型安全
- **Element Plus**: UI组件库
- **Vite**: 构建工具
- **Pinia**: 状态管理
- **Vue Router**: 路由管理
- **Three.js**: 3D图形渲染

## ✨ 项目特点

<div align="center">

| 特性 | 描述 |
|------|------|
| 🔄 **前后端分离** | 完全分离的前后端架构 |
| 🌐 **RESTful API** | 遵循RESTful设计规范 |
| 🔐 **RBAC权限模型** | 基于角色的访问控制 |
| 🌍 **国际化支持** | 多语言支持 |
| ♻️ **逻辑删除** | 软删除机制 |
| 🔒 **数据权限** | 精细化的数据权限控制 |
| 📊 **3D可视化** | 酷炫的3D背景效果 |
| 🗂️ **数据库版本管理** | 使用Flyway进行数据库迁移 |
| 📈 **数据库监控** | 使用Druid监控数据库性能 |

</div>

## 🧩 系统功能模块

### 👤 用户管理
- 用户信息维护
- 用户权限分配
- 用户状态管理

### 👥 角色管理
- 角色信息维护
- 角色权限分配
- 角色状态管理

### 📋 菜单管理
- 菜单结构维护
- 权限标识管理
- 菜单排序管理

### 🏢 部门管理
- 部门信息维护
- 部门层级管理
- 部门人员管理

### 💼 岗位管理
- 岗位信息维护
- 岗位人员分配

### ⚙️ 系统配置
- 系统参数配置
- 配置项管理

### 📚 数据字典
- 字典分类管理
- 字典项维护

### 📝 日志管理
- 登录日志
- 操作日志
- 运行日志

### 📁 文件管理
- 文件上传下载
- 文件分类管理
- 文件权限控制

### 📢 消息推送
- 系统公告发布
- 消息中心

### ⏰ 定时任务
- 任务调度管理
- 任务执行监控

### 🛡️ 安全管理
- 登录日志管理
- 操作日志管理
- 运行日志管理

### 📈 监控运维
- 在线用户监控
- 服务监控检查
- Redis缓存监控
- SQL执行监控
- 系统性能监控

### 📊 统计分析
- 数据统计报表
- 系统使用分析
- 仪表板展示

## 📁 项目结构

```
iboot/
├── scripts/               # 脚本目录
│   ├── build.sh          # 构建脚本
│   └── install-dependencies.sh  # 依赖安装脚本
├── docker/                # Docker 配置目录
│   └── docker-compose.yml       # Docker Compose 配置
├── database/              # 数据库配置目录
│   └── init.sql          # 数据库初始化脚本
├── iboot-admin/          # 后端项目
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   │   └── com/iboot/admin/
│   │   │   │       ├── application/    # 应用层
│   │   │   │       ├── domain/         # 领域层
│   │   │   │       ├── infrastructure/ # 基础设施层
│   │   │   │       ├── interfaces/     # 接口层
│   │   │   │       └── common/         # 公共组件
│   │   │   └── resources/
│   └── pom.xml
├── iboot-portal/         # 前端项目
│   ├── src/
│   │   ├── views/
│   │   ├── components/
│   │   ├── router/
│   │   └── assets/
│   ├── package.json
│   └── vite.config.ts
├── doc/                  # 文档目录
│   ├── README.md         # 文档导航
│   ├── 14-MapStruct 集成.md # MapStruct 对象映射集成指南
│   └── ...
├── LICENSE               # 许可证文件
├── .gitignore           # Git 忽略文件配置
└── README.md            # 项目说明文档
```

## 🚀 快速开始

### 后端启动

1. 进入后端目录
```bash
cd iboot-admin
```

2. 配置数据库连接（application.yml）

3. 启动后端服务
```bash
mvn spring-boot:run
```

### 前端启动

1. 进入前端目录
```bash
cd iboot-portal
```

2. 安装依赖
```bash
npm install
```

or

```bash
cnpm install
```

3. 启动前端服务
```bash
npm run dev
```

or

```bash
cnpm run dev --host
```

## 🛠️ 构建脚本

### 依赖安装脚本

使用 `scripts/install-dependencies.sh` 脚本来安装项目所需的全部依赖：

```bash
# 给脚本执行权限
cd scripts
chmod +x install-dependencies.sh

# 安装所有依赖（默认）
./install-dependencies.sh

# 仅安装 Node.js 和 cnpm
./install-dependencies.sh node

# 仅安装 Maven
./install-dependencies.sh maven

# 查看帮助信息
./install-dependencies.sh help
```

### 项目构建脚本

使用 `scripts/build.sh` 脚本来构建项目：

```bash
# 给脚本执行权限
cd scripts
chmod +x build.sh

# 构建前端和后端（默认）
./build.sh

# 仅构建前端
./build.sh frontend

# 仅构建后端
./build.sh backend

# 查看帮助信息
./build.sh help
```

## 📦 项目部署

### Docker部署

项目支持Docker容器化部署，具体配置请参考各子项目的Dockerfile。

#### 前端部署 (iboot-portal)

构建前端镜像：
```bash
cd iboot-portal
docker build -t iboot-portal .
```

运行前端容器：
```bash
docker run -d -p 80:80 --name iboot-portal iboot-portal
```

#### 部署结构

```
iboot/
├── scripts/               # 脚本目录
│   ├── build.sh          # 构建脚本
│   └── install-dependencies.sh  # 依赖安装脚本
├── docker/                # Docker 配置目录
│   └── docker-compose.yml       # Docker Compose 配置
├── database/              # 数据库配置目录
│   └── init.sql          # 数据库初始化脚本
├── iboot-admin/          # 后端项目
├── iboot-portal/         # 前端项目
├── LICENSE               # 许可证文件
├── .gitignore           # Git 忽略文件配置
└── README.md            # 项目说明文档
```

#### 后端部署 (iboot-admin)

构建后端镜像：
```bash
cd iboot-admin
docker build -t iboot-admin .
```

运行后端容器：
```bash
docker run -d -p 8080:8080 --name iboot-admin iboot-admin
```

#### 使用 Docker Compose 一体化部署

项目提供了 `docker/docker-compose.yml` 文件来快速启动基础服务：

```bash
# 进入 docker 目录
cd docker

# 启动所有服务（后台运行）
docker-compose up -d

# 查看服务状态
docker-compose ps

# 停止所有服务
docker-compose down

# 查看服务日志
docker-compose logs -f
```

该配置会启动以下服务：
- **MySQL 8.0** (端口 3306)
- **Redis 7** (端口 6379)
- **MinIO** (端口 9000, 9001)
- **Prometheus** (端口 9090) - 指标采集
- **Grafana** (端口 3000) - 可视化面板
- **AlertManager** (端口 9093) - 告警管理
- **Elasticsearch** (端口 9200) - 搜索引擎
- **Logstash** (端口 5044, 5000) - 日志处理
- **Kibana** (端口 5601) - ES 可视化
- **MySQL Exporter** (端口 9104) - MySQL 监控
- **Redis Exporter** (端口 9121) - Redis 监控
- **Node Exporter** (端口 9100) - 系统监控
- **Elasticsearch Exporter** (端口 9114) - ES 监控

#### 预拉取 Docker 镜像

为了加快部署速度，可以使用脚本预先拉取所有镜像到本地：

```bash
# 进入 scripts 目录
cd scripts

# 给脚本执行权限
chmod +x pull-docker-images.sh

# 拉取所有镜像
./pull-docker-images.sh
```

该脚本会自动从华为云镜像仓库拉取所有需要的镜像，并进行本地标签转换。

### 📊 可观测性监控

项目集成了完整的可观测性监控体系：

#### Prometheus + Grafana 监控

- **系统监控**: CPU、内存、磁盘、网络等
- **JVM 监控**: 堆内存、GC、线程等
- **数据库监控**: MySQL 连接池、慢查询等
- **缓存监控**: Redis 性能、内存使用等
- **业务监控**: 用户登录、请求统计等

访问地址：
- Prometheus: http://localhost:9090
- Grafana: http://localhost:3000 (admin/admin123)

#### Elasticsearch 监控

项目为 Elasticsearch 集群提供了完整的可观测性配置：

- **Exporter**: 采集 ES 集群健康、节点状态、索引性能等指标
- **Prometheus**: 定期抓取 ES 指标数据
- **Grafana Dashboard**: 可视化 ES 集群状态、JVM 内存、分片分布、磁盘使用等
- **告警规则**: 集群健康异常、节点离线、磁盘不足、性能下降等场景

详细配置请参考 [Elasticsearch 可观测性指南](observability/elasticsearch/README.md)

#### ELK 日志收集

- **Elasticsearch**: 存储和检索日志
- **Logstash**: 处理和转换日志数据
- **Kibana**: 日志可视化分析

访问地址：
- Kibana: http://localhost:5601

## 🤝 贡献指南

我们欢迎任何形式的贡献，包括但不限于：

- 🐛 提交Bug报告
- 💡 提出新功能建议
- 🔧 修复代码缺陷
- 📝 完善文档

## 📄 许可证

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.

See the [LICENSE](LICENSE) file for additional licensing information.