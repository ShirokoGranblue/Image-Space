# Architecture Redesign Spec

**Date**: 2026-05-17  
**Status**: Approved  
**Scope**: Full-stack architecture overhaul

## Motivation

The current architecture has accumulated technical debt across several dimensions:

1. **ImageServiceImpl (317 lines)** mixes upload, validation, authorization, caching, pagination, and VO conversion — violating single responsibility
2. **In-memory pagination** queries all rows then slices in Java; unscalable beyond ~1000 images
3. **Base64 LONGTEXT storage** bloats MySQL rows (each image row can be 10+ MB), making queries and backups slow
4. **Hand-written LRU cache** with ReentrantReadWriteLock — Caffeine offers lock-free design and better eviction policies
5. **No Redis** — cannot support multi-instance deployment or cache consistency across instances
6. **IllegalArgumentException for everything** — no structured error codes, no internationalization support
7. **No Bean Validation** — manual if-checks scattered across service methods
8. **Runtime DDL** in DataMigrationRunner — risky pattern mixing schema changes with application code
9. **Flat RBAC** — only user/admin roles, no granular permissions

## Design Decisions

After user consultation, the following choices were made:

- Architecture pattern: **Enhanced layered architecture** (over DDD-Lite or modular monolith)
- Image storage: **MinIO** (Object Storage), database stores only `storage_key`
- Caching: **Caffeine (L1) + Redis (L2)** dual-layer for future multi-instance support
- Auth: **RBAC with resource-level permissions** (users → roles → permissions, plus image visibility model)
- Validation: **Spring Bean Validation** + custom `BusinessException` + `ErrorCode` enum
- Pagination: **MyBatis-Plus PaginationInnerInterceptor** (real database-level pagination)

## Architecture Overview

```
Presentation (Controller)         ← @Validated, @SaCheckPermission
    ↓
Application (Service interfaces + impls)
    ↓
Domain (Entity, VO, DTO, Repository)
    ↓
Infrastructure (Mapper, Cache, Storage, Auth)
```

### Package Structure

```
com.picmgmt
├── PicmgmtApplication.java
├── common/
│   ├── Result.java
│   ├── ErrorCode.java              (NEW)
│   ├── BusinessException.java      (NEW)
│   └── GlobalExceptionHandler.java
├── config/
│   ├── SaTokenConfig.java
│   ├── WebMvcConfig.java
│   ├── MyBatisPlusConfig.java       (add pagination plugin)
│   ├── RedisConfig.java             (NEW)
│   └── MinioConfig.java            (NEW)
├── auth/                            (NEW)
│   ├── PermissionEnum.java         (NEW)
│   └── SaTokenPermissionImpl.java  (NEW)
├── image/
│   ├── Image.java
│   ├── ImageVO.java
│   ├── ImageQueryDTO.java
│   ├── ImageUpdateDTO.java          (NEW)
│   ├── ImageMapper.java
│   ├── ImageRepository.java         (NEW)
│   ├── ImageWriteService.java       (NEW)
│   ├── ImageReadService.java        (NEW)
│   ├── ImagePermissionService.java  (NEW)
│   └── ImageController.java
├── user/
│   ├── User.java / UserVO.java / UserMapper.java
│   ├── UserRepository.java          (NEW)
│   ├── UserService.java / impl
│   └── UserController.java
├── category/
│   ├── Category.java / CategoryMapper.java
│   ├── CategoryRepository.java      (NEW)
│   ├── CategoryService.java / impl
│   └── CategoryController.java
├── comment/
│   ├── Comment.java / CommentVO.java / CommentMapper.java
│   ├── CommentRepository.java       (NEW)
│   ├── CommentService.java / impl
│   └── CommentController.java
├── storage/                         (NEW)
│   ├── StorageService.java         (Interface)
│   ├── MinioStorageService.java    (MinIO implementation)
│   └── LocalStorageService.java   (Fallback implementation)
└── cache/                           (NEW)
    ├── CacheService.java           (Unified interface)
    ├── CaffeineLocalCache.java    (L1 cache)
    └── RedisCacheService.java     (L2 cache)
```

## Error Handling

### ErrorCode Enum

Enum with numeric codes grouped by domain:

- `1xxx` — Image errors (IMAGE_NOT_FOUND, IMAGE_FORMAT_INVALID, IMAGE_SIZE_EXCEEDED, etc.)
- `2xxx` — User errors (USER_NOT_FOUND, USERNAME_EXISTS, PASSWORD_MISMATCH, LOGIN_FAILED, etc.)
- `3xxx` — Category errors (CATEGORY_NOT_FOUND, CATEGORY_NAME_EXISTS, etc.)
- `4xxx` — Comment errors (COMMENT_NOT_FOUND, COMMENT_EMPTY, etc.)
- `5xxx` — Storage errors (STORAGE_UPLOAD_FAILED, STORAGE_DOWNLOAD_FAILED, etc.)

### BusinessException

```java
public class BusinessException extends RuntimeException {
    private final ErrorCode errorCode;
    // Constructor supports format args for dynamic messages
}
```

### GlobalExceptionHandler Additions

- `MethodArgumentNotValidException` → extracts first field error, returns 400 + error message
- `BusinessException` → maps to ErrorCode's internal code + message
- Existing handlers for `NotLoginException`, `MaxUploadSizeExceededException`, generic `Exception` retained

### Result Enhancement

```java
public static <T> Result<T> error(ErrorCode errorCode) {
    return new Result<>(errorCode.getCode(), errorCode.getMessage(), null);
}
```

## Validation

DTO classes use Bean Validation annotations (`@NotBlank`, `@Size`, `@Pattern`, etc.). Controllers use `@Validated` for automatic validation. Service layer removes manual if-checks for basic field validation and focuses on business logic.

## Image Service Decomposition

`ImageServiceImpl` (317 lines, 11 methods) split into:

| New Service | Methods | Responsibility |
|---|---|---|
| `ImageWriteService` | `upload`, `update`, `delete` | Write operations |
| `ImageReadService` | `getById`, `page`, `getSquare`, `download` | Read/query operations |
| `ImagePermissionService` | `canView`, `validateOwnership` | Authorization checks |

Other services (User, Category, Comment) remain single files but introduce Repository layer.

## Repository Layer

Repository sits between Service and Mapper, encapsulating:

- Cache coordination (Caffeine → Redis → DB fallback)
- MyBatis-Plus pagination
- Cache key management and eviction

```
Service → Repository → Mapper + CacheService
```

### ImageRepository Example Methods

- `findById(id)` — cache-first lookup with automatic backfill
- `insert(image)` — DB insert + cache put
- `updateById(image)` — DB update + cache eviction + page cache invalidation
- `deleteById(id)` — DB delete + cache eviction + page cache invalidation
- `page(...)` — MyBatis-Plus pagination with optional caching

## Caching Architecture

### Dual-Layer Flow

```
CacheService.get("image:1")
  ↓
Caffeine L1 hit? → return (~0.001ms)
  ↓ miss
Redis L2 hit? → backfill L1 → return (~0.5ms)
  ↓ miss
DB query → backfill L2 → backfill L1 → return (~10ms)
```

### Cache Invalidation

- On write/delete: evict entity cache + broadcast Redis Pub/Sub message
- Other instances receive message → evict their local Caffeine (cache consistency across instances)
- Page caches evicted on any mutation (pattern-based: `image:page:*`)

### Caffeine Configuration

- `maximumSize=500`
- `expireAfterWrite=10min`
- `expireAfterAccess=5min`
- `recordStats()` enabled

## Storage Layer

### StorageService Interface

```java
public interface StorageService {
    String upload(String bucket, String objectKey, byte[] bytes, String contentType);
    byte[] download(String bucket, String objectKey);
    void delete(String bucket, String objectKey);
    String getAccessUrl(String bucket, String objectKey);
    FileMeta getFileMeta(String bucket, String objectKey);
}
```

### Bucket Layout

| Bucket | Purpose | Access |
|---|---|---|
| `images` | Image originals | Private, presigned URL |
| `avatars` | User avatars | Public read |
| `backgrounds` | Profile backgrounds | Public read |
| `comments` | Comment attachments | Private |

### MinioStorageService

Primary implementation using MinIO Java SDK. Upload generates presigned URLs (1-hour expiry) for frontend rendering.

### LocalStorageService

Fallback implementation when `storage.type=local`. Stores files under `./storage/{bucket}/{objectKey}`.

### Configuration

```yaml
storage:
  type: minio
  minio:
    endpoint: http://127.0.0.1:9000
    access-key: minioadmin
    secret-key: minioadmin
```

## RBAC Permission Model

### Database Tables

- `permissions` — `id`, `code` (e.g., `image:delete`), `name`, `group_name`
- `roles` — `id`, `code` (e.g., `admin`), `name`
- `role_permissions` — many-to-many association
- `user_roles` — many-to-many association (replaces `users.role` VARCHAR)

### Permission Codes

| Code | Description | Default Role |
|---|---|---|
| `image:upload` | Upload images | user |
| `image:edit` | Edit own images | user |
| `image:delete` | Delete own images | user |
| `image:edit:any` | Edit any image | admin, moderator |
| `image:delete:any` | Delete any image | admin |
| `category:manage` | Manage own categories | user |
| `category:manage:any` | Manage any category | admin |
| `comment:add` | Add comments | user |
| `comment:delete` | Delete own comments | user |
| `comment:delete:any` | Delete any comment | admin, moderator |
| `user:manage` | Manage users | admin |

### Two-Level Authorization

1. **Function-level**: `@SaCheckPermission("image:delete")` — can this user perform this action?
2. **Resource-level**: `ImagePermissionService.canView()` / ownership checks — does this user own this image?

Existing image visibility model (PUBLIC/PRIVATE/SPECIFIED) is preserved at the resource level.

### Sa-Token Integration

`SaTokenPermissionImpl` implements `StpInterface` to resolve `getPermissionList()` and `getRoleList()` from the database.

## Data Migration

### Base64 → MinIO Storage

`StorageMigrationRunner` replaces `DataMigrationRunner`:

1. Check if migration already completed (idempotent flag in DB or config table)
2. For each `images` row with Base64 `image_path`: decode → upload to MinIO → write `storage_key` → clear `image_path`
3. Same for `users.avatar`, `users.background`, `comments.image_path`
4. Drop old Base64 columns after successful migration

### Schema Changes

```sql
-- New columns
ALTER TABLE images   ADD COLUMN storage_key VARCHAR(255);
ALTER TABLE users    ADD COLUMN avatar_key VARCHAR(255);
ALTER TABLE users    ADD COLUMN background_key VARCHAR(255);
ALTER TABLE comments ADD COLUMN image_key VARCHAR(255);

-- Drop old Base64 columns (after migration)
ALTER TABLE images   DROP COLUMN image_path;
ALTER TABLE users    DROP COLUMN avatar;
ALTER TABLE users    DROP COLUMN background;
ALTER TABLE comments DROP COLUMN image_path;

-- RBAC tables (3 new tables)
-- permissions, roles, role_permissions, user_roles
```

## Dependency Changes

### pom.xml Additions

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-cache</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
<dependency>
    <groupId>com.github.ben-manes.caffeine</groupId>
    <artifactId>caffeine</artifactId>
</dependency>
<dependency>
    <groupId>io.minio</groupId>
    <artifactId>minio</artifactId>
    <version>8.5.10</version>
</dependency>
```

## Files to Delete

- `ImageCacheService.java` — replaced by Caffeine + Redis
- `DataMigrationRunner.java` — replaced by StorageMigrationRunner

## Frontend Changes

Minimal changes — only data source for image rendering:

| Component | Change |
|---|---|
| `ImageCard.vue` | `img.src` from `image.imagePath` (Base64) to `image.imageUrl` (presigned URL) |
| `ImageViewer.vue` | Same as above |
| `Profile.vue` | Avatar/background from Base64 to presigned URL |

API contracts unchanged. Axios interceptor behavior unchanged.

## Verification

1. **Build**: `cd backend && mvn clean compile -DskipTests` passes
2. **Tests**: `mvn test` — all existing tests pass; new unit tests for `ImageRepository`, `ImageWriteService`, `ImageReadService`, `ImagePermissionService`, `CacheService`, `StorageService`
3. **Migration**: Start with old database containing Base64 data → migration runs → all `storage_key` columns populated → old columns dropped
4. **Upload flow**: Upload image via frontend → stored in MinIO → viewable via presigned URL
5. **Cache chain**: First request hits DB (cold), second request hits Caffeine (warm), Redis populated between
6. **Permissions**: Non-admin user denied on `image:delete:any`, allowed on `image:delete` (own image)
7. **Frontend E2E**: `npm run dev` → login → upload → view in list → view in square → download → delete
