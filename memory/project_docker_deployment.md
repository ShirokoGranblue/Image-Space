---
name: docker-deployment
description: "Docker Compose deployment on Azure VM with nginx reverse proxy, MySQL, Redis, MinIO"
metadata: 
  node_type: memory
  type: project
  originSessionId: 4c7f230e-5d9e-4453-ac75-d087daafd71c
---

**Stack:** MySQL 8.0 + Redis 7 + MinIO + Spring Boot backend + Vue frontend + nginx reverse proxy

**Dockerfiles:**
- `backend/Dockerfile`: `eclipse-temurin:21-jre-alpine`, copies `target/*.jar` as `app.jar`
- `frontend/Dockerfile`: builds Vue app, output served by nginx

**docker-compose.yml** (gitignored, has real values) / **docker-compose.example.yaml** (committed template):
- mysql: port 3306, healthcheck via mysqladmin ping
- redis: port 6379, appendonly yes
- minio: ports 9000 (API) + 9001 (console), `/data` volume
- backend: port 8088, Spring profile `docker`, depends on mysql/redis/minio healthy
- frontend: builds from `./frontend`, no exposed ports
- nginx: ports 80+443, mounts `deploy/nginx/default.conf`, `frontend/dist`, `/etc/letsencrypt`

**nginx config** (`deploy/nginx/default.conf`):
- 4 server blocks: www HTTP→HTTPS, www HTTPS→bare, HTTP→HTTPS, HTTPS main
- `/api/` proxied to `backend:8088/` (strips `/api` prefix)
- `/minio/` proxied to `minio:9000/`
- `sub_filter` rewrites minio URLs in API responses

**Deploy flow:** `mvn package -DskipTests` locally → scp JAR → `docker compose up -d --build backend` on server

**Why:** Single-server Docker deployment for personal use (≤10 concurrent users).
**How to apply:** Always build backend locally with Maven then scp. Use `up -d --build` not `restart` when .env changes.
