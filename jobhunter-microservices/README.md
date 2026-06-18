# JobHunter Microservices Architecture

[![Java](https://img.shields.io/badge/Java-17-orange)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-green)](https://spring.io/projects/spring-boot)
[![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-2023.0-blue)](https://spring.io/projects/spring-cloud)
[![Docker](https://img.shields.io/badge/Docker-ready-blue)](https://www.docker.com/)
[![Kafka](https://img.shields.io/badge/Kafka-Event--Driven-purple)](https://kafka.apache.org/)

> **📖 [← Back to Main README](../README.md)** | **Architecture Evolution Journey: Monolith → Microservices**

---

## 🎯 Overview

This is the **microservices implementation** of JobHunter recruitment platform, demonstrating a complete Spring Cloud ecosystem with event-driven architecture.

**Key Achievements:**

- ✅ 6 independent business microservices
- ✅ Full Spring Cloud stack (Eureka, Gateway, OpenFeign, Config)
- ✅ Event-driven with Kafka (3 topics, producer-consumer patterns)
- ✅ CDC pipeline (Debezium -> Kafka -> Elasticsearch)
- ✅ Complete observability stack (Zipkin, Prometheus, Grafana, Loki)
- ✅ Database-per-service pattern with PostgreSQL
- ✅ Resilience patterns (Circuit Breaker, Retry, Fallback)
- ✅ Production-ready with Docker orchestration

### Why This Architecture?

```
Monolithic Limitations          Microservices Benefits
─────────────────────          ───────────────────────
❌ Single deployment unit    →  ✅ Independent deployments
❌ Scaling entire app        →  ✅ Scale services individually
❌ Technology lock-in        →  ✅ Polyglot architecture
❌ Single point of failure   →  ✅ Fault isolation
❌ Long build/test cycles    →  ✅ Faster CI/CD per service
```

---

## Tổng Quan

Hệ thống JobHunter là RESTful API được xây dựng bằng Spring Boot, cung cấp các tính năng quản lý tuyển dụng.

### Tính Năng

- API Gateway - Routing, Rate Limiting, Circuit Breaker, JWT Authentication
- Service Discovery - Netflix Eureka
- RBAC - Role & Permission based authorization
- Event-Driven - Kafka messaging
- Service Communication - OpenFeign + Kafka
- Distributed Tracing - Zipkin
- Resilience Patterns - Circuit Breaker, Retry, Fallback
- Object Storage - MinIO
- Monitoring - Actuator + Prometheus + Zipkin
- API Documentation - Swagger/OpenAPI 3.0
- Search Engine - Elasticsearch (near real-time index)

---

## Kiến Trúc

### Infrastructure Services

| Service           | Port      | Mô tả                                  |
| ----------------- | --------- | -------------------------------------- |
| **Eureka Server** | 8761      | Service Discovery                      |
| **API Gateway**   | 8080      | Entry point, routing, authentication   |
| **MySQL**         | 3306      | Database (4 DBs tự động tạo)           |
| **Redis**         | 6379      | Caching & Rate Limiting                |
| **Kafka**         | 9092      | Event streaming                        |
| **Zookeeper**     | 2181      | Kafka coordination                     |
| **Kafka Connect** | 8088      | Debezium CDC connectors                |
| **Elasticsearch** | 9200      | Search index                           |
| **Kibana**        | 5601      | Elasticsearch UI                        |
| **MinIO**         | 9000/9001 | Object Storage (minioadmin/minioadmin) |
| **Zipkin**        | 9411      | Distributed Tracing                    |
| **Prometheus**    | 9090      | Metrics Collection                     |
| **Loki**          | 3100      | Log Aggregation                        |
| **Grafana**       | 3000      | Monitoring Dashboard (admin/admin)     |
| **Kafka UI**      | 8090      | Kafka Management                       |

### Business Services

| Service                  | Port | Database   | Mô tả                | Kafka               |
| ------------------------ | ---- | ---------- | -------------------- | ------------------- |
| **Auth Service**         | 8081 | auth_db    | User/Role/Permission | -                   |
| **Company Service**      | 8082 | company_db | Company Management   | -                   |
| **Job Service**          | 8083 | job_db     | Job & Skill          | Producer + Consumer |
| **Resume Service**       | 8084 | resume_db  | Resume/CV            | Producer            |
| **File Service**         | 8085 | -          | File Upload/Download | -                   |
| **Notification Service** | 8086 | -          | Email                | Consumer            |
| **Search Service**       | 8087 | -          | Job Search API       | CDC Consumer        |

---

## Event-Driven với Kafka

### Kafka Topics

| Topic                   | Producer          | Consumer             | Mô tả                     |
| ----------------------- | ----------------- | -------------------- | ------------------------- |
| **job-created**         | Job Service       | Notification Service | Thông báo job mới         |
| **job-applications**    | Resume Service    | Job Service          | Thông báo ứng viên nộp CV |
| **email-notifications** | Multiple Services | Notification Service | Email queue               |
| **job_db.public.jobs**  | Debezium CDC      | Search Service       | Đồng bộ index jobs        |

### CDC Search Pipeline

```
Job Service (PostgreSQL write)
  -> Debezium (Kafka Connect)
  -> Kafka topic: job_db.public.jobs
  -> Search Service CDC consumer
  -> Elasticsearch index: jobs
  -> Search API: /api/v1/search/jobs
```

### Communication Patterns

**Synchronous (OpenFeign):**

```
Resume Service ──> Job Service (get job details)
Job Service ──> Company Service (get company info)
Resume Service ──> Auth Service (get user info)
```

**Asynchronous (Kafka):**

```
Job Service ──> Notification Service (job alerts)
Resume Service ──> Job Service (application stats)
Any Service ──> Notification Service (emails)
```

---

## Quick Start

### 1. Chuẩn bị

```bash
# Copy và chỉnh sửa .env
cp .env.example .env
# Thêm MAIL_USERNAME và MAIL_PASSWORD
```

**Lấy Gmail App Password:** https://myaccount.google.com/apppasswords

### 2. Build & Run

```bash
# Build tất cả services
build-all-services.bat

# Khởi động
docker-compose up -d

# Đăng ký Debezium connector cho job_db.jobs
powershell -ExecutionPolicy Bypass -File .\scripts\register-job-cdc-connector.ps1

# Kiểm tra
docker-compose ps
docker-compose logs -f
```

### 3. Truy cập

| Service         | URL                                   | Credentials           |
| --------------- | ------------------------------------- | --------------------- |
| **Eureka**      | http://localhost:8761                 | -                     |
| **API Gateway** | http://localhost:8080                 | -                     |
| **Swagger UI**  | http://localhost:8080/swagger-ui.html | -                     |
| **Search API**  | http://localhost:8080/api/v1/search/jobs?q=java | -          |
| **Kibana**      | http://localhost:5601                 | -                     |
| **Zipkin**      | http://localhost:9411                 | -                     |
| **MinIO**       | http://localhost:9001                 | minioadmin/minioadmin |
| **Grafana**     | http://localhost:3000                 | admin/admin           |
| **Prometheus**  | http://localhost:9090                 | -                     |
| **Kafka UI**    | http://localhost:8090                 | -                     |

> 📚 **API Documentation**: Xem [SWAGGER_GUIDE.md](./SWAGGER_GUIDE.md) để biết chi tiết cách sử dụng Swagger UI

### 4. Dừng

```bash
docker-compose down        # Dừng
docker-compose down -v     # Dừng + xóa data
```

---

## Development Mode

```bash
# Infrastructure
docker-compose up -d mysql redis kafka zookeeper minio zipkin

# Services (mỗi service một terminal)
cd eureka-server && gradlew bootRun
cd api-gateway && gradlew bootRun
cd auth-service && gradlew bootRun
cd company-service && gradlew bootRun
cd job-service && gradlew bootRun
cd resume-service && gradlew bootRun
cd file-service && gradlew bootRun
cd notification-service && gradlew bootRun
```

---

## API Endpoints

Tất cả qua Gateway: `http://localhost:8080`

### Authentication

```
POST   /api/v1/auth/register
POST   /api/v1/auth/login
GET    /api/v1/auth/refresh
POST   /api/v1/auth/logout
GET    /api/v1/auth/account
```

### Users

```
GET    /api/v1/users              # Admin
POST   /api/v1/users              # Admin
GET    /api/v1/users/{id}
PUT    /api/v1/users
DELETE /api/v1/users/{id}         # Admin
```

### Companies

```
GET    /api/v1/companies
POST   /api/v1/companies          # HR
GET    /api/v1/companies/{id}
PUT    /api/v1/companies          # HR
DELETE /api/v1/companies/{id}     # Admin
```

### Jobs

```
GET    /api/v1/jobs               # Public
POST   /api/v1/jobs               # HR
GET    /api/v1/jobs/{id}
PUT    /api/v1/jobs               # HR
DELETE /api/v1/jobs/{id}          # HR
GET    /api/v1/skills
POST   /api/v1/skills             # Admin
```

### Search

```
GET    /api/v1/search/jobs
```

### Resumes

```
GET    /api/v1/resumes
POST   /api/v1/resumes
GET    /api/v1/resumes/{id}
PUT    /api/v1/resumes
DELETE /api/v1/resumes/{id}
GET    /api/v1/resumes/by-user
```

### Files

```
POST   /api/v1/files/upload
GET    /api/v1/storage/{filename}
```

### Notifications

```
POST   /api/v1/subscribers        # Public
GET    /api/v1/subscribers        # Admin
PUT    /api/v1/subscribers
DELETE /api/v1/subscribers/{id}
```

---

## Monitoring & Observability

Hệ thống tích hợp **Grafana + Prometheus + Loki** để monitoring và logging.

### Dashboards

1. **JobHunter Microservices Overview**
    - Service health và uptime
    - Request rate và latency (p95)
    - Error rate
    - JVM memory và threads
    - Database connection pool

2. **JobHunter Logs Dashboard**
    - Centralized logs từ tất cả services
    - Log volume by service
    - Error và warning logs
    - Searchable với LogQL

3. **JobHunter Resilience Dashboard**
    - Circuit breaker states
    - Failure rate
    - Retry mechanism
    - Rate limiter metrics

### Access Monitoring

```bash
# Grafana Dashboard
http://localhost:3000
# Login: admin/admin
# Navigate to Dashboards → Browse

# Prometheus Metrics
http://localhost:9090

# Example Queries:
# - Service uptime: up{application="jobhunter"}
# - Request rate: rate(http_server_requests_seconds_count[1m])
# - Memory usage: jvm_memory_used_bytes

# Loki Logs (via Grafana Explore)
# {application="jobhunter", level="ERROR"}
# {service="job-service"}
```

---

## Databases

| Database       | Service         | Mô tả                                  |
| -------------- | --------------- | -------------------------------------- |
| **auth_db**    | Auth Service    | users, roles, permissions, subscribers |
| **company_db** | Company Service | companies                              |
| **job_db**     | Job Service     | jobs, skills                           |
| **resume_db**  | Resume Service  | resumes                                |

---

## Kiến Trúc

```
Client → API Gateway (8080) → Eureka (8761) → Services
                ↓
         Rate Limiting (Redis)
         Circuit Breaker
         JWT Auth

Services:
Auth (8081) ──┐
Company (8082)│
Job (8083) ───┼──> Kafka ──> Notification (8086)
Resume (8084) │
File (8085) ──┘

Infrastructure:
MySQL (auth_db, company_db, job_db, resume_db)
Redis (cache, rate limiting)
Kafka + Zookeeper (events)
MinIO (files)
Zipkin (tracing)
```

---

## License

MIT License
