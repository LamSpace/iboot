# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

iBoot is a modern management system built with Vue 3 + TypeScript (frontend) and Spring Boot 3.5.9 + JDK 25 (backend). The project follows DDD (Domain-Driven Design) architecture with RBAC-based permission control.

## Commands

### Prerequisites
- JDK 25 (minimum 21)
- Node.js 20 LTS
- MySQL 8.x
- Redis 7.x
- Maven 3.9+

### Development

```bash
# Install dependencies (Node.js, cnpm, Maven)
./scripts/install-dependencies.sh

# Frontend development
cd iboot-portal
npm install              # Install dependencies (or: cnpm install)
npm run dev              # Start dev server at http://localhost:3000
npm run build            # Production build
npm run lint             # ESLint check
npm run format           # Prettier format

# Backend development
cd iboot-admin
mvn clean install -DskipTests
mvn spring-boot:run -Dspring-boot.run.profiles=dev
# Or run IbootAdminApplication.java directly from IDE

# Build both
./scripts/build.sh all
./scripts/build.sh frontend    # Frontend only
./scripts/build.sh backend     # Backend only
```

### Docker Deployment

```bash
cd docker
docker-compose up -d       # Start all services
docker-compose ps          # Check service status
docker-compose down        # Stop all services
docker-compose logs -f     # View logs
```

Services: MySQL (3306), Redis (6379), MinIO (9000/9001), Prometheus (9090), Grafana (3000), Elasticsearch (9200), Kibana (5601)

### Testing & Verification

```bash
# Backend health check
curl http://localhost:8080/actuator/health

# Access Swagger UI
http://localhost:8080/swagger-ui.html

# Default credentials
Username: admin
Password: admin123
```

## Architecture

### DDD Layer Structure

```
com.iboot.admin/
├── interfaces/           # REST controllers, DTOs, VOs
├── application/          # Application services, transaction management
├── domain/               # Domain models, repository interfaces, domain services
└── infrastructure/       # Repository implementations, security, config, persistence
```

**Key Packages:**
- `interfaces/controller` - REST API endpoints
- `application/service` - Business logic orchestration
- `domain/*/model` - Domain entities (User, Role, Menu, Dept, etc.)
- `domain/*/repository` - Repository interfaces (DDD style)
- `infrastructure/persistence` - MyBatis mappers, POs, repository implementations
- `infrastructure/security` - JWT auth, Spring Security config
- `common/` - Shared utilities, exceptions, result wrappers, enums

### Frontend Structure

```
iboot-portal/src/
├── api/             # API client modules
├── components/      # Reusable Vue components
├── views/           # Page components
├── router/          # Vue Router config
├── stores/          # Pinia state management
├── utils/           # Utilities (axios wrapper, SSE client)
└── assets/          # Static resources
```

### Core Technologies

| Layer | Technology |
|-------|------------|
| Backend | Spring Boot 3.5.9, JDK 25, MyBatis-Plus, Redis, Caffeine (L2 cache) |
| Security | Spring Security 6.x, JWT (JJWT 0.12.5), BCrypt |
| Frontend | Vue 3, TypeScript 5.4, Element Plus, Pinia, Vite 5 |
| Database | MySQL 8.x, Flyway (migrations), Druid (connection pool) |
| Storage | MinIO (object storage), Elasticsearch 8.17 |
| Messaging | SSE (Server-Sent Events), Redis Pub/Sub (cluster) |
| Monitoring | Micrometer, Prometheus, Grafana, ELK Stack |
| Tools | MapStruct 1.6 (object mapping), Lombok |

### Key Features

- **RBAC Permission Model**: User-Role-Menu hierarchy with data scope control
- **Two-Level Cache**: Caffeine (L1) + Redis (L2) with distributed invalidation via Redis Pub/Sub
- **SSE Push**: Real-time notifications, message read status sync, cluster support
- **CloudEvents**: Event-driven architecture for decoupled business logic
- **Flyway Migrations**: Database version control in `src/main/resources/db/migration/`
- **MapStruct**: Compile-time object mapping between DTOs and entities

### Common Development Patterns

**Repository Pattern (DDD):**
```java
// Domain layer: interface
public interface UserRepository {
    User findById(Long id);
    void save(User user);
    boolean existsByUsername(String username);
}

// Infrastructure layer: implementation
@Repository
public class UserRepositoryImpl implements UserRepository {
    @Autowired private UserMapper userMapper;
    // ...
}
```

**MapStruct Mapping:**
```java
@Mapper(componentModel = "spring")
public interface UserMapper {
    User toEntity(CreateUserRequest request);
    UserResponse toResponse(User user);
    List<UserResponse> toResponseList(List<User> users);
}
```

**SSE Push (Cluster-Aware):**
```java
@Autowired private PushEventService pushService;

// Send to specific user
pushService.sendToUser(userId, PushEvent.builder()
    .eventType("com.iboot.push.new.message")
    .data(messageDTO)
    .build());
```

## File Conventions

- **Backend**: Java entities in `domain/*/model`, POs in `infrastructure/persistence/po`
- **Frontend**: API calls in `src/api/`, components in `src/components/`
- **Migrations**: `V{version}__description.sql` for new migrations, `R__description.sql` for repeatable
- **Config**: `application.yml` (base), `application-dev.yml` (dev), `application-prod.yml` (prod)
