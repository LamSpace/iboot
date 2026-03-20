# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

iBoot is a management system based on Vue 3 and Spring Boot 4.0.3, following DDD (Domain-Driven Design) architecture. It provides RBAC-based user/role/menu management, organizational structure management, file management, monitoring, and more.

**Tech Stack:**
- **Backend**: Spring Boot 4.0.3, JDK 25, MyBatis, Redis, MySQL 8, Flyway
- **Frontend**: Vue 3, TypeScript 5, Element Plus, Vite 5, Pinia
- **Infrastructure**: Docker, Prometheus, Grafana, ELK Stack, MinIO

## Build & Run Commands

### Backend (iboot-admin)

```bash
cd iboot-admin

# Run with Maven (dev profile)
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Skip tests during build
mvn clean package -DskipTests
```

Maven配置文件地址：/home/lam/repo/settings.xml。任何Maven命令行均可以使用该配置文件。

### Frontend (iboot-portal)

```bash
cd iboot-portal

# Install dependencies (use cnpm for faster download in China)
cnpm install
# or
npm install --registry=https://registry.npmmirror.com

# Development server
cnpm run dev

# Build production
cnpm run build

# Type check
cnpm run type-check

# Format
cnpm run format
```

### Docker Deployment

```bash
cd docker

# Start all services (MySQL, Redis, MinIO, Prometheus, Grafana, ELK, etc.)
docker compose up -d --pull=never

# Check status
docker compose ps

# View logs
docker compose logs -f

# Stop all
docker compose down
```

## Architecture Overview

### Backend DDD Layers (iboot-admin)

```
com.iboot.admin
├── application/           # Application Layer - business flow orchestration
│   ├── service/         # Application services (@Service)
│   └── dto/             # Application DTOs and commands
├── domain/               # Domain Layer - core business logic (no external deps)
│   ├── model/           # Domain models
│   │   ├── entity/      # Entities (User, Role, Menu, Dept, etc.)
│   │   └── vo/          # Value Objects
│   ├── repository/      # Repository interfaces
│   └── service/         # Domain services
├── infrastructure/       # Infrastructure Layer - persistence & external services
│   ├── persistence/     # MyBatis mappers, POs
│   ├── repository/      # Repository implementations
│   ├── security/        # Spring Security, JWT
│   └── cache/           # L1 (Caffeine) + L2 (Redis) cache
├── interfaces/           # Interface Layer - controllers, request/response DTOs
│   ├── controller/      # REST controllers
│   └── dto/             # Request/Response DTOs
└── common/               # Shared utilities
    ├── exception/       # Global exception handling
    ├── constant/        # Constants
    ├── enums/           # Enumerations
    └── util/            # Utility classes
```

**Layer Dependencies:**
- `interfaces` → `application` → `domain` ← `infrastructure` (implements domain interfaces)
- Domain layer is pure (no Spring/MyBatis dependencies)

### Frontend Structure (iboot-portal)

```
src/
├── api/          # API封装
├── components/   # Vue components
├── composables/  # Composition API functions
├── directives/   # Custom directives (e.g., v-permission)
├── router/       # Vue Router config
├── stores/       # Pinia stores (user, permission, dict)
├── types/        # TypeScript types
├── utils/        # Utilities (request.ts for Axios)
└── views/        # Page components
```

## Key Technologies

### Backend
- **Security**: Spring Security 7 + JWT (JJWT 0.13.0)
- **ORM**: MyBatis 3.5 + PageHelper
- **Cache**: Caffeine (L1) + Redis (L2) with Pub/Sub for invalidation
- **DB Migration**: Flyway (scripts in `src/main/resources/db/migration/`)
- **API Docs**: SpringDoc OpenAPI (Swagger UI at `/swagger-ui.html`)
- **Monitoring**: Micrometer + Prometheus + Spring Boot Admin
- **Object Storage**: MinIO
- **Logging**: Logback + Logstash → Elasticsearch → Kibana

### Frontend
- **HTTP**: Axios with interceptors (token injection, error handling)
- **State**: Pinia for global state
- **UI**: Element Plus components
- **3D**: Three.js for visual effects

## Database

Flyway migration scripts location:
```
iboot-admin/src/main/resources/db/migration/
├── V1__initial_schema.sql
├── V2__initial_data.sql
├── V3__add_minio_config.sql
├── V4__add_operate_log_cost_time.sql
├── V5__add_minio_monitor_menu.sql
├── V6__add_export_permissions.sql
├── V7__add_observability_monitor_menus.sql
├── V8__add_alertmanager_menu.sql
├── V9__add_dept_id_to_logs.sql
├── V10__add_dept_id_to_post.sql
├── V11__add_dept_id_to_job_and_file.sql
├── V12__add_deleted_column_to_logs.sql
├── V13__add_minio_monitor_enhanced.sql
└── V14__add_thanos_query_menu.sql
```

**Default admin account:**
- Username: `admin`
- Password: `admin123456`
