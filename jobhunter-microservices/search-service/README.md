# Search Service

Search service doc/index for jobs using Elasticsearch.

## Responsibilities

- Consume CDC events from Debezium topic `job_db.public.jobs`
- Upsert/delete job documents in Elasticsearch index `jobs`
- Provide search API: `GET /api/v1/search/jobs`

## Run locally

1. Build jar:

```bash
./gradlew clean build
```

2. Start infrastructure and services with Docker Compose.

3. Register connector:

```powershell
.\scripts\register-job-cdc-connector.ps1
```

## Search API examples

```http
GET /api/v1/search/jobs?q=java&page=1&size=10
GET /api/v1/search/jobs?location=HO_CHI_MINH&level=FRESHER&minSalary=500&maxSalary=2000
```

