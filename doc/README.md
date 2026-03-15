# iBoot 项目文档

欢迎来到 iBoot 项目文档中心。本文档提供了完整的项目说明，包括项目概述、技术栈、架构设计、功能模块、快速开始、API 集成、数据库设计和部署指南。

## 文档目录

### 基础文档

| 文档 | 描述 |
|------|------|
| [01-项目概述](./01-项目概述.md) | 项目简介、目标、技术选型理由、业务价值与技术价值 |
| [02-技术栈](./02-技术栈.md) | 前后端技术栈详解、数据库与中间件、监控与可观测性 |
| [03-架构设计](./03-架构设计.md) | DDD 分层架构、项目结构、核心组件、数据流设计、权限模型 |

### 开发文档

| 文档 | 描述 |
|------|------|
| [04-功能模块](./04-功能模块.md) | 系统管理、组织架构、消息推送、安全管理等十大功能模块详解 |
| [05-快速开始](./05-快速开始.md) | 环境准备、后端启动、前端启动、数据库初始化、Docker 部署、常见问题 |
| [06-API 集成](./06-API 集成.md) | API 设计规范、认证授权、请求封装、接口列表、数据格式、错误处理 |
| [07-数据库设计](./07-数据库设计.md) | 表结构设计、表关系说明、索引设计、数据字典 |

### 后端文档

| 文档 | 描述 |
|------|------|
| [09-后端技术架构详解](./09-后端技术架构详解.md) | DDD 分层架构、技术栈详解、核心模块设计、安全架构、缓存架构 |
| [10-后端配置详解](./10-后端配置详解.md) | 配置文件概述、数据源、Redis、MyBatis、Flyway、Quartz、JWT、Actuator 等配置详解 |
| [12-二级缓存设计](./12-二级缓存设计.md) | L1+Caffeine+L2+Redis 二级缓存架构、分布式一致性、缓存使用指南 |

### 运维文档

| 文档 | 描述 |
|------|------|
| [08-部署指南](./08-部署指南.md) | 环境配置、本地开发部署、Docker 容器化部署、生产环境部署、CI/CD 部署 |
| [11-可观测性设计](./11-可观测性设计.md) | 监控架构、指标监控、日志系统、告警系统、Grafana 仪表板、Thanos 长期存储 |

### 特色功能

| 文档 | 描述 |
|------|------|
| [13-CloudEvents 集成](./13-CloudEvents 集成.md) | CloudEvents 1.0 规范集成、架构设计、接口覆盖清单、响应格式示例 |
| [14-MapStruct 集成](./14-MapStruct 集成.md) | MapStruct 对象映射框架集成指南、使用规范、最佳实践 |
| [15-SSE 推送集成](./15-SSE 推送集成.md) | SSE 消息推送技术详解、架构设计、前后端实现、集群支持、监控指标 |

## 快速导航

### 🚀 快速开始

如果你是第一次接触这个项目，建议从以下文档开始：

1. [项目概述](./01-项目概述.md) - 了解项目基本情况
2. [快速开始](./05-快速开始.md) - 快速搭建开发环境
3. [功能模块](./04-功能模块.md) - 了解系统功能

### 💻 后端开发人员

后端开发人员建议阅读以下文档：

1. [技术栈](./02-技术栈.md) - 了解使用的技术
2. [架构设计](./03-架构设计.md) - 理解系统架构
3. [后端技术架构详解](./09-后端技术架构详解.md) - DDD 分层架构、核心模块设计
4. [后端配置详解](./10-后端配置详解.md) - 所有配置项详细说明
5. [二级缓存设计](./12-二级缓存设计.md) - Caffeine+Redis 二级缓存架构
6. [数据库设计](./07-数据库设计.md) - 数据库表结构
7. [API 集成](./06-API 集成.md) - 接口调用规范
8. [CloudEvents 集成](./13-CloudEvents 集成.md) - CloudEvents 1.0 规范集成、事件类型定义
9. [MapStruct 集成](./14-MapStruct 集成.md) - 对象映射框架使用指南
10. [SSE 推送集成](./15-SSE 推送集成.md) - SSE 消息推送技术详解、前后端实现、集群支持

### 🔧 运维人员

运维人员建议阅读以下文档：

1. [部署指南](./08-部署指南.md) - 部署流程
2. [可观测性设计](./11-可观测性设计.md) - 监控、日志、告警系统
3. [快速开始](./05-快速开始.md) - 环境准备
4. [架构设计](./03-架构设计.md) - 系统架构

## 项目结构

```
iboot/
├── doc/                     # 文档目录
│   ├── README.md            # 本文档
│   ├── 01-项目概述.md        # 项目概述
│   ├── 02-技术栈.md          # 技术栈
│   ├── 03-架构设计.md        # 架构设计
│   ├── 04-功能模块.md        # 功能模块
│   ├── 05-快速开始.md        # 快速开始
│   ├── 06-API 集成.md        # API 集成
│   ├── 07-数据库设计.md      # 数据库设计
│   ├── 08-部署指南.md        # 部署指南
│   ├── 09-后端技术架构详解.md # 后端技术架构详解
│   ├── 10-后端配置详解.md     # 后端配置详解
│   ├── 11-可观测性设计.md     # 可观测性设计
│   ├── 12-二级缓存设计.md     # 二级缓存设计
│   ├── 13-CloudEvents 集成.md # CloudEvents 集成
│   ├── 14-MapStruct 集成.md   # MapStruct 集成
│   └── 15-SSE 推送集成.md     # SSE 推送集成
├── iboot-admin/             # 后端项目
├── iboot-portal/            # 前端项目
├── docker/                  # Docker 配置
├── database/                # 数据库脚本
└── scripts/                 # 构建脚本
```

## 技术栈概览

### 后端技术

- **框架**: Spring Boot 3.5.9 (JDK 25)
- **安全**: Spring Security 6.x + JWT (JJWT 0.12.5)
- **ORM**: MyBatis 3.5.x + MyBatis-Plus 3.5.9 + PageHelper 2.1.0
- **缓存**: Redis 7.x + Caffeine 3.1.8 (二级缓存)
- **数据库**: MySQL 8.x
- **连接池**: Druid 1.2.24
- **监控**: Micrometer + Prometheus + Grafana
- **日志**: Logback + ELK Stack

### 前端技术

- **框架**: Vue 3 + TypeScript
- **UI**: Element Plus
- **状态管理**: Pinia
- **路由**: Vue Router
- **构建工具**: Vite 5.x
- **HTTP**: Axios

### 运维技术

- **容器化**: Docker + Docker Compose
- **CI/CD**: GitHub Actions / Jenkins
- **反向代理**: Nginx
- **监控**: Prometheus + Grafana
- **日志**: ELK Stack

## 功能模块概览

| 模块 | 描述 |
|------|------|
| 系统管理 | 用户、角色、权限、菜单、字典、配置管理 |
| 组织架构管理 | 部门、岗位、组织架构图管理 |
| 个人中心 | 个人信息、密码修改、头像上传 |
| 消息推送 | 系统公告、消息中心、邮件通知 |
| 安全管理 | 登录日志、操作日志、运行日志、审计 |
| 文件管理 | 文件上传下载、预览、分类管理 |
| 数据管理 | 数据备份恢复、清理、归档 |
| 监控运维 | 在线用户、服务、Redis、SQL、性能监控 |
| 定时任务 | 任务调度、执行监控 |
| 统计分析 | 数据报表、使用分析、仪表板 |

## 开发环境搭建

### 前置要求

- JDK 25 (最低 21)
- Node.js 20 LTS
- MySQL 8.x
- Redis 7.x
- Maven 3.9+

### 快速启动

```bash
# 1. 克隆项目
git clone <repository-url>
cd iboot

# 2. 启动后端
cd iboot-admin
mvn spring-boot:run

# 3. 启动前端（新终端）
cd iboot-portal
npm install
npm run dev
```

### 默认账号

- 用户名：admin
- 密码：admin123

## 生产环境部署

### Docker 部署

```bash
cd docker
docker-compose up -d
```

### 访问地址

- 前端：http://localhost:80
- 后端 API：http://localhost:8080
- Grafana：http://localhost:3000 (admin/admin123)
- Prometheus：http://localhost:9090

## 版本信息

| 组件 | 版本 |
|------|------|
| iBoot | 1.0.0 |
| Spring Boot | 3.5.9 |
| JDK | 25 |
| Vue | 3.x |
| MySQL | 8.x |
| Redis | 7.x |

## 贡献指南

我们欢迎任何形式的贡献，包括但不限于：

- 🐛 提交 Bug 报告
- 💡 提出新功能建议
- 🔧 修复代码缺陷
- 📝 完善文档

## 许可证

Licensed under the Apache License, Version 2.0 (the "License").
See the [LICENSE](../LICENSE) file for additional licensing information.

## 联系方式

- 项目地址：https://github.com/your-repo/iboot
- 问题反馈：https://github.com/your-repo/iboot/issues
