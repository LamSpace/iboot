# iBoot Admin GraalVM Native Image 实施计划

## 1. 项目概述

| 项目 | 详情 |
|------|------|
| **应用名称** | iBoot Admin |
| **当前版本** | Spring Boot 3.5.9, Java 25 |
| **目标** | 编译为 GraalVM Native Image |
| **预期收益** | 启动时间 < 1s，内存占用减少 30-50%，无需 JVM |

---

## 2. 前置条件检查

### 2.1 GraalVM 安装要求

```bash
# 1. 下载 GraalVM JDK 25 (必须与项目 Java 版本一致)
# 推荐：GraalVM Community Edition for JDK 25

# 2. 验证安装
java -version
# 输出应包含 "GraalVM"

# 3. 安装 native-image 工具
gu install native-image
```

### 2.2 环境变量配置

```bash
# ~/.bashrc 或 ~/.zshrc
export GRAALVM_HOME=/path/to/graalvm-jdk-25
export PATH=$GRAALVM_HOME/bin:$PATH
```

---

## 3. 兼容性分析与风险项

### 3.1 高风险依赖（需要额外配置）

| 依赖 | 风险等级 | 问题描述 | 解决方案 |
|------|----------|----------|----------|
| **Druid 连接池** | 🔴 高 | 大量反射和动态代理 | 添加 reflection-config.json |
| **MyBatis** | 🔴 高 | XML 解析 + 反射 | 添加 resource-config.json + reflection-config.json |
| **Spring Security** | 🟡 中 | JDK 动态代理 | Spring Native 自动处理 |
| **JWT (jjwt)** | 🟡 中 | 反射序列化 | 添加 reflection-config.json |
| **MapStruct** | 🟡 中 | 编译时代码生成 | 通常兼容，需测试 |
| **Elasticsearch Client** | 🔴 高 | 复杂反射 + HTTP 客户端 | 添加完整反射配置 |
| **MinIO** | 🟡 中 | OKHttp 动态代理 | 添加 proxy-config.json |
| **Logstash Encoder** | 🟡 中 | Logback 配置解析 | 添加 resource-config.json |
| **Quartz** | 🟡 中 | 数据库表 + 反射 | 添加 reflection-config.json |
| **Spring Boot Admin** | 🟡 中 | Actuator 端点 | Spring Native 自动处理 |

### 3.2 代码层面风险点

```
关键文件                                  风险类型
------------------------------------------------------------------
RedisConfig.java                          Jackson 序列化反射
SecurityConfig.java                       Security 过滤器链动态代理
JwtAuthenticationFilter.java              过滤器链
TwoLevelCacheManager.java                 缓存抽象层反射
ExcelExportUtil.java                      POI/反射 (如有)
LogAspect.java                            AOP 动态代理
DataScopeInterceptor.java                 MyBatis 拦截器
```

---

## 4. 实施步骤

### 阶段一：项目改造（预计 2-4 小时）

#### 4.1 添加 GraalVM Native Build Tools 插件

**文件**: `iboot-admin/pom.xml`

在 `<build><plugins>` 中添加：

```xml
<!-- GraalVM Native Build Tools -->
<plugin>
    <groupId>org.graalvm.buildtools</groupId>
    <artifactId>native-maven-plugin</artifactId>
    <version>0.10.4</version>
    <extensions>true</extensions>
    <configuration>
        <fallback>false</fallback>
        <verbose>true</verbose>
        <quickBuild>true</quickBuild>
        <metadataRepository>
            <enabled>true</enabled>
        </metadataRepository>
        <classesDirectory>${project.build.outputDirectory}</classesDirectory>
        <metadataService>
            <enabled>true</enabled>
            <url>https://metadata.graalvm.cloud</url>
        </metadataService>
        <buildArgs>
            <arg>--initialize-at-build-time=com.iboot.admin</arg>
            <arg>--initialize-at-run-time=com.alibaba.druid</arg>
            <arg>--initialize-at-run-time=org.elasticsearch</arg>
            <arg>--report-unsupported-elements-at-runtime</arg>
        </buildArgs>
    </configuration>
    <executions>
        <execution>
            <id>build-native</id>
            <goals>
                <goal>compile-no-fork</goal>
            </goals>
            <phase>package</phase>
        </execution>
        <execution>
            <id>test-native</id>
            <goals>
                <goal>test</goal>
            </goals>
            <phase>test</phase>
        </execution>
    </executions>
</plugin>
```

#### 4.2 添加 Spring Boot 3 Native 依赖

**文件**: `iboot-admin/pom.xml`

在 `<dependencies>` 中添加：

```xml
<!-- GraalVM Native Support -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-native</artifactId>
    <version>${spring-boot.version}</version>
</dependency>
```

#### 4.3 创建 GraalVM 配置文件目录

```bash
mkdir -p iboot-admin/src/main/resources/META-INF/native-image
mkdir -p iboot-admin/src/main/resources/META-INF/native-image/com.iboot.admin
```

#### 4.4 创建反射配置文件

**文件**: `iboot-admin/src/main/resources/META-INF/native-image/reflect-config.json`

```json
[
  {
    "name": "com.iboot.admin.IBootAdminApplication",
    "allDeclaredConstructors": true,
    "allPublicConstructors": true,
    "allDeclaredMethods": true,
    "allPublicMethods": true
  },
  {
    "name": "com.alibaba.druid.pool.DruidDataSource",
    "allDeclaredConstructors": true,
    "allPublicConstructors": true,
    "allDeclaredMethods": true,
    "allPublicMethods": true,
    "allDeclaredFields": true,
    "allPublicFields": true
  },
  {
    "name": "com.mysql.cj.jdbc.Driver",
    "allDeclaredConstructors": true,
    "allPublicConstructors": true
  },
  {
    "name": "io.jsonwebtoken.impl.JwtParser",
    "allDeclaredMethods": true,
    "allPublicMethods": true
  },
  {
    "name": "io.jsonwebtoken.impl.io.JacksonSerializer",
    "allDeclaredConstructors": true
  },
  {
    "name": "io.jsonwebtoken.impl.io.JacksonDeserializer",
    "allDeclaredConstructors": true
  },
  {
    "name": "org.mybatis.spring.SqlSessionFactoryBean",
    "allDeclaredMethods": true,
    "allPublicMethods": true
  },
  {
    "name": "org.elasticsearch.client.RestClient",
    "allDeclaredConstructors": true,
    "allPublicConstructors": true,
    "allDeclaredMethods": true,
    "allPublicMethods": true
  },
  {
    "name": "co.elastic.clients.elasticsearch.ElasticsearchClient",
    "allDeclaredConstructors": true,
    "allPublicConstructors": true
  },
  {
    "name": "io.minio.MinioClient",
    "allDeclaredConstructors": true,
    "allPublicConstructors": true
  },
  {
    "name": "org.quartz.impl.StdSchedulerFactory",
    "allDeclaredConstructors": true,
    "allPublicConstructors": true
  },
  {
    "name": "org.quartz.simpl.SimpleThreadPool",
    "allDeclaredConstructors": true
  },
  {
    "name": "com.fasterxml.jackson.databind.ObjectMapper",
    "allDeclaredConstructors": true,
    "allPublicConstructors": true
  },
  {
    "name": "com.fasterxml.jackson.datatype.jsr310.JavaTimeModule",
    "allDeclaredConstructors": true
  },
  {
    "name": "org.springframework.security.web.FilterChainProxy",
    "allDeclaredMethods": true
  },
  {
    "name": "org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter",
    "allDeclaredMethods": true
  },
  {
    "name": "org.springframework.web.cors.CorsConfiguration",
    "allDeclaredMethods": true
  },
  {
    "name": "de.codecentric.boot.admin.server.config.AdminServerAutoConfiguration",
    "allDeclaredMethods": true
  },
  {
    "name": "net.logstash.logback.encoder.LogstashEncoder",
    "allDeclaredConstructors": true
  },
  {
    "name": "com.github.benmanes.caffeine.cache.Caffeine",
    "allDeclaredMethods": true
  }
]
```

#### 4.5 创建资源配置文件

**文件**: `iboot-admin/src/main/resources/META-INF/native-image/resource-config.json`

```json
{
  "resources": {
    "includes": [
      {
        "pattern": "application\\.yml$"
      },
      {
        "pattern": "application.properties$"
      },
      {
        "pattern": "banner\\.txt$"
      },
      {
        "pattern": "db/migration/.*\\.sql$"
      },
      {
        "pattern": "mapper/.*\\.xml$"
      },
      {
        "pattern": "org/mybatis/.*\\.dtd$"
      },
      {
        "pattern": "org/apache/ibatis/.*\\.dtd$"
      },
      {
        "pattern": "META-INF/spring\\.factories$"
      },
      {
        "pattern": "META-INF/spring/.*\\.imports$"
      },
      {
        "pattern": "META-INF/services/.*"
      },
      {
        "pattern": "org/springframework/security/.*\\.properties$"
      },
      {
        "pattern": "org/elasticsearch/.*\\.properties$"
      },
      {
        "pattern": "io/minio/.*\\.properties$"
      },
      {
        "pattern": "com/mysql/cj/.*\\.properties$"
      },
      {
        "pattern": "com/alibaba/druid/.*\\.properties$"
      }
    ]
  },
  "bundles": []
}
```

#### 4.6 创建代理配置文件

**文件**: `iboot-admin/src/main/resources/META-INF/native-image/proxy-config.json`

```json
[
  {
    "interfaces": [
      "org.springframework.security.web.authentication.AuthenticationFilter"
    ]
  },
  {
    "interfaces": [
      "org.springframework.web.servlet.HandlerInterceptor"
    ]
  },
  {
    "interfaces": [
      "org.aspectj.lang.JoinPoint"
    ]
  },
  {
    "interfaces": [
      "org.aopalliance.intercept.MethodInterceptor"
    ]
  },
  {
    "interfaces": [
      "org.apache.ibatis.plugin.Interceptor"
    ]
  }
]
```

#### 4.7 创建 JNI 配置文件（如需要）

**文件**: `iboot-admin/src/main/resources/META-INF/native-image/jni-config.json`

```json
[]
```

### 阶段二：构建配置调整（预计 1-2 小时）

#### 4.8 调整 application.yml 配置

需要移除或调整以下配置：

```yaml
# 移除或注释 - GraalVM 不支持动态代理的 JMX
spring:
  jmx:
    enabled: false  # 确保已设置为 false

# Flyway 配置可能需要调整
spring:
  flyway:
    enabled: true
    # Native Image 中可能需要设置 baseline-on-migrate: always
```

#### 4.9 添加 Native Image 初始化配置

**文件**: `iboot-admin/src/main/resources/META-INF/native-image/native-image.properties`

```properties
Args=--initialize-at-build-time=com.iboot.admin,\
     com.fasterxml.jackson,\
     org.springframework,\
     org.mybatis,\
     org.apache.ibatis,\
     org.springdoc,\
     io.swagger,\
     de.codecentric.boot.admin,\
     com.github.benmanes.caffeine \
     --initialize-at-run-time=com.alibaba.druid,\
     com.mysql.cj,\
     org.elasticsearch,\
     co.elastic.clients,\
     io.minio,\
     org.quartz,\
     org.apache.http,\
     okhttp3,\
     okio \
     --add-opens=java.base/java.util=ALL-UNNAMED \
     --add-opens=java.base/java.lang=ALL-UNNAMED \
     --report-unsupported-elements-at-runtime
```

### 阶段三：构建与测试（预计 2-4 小时）

#### 4.10 首次构建（Trace 模式收集配置）

```bash
cd iboot-admin

# 方式一：使用 GraalVM Native Build Tools（推荐）
mvn -Pnative native:compile

# 方式二：使用 Spring Boot Maven Plugin
mvn -Pnative spring-boot:process-aot
mvn -Pnative native:compile

# 方式三：使用 Agent 收集配置（推荐首次使用）
# 1. 先用 Agent 运行应用收集反射信息
java -agentlib:native-image-agent=config-output-dir=src/main/resources/META-INF/native-image \
     -jar target/iboot-admin-1.0.0.jar

# 2. 合并收集的配置文件
```

#### 4.11 完整构建命令

```bash
# 设置 GRAALVM_HOME
export GRAALVM_HOME=/path/to/graalvm-jdk-25

# 清理并构建
cd iboot-admin
mvn clean

# 使用 GraalVM 编译
$GRAALVM_HOME/bin/mvn -Pnative native:compile \
  -Dspring.profiles.active=native \
  -Dmaven.test.skip=true

# 或使用 spring-boot:build-image（需要 Docker + BuildPacks）
mvn spring-boot:build-image -Pnative
```

#### 4.12 构建产物验证

```bash
# 查看生成的可执行文件
ls -lh target/iboot-admin

# 验证是否为 Native Image
file target/iboot-admin

# 测试启动时间
time ./target/iboot-admin --spring.profiles.active=native
```

### 阶段四：问题排查与优化（预计 4-8 小时）

#### 4.13 常见问题处理

**问题 1**: `Error: Class initialization failed`

```bash
# 解决方案：将相关类移到 run-time 初始化
--initialize-at-run-time=com.problematic.Class
```

**问题 2**: `UnsupportedFeatureError: Proxy class`

```bash
# 解决方案：添加到 proxy-config.json
{
  "interfaces": ["com.example.Interface1", "com.example.Interface2"]
}
```

**问题 3**: `Resource not found`

```bash
# 解决方案：添加到 resource-config.json
{
  "pattern": "path/to/resource"
}
```

**问题 4**: `Reflective access unsupported`

```bash
# 解决方案：添加到 reflect-config.json
{
  "name": "com.example.Class",
  "allDeclaredMethods": true
}
```

#### 4.14 性能优化参数

```bash
# 优化构建参数
--optimize=3                    # 最高优化级别
--gc=G1                         # 使用 G1 GC（默认）
--gc=Serial                     # 使用 Serial GC（更小体积）
-mt                             # 多线程编译
--pgo                           # 使用 PGO 优化（需要性能分析数据）
```

---

## 5. Docker 集成

### 5.1 Dockerfile（多阶段构建）

**文件**: `Dockerfile.native`

```dockerfile
# 阶段一：构建 Native Image
FROM ghcr.io/graalvm/graalvm-ce:ol8-java25 AS builder

WORKDIR /app

# 安装 Maven
RUN dnf install -y maven && dnf clean all

COPY iboot-admin/pom.xml .
COPY iboot-admin/src ./src

RUN mvn dependency:go-offline
RUN mvn clean package -DskipTests
RUN mvn -Pnative native:compile -DskipTests

# 阶段二：运行时镜像
FROM gcr.io/distroless/base-debian12

WORKDIR /app

# 从构建阶段复制可执行文件
COPY --from=builder /app/target/iboot-admin ./iboot-admin

# 复制必要的资源文件（如需要）
COPY --from=builder /app/src/main/resources/application.yml ./application.yml

EXPOSE 8080

ENTRYPOINT ["./iboot-admin"]
```

### 5.2 docker-compose 调整

**文件**: `docker/docker-compose.native.yml`

```yaml
version: '3.8'

services:
  iboot-admin-native:
    build:
      context: ..
      dockerfile: docker/Dockerfile.native
    ports:
      - "8080:8080"
    environment:
      - MYSQL_HOST=mysql
      - MYSQL_PORT=3306
      - MYSQL_USERNAME=root
      - MYSQL_PASSWORD=root
      - REDIS_HOST=redis
      - REDIS_PORT=6379
      - ELASTICSEARCH_URIS=http://elasticsearch:9200
    depends_on:
      - mysql
      - redis
      - elasticsearch
    networks:
      - iboot-network

  # ... 其他服务（MySQL, Redis, Elasticsearch）
```

---

## 6. 测试验证清单

### 6.1 功能测试

- [ ] 应用正常启动（目标 < 5 秒）
- [ ] MySQL 数据库连接正常
- [ ] Redis 缓存正常
- [ ] JWT 认证正常
- [ ] Swagger UI 正常访问
- [ ] Actuator 端点正常
- [ ] 定时任务正常执行
- [ ] Elasticsearch 连接正常
- [ ] MinIO 文件上传下载正常
- [ ] Spring Boot Admin 监控正常

### 6.2 性能测试

```bash
# 启动时间对比
echo "JVM 模式启动时间:"
time java -jar target/iboot-admin-1.0.0.jar

echo "Native Image 启动时间:"
time ./target/iboot-admin

# 内存占用对比
ps aux | grep iboot-admin
```

### 6.3 压力测试

```bash
# 使用 JMeter 或 ab 进行压力测试
ab -n 10000 -c 100 http://localhost:8080/api/health

# 观察 Native Image 的响应时间和稳定性
```

---

## 7. 预期收益

| 指标 | JVM 模式 | Native Image | 改善 |
|------|----------|--------------|------|
| **启动时间** | 15-30 秒 | 1-3 秒 | 85-90% ↓ |
| **内存占用** | 500MB-1GB | 200-400MB | 50-60% ↓ |
| **CPU 使用率** | 基准 | 略低 | 10-20% ↓ |
| **部署复杂度** | 需要 JVM | 无需 JVM | 简化 |
| **冷启动** | 慢 | 极快 | 显著改善 |

---

## 8. 风险与注意事项

### 8.1 技术风险

1. **动态代理依赖**: Druid、MyBatis 大量使用动态代理，可能遇到兼容性问题
2. **反射密集型**: JWT、Jackson 序列化依赖反射，需要完整配置
3. **资源文件**: MyBatis XML、Flyway SQL 需要显式包含
4. **第三方库**: Elasticsearch、MinIO 客户端可能有未文档化的反射使用

### 8.2 运维风险

1. **调试困难**: Native Image 调试比 JVM 复杂
2. **内存限制**: 编译时堆内存需求较大（建议 16GB+）
3. **构建时间**: Native 编译比 JVM 编译慢 5-10 倍
4. **跨平台**: 需要为目标 OS 分别编译

### 8.3 回滚方案

保留 JVM 模式的部署脚本和 Docker 镜像，如遇问题可快速回退。

---

## 9. 时间估算

| 阶段 | 任务 | 预计时间 |
|------|------|----------|
| 阶段一 | 项目改造 | 2-4 小时 |
| 阶段二 | 构建调整 | 1-2 小时 |
| 阶段三 | 构建测试 | 2-4 小时 |
| 阶段四 | 问题排查 | 4-8 小时 |
| **总计** | | **9-18 小时** |

---

## 10. 参考文档

- [Spring Native Reference](https://docs.spring.io/spring-native/docs/current/reference/htmlsingle/)
- [GraalVM Native Build Tools](https://graalvm.github.io/native-build-tools/)
- [GraalVM Reference Documentation](https://www.graalvm.org/latest/reference-manual/native-image/)
- [Spring Boot GraalVM Support](https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#native-image)

---

## 附录 A：快速开始命令

```bash
# 1. 安装 GraalVM JDK 25
sdk install java 25-graalce
sdk use java 25-graalce

# 2. 验证安装
java -version

# 3. 进入项目目录
cd iboot-admin

# 4. 添加 Native 依赖和插件（手动编辑 pom.xml）

# 5. 创建配置文件目录
mkdir -p src/main/resources/META-INF/native-image

# 6. 首次构建（使用 Agent 收集配置）
mvn clean package -DskipTests
java -agentlib:native-image-agent=config-merge-dir=src/main/resources/META-INF/native-image \
     -jar target/iboot-admin-1.0.0.jar

# 访问几个 API 端点后停止应用

# 7. 编译 Native Image
mvn -Pnative native:compile -DskipTests

# 8. 测试运行
./target/iboot-admin
```
