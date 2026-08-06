# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

JobHunter Microservices is the Spring Cloud rewrite of a monolithic recruitment platform (see `../README.md` for the monolith → microservices story). It's a Java 17/21, Spring Boot 3.2 system: 9 Gradle-built services behind an API Gateway, using Eureka for discovery, Kafka for async events + CDC-driven search indexing, PostgreSQL (database-per-service), Redis, MinIO, and an observability stack (Zipkin/Prometheus/Loki/Grafana — several of these are commented out in `docker-compose.yml` and must be re-enabled to use).

## Commands

### Build
```bash
build-all-services.bat              # builds all 9 services (gradlew clean build -x test), Windows only
cd <service> && gradlew.bat build   # build a single service
```

### Test
```bash
cd <service> && gradlew.bat test                          # run all tests in a service
cd <service> && gradlew.bat test --tests "SomeClassTest"   # run a single test class
```
Tests use JUnit 5 (`useJUnitPlatform()` in every `build.gradle.kts`).

### Run (full stack via Docker)
```bash
cp .env.example .env        # set MAIL_USERNAME / MAIL_PASSWORD (Gmail app password) for notification-service
docker-compose up -d
powershell -ExecutionPolicy Bypass -File .\scripts\register-job-cdc-connector.ps1   # required once, wires Debezium -> job_db.jobs
```

### Run (local dev, mixed)
```bash
docker-compose up -d postgres redis kafka zookeeper minio
cd eureka-server && gradlew.bat bootRun     # start first
cd api-gateway && gradlew.bat bootRun       # then gateway
cd auth-service && gradlew.bat bootRun      # then any business service(s) needed
```
Each service registers with Eureka at `http://localhost:8761/eureka/` and expects its own Postgres DB (`auth_db`, `company_db`, `job_db`, `resume_db` — created automatically by `docker/postgres/init`).

### Benchmarking
`scripts/benchmark-*.ps1` — PowerShell load-test scripts for critical flows (auth, job search).

## Architecture

### Service map (all routed through API Gateway on :8080)
| Service | Port | Owns DB | Kafka role |
|---|---|---|---|
| eureka-server | 8761 | — | — |
| api-gateway | 8080 | — | — |
| auth-service | 8081 | auth_db | — |
| company-service | 8082 | company_db | — |
| job-service | 8083 | job_db | producer (`job-created`) + consumer (`job-applications`) |
| resume-service | 8084 | resume_db | producer (`job-applications`) |
| file-service | 8085 | — (MinIO) | — |
| notification-service | 8086 | — | consumer (`job-created`, `email-notifications`) |
| search-service | 8087 | — (Elasticsearch) | CDC consumer of `job_db.public.jobs` |

Every service is a standalone Gradle project with its own `build.gradle.kts`, `Dockerfile`, and `src/main/java/vn/hoidanit/<service>/...` package — there is no shared/common module; cross-service contracts are duplicated per service (DTOs, Feign clients) rather than shared as a library.

### Request flow
`Client → API Gateway (8080)` — Spring Cloud Gateway does routing (`lb://<service>` via Eureka), JWT validation, Redis-backed rate limiting, and circuit breaking before requests reach a business service. Route/predicate definitions live in `api-gateway/src/main/resources/application.yml`.

### Inter-service communication
- **Synchronous (OpenFeign)**: resume-service → job-service (job details), job-service → company-service (company info), resume-service → auth-service (user info). Feign clients live under each consuming service's `client/` package.
- **Asynchronous (Kafka)**: job-service → notification-service (job alerts), resume-service → job-service (application stats/counts), any service → notification-service (email queue). Producers/consumers live under `kafka/producer` and `kafka/consumer` packages per service.

### CDC search pipeline (the non-obvious part)
Job data is **not** written to Elasticsearch directly. Flow: `job-service writes to Postgres job_db` → Debezium (via `kafka-connect`, registered through `scripts/register-job-cdc-connector.ps1`) → Kafka topic `job_db.public.jobs` → search-service's CDC consumer → Elasticsearch `jobs` index → exposed via `GET /api/v1/search/jobs`. If search results look stale, check the Debezium connector status at `http://localhost:8088/connectors`, not job-service.

### Per-service internal layout
Standard layering (seen in job-service, applies across services): `controller/ → service/ → repository/`, plus `domain/` (entities + `domain/response/` DTOs), `dto/` (request DTOs), `client/` (Feign clients), `kafka/{producer,consumer}/`, `filter/` and `config/` (security, Redis, Kafka, etc.), `exception/`, `util/` + `util/constant/`, and custom `annotation/` + `resolver/` for filtering support (`turkraft.springfilter` is used for dynamic query filtering, e.g. `/api/v1/jobs?filter=...`).

### Databases
Postgres (single container, `wal_level=logical` for Debezium) hosts one schema/DB per service: `auth_db` (users/roles/permissions/subscribers), `company_db` (companies), `job_db` (jobs/skills), `resume_db` (resumes). file-service and notification-service are stateless (MinIO/SMTP only). No cross-service DB joins — access other services' data via Feign or Kafka, never direct SQL.

### Auth
JWT-based (`spring-security-oauth2-resource-server` + `-jose`), issued/validated via auth-service, enforced at the gateway and re-validated per service. RBAC (role/permission based authorization) is modeled in auth-service (`auth_db`).

### Observability
Zipkin tracing and Prometheus metrics are wired into every service (`micrometer-tracing-bridge-brave`, `micrometer-registry-prometheus`) but several supporting containers (zipkin, prometheus, loki, promtail, grafana) are **commented out** in `docker-compose.yml` — uncomment them if you need tracing/metrics/log dashboards locally.

### API docs
Swagger/OpenAPI per service, aggregated at the gateway: `http://localhost:8080/swagger-ui.html`. See `SWAGGER_GUIDE.md` for details.
