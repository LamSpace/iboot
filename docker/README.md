# iBoot Docker 部署指南

## 快速开始

### 前置要求

- Docker 20.10+
- Docker Compose 2.0+
- Maven 3.8+ (用于构建后端)
- Node.js 20.x (用于构建前端)

### 一键部署

```bash
cd docker

# 构建并启动所有服务
./deploy.sh

# 或者分步执行
./deploy.sh build-admin    # 构建后端
./deploy.sh build-portal   # 构建前端
./deploy.sh start          # 启动服务
```

## 服务清单

| 服务 | 端口 | 说明 |
|------|------|------|
| iboot-portal | 80 | 前端应用 |
| iboot-admin | 8080 | 后端 API |
| mysql | 3306 | MySQL 数据库 |
| redis | 6379 | Redis 缓存 |
| minio | 9000/9001 | MinIO 对象存储 |
| prometheus | 9090 | Prometheus 监控 |
| grafana | 3000 | Grafana 可视化 |
| alertmanager | 9093 | 告警管理 |
| elasticsearch | 9200 | Elasticsearch 日志存储 |
| logstash | 5044 | Logstash 日志收集 |
| kibana | 5601 | Kibana 日志展示 |
| thanos-query | 19192 | Thanos 查询 |

## 访问地址

### 应用服务
- **前端**: http://localhost:80
- **后端 API**: http://localhost:8080
- **Swagger**: http://localhost:8080/swagger-ui.html

### 监控服务
- **Grafana**: http://localhost:3000 (admin/admin123)
- **Prometheus**: http://localhost:9090
- **AlertManager**: http://localhost:9093

### 日志服务
- **Kibana**: http://localhost:5601
- **Elasticsearch**: http://localhost:9200

### 存储服务
- **MinIO Console**: http://localhost:9001 (minioadmin/minioadmin)

## 常用命令

```bash
# 查看服务状态
docker-compose ps

# 查看所有服务日志
docker-compose logs -f

# 查看特定服务日志
docker-compose logs -f iboot-admin
docker-compose logs -f iboot-portal

# 停止服务
docker-compose down

# 重启服务
docker-compose restart

# 重新构建服务
docker-compose build --no-cache iboot-admin
docker-compose up -d --force-recreate iboot-admin
```

## 配置说明

### 后端环境变量

| 变量 | 说明 | 默认值 |
|------|------|--------|
| MYSQL_HOST | MySQL 主机 | mysql |
| MYSQL_PORT | MySQL 端口 | 3306 |
| MYSQL_USERNAME | MySQL 用户名 | root |
| MYSQL_PASSWORD | MySQL 密码 | root |
| REDIS_HOST | Redis 主机 | redis |
| REDIS_PORT | Redis 端口 | 6379 |
| ELASTICSEARCH_URIS | ES 地址 | http://elasticsearch:9200 |
| JAVA_MAX_MEM_RATIO | JVM 最大内存比例 | 75 |

### 前端 Nginx 配置

前端通过 nginx 反向代理后端 API，SSE 推送已配置支持：
- `proxy_buffering off` - 禁用缓冲，实时推送
- `proxy_read_timeout 3600s` - 长连接超时 1 小时
- `keepalive_timeout 3600` - 保持连接 1 小时

## 故障排查

### 后端启动失败

```bash
# 查看后端日志
docker-compose logs iboot-admin

# 检查数据库连接
docker-compose exec mysql mysql -uroot -proot -e "SHOW DATABASES;"

# 检查 Redis 连接
docker-compose exec redis redis-cli ping
```

### 前端无法访问后端

```bash
# 检查前端配置
docker-compose exec iboot-portal cat /etc/nginx/nginx.conf

# 测试后端连通性
docker-compose exec iboot-portal wget -q -O - http://iboot-admin:8080/actuator/health
```

### SSE 连接断开

检查 nginx 配置是否包含：
```nginx
proxy_buffering off;
proxy_set_header X-Accel-Buffering no;
proxy_read_timeout 3600s;
```

## 网络架构

所有服务都在 `iboot-network` 网络中：

```
┌─────────────────────────────────────────────────────┐
│                 iboot-network                       │
│                                                     │
│  ┌──────────────┐    ┌──────────────┐              │
│  │ iboot-portal │───▶│ iboot-admin  │              │
│  │     :80      │    │     :8080    │              │
│  └──────────────┘    └───────┬──────┘              │
│                              │                      │
│         ┌────────────────────┼────────────┐        │
│         │                    │            │         │
│    ┌────▼────┐         ┌────▼────┐  ┌────▼────┐   │
│    │  mysql  │         │  redis  │  │elastic  │   │
│    │  :3306  │         │  :6379  │  │  :9200  │   │
│    └─────────┘         └─────────┘  └─────────┘   │
│                                                     │
└─────────────────────────────────────────────────────┘
```

## 数据持久化

以下数据通过 Docker Volume 持久化：

- `mysql_data` - MySQL 数据
- `redis_data` - Redis 数据
- `minio_data` - MinIO 对象存储
- `prometheus_data` - Prometheus 监控数据
- `grafana_data` - Grafana 配置和面板
- `es_data` - Elasticsearch 日志数据
- `admin_logs` - 应用日志

## 清理数据

```bash
# 停止服务并删除容器
docker-compose down

# 停止服务并删除所有数据（谨慎使用）
docker-compose down -v
```
