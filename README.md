# 🚀 JobHunter - Full-Stack Recruitment Platform

[![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-green?logo=springboot)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue?logo=postgresql)](https://www.postgresql.org/)
[![Docker](https://img.shields.io/badge/Docker-Ready-2496ED?logo=docker)](https://www.docker.com/)
[![Microservices](https://img.shields.io/badge/Architecture-Microservices-blueviolet)](./jobhunter-microservices/)

> **Enterprise-grade job recruitment platform demonstrating both Monolithic and Microservices architectures**

A comprehensive recruitment platform connecting job seekers with employers, built to demonstrate architectural evolution from monolith to distributed microservices.

---

## 🎯 Project Overview

This repository showcases **two architectural implementations** of the same recruitment platform:

| Architecture         | Status                | Highlights                                | Documentation                                         |
| -------------------- | --------------------- | ----------------------------------------- | ----------------------------------------------------- |
| **🏢 Monolith**      | ✅ Production-Ready   | Spring Boot, JWT Auth, Redis Cache, Kafka | [View Details](#-monolithic-version)                  |
| **🚀 Microservices** | ✅ Active Development | 6 Services, Spring Cloud, Event-Driven    | **[📁 View Full Docs →](./jobhunter-microservices/)** |

### Why Two Architectures?

This project demonstrates:

- ✅ **Evolution journey** from monolith to microservices
- ✅ **Architectural decision-making** and trade-offs
- ✅ **Real-world migration patterns** and challenges
- ✅ **Comparative understanding** of both approaches

---

## ⚡ Quick Navigation

- 🎯 [Features Overview](#-core-features)
- 🏢 [Monolithic Version](#-monolithic-version)
- 🚀 [Microservices Version](#-microservices-architecture)
- 🛠️ [Tech Stack Comparison](#-tech-stack-comparison)
- 🚀 [Quick Start](#-quick-start)
- 📖 [API Documentation](#-api-documentation)

---

## ✨ Core Features

### 🔐 Authentication & Authorization

- JWT-based authentication (Access Token + Refresh Token)
- Role-Based Access Control (RBAC) with granular permissions
- Redis-backed token management and blacklisting
- Secure password encryption with BCrypt

### 👥 User Management

- Multi-role support (Admin, Employer, Job Seeker)
- User profile management with avatar upload
- Email verification and password reset
- Activity tracking and audit logs

### 🏢 Company Management

- Company profiles with logo and detailed information
- Search and filtering with pagination
- Company-specific job postings
- Analytics dashboard

### 💼 Job Management

- Advanced job posting with rich details
- Multi-criteria search (skills, location, salary, level)
- Job recommendations based on user skills
- Application tracking and status management

### 📄 Resume/Application Management

- Resume submission and tracking
- Application status workflow (Pending → Approved/Rejected)
- Bulk operations support
- File upload with validation

### 🔔 Notification System

- Real-time email notifications
- Job alert subscriptions
- Application status updates
- Asynchronous processing with Kafka

---

## 🏢 Monolithic Version

### Architecture Overview

```
┌─────────────────────────────────────────┐
│          Spring Boot Application        │
│  ┌─────────────────────────────────┐   │
│  │     API Layer (Controllers)     │   │
│  └─────────────────────────────────┘   │
│  ┌─────────────────────────────────┐   │
│  │   Business Logic (Services)     │   │
│  └─────────────────────────────────┘   │
│  ┌─────────────────────────────────┐   │
│  │   Data Access (Repositories)    │   │
│  └─────────────────────────────────┘   │
└─────────────────────────────────────────┘
         │              │              │
    PostgreSQL        Redis          Kafka
```

### Tech Stack

- **Backend**: Java 21, Spring Boot 3.2.4
- **Security**: Spring Security, JWT
- **Database**: PostgreSQL 16
- **Cache**: Redis 7
- **Messaging**: Apache Kafka
- **Storage**: MinIO (S3-compatible)
- **Build**: Gradle
- **Testing**: JUnit 5, Mockito
- **Documentation**: Swagger/OpenAPI 3.0

### Key Features

- ✅ 15+ RESTful API endpoints
- ✅ Response time averaging <200ms
- ✅ Comprehensive error handling
- ✅ Request validation and sanitization
- ✅ Rate limiting for sensitive endpoints
- ✅ Docker containerization ready

### Quick Start (Monolith)

```bash
# 1. Start infrastructure services
docker-compose up -d

# 2. Configure environment
cp src/main/resources/application.properties.example application.properties
# Edit database and Redis connection details

# 3. Run application
./gradlew bootRun

# 4. Access API
# API: http://localhost:8080/api/v1
# Swagger UI: http://localhost:8080/swagger-ui.html
```

---

## 🚀 Microservices Architecture

### 🎯 **[📁 View Full Microservices Documentation →](./jobhunter-microservices/)**

### Architecture Overview

```
                    ┌──────────────────┐
                    │   API Gateway    │
                    │  (Port 8080)     │
                    │  JWT, Rate Limit │
                    └────────┬─────────┘
                             │
              ┌──────────────┼──────────────┐
              │              │              │
         ┌────▼────┐    ┌───▼────┐    ┌───▼────┐
         │  Auth   │    │Company │    │  Job   │
         │ Service │    │Service │    │Service │
         │ :8081   │    │ :8082  │    │ :8083  │
         └────┬────┘    └───┬────┘    └───┬────┘
              │              │              │
              └──────────────┼──────────────┘
                             │
                    ┌────────▼─────────┐
                    │   Kafka Broker   │
                    │  Event Streaming │
                    └──────────────────┘
```

### Key Services (6 Microservices)

| Service                  | Port | Database   | Responsibilities                        |
| ------------------------ | ---- | ---------- | --------------------------------------- |
| **Auth Service**         | 8081 | auth_db    | User authentication, authorization, JWT |
| **Company Service**      | 8082 | company_db | Company management, CRUD operations     |
| **Job Service**          | 8083 | job_db     | Job postings, skills, search            |
| **Resume Service**       | 8084 | resume_db  | Application management, CV handling     |
| **File Service**         | 8085 | -          | File upload/download with MinIO         |
| **Notification Service** | 8086 | -          | Email notifications via Kafka           |

### Infrastructure Services

| Service           | Port | Purpose                              |
| ----------------- | ---- | ------------------------------------ |
| **Eureka Server** | 8761 | Service discovery and registry       |
| **API Gateway**   | 8080 | Entry point, routing, authentication |
| **PostgreSQL**    | 5432 | Databases (4 separate DBs)           |
| **Redis**         | 6379 | Caching, rate limiting               |
| **Kafka**         | 9092 | Event streaming, async messaging     |
| **Zipkin**        | 9411 | Distributed tracing                  |
| **Prometheus**    | 9090 | Metrics collection                   |
| **Grafana**       | 3000 | Monitoring dashboards                |

### Key Features

#### 🌐 Service Communication

- **Synchronous**: OpenFeign with Circuit Breaker (Resilience4j)
- **Asynchronous**: Kafka event-driven messaging
- **Service Discovery**: Eureka for dynamic service registration

#### 🔒 Security

- Centralized JWT authentication at API Gateway
- Token blacklist verification with Redis
- Signature-based internal service communication
- Rate limiting (5 req/min for login, 10 req/min for refresh)

#### 📊 Observability

- Distributed tracing with Zipkin
- Metrics collection with Prometheus
- Real-time monitoring with Grafana dashboards
- Centralized logging with Loki

#### 🔄 Event-Driven Architecture

- **3 Kafka Topics**: `job-created`, `job-applications`, `email-notifications`
- **Producers**: Job Service, Resume Service
- **Consumers**: Notification Service, Job Service

### Quick Start (Microservices)

```bash
# Navigate to microservices directory
cd jobhunter-microservices

# 1. Build all services
./build-all-services.bat

# 2. Start all services with Docker
docker-compose up -d

# 3. Verify services are running
docker-compose ps

# 4. Access services
# API Gateway: http://localhost:8080
# Eureka Dashboard: http://localhost:8761
# Swagger API Docs: http://localhost:8080/swagger-ui.html
# Zipkin Tracing: http://localhost:9411
# Grafana Dashboard: http://localhost:3000 (admin/admin)
```

**[📖 Read Full Microservices Documentation →](./jobhunter-microservices/README.md)**

---

## 🛠️ Tech Stack Comparison

| Category                        | Monolithic             | Microservices                         |
| ------------------------------- | ---------------------- | ------------------------------------- |
| **Framework**                   | Spring Boot 3.2        | Spring Boot 3.2 + Spring Cloud 2023.0 |
| **Architecture**                | Layered (MVC)          | Domain-Driven Design (DDD)            |
| **Database**                    | Single PostgreSQL      | 4 separate PostgreSQL databases       |
| **Service Discovery**           | N/A                    | Eureka Server                         |
| **API Gateway**                 | Built-in               | Spring Cloud Gateway                  |
| **Inter-Service Communication** | N/A                    | OpenFeign + Kafka                     |
| **Load Balancing**              | External               | Client-side (Ribbon)                  |
| **Circuit Breaker**             | N/A                    | Resilience4j                          |
| **Distributed Tracing**         | N/A                    | Zipkin + Sleuth                       |
| **Configuration Management**    | application.properties | Spring Cloud Config (future)          |
| **Deployment**                  | Single container       | 12+ containers with Docker Compose    |
| **Scalability**                 | Vertical               | Horizontal (independent scaling)      |

---

## 📖 API Documentation

### Core Endpoints

#### Authentication

```http
POST   /api/v1/auth/register      # User registration
POST   /api/v1/auth/login         # User login (returns JWT)
POST   /api/v1/auth/logout        # Logout (blacklist token)
GET    /api/v1/auth/account       # Get current user info
GET    /api/v1/auth/refresh       # Refresh access token
```

#### Users (Admin only)

```http
GET    /api/v1/users              # List all users (paginated)
GET    /api/v1/users/{id}         # Get user by ID
POST   /api/v1/users              # Create new user
PUT    /api/v1/users              # Update user
DELETE /api/v1/users/{id}         # Delete user
```

#### Companies

```http
GET    /api/v1/companies          # List companies (paginated)
GET    /api/v1/companies/{id}     # Get company details
POST   /api/v1/companies          # Create company (Admin/Employer)
PUT    /api/v1/companies          # Update company
DELETE /api/v1/companies/{id}     # Delete company
```

#### Jobs

```http
GET    /api/v1/jobs               # Search jobs (multi-criteria)
GET    /api/v1/jobs/{id}          # Get job details
POST   /api/v1/jobs               # Create job posting
PUT    /api/v1/jobs               # Update job
DELETE /api/v1/jobs/{id}          # Delete job
```

#### Resumes

```http
GET    /api/v1/resumes            # List resumes (filtered by role)
GET    /api/v1/resumes/{id}       # Get resume details
POST   /api/v1/resumes            # Submit job application
PUT    /api/v1/resumes            # Update resume status
DELETE /api/v1/resumes/{id}       # Withdraw application
```

### API Documentation Tools

- **Swagger UI**: Interactive API documentation
- **Postman Collection**: Available in `docs/postman/`
- **OpenAPI Spec**: `docs/openapi.yaml`

---

## 🎓 Learning Resources & Highlights

### What You'll Learn from This Project

#### Monolithic Architecture

- ✅ RESTful API design principles
- ✅ JWT authentication implementation
- ✅ Spring Security configuration
- ✅ Database design with JPA/Hibernate
- ✅ Redis caching strategies
- ✅ Kafka for async processing
- ✅ Exception handling and validation
- ✅ Testing strategies (Unit + Integration)

#### Microservices Architecture

- ✅ Service decomposition strategies
- ✅ API Gateway pattern implementation
- ✅ Service discovery with Eureka
- ✅ Inter-service communication (sync + async)
- ✅ Event-driven architecture with Kafka
- ✅ Distributed tracing and monitoring
- ✅ Circuit breaker pattern
- ✅ Database per service pattern
- ✅ Docker orchestration
- ✅ Observability (Zipkin, Prometheus, Grafana)

### Architecture Evolution Insights

```
Monolith → Microservices Migration Journey

Why Migrate?
├─ Scalability: Independent service scaling
├─ Technology flexibility: Use different tech per service
├─ Team autonomy: Different teams own different services
├─ Fault isolation: One service failure doesn't crash entire system
└─ Deployment flexibility: Deploy services independently

Trade-offs Learned:
├─ Increased complexity in infrastructure
├─ Network latency for inter-service calls
├─ Distributed transaction challenges
├─ More DevOps overhead
└─ Testing complexity (integration testing)
```

---

## 🚀 Deployment

### Monolithic Deployment

```bash
# Build JAR
./gradlew build

# Run with Docker
docker build -t jobhunter:latest .
docker run -p 8080:8080 jobhunter:latest
```

### Microservices Deployment

```bash
# Docker Compose (Development)
cd jobhunter-microservices
docker-compose up -d

# Kubernetes (Production - future)
kubectl apply -f k8s/
```

---

## 🧪 Testing

### Monolithic Tests

```bash
# Run all tests
./gradlew test

# Run with coverage
./gradlew test jacocoTestReport
```

### Microservices Tests

```bash
# Test individual service
cd jobhunter-microservices/auth-service
./gradlew test

# Integration tests
./gradlew integrationTest
```

---

## 📊 Project Statistics

| Metric                | Monolithic | Microservices                 |
| --------------------- | ---------- | ----------------------------- |
| **Lines of Code**     | ~8,000     | ~15,000 (total)               |
| **API Endpoints**     | 15+        | 40+ (across services)         |
| **Services**          | 1          | 6 business + 6 infrastructure |
| **Databases**         | 1          | 4 (one per domain)            |
| **Docker Containers** | 4          | 12+                           |
| **Test Coverage**     | 75%+       | 70%+ (per service)            |

---

## 🤝 Contributing

This is a personal learning project, but feedback and suggestions are welcome!

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Open a Pull Request

---

## 📝 License

This project is for educational purposes.

---

## 👤 Author

**Huy Huynh**

- GitHub: [@htqhuy1762](https://github.com/htqhuy1762)
- Email: htqhuy1762@gmail.com

---

## 🙏 Acknowledgments

- Spring Framework team for excellent documentation
- Microservices patterns from industry best practices
- Docker and Kubernetes communities

---

## 📚 Additional Resources

- [Monolithic Version Documentation](./docs/monolith.md)
- **[Microservices Full Documentation →](./jobhunter-microservices/README.md)**
- [Architecture Decision Records](./docs/adr/)
- [API Postman Collection](./docs/postman/)
- [Database Schema Diagrams](./docs/database/)

---

<div align="center">

**⭐ If you find this project helpful, please consider giving it a star!**

[![GitHub stars](https://img.shields.io/github/stars/htqhuy1762/jobhunter?style=social)](https://github.com/htqhuy1762/jobhunter)

</div>
