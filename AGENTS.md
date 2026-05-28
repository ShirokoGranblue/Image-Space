# AGENTS.md

Development guide for Claude Code (claude.ai/code) working in this repository.
Please read files at 'C:\Users\l2653\Desktops\picture management\memory' when Codex launching
Always keep this file updating.

## Common Commands

```bash
# Backend
cd backend
mvn spring-boot:run                          # Start backend, listens on :8088
mvn test                                      # Run all tests

# Frontend
cd frontend
npm install                                   # Install dependencies
npm run dev                                   # Start dev server, listens on :3000
npm run build                                 # Production build
npm test                                      # Run all tests

# Database — Before first startup, manually run schema.sql to initialize MySQL
# Database name: picture_management, default credentials root/root, see application.yml
```

API docs are auto-generated at `http://localhost:8088/doc.html` (SpringDoc + Knife4j UI).

## Architecture

**Backend** (`com.picmgmt`) — Layered architecture, Spring Boot 3.2 + JDK 21:

```
controller → service/impl → mapper (MyBatis-Plus BaseMapper)
     ↓            ↓
   dto/vo      entity (@TableName maps to snake_case table names)
```

- **Auth**: Sa-Token (not Spring Security). Token stored in `localStorage['satoken']`, passed via `satoken` request header. `SaTokenConfig` intercepts all routes, only allows `/user/login`, `/user/register`, `/doc.html/**`, `/v3/api-docs/**`, `/swagger-ui/**`, `/image/square`.
- **Human verification**: Cloudflare Turnstile is rendered by `frontend/src/components/TurnstileWidget.vue` with sitekey `0x4AAAAAADXRE_jtv9_OBFRo`. The frontend sends `turnstileToken` on password login, registration, and email-code sending. The backend verifies tokens in `TurnstileServiceImpl` by calling Cloudflare Siteverify before continuing.
- **Password encryption**: BCrypt via Hutool (`BCrypt.hashpw` / `BCrypt.checkpw`), not Spring Security's encoder.
- **Image storage**: MinIO object storage via `MinioStorageService`. Images uploaded as `MultipartFile → byte[]` stored in MinIO buckets (`avatars`, `backgrounds`, `images`, `comments`). `StorageMigrationRunner` automatically migrates legacy Base64 data on first startup. nginx proxies `/minio/` to MinIO and rewrites internal URLs via `sub_filter`.
- **Data migration**: `DataMigrationRunner` automatically detects legacy file-path data (`/upload/...`) on first startup, reads local files, converts to Base64, stores in database, then removes the `upload` directory.
- **CORS**: `WebMvcConfig` allows all origins. Frontend dev uses Vite proxy (`/api` → `:8088`), so CORS config only applies when frontend and backend are deployed together.

**Frontend** — Vue 3 + Element Plus + Pinia + Vue Router:

| Route | Page | Auth Required |
|-------|------|:---:|
| `/login` | Login | No |
| `/register` | Register | No |
| `/home` | My Images (CRUD) | Yes |
| `/square` | Image Square | No |
| `/categories` | Category Management | Yes |

Route guard in `router/index.js`, checks `localStorage['satoken']` for `meta.requiresAuth` routes.

## Key Design Decisions

### In-memory pagination for images
`ImageServiceImpl.page()` uses `ImageMapper.selectImageVOList()` to query all matching rows, then slices in Java. The query uses dynamic `<if>` tags and `ORDER BY ${sortField}`. Sort fields are **whitelist-validated** (`upload_time`, `image_name`, `file_size`) before interpolation to prevent SQL injection. This approach is viable given the local-use scenario (≤10 concurrent users).

### Service-layer ownership checks
`ImageServiceImpl.delete()` and `update()` both verify `image.userId == loginId || hasRole("admin")` before mutating. `CategoryServiceImpl` follows the same pattern.

### Deleting a category reassigns images to "uncategorized"
When a category is deleted, `CategoryServiceImpl.delete()` sets all child images' `category_id` to `NULL` rather than deleting the images themselves.

### Media URLs are versioned to avoid stale CDN cache
Backend image/avatar/background URLs are generated through `MediaUrlUtil`, which appends `?v=<sha256(storage_key)>` to API proxy URLs such as `/api/image/download/{id}`, `/api/user/avatar/{id}`, and `/api/user/background/{id}`. When content is replaced and the MinIO storage key changes, the URL changes too, so Cloudflare fetches fresh bytes. Frontend image rendering should prefer backend-provided `imageUrl` values via `getImageDownloadUrl(image)` instead of rebuilding fixed download URLs.

### Custom SQL exists only in ImageMapper
`UserMapper` and `CategoryMapper` use MyBatis-Plus `BaseMapper` methods directly. `ImageMapper.selectImageVOList` is the only custom query — it LEFT JOINs users and categories tables to build `ImageVO` in a single query.

### API response format
All endpoints return `Result<T>` with structure `{ code: 200, message: "success", data: ... }`. The axios interceptor in `api/index.js` unwraps the response — Vue components receive `Result` objects as `res.data`. Non-200 codes trigger `ElMessage.error`.

## Production Deployment

- **Server**: Azure VM `4.230.10.11`, Ubuntu 24.04, SSH key `~/Downloads/ShirokoGranblue_key.pem`, user `azureuser`
- **Domain**: `image-space.app` (name.com), DNS resolves to `4.230.10.11`
- **SSL**: Let's Encrypt via certbot, cert at `/etc/letsencrypt/live/image-space.app/`, expires 2026-08-24, auto-renews
- **Deploy path**: `/home/azureuser/Picture-Managentor/`
- **nginx config**: `deploy/nginx/default.conf` — 4 server blocks (www HTTP/HTTPS redirect + main HTTP→HTTPS + main HTTPS). **Do NOT replace with HTTP-only config** — HTTPS will break.
- **Docker volumes**: `/etc/letsencrypt:/etc/letsencrypt:ro` mounted into nginx for SSL certs
- **Turnstile env**: set `TURNSTILE_ENABLED=true` and `TURNSTILE_SECRET_KEY=<Cloudflare Turnstile secret>` in the server `.env`, set `VITE_TURNSTILE_ENABLED=true` before building the frontend, then recreate containers with `docker compose up -d`. Keep the secret out of git. `VITE_TURNSTILE_SITE_KEY` can override the frontend sitekey at build time, but the default is already the production sitekey above.
- **.env file**: Server has `.env` with real credentials. `docker compose restart` does NOT reload `.env` — use `docker compose up -d` to recreate container when .env changes.

## Permission System (RBAC)

`SaTokenPermissionImpl` resolves permissions via table chain:
```
users → user_roles (role_id) → role_permissions → permissions (code)
```
- `@SaCheckPermission("image:upload")` etc. on ImageController checks permission codes
- UserController (avatar/background/comments) only uses `StpUtil.getLoginIdAsLong()`, no permission check
- `users.role` is a display string only — actual RBAC is table-driven
- New user registration (OAuth or regular) MUST insert `user_roles` row with `role_id=3` (user role)
- `UserServiceImpl.register()` and `OAuthServiceImpl.handleCallback()` both call `ensureUserRole()`
- Permission codes: `image:upload`, `image:edit`, `image:delete`, `image:edit:any`, `image:delete:any`, `category:manage`, `category:manage:any`, `comment:add`, `comment:delete`, `comment:delete:any`, `user:manage`

## .gitignore Protected Files

These files contain real credentials and are gitignored — update them directly on server:
- `application.yml` — local dev config with real defaults
- `docker-compose.yml` — local docker compose with real values
- `.env` — server environment variables (only on server)

Templates without secrets: `application.example.yml`, `docker-compose.example.yaml`

## Notes
Every response sentence must end with "喵~" . For emphasis or strong and excited emotion, use "喵!" .Example: "Hello 喵~, I like you 喵~。完成了喵！".
When you finished some work/task,automatically submit and push the modified content to my Github repository and sync the server's and host's changes or configurations.
For every future code change, keep local/GitHub/server copies synchronized, then run the relevant build and test commands on both the local host and the Azure server before reporting completion.
