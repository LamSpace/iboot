# iBoot Admin - 后端管理系统

iBoot Admin是基于Spring Boot的后端管理系统，采用DDD架构设计，提供完整的后端服务支持。

## 技术栈

- **Spring Boot 4.x**: 快速开发框架
- **MyBatis-Plus**: ORM框架，简化数据库操作
- **Redis**: 缓存和会话管理
- **Spring Security**: 安全框架，提供认证和授权
- **JWT**: 无状态身份验证
- **MySQL/PostgreSQL**: 关系型数据库
- **Swagger/OpenAPI**: API文档自动生成
- **Lombok**: 简化代码编写
- **Apache Commons Lang**: 工具类库
- **Jackson**: JSON处理

## 架构设计

项目采用DDD（领域驱动设计）的分层架构：

```
com.iboot.admin
├── application/           # 应用层
│   └── service/          # 应用服务
├── domain/               # 领域层
│   ├── system/           # 系统领域
│   │   ├── model/        # 领域模型
│   │   └── repository/   # 仓储接口
│   └── common/           # 通用领域
├── infrastructure/       # 基础设施层
│   ├── config/           # 配置类
│   ├── persistence/      # 持久化实现
│   │   ├── mapper/       # MyBatis映射器
│   │   └── po/           # 持久化对象
│   └── security/         # 安全实现
├── interfaces/           # 接口层
│   ├── controller/       # 控制器
│   └── dto/              # 数据传输对象
└── common/               # 公共组件
    ├── annotation/       # 注解
    ├── constant/         # 常量
    ├── enums/            # 枚举
    ├── exception/        # 异常处理
    └── result/           # 统一响应结果
```

## 核心功能

### 1. 用户管理

- 用户注册、登录、登出
- 用户信息维护
- 用户状态管理
- 用户权限分配

### 2. 角色管理

- 角色创建、编辑、删除
- 角色权限分配
- 角色状态管理

### 3. 菜单管理

- 菜单结构维护
- 权限标识管理
- 菜单排序管理

### 4. 部门管理

- 部门层级管理
- 部门人员分配

### 5. 岗位管理

- 岗位信息维护
- 岗位人员分配

### 6. 安全管理

- JWT身份验证
- RBAC权限控制
- 数据权限控制
- 操作日志记录

## 安全机制

### JWT身份验证

- 无状态身份验证
- 自动刷新令牌
- Redis存储会话信息

### 权限控制

- 基于角色的访问控制（RBAC）
- 细粒度权限管理
- 数据权限控制

### 数据安全

- 敏感信息加密存储
- SQL注入防护
- XSS攻击防护

## API文档

系统集成了Swagger/OpenAPI，提供交互式API文档：

- 访问地址: `http://localhost:8080/swagger-ui.html`
- API文档: `http://localhost:8080/v3/api-docs`

## 配置说明

### 数据库配置

在`application.yml`中配置数据库连接信息：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/iboot?useUnicode=true&characterEncoding=utf8&zeroDateTimeBehavior=convertToNull&useSSL=false&serverTimezone=GMT%2B8
    username: root
    password: password
    driver-class-name: com.mysql.cj.jdbc.Driver
```

### JWT配置

```yaml
jwt:
  secret: iboot-admin-secret-key-for-jwt-token-generation-minimum-256-bits
  expiration: 1800  # 30分钟
```

## 启动方式

### 开发环境启动

```bash
# 使用Maven启动
mvn spring-boot:run

# 或者打包后运行
mvn clean package
java -jar target/iboot-admin.jar
```

### 生产环境启动

```bash
# 使用JVM参数优化
java -Xms512m -Xmx1024m -jar target/iboot-admin.jar
```

## Docker部署

### 构建镜像

```bash
docker build -t iboot-admin .
```

### 运行容器

```bash
docker run -d -p 8080:8080 --name iboot-admin iboot-admin
```

## 项目特点

- **DDD架构**: 领域驱动设计，清晰的分层结构
- **安全可靠**: 完善的安全机制和权限控制
- **高性能**: Redis缓存、连接池优化
- **易扩展**: 模块化设计，易于扩展功能
- **标准化**: 遵循RESTful API规范
- **文档完善**: 集成Swagger文档

## 开发规范

- 使用统一的异常处理机制
- 遵循Java代码规范和命名约定
- 实现统一的响应结果封装
- 使用注解简化代码开发

## 贡献指南

1. Fork项目
2. 创建特性分支
3. 提交更改
4. 发起Pull Request

## 许可证

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
License. You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "
AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
language governing permissions and limitations under the License.

See the [LICENSE](../LICENSE) file for additional licensing information.