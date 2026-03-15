# iBoot 技术栈说明

本文档详细说明 iBoot 后台管理系统所使用的技术栈及其开源社区来源。

## 核心框架

### Spring Framework
- **版本**：6.1.x（随 Spring Boot 3.3.0）
- **来源**：Apache License 2.0
- **社区**：Spring（VMware/Broadcom）
- **官网**：https://spring.io/projects/spring-framework
- **说明**：Java 企业级应用开发框架

### Spring Boot
- **版本**：3.3.0
- **来源**：Apache License 2.0
- **社区**：Spring（VMware/Broadcom）
- **官网**：https://spring.io/projects/spring-boot
- **说明**：简化 Spring 应用开发的快速开发框架

### Spring Security
- **版本**：6.3.0
- **来源**：Apache License 2.0
- **社区**：Spring（VMware/Broadcom）
- **官网**：https://spring.io/projects/spring-security
- **说明**：强大的身份验证和访问控制框架

### Spring Data Redis
- **版本**：3.3.0（随 Spring Boot）
- **来源**：Apache License 2.0
- **社区**：Spring（VMware/Broadcom）
- **官网**：https://spring.io/projects/spring-data-redis
- **说明**：Redis 数据访问框架

## 持久层框架

### MyBatis
- **版本**：3.5.16
- **来源**：Apache License 2.0
- **社区**：Apache Software Foundation / MyBatis.org
- **官网**：https://mybatis.org/mybatis-3/
- **说明**：优秀的持久层框架，支持定制化 SQL

### MyBatis Spring Boot Starter
- **版本**：3.0.3
- **来源**：Apache License 2.0
- **社区**：MyBatis.org
- **官网**：https://github.com/mybatis/spring-boot-starter
- **说明**：MyBatis 与 Spring Boot 集成

## 数据库相关

### MySQL Connector/J
- **版本**：8.0.33
- **来源**：GPL v2 with FOSS Exception
- **社区**：Oracle Corporation
- **官网**：https://dev.mysql.com/downloads/connector/j/
- **说明**：MySQL 官方 JDBC 驱动

### HikariCP
- **版本**：5.1.0（Spring Boot 默认）
- **来源**：Apache License 2.0
- **社区**：Brett Wooldridge
- **官网**：https://github.com/brettwooldridge/HikariCP
- **说明**：高性能 JDBC 连接池

## 缓存框架

### Lettuce
- **版本**：6.3.x（Spring Boot 默认）
- **来源**：Apache License 2.0
- **社区**：Lettuce Team
- **官网**：https://lettuce.io/
- **说明**：高级 Redis 客户端

## JWT 认证

### JJWT
- **版本**：0.12.5
- **来源**：Apache License 2.0
- **社区**：jsonwebtoken.io
- **官网**：https://github.com/jwtk/jjwt
- **说明**：Java JWT 实现库

## API 文档

### Springdoc OpenAPI
- **版本**：2.3.0
- **来源**：Apache License 2.0
- **社区**：Springdoc
- **官网**：https://springdoc.org/
- **说明**：自动生成 OpenAPI 3 规范的 API 文档

## 工具库

### Apache Commons Lang3
- **版本**：3.14.0
- **来源**：Apache License 2.0
- **社区**：Apache Software Foundation
- **官网**：https://commons.apache.org/proper/commons-lang/
- **说明**：Java 核心类库扩展

### Apache Commons IO
- **版本**：2.15.1
- **来源**：Apache License 2.0
- **社区**：Apache Software Foundation
- **官网**：https://commons.apache.org/proper/commons-io/
- **说明**：IO 工具类库

### Apache Commons Collections4
- **版本**：4.4
- **来源**：Apache License 2.0
- **社区**：Apache Software Foundation
- **官网**：https://commons.apache.org/proper/commons-collections/
- **说明**：集合工具类库

### Apache Commons Codec
- **版本**：1.16.1
- **来源**：Apache License 2.0
- **社区**：Apache Software Foundation
- **官网**：https://commons.apache.org/proper/commons-codec/
- **说明**：编解码工具类库

### Google Guava
- **版本**：33.0.0-jre
- **来源**：Apache License 2.0
- **社区**：Google
- **官网**：https://github.com/google/guava
- **说明**：Google 核心 Java 库

### Lombok
- **版本**：1.18.32（Spring Boot 管理）
- **来源**：MIT License
- **社区**：Project Lombok
- **官网**：https://projectlombok.org/
- **说明**：简化 Java 代码的注解处理器

## JSON 处理

### Jackson
- **版本**：2.17.x（Spring Boot 默认）
- **来源**：Apache License 2.0
- **社区**：FasterXML
- **官网**：https://github.com/FasterXML/jackson
- **说明**：高性能 JSON 处理库

## 校验框架

### Jakarta Validation API
- **版本**：3.0.x（Spring Boot 管理）
- **来源**：Apache License 2.0
- **社区**：Eclipse Foundation
- **官网**：https://jakarta.ee/specifications/bean-validation/
- **说明**：Bean 验证标准

### Hibernate Validator
- **版本**：8.0.x（Spring Boot 默认）
- **来源**：Apache License 2.0
- **社区**：Hibernate / Red Hat
- **官网**：https://hibernate.org/validator/
- **说明**：Jakarta Validation 实现

## 日志框架

### SLF4J
- **版本**：2.0.x（Spring Boot 管理）
- **来源**：MIT License
- **社区**：QOS.ch
- **官网**：http://www.slf4j.org/
- **说明**：Java 日志门面

### Logback
- **版本**：1.5.x（Spring Boot 默认）
- **来源**：EPL v1.0 / LGPL 2.1
- **社区**：QOS.ch
- **官网**：http://logback.qos.ch/
- **说明**：高性能日志框架

## 构建工具

### Apache Maven
- **版本**：3.6+
- **来源**：Apache License 2.0
- **社区**：Apache Software Foundation
- **官网**：https://maven.apache.org/
- **说明**：项目构建管理工具

## JDK 版本

### JDK
- **运行环境**：JDK 25
- **编译目标**：Java 21 LTS
- **来源**：GPL v2 with Classpath Exception
- **社区**：Oracle / OpenJDK Community
- **官网**：https://openjdk.org/
- **说明**：Java 开发工具包

## 开源框架选择原则

1. **国际主流**：优先选择 Apache、Spring、Google、Eclipse 等国际知名开源组织的项目
2. **许可证友好**：选择 Apache License 2.0、MIT License 等商业友好的许可证
3. **活跃维护**：选择社区活跃、持续维护的项目
4. **广泛使用**：选择业界广泛使用、经过验证的技术
5. **避免国内框架**：不使用 MyBatis-Plus、Hutool、Knife4j、Druid 等国内开源框架

## 技术支持

如有技术问题，请联系：
- 邮箱：support@iboot.com
- 官网：https://iboot.com
