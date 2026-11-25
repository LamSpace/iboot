<div align="center">

# iBoot

**An engineering project that treats Agent, Cloud, Security, Micro Service and SRE Native as default system properties**

Not just another admin panel: on a working management platform, it turns service boundaries, zero trust, AI as an execution unit, observability and cloud-native delivery into engineering capabilities with explicit acceptance criteria.

[English](README.md) · [中文](README.zh-CN.md)

<p align="center">
  <a href="https://github.com/vuejs/core"><img src="https://img.shields.io/badge/Vue.js-3.4-brightgreen?style=flat-square&logo=vue.js&logoColor=white" alt="Vue.js"></a>&nbsp;
  <a href="https://github.com/spring-projects/spring-boot"><img src="https://img.shields.io/badge/Spring%20Boot-4.0.3-blue?style=flat-square&logo=spring-boot&logoColor=white" alt="Spring Boot"></a>&nbsp;
  <a href="https://github.com/alibaba/spring-cloud-alibaba"><img src="https://img.shields.io/badge/Spring%20Cloud%20Alibaba-2025.1.0.0-orange?style=flat-square" alt="Spring Cloud Alibaba"></a>&nbsp;
  <a href="https://www.java.com/"><img src="https://img.shields.io/badge/Java-25-red?style=flat-square&logo=openjdk&logoColor=white" alt="Java 25"></a>&nbsp;
  <a href="https://www.docker.com/"><img src="https://img.shields.io/badge/Docker-Compose-2496ED?style=flat-square&logo=docker&logoColor=white" alt="Docker"></a>&nbsp;
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-Apache%202.0-blue?style=flat-square" alt="License"></a>
</p>

<p align="center">
  <a href="#why-it-stands-out">Why it stands out</a> •
  <a href="#the-five-natives-at-a-glance">Five Natives</a> •
  <a href="#architecture">Architecture</a> •
  <a href="#tech-stack">Tech stack</a> •
  <a href="#core-capabilities">Capabilities</a> •
  <a href="#quick-start">Quick start</a> •
  <a href="#documentation-map">Docs</a>
</p>

</div>

## Why it stands out

- **The five Natives are acceptance criteria, not slogans.** Agent, Cloud, Security, Micro Service and SRE each define what "done" means. A capability that has not met its criteria does not enter the completed list — installing a component is not the same as having the capability.
- **The microservices are actually split.** Eight independently deployable business services, a gateway and a side-channel operations console — ten deployable services in total. DDD layers, table ownership, Seata AT + TCC, and a producer-side outbox with RocketMQ run on real request paths, not just in the directory tree.
- **Zero trust is the default.** OAuth2/OIDC + PKCE, service-to-service mTLS, SPIFFE workload identity, path-level authorization, centralized OPA decisions, per-service database accounts, table/key-level least privilege, three-phase CA rotation and automatic SVID refresh. It is on by default in local development and can be rolled back with a single switch.
- **AI is a first-class execution unit.** A dedicated `iboot-ai` service provides streaming chat, hybrid-retrieval RAG, governed tool orchestration and a knowledge graph. Tool calls are allow-listed, writes require confirmation, runs are audited and deterministically replayable, and retrieval inherits the caller's RBAC and data scope. A skill library and MCP client are the next roadmap items.
- **The engineering loop is verifiable.** `dev-init.sh` brings up the whole local stack with one command. Full-stack regression covers contract snapshots, path conservation, route coverage, TLS/authorization, smoke tests and a permission matrix. A CLI plus personal access tokens enable browserless automation. Every capability contract is recorded under `openspec/`.

> This is a hands-on engineering project, not a production product. Its capabilities target reproducible, verifiable engineering criteria; boundaries and trade-offs are recorded in the [ROADMAP](ROADMAP.md).

## The five Natives at a glance

| Capability line | Already in place | Next |
|---|---|---|
| **Agent Native** | Streaming chat, knowledge-base RAG (BM25 + kNN + RRF), governed tool orchestration, organization/RBAC knowledge graph | Skill library (SKILL), MCP client, Agent Graph, cross-service agentification |
| **Cloud Native** | Full local Compose environment, shared image template, one-command deployment | Helmfile + ACK, HPA, GitOps (Argo CD), GraalVM Native |
| **Security Native** | OAuth2/OIDC + PKCE, mTLS/SPIFFE, OPA, per-service credentials, certificate rotation, CLI/PAT | Token Exchange (RFC 8693), multi-client sessions, WeChat Mini Program identity |
| **Micro Service Native** | Business-service split, Nacos discovery, Sentinel governance, Seata AT/TCC, outbox + RocketMQ, contract/route regression | Consumer-driven contract tests, EventStorming boundary review, sharding |
| **SRE Native** | Metrics, logs, tracing, alerting, backup and restore drills | SLI/SLO, burn-rate alerts, trace-log correlation, cost and elasticity visibility |

The full roadmap, trigger conditions, version matrix and risk register are in [ROADMAP](ROADMAP.md) (Chinese).

## Architecture

```text
Browser / CLI
   │  HTTPS · OAuth2/OIDC + PKCE
   ▼
iboot-portal ── /iboot/api ──▶ iboot-gateway :8088
                                      │ Nacos discovery · route rate limiting · SSE passthrough
                                      ▼
        ┌──────────────────────────────────────────────────┐
        │  Eight independently deployable business services  │
        │  system 8080   ai 8081        file 8082           │
        │  auth 8083     notify 8084    job 8085            │
        │  workflow 8086 monitor 8087                       │
        └──────────────────────────────────────────────────┘
                    │                          │
                    ▼                          ▼
   iboot-ops :8089 (side console)       MySQL · Redis · Elasticsearch · Silo
                                        Nacos · RocketMQ · Kafka · Seata
                                        OPA · SPIRE · Tempo
                                        Prometheus · Grafana · ELK
```

- The portal is the only public entry point and reverse-proxies `/iboot/api` to the gateway. Services find each other by Nacos service name and do not route through the gateway.
- `iboot-ops`, the Sentinel dashboard and the Nacos/Sentinel consoles are side channels: same-origin proxying in development, internal-only in production.
- All services share one database, `iboot`, but each table has exactly one owner. Read-only sharing is a long-term accepted mode; the contract is in [docs/23 §11.12](<./docs/23-微服务底座操作指南.md>) (Chinese).
- The authoritative service/port list is [docs/23 §8.1](<./docs/23-微服务底座操作指南.md>); container and network boundaries are in [`docker/README.md`](docker/README.md).

## Tech stack

| Layer | Choices |
|---|---|
| Runtime | Java 25 (virtual threads), Spring Boot 4.0.3, Spring Framework 7, Spring Security 7 |
| Microservices | Spring Cloud 2025.1.0, Spring Cloud Alibaba 2025.1.0.0, Nacos 3.3.0, Sentinel, Seata, RocketMQ, Kafka |
| Data | MySQL 8.4, Redis 8.8 + Caffeine, Elasticsearch 9.5, Silo (MinIO-compatible), Neo4j 5.26, Flowable 8, Quartz |
| Security | OAuth2/OIDC + PKCE, mTLS, SPIFFE, OPA, SPIRE, per-service database accounts, CA/certificate rotation |
| AI | Spring AI 2.0.1, AgentScope Java 2.0.1, Bailian Qwen, BM25 + kNN + RRF hybrid retrieval, Neo4j knowledge graph |
| Observability | Actuator + Micrometer, Prometheus / Grafana / Alertmanager / Thanos, ELK, Grafana Tempo |
| Frontend | Vue 3.4, TypeScript 5.4, Vite 5.2, Element Plus, Pinia, vue-i18n |

The full version matrix and selection rationale are in [docs/02](docs/02-技术栈.md) (Chinese); version locks and upgrade constraints are in [ROADMAP §9](ROADMAP.md#9-版本矩阵).

## Core capabilities

- **System and organization**: users, roles, menus, department tree, positions, dictionaries, parameters; RBAC and five-level data scope; dashboards and Excel export.
- **Messaging and notifications**: in-app messages, announcements, templates, multi-channel delivery (in-app/email/DingTalk/SMS), SSE unicast/multicast/broadcast with cluster-wide broadcast, full-text search.
- **Files and search**: upload, download, preview, categorization; Silo / local / OSS storage strategies; Elasticsearch content search.
- **Approvals and jobs**: Flowable 8 leave approval, approval center, rejection and archiving; Quartz clustered jobs; backup and restore drills.
- **Monitoring and operations**: login/operation/runtime logs, online sessions and forced logout, service and middleware resource checks, side-channel operations console.
- **AI workbench**: multi-turn streaming chat, knowledge-base RAG, governed agent tools, organization/RBAC knowledge graph, daily quotas and token visibility.
- **Developer CLI**: authorization-code + PKCE login, multiple contexts, read-only queries, operational write commands, personal access tokens (PAT) and the `iboot api` escape hatch.
- **Service governance**: route-level rate limiting at the gateway, cross-service circuit breaking and degradation, Nacos as the rule source, Sentinel dashboard, reliable approval-to-notification delivery.

Module-level feature details are in [docs/04](docs/04-功能模块.md); the AI workbench in [docs/22](<./docs/22-AI 工作台操作指南.md>); the CLI in [docs/25](<./docs/25-开发者 CLI 操作指南.md>).

## Quick start

**Prerequisites**: JDK 25, Maven 3.9+, Docker and Docker Compose, Node.js 18+ with cnpm (frontend only).

```bash
# 1. Prepare local configuration and secrets
cp .env.example .env
#    At minimum, fill in the Bailian API key; the rest is optional

# 2. Generate the local CA/certificates and trust the CA in your browser (one-time)
bash scripts/tls-init.sh
CAROOT=docker/certs/ca mkcert -install

# 3. Start middleware, Sentinel, database migration, the eight services, gateway and ops console
bash scripts/dev-init.sh

# 4. Start the frontend when needed (HTTPS dev server)
bash scripts/dev-init.sh --frontend
```

- Frontend: <https://localhost:5173>
- Default administrator: `admin` / `admin123456` (change it immediately after the first login)
- Gateway health: <https://localhost:8088/actuator/health> (internal address, client certificate required)
- Common flags: `--status`, `--stop` / `--down` / `--purge`, `--build`.

Full startup, manual startup, database initialization and troubleshooting are in [docs/05](docs/05-快速开始.md); certificates and trust are in [docs/27](<./docs/27-TLS 与零信任操作指南.md>).

## Build, test and regression

```bash
# Build frontend and backend
bash scripts/build.sh

# Full backend build and a single-module test
mvn clean package
mvn -pl iboot-system test

# Full-stack regression: contract snapshots → path conservation → route coverage
# → TLS/authorization → smoke → permission matrix
bash scripts/regress/regress.sh
bash scripts/regress/cli.sh
```

Regression requires the eight business services, gateway, ops console and middleware to be running. Use `--refresh` only when interface changes are confirmed to be intentional. CI lives in `.github/workflows/regress.yml`. See [docs/23 §1](<./docs/23-微服务底座操作指南.md>) and [`scripts/README.md`](scripts/README.md) for the regression layers and script inventory.

## Deployment

- **Local full stack**: `scripts/docker-deploy.sh up` builds images and starts the Compose stack. Containers, ports, volumes and troubleshooting are in [`docker/README.md`](docker/README.md); environments and production guidance are in [docs/08](docs/08-部署指南.md).
- **Cloud native (planned)**: Helmfile + ACK + HPA + Argo CD, with the same images and different values locally (Kind) and in the cloud (ACK). See [ROADMAP §5 Cloud Native](ROADMAP.md#cloud-native).

## Documentation map

| Document | Content |
|---|---|
| [docs/README.md](docs/README.md) | Entry point and reading paths for all project docs |
| [ROADMAP.md](ROADMAP.md) | Five Native roadmap, version matrix, risks and decision records |
| [docs/03](docs/03-架构设计.md) | Module topology, DDD layers, data flow, permission model |
| [docs/23](<./docs/23-微服务底座操作指南.md>) | Service inventory, Nacos, gateway, tracing, Sentinel, Seata, least privilege |
| [docs/27](<./docs/27-TLS 与零信任操作指南.md>) | Local CA, mTLS, SPIFFE, OPA, rotation and troubleshooting |
| [docs/11](docs/11-可观测性设计.md) | Metrics, logs, tracing, alerting and Grafana dashboards |
| [docs/22](<./docs/22-AI 工作台操作指南.md>) | Model configuration, RAG, agents and knowledge graph |
| [docs/25](<./docs/25-开发者 CLI 操作指南.md>) | CLI installation, login, contexts, commands and security boundary |
| [openspec/specs](openspec/specs) | Capability contracts and change records |

> Most linked documents are currently written in Chinese; the Chinese README is at [README.zh-CN.md](README.zh-CN.md).

## Project layout

```text
iboot/
├── pom.xml                  # Parent POM aggregating 16 Maven modules
├── bin/iboot                # Developer CLI launcher
├── docker/                  # Compose orchestration, image templates and middleware init
├── scripts/                 # Development, deployment and regression scripts
├── docs/                    # Project documentation; entry point docs/README.md
├── openspec/                # Capability contracts and change records
├── iboot-kernel/            # Shared kernel library (not deployed)
├── iboot-service-authz/     # Service-to-service authorization library (not deployed)
├── iboot-logging/           # Shared logging library (not deployed)
├── iboot-sentinel/          # Governance metrics bridge (not deployed)
├── iboot-db-init/           # Database creation and centralized migration; exits after run
├── iboot-gateway/           # Gateway :8088
├── iboot-system/            # System domain :8080
├── iboot-ai/                # AI workbench :8081
├── iboot-file/              # File service :8082
├── iboot-auth/              # Authorization server :8083
├── iboot-notify/            # Messaging and notifications :8084
├── iboot-job/               # Scheduling and backup :8085
├── iboot-workflow/          # Approval workflow :8086
├── iboot-monitor/           # Monitoring and logs :8087
├── iboot-ops/               # Operations console :8089
├── iboot-cli/               # Developer command-line tool (not deployed)
└── iboot-portal/            # Frontend (Vue 3 + Vite)
```

Business services follow the four DDD layers: `interfaces` → `application` → `domain` → `infrastructure`. The gateway and operations console have no business domain and do not force-fit those layers.

## License

Licensed under the Apache License, Version 2.0. See [LICENSE](LICENSE).
