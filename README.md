# Image Space · 图片空间

A full-stack picture management system with social features — upload, organize, and share your images.

**Live Site**: [image-space.app](https://image-space.app)

---

## Features

- **Image Management** — Upload, edit, delete images with category organization and in-memory pagination
- **Public Square** — Browse all publicly shared images in a gallery view with likes and comments
- **User Profiles** — Customizable avatar, background image, and personal profile page
- **OAuth Login** — Sign in with GitHub or Google accounts (subdomain-preserving redirect)
- **RBAC Permissions** — Role-based access control with `admin` and `user` roles, table-driven
- **Cloudflare Turnstile** — Human verification on login, registration, and email-code sending
- **Particle Fireworks** — Interactive particle animation page (admin subdomain)
- **API Documentation** — Auto-generated SpringDoc + Knife4j UI at `/doc.html`

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Backend | Spring Boot 3.2 · Java 21 · MyBatis-Plus 3.5 · Sa-Token · Hutool |
| Frontend | Vue 3.4 · Element Plus 2.7 · Pinia · Vue Router 4 · Vite 5 |
| Storage | MinIO · MySQL 8.0 · Redis 7 |
| Auth | Sa-Token RBAC · JustAuth (GitHub/Google OAuth) · BCrypt |
| Deploy | Docker Compose · Nginx · Let's Encrypt · Azure VM |

## Quick Start

### Prerequisites

- JDK 21, Maven 3.9+, Node.js 18+, MySQL 8.0, Redis 7, MinIO

### Backend

```bash
cd backend

# Copy and configure application.yml (real credentials are gitignored)
cp src/main/resources/application.example.yml src/main/resources/application.yml

# Initialize the database
mysql -u root -p < docker/mysql/init/schema.sql

# Start (listens on :8088)
mvn spring-boot:run
```

### Frontend

```bash
cd frontend
npm install
npm run dev    # Development server on :3000, proxies /api → :8088
```

Visit `http://localhost:3000` — the dev server proxies API calls to the backend.

## API Endpoints

API docs are auto-generated at `http://localhost:8088/doc.html` (Knife4j / Swagger UI).

All endpoints return `Result<T>`:
```json
{ "code": 200, "message": "success", "data": { ... } }
```

### Pages

| Route | Page | Auth |
|-------|------|:----:|
| `/login` | Login | No |
| `/register` | Register | No |
| `/home` | My Images | Yes |
| `/square` | Image Square | No |
| `/image/:id` | Image Detail | No |
| `/profile/:id` | User Profile | No |
| `/particle-fireworks` | Fireworks (admin) | No |

## Docker Deployment

```bash
# Clone and configure
cp docker-compose.example.yaml docker-compose.yaml
cp .env.example .env
# Edit .env with your credentials

# Build and start all services
docker compose up -d --build
```

### Services

| Service | Container | Port |
|---------|-----------|------|
| MySQL 8.0 | `mysql` | 3306 |
| Redis 7 | `redis` | 6379 |
| MinIO | `minio` | 9000 (API) · 9001 (Console) |
| Spring Boot | `backend` | 8088 |
| Nginx | `nginx` | 80 · 443 |

## Project Structure

```
picture management/
├── backend/                     # Spring Boot application
│   └── src/main/java/com/picmgmt/
│       ├── controller/          # REST controllers
│       ├── service/impl/        # Business logic
│       ├── mapper/              # MyBatis-Plus mappers
│       ├── entity/              # Database entities
│       └── dto/vo/config/       # DTOs, VOs, configuration
├── frontend/                    # Vue 3 application
│   └── src/
│       ├── views/               # Page components
│       ├── components/          # Shared components
│       ├── api/                 # Axios API modules
│       ├── stores/              # Pinia stores
│       └── router/              # Vue Router config
├── deploy/                      # Deployment configs
│   └── nginx/                   # Nginx server blocks
├── docker/                      # Docker init scripts
│   └── mysql/init/              # Schema SQL
├── docker-compose.yaml          # Production compose
└── docker-compose.example.yaml  # Template (safe to commit)
```

## Architecture

```
Browser → Nginx (80/443) → Frontend (static files)
                         → Backend (:8088) → MySQL · Redis · MinIO
                         → /minio/ → MinIO (:9000)
```

- **Image storage**: MinIO object storage with bucket-per-type (`avatars`, `backgrounds`, `images`, `comments`)
- **Caching**: Caffeine in-memory LRU + Redis for OAuth state and sessions
- **Auth flow**: Sa-Token intercepts all routes; token in `satoken` request header from `localStorage`
- **RBAC**: `users → user_roles → role_permissions → permissions` table chain

## License

MIT
