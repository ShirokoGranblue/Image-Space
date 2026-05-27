# CLAUDE.md

Development guide for Claude Code (claude.ai/code) working in this repository.
Please read files at 'C:\Users\l2653\.claude\projects\C--Users-l2653-Desktop-picture-management\memory' when Claude Code launching

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
- **Password encryption**: BCrypt via Hutool (`BCrypt.hashpw` / `BCrypt.checkpw`), not Spring Security's encoder.
- **Image storage**: All images (pictures, avatars, backgrounds, comment images) stored as Base64 Data URLs in MySQL `LONGTEXT` columns. Upload: `MultipartFile → byte[] → Base64 Data URL` inserted into database. Frontend renders directly via `<img :src="dataUrl">`. `ImageCacheService` provides LRU in-memory cache — Base64 ↔ byte[] conversion checks cache first, falls back to direct conversion and writes to buffer on miss.
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

### Custom SQL exists only in ImageMapper
`UserMapper` and `CategoryMapper` use MyBatis-Plus `BaseMapper` methods directly. `ImageMapper.selectImageVOList` is the only custom query — it LEFT JOINs users and categories tables to build `ImageVO` in a single query.

### API response format
All endpoints return `Result<T>` with structure `{ code: 200, message: "success", data: ... }`. The axios interceptor in `api/index.js` unwraps the response — Vue components receive `Result` objects as `res.data`. Non-200 codes trigger `ElMessage.error`.

### Dynamic OAuth callback URLs
`OAuthService.getAuthorizeUrl()` and `handleCallback()` accept a `baseUrl` parameter built from the request `Host` header + `X-Forwarded-Proto`. This preserves the domain when logging in from subdomains (e.g. `admin.image-space.app`). The OAuth redirect_uri is constructed as `{baseUrl}/api/user/oauth/{provider}/callback`. OAuth providers (GitHub/Google) must have all subdomain callback URLs registered.

### Admin subdomain
`admin.image-space.app` serves the same frontend as the main domain, protected by Cloudflare Access (Zero Trust). nginx server_name includes both `image-space.app` and `admin.image-space.app` in the main HTTPS server block. `www.image-space.app` and `api.image-space.app` 301 redirect to the bare domain.

## Notes
Every response sentence must end with "喵~" . For emphasis or strong emotion, use "喵!" .
Example: "Hello 喵~, I like you 喵~。完成了喵！"
