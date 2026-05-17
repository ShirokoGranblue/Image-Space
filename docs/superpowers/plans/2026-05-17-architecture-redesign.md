# Architecture Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Full-stack architecture overhaul — replace Base64 storage with MinIO, introduce Caffeine+Redis caching, RBAC permissions, Bean Validation, Repository layer, and service decomposition.

**Architecture:** Enhanced layered architecture with Presentation → Application → Domain → Infrastructure. New packages: `common/`, `auth/`, `storage/`, `cache/`. Each domain (image/user/category/comment) gets its own Repository.

**Tech Stack:** Spring Boot 3.2 + JDK 21, MyBatis-Plus 3.5.6, Sa-Token 1.38, MinIO 8.5, Caffeine, Redis (Spring Data Redis), Vue 3 + Element Plus

---

## Phase 1: Infrastructure Setup

### Task 1.1: Add Maven dependencies

**Files:**
- Modify: `backend/pom.xml`

- [ ] **Step 1: Add new dependencies**

Add inside `<dependencies>`, after the existing `spring-boot-starter-validation` dependency:

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

<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-pool2</artifactId>
</dependency>
```

- [ ] **Step 2: Verify dependencies resolve**

Run: `cd backend && mvn dependency:resolve -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add backend/pom.xml
git commit -m "feat: add MinIO, Caffeine, Redis, commons-pool2 dependencies"
```

---

### Task 1.2: Add application.yml storage and Redis config

**Files:**
- Modify: `backend/src/main/resources/application.yml`

- [ ] **Step 1: Append storage and Redis configuration**

Append to `backend/src/main/resources/application.yml`:

```yaml
storage:
  type: minio
  minio:
    endpoint: http://127.0.0.1:9000
    access-key: minioadmin
    secret-key: minioadmin
  local:
    base-path: ./storage

spring:
  data:
    redis:
      host: localhost
      port: 6379
      timeout: 3000ms
      lettuce:
        pool:
          max-active: 8
          max-idle: 8
          min-idle: 0
  cache:
    type: redis
    redis:
      time-to-live: 30m
      cache-null-values: false
```

- [ ] **Step 2: Commit**

```bash
git add backend/src/main/resources/application.yml
git commit -m "feat: add MinIO storage and Redis cache configuration"
```

---

### Task 1.3: Create MinioConfig

**Files:**
- Create: `backend/src/main/java/com/picmgmt/config/MinioConfig.java`

- [ ] **Step 1: Write MinioConfig**

```java
package com.picmgmt.config;

import io.minio.MinioClient;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "storage.minio")
public class MinioConfig {

    private String endpoint;
    private String accessKey;
    private String secretKey;

    @Bean
    @ConditionalOnProperty(name = "storage.type", havingValue = "minio", matchIfMissing = true)
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }
}
```

- [ ] **Step 2: Compile check**

Run: `cd backend && mvn compile -pl . -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/picmgmt/config/MinioConfig.java
git commit -m "feat: add MinioConfig with conditional MinioClient bean"
```

---

### Task 1.4: Create RedisConfig

**Files:**
- Create: `backend/src/main/java/com/picmgmt/config/RedisConfig.java`

- [ ] **Step 1: Write RedisConfig**

```java
package com.picmgmt.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@EnableCaching
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add backend/src/main/java/com/picmgmt/config/RedisConfig.java
git commit -m "feat: add RedisConfig with Jackson JSON serialization"
```

---

## Phase 2: Common Layer (Error Handling + Validation)

### Task 2.1: Create ErrorCode enum

**Files:**
- Create: `backend/src/main/java/com/picmgmt/common/ErrorCode.java`

- [ ] **Step 1: Write ErrorCode**

```java
package com.picmgmt.common;

import lombok.Getter;

@Getter
public enum ErrorCode {
    SUCCESS(200, "操作成功"),
    BAD_REQUEST(400, "参数错误"),
    UNAUTHORIZED(401, "请先登录"),
    FORBIDDEN(403, "无权访问"),
    NOT_FOUND(404, "资源不存在"),
    CONFLICT(409, "数据冲突"),
    INTERNAL_ERROR(500, "服务器内部错误"),

    // Image errors (1xxx)
    IMAGE_NOT_FOUND(1001, "图片不存在"),
    IMAGE_FORMAT_INVALID(1002, "仅支持 JPG/PNG/WEBP 格式"),
    IMAGE_SIZE_EXCEEDED(1003, "图片大小不能超过20MB"),
    IMAGE_DESCRIPTION_TOO_LONG(1004, "描述不能超过500个字符"),
    IMAGE_PERMISSION_DENIED(1005, "无权操作该图片"),
    IMAGE_VISIBILITY_INVALID(1006, "可见权限参数无效"),

    // User errors (2xxx)
    USER_NOT_FOUND(2001, "用户不存在"),
    USERNAME_EXISTS(2002, "用户名已存在"),
    PASSWORD_MISMATCH(2003, "两次密码不一致"),
    LOGIN_FAILED(2004, "用户名或密码错误"),

    // Category errors (3xxx)
    CATEGORY_NOT_FOUND(3001, "分类不存在"),
    CATEGORY_NAME_EXISTS(3002, "分类名称已存在"),
    CATEGORY_NAME_TOO_LONG(3003, "分类名称不能超过20个字符"),

    // Comment errors (4xxx)
    COMMENT_NOT_FOUND(4001, "评论不存在"),
    COMMENT_EMPTY(4002, "评论内容不能为空"),

    // Storage errors (5xxx)
    STORAGE_UPLOAD_FAILED(5001, "文件上传失败"),
    STORAGE_DOWNLOAD_FAILED(5002, "文件下载失败");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add backend/src/main/java/com/picmgmt/common/ErrorCode.java
git commit -m "feat: add ErrorCode enum with domain-grouped error codes"
```

---

### Task 2.2: Create BusinessException

**Files:**
- Create: `backend/src/main/java/com/picmgmt/common/BusinessException.java`

- [ ] **Step 1: Write BusinessException**

```java
package com.picmgmt.common;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add backend/src/main/java/com/picmgmt/common/BusinessException.java
git commit -m "feat: add BusinessException wrapping ErrorCode"
```

---

### Task 2.3: Enhance Result with ErrorCode support

**Files:**
- Modify: `backend/src/main/java/com/picmgmt/common/Result.java:26-31`

- [ ] **Step 1: Add error(ErrorCode) static method**

Add this method to `Result.java` after the existing `error(String)` method:

```java
public static <T> Result<T> error(ErrorCode errorCode) {
    return new Result<>(errorCode.getCode(), errorCode.getMessage(), null);
}
```

- [ ] **Step 2: Commit**

```bash
git add backend/src/main/java/com/picmgmt/common/Result.java
git commit -m "feat: add Result.error(ErrorCode) convenience method"
```

---

### Task 2.4: Enhance GlobalExceptionHandler

**Files:**
- Modify: `backend/src/main/java/com/picmgmt/common/GlobalExceptionHandler.java`

- [ ] **Step 1: Add MethodArgumentNotValidException and BusinessException handlers**

Replace the import section and class body. New imports to add:

```java
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.validation.FieldError;
```

Add these handler methods inside the class (keep existing handlers):

```java
@ExceptionHandler(MethodArgumentNotValidException.class)
public Result<Void> handleValidation(MethodArgumentNotValidException e) {
    String msg = e.getBindingResult().getFieldErrors().stream()
            .findFirst()
            .map(FieldError::getDefaultMessage)
            .orElse("参数校验失败");
    return Result.error(400, msg);
}

@ExceptionHandler(BusinessException.class)
public Result<Void> handleBusiness(BusinessException e) {
    return Result.error(e.getErrorCode().getCode(), e.getMessage());
}
```

- [ ] **Step 2: Commit**

```bash
git add backend/src/main/java/com/picmgmt/common/GlobalExceptionHandler.java
git commit -m "feat: add validation and BusinessException handlers to GlobalExceptionHandler"
```

---

## Phase 3: Storage Layer

### Task 3.1: Create StorageService interface

**Files:**
- Create: `backend/src/main/java/com/picmgmt/storage/StorageService.java`

- [ ] **Step 1: Write StorageService interface**

```java
package com.picmgmt.storage;

public interface StorageService {

    String upload(String bucket, String objectKey, byte[] bytes, String contentType);

    byte[] download(String bucket, String objectKey);

    void delete(String bucket, String objectKey);

    String getAccessUrl(String bucket, String objectKey);

    FileMeta getFileMeta(String bucket, String objectKey);

    record FileMeta(long size, String contentType) {}
}
```

- [ ] **Step 2: Commit**

```bash
git add backend/src/main/java/com/picmgmt/storage/StorageService.java
git commit -m "feat: add StorageService interface"
```

---

### Task 3.2: Create MinioStorageService

**Files:**
- Create: `backend/src/main/java/com/picmgmt/storage/MinioStorageService.java`

- [ ] **Step 1: Write MinioStorageService**

```java
package com.picmgmt.storage;

import com.picmgmt.common.BusinessException;
import com.picmgmt.common.ErrorCode;
import io.minio.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "storage.type", havingValue = "minio", matchIfMissing = true)
public class MinioStorageService implements StorageService {

    private final MinioClient minioClient;

    @Override
    public String upload(String bucket, String objectKey, byte[] bytes, String contentType) {
        ensureBucket(bucket);
        try (var is = new ByteArrayInputStream(bytes)) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .stream(is, bytes.length, -1)
                    .contentType(contentType)
                    .build());
            log.debug("上传成功: {}/{} ({} bytes)", bucket, objectKey, bytes.length);
            return objectKey;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.STORAGE_UPLOAD_FAILED, e);
        }
    }

    @Override
    public byte[] download(String bucket, String objectKey) {
        try (var is = minioClient.getObject(GetObjectArgs.builder()
                .bucket(bucket).object(objectKey).build())) {
            return is.readAllBytes();
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.STORAGE_DOWNLOAD_FAILED, e);
        }
    }

    @Override
    public void delete(String bucket, String objectKey) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucket).object(objectKey).build());
        } catch (Exception e) {
            log.warn("删除文件失败: {}/{}", bucket, objectKey);
        }
    }

    @Override
    public String getAccessUrl(String bucket, String objectKey) {
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .bucket(bucket).object(objectKey)
                    .method(io.minio.http.Method.GET)
                    .expiry(1, TimeUnit.HOURS)
                    .build());
        } catch (Exception e) {
            log.warn("生成预签名 URL 失败: {}/{}", bucket, objectKey);
            return null;
        }
    }

    @Override
    public FileMeta getFileMeta(String bucket, String objectKey) {
        try {
            var stat = minioClient.statObject(StatObjectArgs.builder()
                    .bucket(bucket).object(objectKey).build());
            return new FileMeta(stat.size(), stat.contentType());
        } catch (Exception e) {
            return null;
        }
    }

    private void ensureBucket(String bucket) {
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                log.info("创建桶: {}", bucket);
            }
        } catch (Exception e) {
            log.warn("创建桶失败: {}", bucket, e.getMessage());
        }
    }
}
```

- [ ] **Step 2: Compile check**

Run: `cd backend && mvn compile -pl . -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/picmgmt/storage/MinioStorageService.java
git commit -m "feat: add MinioStorageService with presigned URL support"
```

---

### Task 3.3: Create LocalStorageService (fallback)

**Files:**
- Create: `backend/src/main/java/com/picmgmt/storage/LocalStorageService.java`

- [ ] **Step 1: Write LocalStorageService**

```java
package com.picmgmt.storage;

import com.picmgmt.common.BusinessException;
import com.picmgmt.common.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@Service
@ConditionalOnProperty(name = "storage.type", havingValue = "local")
public class LocalStorageService implements StorageService {

    @Value("${storage.local.base-path:./storage}")
    private String basePath;

    @Override
    public String upload(String bucket, String objectKey, byte[] bytes, String contentType) {
        try {
            Path targetPath = Paths.get(basePath, bucket, objectKey);
            Files.createDirectories(targetPath.getParent());
            Files.write(targetPath, bytes);
            return "/storage/" + bucket + "/" + objectKey;
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.STORAGE_UPLOAD_FAILED, e);
        }
    }

    @Override
    public byte[] download(String bucket, String objectKey) {
        try {
            Path targetPath = Paths.get(basePath, bucket, objectKey);
            return Files.readAllBytes(targetPath);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.STORAGE_DOWNLOAD_FAILED, e);
        }
    }

    @Override
    public void delete(String bucket, String objectKey) {
        try {
            Files.deleteIfExists(Paths.get(basePath, bucket, objectKey));
        } catch (IOException e) {
            log.warn("本地文件删除失败: {}/{}", bucket, objectKey);
        }
    }

    @Override
    public String getAccessUrl(String bucket, String objectKey) {
        return "/api/files/" + bucket + "/" + objectKey;
    }

    @Override
    public FileMeta getFileMeta(String bucket, String objectKey) {
        try {
            Path path = Paths.get(basePath, bucket, objectKey);
            long size = Files.size(path);
            String ct = Files.probeContentType(path);
            return new FileMeta(size, ct != null ? ct : "application/octet-stream");
        } catch (IOException e) {
            return null;
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add backend/src/main/java/com/picmgmt/storage/LocalStorageService.java
git commit -m "feat: add LocalStorageService as fallback storage implementation"
```

---

## Phase 4: Cache Layer

### Task 4.1: Create CacheService interface

**Files:**
- Create: `backend/src/main/java/com/picmgmt/cache/CacheService.java`

- [ ] **Step 1: Write CacheService interface**

```java
package com.picmgmt.cache;

import java.time.Duration;
import java.util.Optional;
import java.util.function.Supplier;

public interface CacheService {

    <T> Optional<T> get(String key, Class<T> type);

    <T> void put(String key, T value, Duration ttl);

    void evict(String key);

    void evictByPattern(String pattern);

    <T> T getOrLoad(String key, Class<T> type, Supplier<T> loader, Duration ttl);
}
```

- [ ] **Step 2: Commit**

```bash
git add backend/src/main/java/com/picmgmt/cache/CacheService.java
git commit -m "feat: add CacheService interface for dual-layer caching"
```

---

### Task 4.2: Create CaffeineLocalCache (L1)

**Files:**
- Create: `backend/src/main/java/com/picmgmt/cache/CaffeineLocalCache.java`

- [ ] **Step 1: Write CaffeineLocalCache**

```java
package com.picmgmt.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Component
public class CaffeineLocalCache {

    private final Cache<String, Object> cache;

    public CaffeineLocalCache() {
        this.cache = Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .expireAfterAccess(5, TimeUnit.MINUTES)
                .recordStats()
                .build();
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<T> get(String key, Class<T> type) {
        Object value = cache.getIfPresent(key);
        if (value != null && type.isInstance(value)) {
            return Optional.of((T) value);
        }
        return Optional.empty();
    }

    public void put(String key, Object value, Duration ttl) {
        // Caffeine's own expiry takes precedence; we set it for semantics
        cache.put(key, value);
    }

    public void evict(String key) {
        cache.invalidate(key);
    }

    public void evictByPattern(String pattern) {
        String prefix = pattern.endsWith("*") ? pattern.substring(0, pattern.length() - 1) : pattern;
        cache.asMap().keySet().removeIf(key -> key.startsWith(prefix));
    }

    public Cache<String, Object> getCache() {
        return cache;
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add backend/src/main/java/com/picmgmt/cache/CaffeineLocalCache.java
git commit -m "feat: add CaffeineLocalCache as L1 cache with LRU eviction"
```

---

### Task 4.3: Create RedisCacheService (L2)

**Files:**
- Create: `backend/src/main/java/com/picmgmt/cache/RedisCacheService.java`

- [ ] **Step 1: Write RedisCacheService**

```java
package com.picmgmt.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisCacheService implements CacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final CaffeineLocalCache caffeineLocalCache;
    private final ObjectMapper objectMapper;

    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<T> get(String key, Class<T> type) {
        // L1: Caffeine
        Optional<T> l1Result = caffeineLocalCache.get(key, type);
        if (l1Result.isPresent()) {
            return l1Result;
        }
        // L2: Redis
        Object value = redisTemplate.opsForValue().get(key);
        if (value != null) {
            if (type.isInstance(value)) {
                caffeineLocalCache.put(key, value, Duration.ofMinutes(5));
                return Optional.of((T) value);
            }
            // If stored as LinkedHashMap (Jackson default), convert
            try {
                T converted = objectMapper.convertValue(value, type);
                caffeineLocalCache.put(key, converted, Duration.ofMinutes(5));
                return Optional.of(converted);
            } catch (Exception e) {
                log.debug("Redis value type conversion failed for key: {}", key);
            }
        }
        return Optional.empty();
    }

    @Override
    public <T> void put(String key, T value, Duration ttl) {
        redisTemplate.opsForValue().set(key, value, ttl);
        caffeineLocalCache.put(key, value, ttl);
    }

    @Override
    public void evict(String key) {
        redisTemplate.delete(key);
        caffeineLocalCache.evict(key);
        // Notify other instances via pub/sub
        redisTemplate.convertAndSend("cache:invalidate", key);
    }

    @Override
    public void evictByPattern(String pattern) {
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
        caffeineLocalCache.evictByPattern(pattern);
        redisTemplate.convertAndSend("cache:invalidate:pattern", pattern);
    }

    @Override
    public <T> T getOrLoad(String key, Class<T> type, Supplier<T> loader, Duration ttl) {
        Optional<T> cached = get(key, type);
        if (cached.isPresent()) {
            return cached.get();
        }
        T value = loader.get();
        if (value != null) {
            put(key, value, ttl);
        }
        return value;
    }
}
```

- [ ] **Step 2: Compile check**

Run: `cd backend && mvn compile -pl . -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/picmgmt/cache/RedisCacheService.java
git commit -m "feat: add RedisCacheService with L1→L2→DB cache chain and pub/sub invalidation"
```

---

## Phase 5: RBAC Permissions

### Task 5.1: Add RBAC schema to schema.sql

**Files:**
- Modify: `backend/src/main/resources/db/schema.sql`

- [ ] **Step 1: Append RBAC tables and seed data**

Append to `backend/src/main/resources/db/schema.sql`:

```sql
-- Permissions table
CREATE TABLE IF NOT EXISTS permissions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(50) NOT NULL,
    group_name VARCHAR(30) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Roles table
CREATE TABLE IF NOT EXISTS roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(30) NOT NULL UNIQUE,
    name VARCHAR(30) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Role-permission mapping
CREATE TABLE IF NOT EXISTS role_permissions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    UNIQUE KEY uk_role_perm (role_id, permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- User-role mapping
CREATE TABLE IF NOT EXISTS user_roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    UNIQUE KEY uk_user_role (user_id, role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Seed roles
INSERT IGNORE INTO roles (id, code, name) VALUES (1, 'admin', '管理员');
INSERT IGNORE INTO roles (id, code, name) VALUES (2, 'moderator', '版主');
INSERT IGNORE INTO roles (id, code, name) VALUES (3, 'user', '普通用户');

-- Seed permissions
INSERT IGNORE INTO permissions (id, code, name, group_name) VALUES
(1, 'image:upload', '上传图片', 'image'),
(2, 'image:edit', '编辑自己的图片', 'image'),
(3, 'image:delete', '删除自己的图片', 'image'),
(4, 'image:edit:any', '编辑任意图片', 'image'),
(5, 'image:delete:any', '删除任意图片', 'image'),
(6, 'category:manage', '管理自己的分类', 'category'),
(7, 'category:manage:any', '管理任意分类', 'category'),
(8, 'comment:add', '添加评论', 'comment'),
(9, 'comment:delete', '删除自己的评论', 'comment'),
(10, 'comment:delete:any', '删除任意评论', 'comment'),
(11, 'user:manage', '管理用户', 'admin');

-- Seed role_permissions (admin gets all)
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT 1, id FROM permissions;

-- moderator permissions
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT 2, id FROM permissions WHERE code IN ('comment:delete:any', 'image:edit:any');

-- user permissions
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT 3, id FROM permissions WHERE code IN ('image:upload', 'image:edit', 'image:delete', 'category:manage', 'comment:add', 'comment:delete');
```

- [ ] **Step 2: Commit**

```bash
git add backend/src/main/resources/db/schema.sql
git commit -m "feat: add RBAC tables and seed data to schema.sql"
```

---

### Task 5.2: Create RBAC entity, mapper, and StpInterface

**Files:**
- Create: `backend/src/main/java/com/picmgmt/auth/Permission.java`
- Create: `backend/src/main/java/com/picmgmt/auth/Role.java`
- Create: `backend/src/main/java/com/picmgmt/auth/RolePermission.java`
- Create: `backend/src/main/java/com/picmgmt/auth/UserRole.java`
- Create: `backend/src/main/java/com/picmgmt/auth/PermissionMapper.java`
- Create: `backend/src/main/java/com/picmgmt/auth/RoleMapper.java`
- Create: `backend/src/main/java/com/picmgmt/auth/RolePermissionMapper.java`
- Create: `backend/src/main/java/com/picmgmt/auth/UserRoleMapper.java`
- Create: `backend/src/main/java/com/picmgmt/auth/SaTokenPermissionImpl.java`

- [ ] **Step 1: Write Permission entity**

`backend/src/main/java/com/picmgmt/auth/Permission.java`:

```java
package com.picmgmt.auth;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("permissions")
public class Permission {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String code;
    private String name;
    private String groupName;
}
```

- [ ] **Step 2: Write Role entity**

`backend/src/main/java/com/picmgmt/auth/Role.java`:

```java
package com.picmgmt.auth;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("roles")
public class Role {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String code;
    private String name;
}
```

- [ ] **Step 3: Write RolePermission entity**

`backend/src/main/java/com/picmgmt/auth/RolePermission.java`:

```java
package com.picmgmt.auth;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("role_permissions")
public class RolePermission {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long roleId;
    private Long permissionId;
}
```

- [ ] **Step 4: Write UserRole entity**

`backend/src/main/java/com/picmgmt/auth/UserRole.java`:

```java
package com.picmgmt.auth;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("user_roles")
public class UserRole {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long roleId;
}
```

- [ ] **Step 5: Write Mappers**

`backend/src/main/java/com/picmgmt/auth/PermissionMapper.java`:

```java
package com.picmgmt.auth;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PermissionMapper extends BaseMapper<Permission> {}
```

`backend/src/main/java/com/picmgmt/auth/RoleMapper.java`:

```java
package com.picmgmt.auth;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RoleMapper extends BaseMapper<Role> {}
```

`backend/src/main/java/com/picmgmt/auth/RolePermissionMapper.java`:

```java
package com.picmgmt.auth;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface RolePermissionMapper extends BaseMapper<RolePermission> {
    @Select("SELECT permission_id FROM role_permissions WHERE role_id IN (#{roleIds})")
    List<Long> selectPermIdsByRoleIds(List<Long> roleIds);
}
```

`backend/src/main/java/com/picmgmt/auth/UserRoleMapper.java`:

```java
package com.picmgmt.auth;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface UserRoleMapper extends BaseMapper<UserRole> {
    @Select("SELECT role_id FROM user_roles WHERE user_id = #{userId}")
    List<Long> selectRoleIdsByUserId(Long userId);
}
```

- [ ] **Step 6: Write SaTokenPermissionImpl**

`backend/src/main/java/com/picmgmt/auth/SaTokenPermissionImpl.java`:

```java
package com.picmgmt.auth;

import cn.dev33.satoken.stp.StpInterface;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class SaTokenPermissionImpl implements StpInterface {

    private final UserRoleMapper userRoleMapper;
    private final RolePermissionMapper rolePermissionMapper;
    private final PermissionMapper permissionMapper;
    private final RoleMapper roleMapper;

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        List<Long> roleIds = userRoleMapper.selectRoleIdsByUserId(Long.parseLong(loginId.toString()));
        if (roleIds.isEmpty()) return List.of();

        List<Long> permIds = rolePermissionMapper.selectPermIdsByRoleIds(roleIds);
        if (permIds.isEmpty()) return List.of();

        return permissionMapper.selectBatchIds(permIds).stream()
                .map(Permission::getCode)
                .toList();
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        List<Long> roleIds = userRoleMapper.selectRoleIdsByUserId(Long.parseLong(loginId.toString()));
        if (roleIds.isEmpty()) return List.of();

        return roleMapper.selectBatchIds(roleIds).stream()
                .map(Role::getCode)
                .toList();
    }
}
```

- [ ] **Step 7: Update SaTokenConfig to check permissions (not just login)**

Modify `backend/src/main/java/com/picmgmt/config/SaTokenConfig.java`:

Replace the interceptor registration with permission-checking variant:

```java
package com.picmgmt.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handle -> {
                    // Check login for all matched paths
                    SaRouter.match("/**")
                            .notMatch("/user/login", "/user/register",
                                    "/doc.html", "/v3/api-docs/**", "/swagger-ui/**",
                                    "/image/square", "/user/profile/**",
                                    "/comment/list/**")
                            .check(r -> StpUtil.checkLogin());
                }))
                .addPathPatterns("/**");
    }
}
```

- [ ] **Step 8: Compile check**

Run: `cd backend && mvn compile -pl . -q`
Expected: BUILD SUCCESS

- [ ] **Step 9: Commit**

```bash
git add backend/src/main/java/com/picmgmt/auth/ backend/src/main/java/com/picmgmt/config/SaTokenConfig.java
git commit -m "feat: add RBAC entities, mappers, and SaTokenPermissionImpl"
```

---

## Phase 6: Entity & Schema Changes

### Task 6.1: Add storage_key columns and drop old Base64 columns

**Files:**
- Create: `backend/src/main/resources/db/migration_storage.sql`

- [ ] **Step 1: Write migration SQL**

```sql
-- Add new storage_key columns
ALTER TABLE images ADD COLUMN IF NOT EXISTS storage_key VARCHAR(255) AFTER image_path;
ALTER TABLE users ADD COLUMN IF NOT EXISTS avatar_key VARCHAR(255) AFTER avatar;
ALTER TABLE users ADD COLUMN IF NOT EXISTS background_key VARCHAR(255) AFTER background;
ALTER TABLE comments ADD COLUMN IF NOT EXISTS image_key VARCHAR(255) AFTER image_path;
```

- [ ] **Step 2: Commit**

```bash
git add backend/src/main/resources/db/migration_storage.sql
git commit -m "feat: add migration SQL for storage_key columns"
```

---

### Task 6.2: Update entities with new fields

**Files:**
- Modify: `backend/src/main/java/com/picmgmt/entity/Image.java`
- Modify: `backend/src/main/java/com/picmgmt/entity/User.java`
- Modify: `backend/src/main/java/com/picmgmt/entity/Comment.java`

- [ ] **Step 1: Add storageKey to Image entity**

Add field to Image.java:

```java
private String storageKey;   // MinIO object key, replaces imagePath
```

- [ ] **Step 2: Add avatarKey/backgroundKey to User entity**

Add fields to User.java:

```java
private String avatarKey;      // MinIO object key for avatar
private String backgroundKey;  // MinIO object key for background
```

- [ ] **Step 3: Add imageKey to Comment entity**

Add field to Comment.java:

```java
private String imageKey;       // MinIO object key for comment image
```

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/java/com/picmgmt/entity/Image.java backend/src/main/java/com/picmgmt/entity/User.java backend/src/main/java/com/picmgmt/entity/Comment.java
git commit -m "feat: add storage_key fields to Image, User, Comment entities"
```

---

### Task 6.3: Update VOs with url fields

**Files:**
- Modify: `backend/src/main/java/com/picmgmt/vo/ImageVO.java`
- Modify: `backend/src/main/java/com/picmgmt/vo/UserVO.java`
- Modify: `backend/src/main/java/com/picmgmt/vo/CommentVO.java`

- [ ] **Step 1: Add imageUrl to ImageVO**

Add to `ImageVO.java`:

```java
private String imageUrl;       // MinIO presigned URL, replaces imagePath for frontend
private String storageKey;     // Internal storage key
```

- [ ] **Step 2: Add avatarUrl/backgroundUrl to UserVO**

Add to `UserVO.java`:

```java
private String avatarUrl;      // MinIO presigned URL for avatar
private String backgroundUrl;  // MinIO presigned URL for background
```

- [ ] **Step 3: Add imageUrl to CommentVO**

Add to `CommentVO.java`:

```java
private String imageUrl;       // MinIO presigned URL for comment image
```

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/java/com/picmgmt/vo/ImageVO.java backend/src/main/java/com/picmgmt/vo/UserVO.java backend/src/main/java/com/picmgmt/vo/CommentVO.java
git commit -m "feat: add URL fields to VOs for MinIO presigned URL"
```

---

## Phase 7: Repository Layer

### Task 7.1: Create ImageRepository

**Files:**
- Create: `backend/src/main/java/com/picmgmt/image/ImageRepository.java`

- [ ] **Step 1: Write ImageRepository**

```java
package com.picmgmt.image;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.picmgmt.cache.CacheService;
import com.picmgmt.entity.Category;
import com.picmgmt.entity.Image;
import com.picmgmt.entity.User;
import com.picmgmt.mapper.CategoryMapper;
import com.picmgmt.mapper.ImageMapper;
import com.picmgmt.mapper.UserMapper;
import com.picmgmt.storage.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ImageRepository {

    private final ImageMapper imageMapper;
    private final UserMapper userMapper;
    private final CategoryMapper categoryMapper;
    private final StorageService storageService;
    private final CacheService cacheService;

    private static final Duration ENTITY_TTL = Duration.ofMinutes(30);
    private static final Duration PAGE_TTL = Duration.ofMinutes(5);
    private static final String ENTITY_KEY_PREFIX = "image:entity:";
    private static final String PAGE_KEY_PREFIX = "image:page:";

    public Optional<Image> findById(Long id) {
        String key = ENTITY_KEY_PREFIX + id;
        return cacheService.get(key, Image.class)
                .or(() -> {
                    Image image = imageMapper.selectById(id);
                    if (image != null) {
                        cacheService.put(key, image, ENTITY_TTL);
                    }
                    return Optional.ofNullable(image);
                });
    }

    public void insert(Image image) {
        imageMapper.insert(image);
        cacheService.put(ENTITY_KEY_PREFIX + image.getId(), image, ENTITY_TTL);
    }

    public void updateById(Image image) {
        imageMapper.updateById(image);
        cacheService.evict(ENTITY_KEY_PREFIX + image.getId());
        cacheService.evictByPattern(PAGE_KEY_PREFIX + "*");
    }

    public void deleteById(Long id) {
        imageMapper.deleteById(id);
        cacheService.evict(ENTITY_KEY_PREFIX + id);
        cacheService.evictByPattern(PAGE_KEY_PREFIX + "*");
    }

    public Page<ImageVO> page(Long userId, String keyword, Long categoryId, String visibility,
                               String sortField, String sortOrder, int pageNum, int pageSize) {
        Page<Image> pageParam = new Page<>(pageNum, pageSize);
        Page<ImageVO> resultPage = imageMapper.selectImageVOPage(
                pageParam, userId, keyword, categoryId, visibility, sortField, sortOrder);
        for (ImageVO vo : resultPage.getRecords()) {
            if (vo.getStorageKey() != null) {
                vo.setImageUrl(storageService.getAccessUrl("images", vo.getStorageKey()));
            }
        }
        return resultPage;
    }

    public ImageVO toVO(Image image) {
        ImageVO vo = new ImageVO();
        vo.setId(image.getId());
        vo.setUserId(image.getUserId());
        vo.setCategoryId(image.getCategoryId());
        vo.setImageName(image.getImageName());
        vo.setStorageKey(image.getStorageKey());
        vo.setFileSize(image.getFileSize());
        vo.setImageType(image.getImageType());
        vo.setDescription(image.getDescription());
        vo.setTags(image.getTags());
        vo.setVisibility(image.getVisibility());
        vo.setVisibleUsernames(image.getVisibleUsernames());
        vo.setUploadTime(image.getUploadTime());

        if (image.getStorageKey() != null) {
            vo.setImageUrl(storageService.getAccessUrl("images", image.getStorageKey()));
        }
        User user = userMapper.selectById(image.getUserId());
        if (user != null) {
            vo.setUsername(user.getUsername());
            vo.setDisplayName(user.getDisplayName());
        }
        if (image.getCategoryId() != null) {
            Category category = categoryMapper.selectById(image.getCategoryId());
            if (category != null) {
                vo.setCategoryName(category.getCategoryName());
            }
        }
        return vo;
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add backend/src/main/java/com/picmgmt/image/ImageRepository.java
git commit -m "feat: add ImageRepository with cache-backed CRUD and pagination"
```

---

### Task 7.2: Update ImageMapper for database-level pagination

**Files:**
- Modify: `backend/src/main/java/com/picmgmt/mapper/ImageMapper.java`

- [ ] **Step 1: Add paginated custom query method**

Add `selectImageVOPage` method to ImageMapper.java:

```java
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

@Select("""
    <script>
        SELECT i.id, i.user_id, i.category_id, i.image_name, i.storage_key,
               i.file_size, i.image_type, i.description, i.tags,
               i.visibility, i.visible_usernames, i.upload_time,
               u.username, u.display_name, c.category_name
        FROM images i
        LEFT JOIN users u ON i.user_id = u.id
        LEFT JOIN categories c ON i.category_id = c.id
        <where>
            <if test='userId != null'>AND i.user_id = #{userId}</if>
            <if test='keyword != null and keyword != \"\"'>AND i.image_name LIKE CONCAT('%', #{keyword}, '%')</if>
            <if test='categoryId != null'>AND i.category_id = #{categoryId}</if>
            <if test='visibility != null and visibility != \"\"'>AND i.visibility = #{visibility}</if>
        </where>
        <choose>
            <when test='sortField == \"image_name\"'>ORDER BY i.image_name ${sortOrder}</when>
            <when test='sortField == \"file_size\"'>ORDER BY i.file_size ${sortOrder}</when>
            <otherwise>ORDER BY i.upload_time ${sortOrder}</otherwise>
        </choose>
    </script>
""")
Page<ImageVO> selectImageVOPage(Page<ImageVO> page,
                                 @Param("userId") Long userId,
                                 @Param("keyword") String keyword,
                                 @Param("categoryId") Long categoryId,
                                 @Param("visibility") String visibility,
                                 @Param("sortField") String sortField,
                                 @Param("sortOrder") String sortOrder);
```

- [ ] **Step 2: Commit**

```bash
git add backend/src/main/java/com/picmgmt/mapper/ImageMapper.java
git commit -m "feat: add paginated selectImageVOPage to ImageMapper"
```

---

### Task 7.3: Create UserRepository

**Files:**
- Create: `backend/src/main/java/com/picmgmt/user/UserRepository.java`

- [ ] **Step 1: Write UserRepository**

```java
package com.picmgmt.user;

import com.picmgmt.cache.CacheService;
import com.picmgmt.entity.User;
import com.picmgmt.mapper.UserMapper;
import com.picmgmt.storage.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserRepository {

    private final UserMapper userMapper;
    private final StorageService storageService;
    private final CacheService cacheService;

    private static final Duration TTL = Duration.ofMinutes(30);
    private static final String KEY_PREFIX = "user:entity:";

    public Optional<User> findById(Long id) {
        String key = KEY_PREFIX + id;
        return cacheService.get(key, User.class)
                .or(() -> {
                    User user = userMapper.selectById(id);
                    if (user != null) {
                        cacheService.put(key, user, TTL);
                    }
                    return Optional.ofNullable(user);
                });
    }

    public void updateById(User user) {
        userMapper.updateById(user);
        cacheService.evict(KEY_PREFIX + user.getId());
    }

    public UserVO toVO(User user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setDisplayName(user.getDisplayName());
        vo.setRole(user.getRole());
        vo.setEmail(user.getEmail());
        vo.setPhone(user.getPhone());
        vo.setBio(user.getBio());
        vo.setCreateTime(user.getCreateTime());

        if (user.getAvatarKey() != null) {
            vo.setAvatarUrl(storageService.getAccessUrl("avatars", user.getAvatarKey()));
        }
        if (user.getBackgroundKey() != null) {
            vo.setBackgroundUrl(storageService.getAccessUrl("backgrounds", user.getBackgroundKey()));
        }
        return vo;
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add backend/src/main/java/com/picmgmt/user/UserRepository.java
git commit -m "feat: add UserRepository with cache-backed user lookup and VO conversion"
```

---

### Task 7.4: Create CategoryRepository and CommentRepository

**Files:**
- Create: `backend/src/main/java/com/picmgmt/category/CategoryRepository.java`
- Create: `backend/src/main/java/com/picmgmt/comment/CommentRepository.java`

- [ ] **Step 1: Write CategoryRepository**

```java
package com.picmgmt.category;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.picmgmt.cache.CacheService;
import com.picmgmt.entity.Category;
import com.picmgmt.mapper.CategoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CategoryRepository {

    private final CategoryMapper categoryMapper;
    private final CacheService cacheService;

    private static final Duration TTL = Duration.ofMinutes(30);
    private static final String KEY_PREFIX = "category:entity:";
    private static final String LIST_KEY_PREFIX = "category:list:";

    public Optional<Category> findById(Long id) {
        String key = KEY_PREFIX + id;
        return cacheService.get(key, Category.class)
                .or(() -> {
                    Category category = categoryMapper.selectById(id);
                    if (category != null) cacheService.put(key, category, TTL);
                    return Optional.ofNullable(category);
                });
    }

    public void insert(Category category) {
        categoryMapper.insert(category);
        cacheService.evictByPattern(LIST_KEY_PREFIX + category.getUserId() + "*");
    }

    public void updateById(Category category) {
        categoryMapper.updateById(category);
        cacheService.evict(KEY_PREFIX + category.getId());
        cacheService.evictByPattern(LIST_KEY_PREFIX + category.getUserId() + "*");
    }

    public void deleteById(Long id) {
        Category category = categoryMapper.selectById(id);
        categoryMapper.deleteById(id);
        if (category != null) {
            cacheService.evict(KEY_PREFIX + id);
            cacheService.evictByPattern(LIST_KEY_PREFIX + category.getUserId() + "*");
        }
    }

    public List<Category> listByUserId(Long userId) {
        String key = LIST_KEY_PREFIX + userId;
        return cacheService.getOrLoad(key, List.class,
                () -> {
                    LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
                    wrapper.eq(Category::getUserId, userId).orderByAsc(Category::getId);
                    return categoryMapper.selectList(wrapper);
                }, TTL);
    }
}
```

- [ ] **Step 2: Write CommentRepository**

```java
package com.picmgmt.comment;

import com.picmgmt.cache.CacheService;
import com.picmgmt.entity.Comment;
import com.picmgmt.mapper.CommentMapper;
import com.picmgmt.storage.StorageService;
import com.picmgmt.vo.CommentVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class CommentRepository {

    private final CommentMapper commentMapper;
    private final StorageService storageService;
    private final CacheService cacheService;

    private static final Duration TTL = Duration.ofMinutes(10);
    private static final String LIST_KEY_PREFIX = "comment:list:";

    public void insert(Comment comment) {
        commentMapper.insert(comment);
        cacheService.evict(LIST_KEY_PREFIX + comment.getImageId());
    }

    public void deleteById(Long commentId) {
        Comment comment = commentMapper.selectById(commentId);
        commentMapper.deleteById(commentId);
        if (comment != null) {
            cacheService.evict(LIST_KEY_PREFIX + comment.getImageId());
            if (comment.getImageKey() != null) {
                storageService.delete("comments", comment.getImageKey());
            }
        }
    }

    public List<CommentVO> listByImageId(Long imageId) {
        String key = LIST_KEY_PREFIX + imageId;
        return cacheService.getOrLoad(key, List.class,
                () -> {
                    List<CommentVO> list = commentMapper.selectCommentVOList(imageId);
                    for (CommentVO vo : list) {
                        if (vo.getImageUrl() != null) {
                            vo.setImageUrl(storageService.getAccessUrl("comments", vo.getImagePath()));
                        }
                    }
                    return list;
                }, TTL);
    }

    public Comment findById(Long id) {
        return commentMapper.selectById(id);
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/picmgmt/category/CategoryRepository.java backend/src/main/java/com/picmgmt/comment/CommentRepository.java
git commit -m "feat: add CategoryRepository and CommentRepository with cache support"
```

---

## Phase 8: Image Service Decomposition

### Task 8.1: Create ImagePermissionService

**Files:**
- Create: `backend/src/main/java/com/picmgmt/image/ImagePermissionService.java`

- [ ] **Step 1: Write ImagePermissionService**

```java
package com.picmgmt.image;

import cn.dev33.satoken.stp.StpUtil;
import com.picmgmt.common.BusinessException;
import com.picmgmt.common.ErrorCode;
import com.picmgmt.entity.Image;
import com.picmgmt.entity.User;
import com.picmgmt.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@Service
@RequiredArgsConstructor
public class ImagePermissionService {

    private final UserMapper userMapper;

    public boolean canView(Image image) {
        if ("PUBLIC".equals(image.getVisibility())) return true;
        if (!StpUtil.isLogin()) return false;

        long userId = StpUtil.getLoginIdAsLong();
        if (image.getUserId().equals(userId) || StpUtil.hasRole("admin")) return true;
        if (!"SPECIFIED".equals(image.getVisibility())) return false;

        User viewer = userMapper.selectById(userId);
        if (viewer == null || image.getVisibleUsernames() == null) return false;

        return Arrays.stream(image.getVisibleUsernames().split(","))
                .map(String::trim)
                .anyMatch(viewer.getUsername()::equals);
    }

    public void validateOwnershipOrAdmin(Image image) {
        long userId = StpUtil.getLoginIdAsLong();
        if (!image.getUserId().equals(userId) && !StpUtil.hasRole("admin")) {
            throw new BusinessException(ErrorCode.IMAGE_PERMISSION_DENIED);
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add backend/src/main/java/com/picmgmt/image/ImagePermissionService.java
git commit -m "feat: add ImagePermissionService for visibility and ownership checks"
```

---

### Task 8.2: Create ImageWriteService

**Files:**
- Create: `backend/src/main/java/com/picmgmt/image/ImageWriteService.java`

- [ ] **Step 1: Write ImageWriteService**

```java
package com.picmgmt.image;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.io.FileUtil;
import com.picmgmt.common.BusinessException;
import com.picmgmt.common.ErrorCode;
import com.picmgmt.entity.Category;
import com.picmgmt.entity.Image;
import com.picmgmt.mapper.CategoryMapper;
import com.picmgmt.storage.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ImageWriteService {

    private final ImageRepository imageRepository;
    private final StorageService storageService;
    private final CategoryMapper categoryMapper;
    private final ImagePermissionService permissionService;

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");
    private static final int MAX_DESCRIPTION_LENGTH = 500;

    @Transactional
    public ImageVO upload(MultipartFile file, Long categoryId, String description,
                          String tags, String visibility, String visibleUsernames) {
        if (file.isEmpty()) throw new BusinessException(ErrorCode.BAD_REQUEST, "文件不能为空");

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "文件名无效");
        }

        String ext = FileUtil.extName(originalFilename).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new BusinessException(ErrorCode.IMAGE_FORMAT_INVALID);
        }
        if (description != null && description.length() > MAX_DESCRIPTION_LENGTH) {
            throw new BusinessException(ErrorCode.IMAGE_DESCRIPTION_TOO_LONG);
        }

        long userId = StpUtil.getLoginIdAsLong();

        if (categoryId != null) {
            Category category = categoryMapper.selectById(categoryId);
            if (category == null || !category.getUserId().equals(userId)) {
                throw new BusinessException(ErrorCode.CATEGORY_NOT_FOUND);
            }
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.STORAGE_UPLOAD_FAILED, e);
        }

        if (!isValidImageContent(bytes)) {
            throw new BusinessException(ErrorCode.IMAGE_FORMAT_INVALID);
        }

        // Upload to MinIO
        String objectKey = "images/" + userId + "/" + UUID.randomUUID() + "." + ext;
        String mimeType = switch (ext) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "webp" -> "image/webp";
            default -> "application/octet-stream";
        };
        storageService.upload("images", objectKey, bytes, mimeType);

        Image image = new Image();
        image.setUserId(userId);
        image.setCategoryId(categoryId);
        image.setImageName(originalFilename);
        image.setStorageKey(objectKey);
        image.setFileSize(file.getSize());
        image.setImageType(ext.toUpperCase());
        image.setDescription(description);
        image.setTags(tags);
        image.setVisibility(visibility != null ? visibility.trim().toUpperCase() : "PRIVATE");
        image.setVisibleUsernames(visibleUsernames);
        image.setUploadTime(LocalDateTime.now());
        imageRepository.insert(image);

        return imageRepository.toVO(image);
    }

    @Transactional
    public void delete(Long imageId) {
        Image image = imageRepository.findById(imageId)
                .orElseThrow(() -> new BusinessException(ErrorCode.IMAGE_NOT_FOUND));

        permissionService.validateOwnershipOrAdmin(image);

        if (image.getStorageKey() != null) {
            storageService.delete("images", image.getStorageKey());
        }
        imageRepository.deleteById(imageId);
    }

    @Transactional
    public ImageVO update(Long imageId, ImageUpdateDTO dto) {
        Image image = imageRepository.findById(imageId)
                .orElseThrow(() -> new BusinessException(ErrorCode.IMAGE_NOT_FOUND));

        permissionService.validateOwnershipOrAdmin(image);

        long userId = StpUtil.getLoginIdAsLong();

        if (dto.getImageName() != null) image.setImageName(dto.getImageName());
        if (dto.getCategoryId() != null) {
            Category category = categoryMapper.selectById(dto.getCategoryId());
            if (category == null || !category.getUserId().equals(userId)) {
                throw new BusinessException(ErrorCode.CATEGORY_NOT_FOUND);
            }
            image.setCategoryId(dto.getCategoryId());
        }
        if (dto.getDescription() != null) {
            if (dto.getDescription().length() > MAX_DESCRIPTION_LENGTH) {
                throw new BusinessException(ErrorCode.IMAGE_DESCRIPTION_TOO_LONG);
            }
            image.setDescription(dto.getDescription());
        }
        if (dto.getTags() != null) image.setTags(dto.getTags());
        if (dto.getVisibility() != null) image.setVisibility(dto.getVisibility().trim().toUpperCase());
        if (dto.getVisibleUsernames() != null) image.setVisibleUsernames(dto.getVisibleUsernames());

        imageRepository.updateById(image);
        return imageRepository.toVO(image);
    }

    private boolean isValidImageContent(byte[] bytes) {
        if (bytes == null || bytes.length < 4) return false;
        if (bytes[0] == (byte) 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4E && bytes[3] == 0x47) return true;
        if (bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xD8 && bytes[2] == (byte) 0xFF) return true;
        if (bytes.length >= 12 && bytes[0] == 0x52 && bytes[1] == 0x49 && bytes[2] == 0x46 && bytes[3] == 0x46
                && bytes[8] == 0x57 && bytes[9] == 0x45 && bytes[10] == 0x42 && bytes[11] == 0x50) return true;
        return false;
    }
}
```

- [ ] **Step 2: Compile check**

Run: `cd backend && mvn compile -pl . -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/picmgmt/image/ImageWriteService.java
git commit -m "feat: add ImageWriteService with upload/delete/update logic"
```

---

### Task 8.3: Create ImageReadService

**Files:**
- Create: `backend/src/main/java/com/picmgmt/image/ImageReadService.java`
- Create: `backend/src/main/java/com/picmgmt/image/ImageUpdateDTO.java`

- [ ] **Step 1: Write ImageUpdateDTO**

```java
package com.picmgmt.image;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ImageUpdateDTO {
    @Size(max = 255, message = "图片名称不能超过255个字符")
    private String imageName;
    private Long categoryId;
    @Size(max = 500, message = "描述不能超过500个字符")
    private String description;
    private String tags;
    @Pattern(regexp = "PUBLIC|PRIVATE|SPECIFIED", message = "可见权限无效")
    private String visibility;
    private String visibleUsernames;
}
```

- [ ] **Step 2: Write ImageReadService**

```java
package com.picmgmt.image;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.picmgmt.common.BusinessException;
import com.picmgmt.common.ErrorCode;
import com.picmgmt.dto.ImageQueryDTO;
import com.picmgmt.entity.Image;
import com.picmgmt.storage.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class ImageReadService {

    private final ImageRepository imageRepository;
    private final ImagePermissionService permissionService;
    private final StorageService storageService;

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("upload_time", "image_name", "file_size");

    public ImageVO getById(Long id) {
        Image image = imageRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.IMAGE_NOT_FOUND));
        if (!permissionService.canView(image)) {
            throw new BusinessException(ErrorCode.IMAGE_PERMISSION_DENIED);
        }
        return imageRepository.toVO(image);
    }

    public Page<ImageVO> page(ImageQueryDTO dto) {
        String sortField = ALLOWED_SORT_FIELDS.contains(dto.getSortField()) ? dto.getSortField() : "upload_time";
        String sortOrder = "asc".equalsIgnoreCase(dto.getSortOrder()) ? "asc" : "desc";

        return imageRepository.page(dto.getUserId(), dto.getKeyword(), dto.getCategoryId(),
                null, sortField, sortOrder, dto.getPage(), dto.getLimit());
    }

    public Page<ImageVO> getSquare(Integer page, Integer limit) {
        return imageRepository.page(null, null, null, "PUBLIC",
                "upload_time", "desc", page, limit);
    }

    public byte[] download(Long id) {
        Image image = imageRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.IMAGE_NOT_FOUND));
        if (!permissionService.canView(image)) {
            throw new BusinessException(ErrorCode.IMAGE_PERMISSION_DENIED);
        }
        return storageService.download("images", image.getStorageKey());
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/picmgmt/image/ImageUpdateDTO.java backend/src/main/java/com/picmgmt/image/ImageReadService.java
git commit -m "feat: add ImageReadService and ImageUpdateDTO with Bean Validation"
```

---

## Phase 9: Refactor Other Services

### Task 9.1: Refactor UserServiceImpl to use UserRepository

**Files:**
- Modify: `backend/src/main/java/com/picmgmt/service/impl/UserServiceImpl.java`

- [ ] **Step 1: Rewrite UserServiceImpl**

```java
package com.picmgmt.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.picmgmt.common.BusinessException;
import com.picmgmt.common.ErrorCode;
import com.picmgmt.dto.LoginDTO;
import com.picmgmt.dto.RegisterDTO;
import com.picmgmt.entity.User;
import com.picmgmt.mapper.UserMapper;
import com.picmgmt.service.UserService;
import com.picmgmt.user.UserRepository;
import com.picmgmt.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final UserRepository userRepository;

    @Override
    public UserVO register(RegisterDTO dto) {
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_MISMATCH);
        }
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, dto.getUsername());
        if (userMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ErrorCode.USERNAME_EXISTS);
        }
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setDisplayName(dto.getUsername());
        user.setPassword(BCrypt.hashpw(dto.getPassword(), BCrypt.gensalt()));
        user.setRole("user");
        userMapper.insert(user);
        return BeanUtil.copyProperties(user, UserVO.class);
    }

    @Override
    public String login(LoginDTO dto) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, dto.getUsername());
        User user = userMapper.selectOne(wrapper);
        if (user == null || !BCrypt.checkpw(dto.getPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }
        StpUtil.login(user.getId());
        return StpUtil.getTokenValue();
    }

    @Override
    public void logout() {
        StpUtil.logout();
    }

    @Override
    public User getById(Long id) {
        return userRepository.findById(id).orElse(null);
    }

    @Override
    public UserVO getUserVOById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        return userRepository.toVO(user);
    }

    @Override
    public UserVO updateProfile(Long userId, String displayName, String email, String phone, String bio) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        if (displayName != null) user.setDisplayName(displayName);
        if (email != null) user.setEmail(email);
        if (phone != null) user.setPhone(phone);
        if (bio != null) user.setBio(bio);
        userRepository.updateById(user);
        return userRepository.toVO(user);
    }

    @Override
    public void updateAvatar(Long userId, String avatarKey) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        user.setAvatarKey(avatarKey);
        userRepository.updateById(user);
    }

    @Override
    public void updateBackground(Long userId, String backgroundKey) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        user.setBackgroundKey(backgroundKey);
        userRepository.updateById(user);
    }

    @Override
    public Page<UserVO> getUserList(Integer page, Integer limit) {
        Page<User> pageParam = new Page<>(page, limit);
        Page<User> userPage = userMapper.selectPage(pageParam,
                new LambdaQueryWrapper<User>().orderByDesc(User::getCreateTime));
        List<UserVO> voList = userPage.getRecords().stream()
                .map(userRepository::toVO)
                .toList();
        Page<UserVO> voPage = new Page<>(page, limit, userPage.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add backend/src/main/java/com/picmgmt/service/impl/UserServiceImpl.java
git commit -m "refactor: UserServiceImpl uses UserRepository and BusinessException"
```

---

### Task 9.2: Refactor CategoryServiceImpl and CommentServiceImpl

**Files:**
- Modify: `backend/src/main/java/com/picmgmt/service/impl/CategoryServiceImpl.java`
- Modify: `backend/src/main/java/com/picmgmt/service/impl/CommentServiceImpl.java`

- [ ] **Step 1: Rewrite CategoryServiceImpl**

```java
package com.picmgmt.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.picmgmt.category.CategoryRepository;
import com.picmgmt.common.BusinessException;
import com.picmgmt.common.ErrorCode;
import com.picmgmt.entity.Category;
import com.picmgmt.entity.Image;
import com.picmgmt.mapper.CategoryMapper;
import com.picmgmt.mapper.ImageMapper;
import com.picmgmt.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryMapper categoryMapper;
    private final CategoryRepository categoryRepository;
    private final ImageMapper imageMapper;

    @Override
    public Category create(String categoryName) {
        if (categoryName == null || categoryName.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "分类名称不能为空");
        }
        if (categoryName.length() > 20) {
            throw new BusinessException(ErrorCode.CATEGORY_NAME_TOO_LONG);
        }
        long userId = StpUtil.getLoginIdAsLong();
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Category::getUserId, userId).eq(Category::getCategoryName, categoryName);
        if (categoryMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ErrorCode.CATEGORY_NAME_EXISTS);
        }
        Category category = new Category();
        category.setUserId(userId);
        category.setCategoryName(categoryName);
        categoryRepository.insert(category);
        return category;
    }

    @Override
    @Transactional
    public void delete(Long categoryId) {
        long userId = StpUtil.getLoginIdAsLong();
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));

        if (!StpUtil.hasRole("admin") && !category.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        LambdaUpdateWrapper<Image> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Image::getCategoryId, categoryId).set(Image::getCategoryId, null);
        imageMapper.update(null, updateWrapper);
        categoryRepository.deleteById(categoryId);
    }

    @Override
    public Category update(Long categoryId, String categoryName) {
        if (categoryName == null || categoryName.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "分类名称不能为空");
        }
        if (categoryName.length() > 20) {
            throw new BusinessException(ErrorCode.CATEGORY_NAME_TOO_LONG);
        }
        long userId = StpUtil.getLoginIdAsLong();
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));

        if (!StpUtil.hasRole("admin") && !category.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        category.setCategoryName(categoryName);
        categoryRepository.updateById(category);
        return category;
    }

    @Override
    public List<Category> listByUser() {
        return categoryRepository.listByUserId(StpUtil.getLoginIdAsLong());
    }
}
```

- [ ] **Step 2: Rewrite CommentServiceImpl**

```java
package com.picmgmt.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.picmgmt.comment.CommentRepository;
import com.picmgmt.common.BusinessException;
import com.picmgmt.common.ErrorCode;
import com.picmgmt.entity.Comment;
import com.picmgmt.mapper.ImageMapper;
import com.picmgmt.service.CommentService;
import com.picmgmt.vo.CommentVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final ImageMapper imageMapper;

    @Override
    public Comment add(Long imageId, String content, String imagePath) {
        if (content == null || content.isBlank()) {
            throw new BusinessException(ErrorCode.COMMENT_EMPTY);
        }
        if (imageMapper.selectById(imageId) == null) {
            throw new BusinessException(ErrorCode.IMAGE_NOT_FOUND);
        }
        Comment comment = new Comment();
        comment.setImageId(imageId);
        comment.setUserId(StpUtil.getLoginIdAsLong());
        comment.setContent(content);
        comment.setImageKey(imagePath);
        commentRepository.insert(comment);
        return comment;
    }

    @Override
    public void delete(Long commentId) {
        Comment comment = commentRepository.findById(commentId);
        if (comment == null) {
            throw new BusinessException(ErrorCode.COMMENT_NOT_FOUND);
        }
        long userId = StpUtil.getLoginIdAsLong();
        if (!StpUtil.hasRole("admin") && !comment.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        commentRepository.deleteById(commentId);
    }

    @Override
    public List<CommentVO> listByImage(Long imageId) {
        return commentRepository.listByImageId(imageId);
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/picmgmt/service/impl/CategoryServiceImpl.java backend/src/main/java/com/picmgmt/service/impl/CommentServiceImpl.java
git commit -m "refactor: CategoryServiceImpl and CommentServiceImpl use Repository and BusinessException"
```

---

## Phase 10: Controller Updates

### Task 10.1: Rewrite ImageController

**Files:**
- Modify: `backend/src/main/java/com/picmgmt/controller/ImageController.java`

- [ ] **Step 1: Rewrite ImageController**

```java
package com.picmgmt.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.picmgmt.common.Result;
import com.picmgmt.dto.ImageQueryDTO;
import com.picmgmt.image.ImageReadService;
import com.picmgmt.image.ImageUpdateDTO;
import com.picmgmt.image.ImageWriteService;
import com.picmgmt.storage.StorageService;
import com.picmgmt.vo.ImageVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Tag(name = "图片模块")
@RestController
@RequestMapping("/image")
@RequiredArgsConstructor
public class ImageController {

    private final ImageWriteService imageWriteService;
    private final ImageReadService imageReadService;
    private final StorageService storageService;

    @Operation(summary = "上传图片")
    @PostMapping("/upload")
    @SaCheckPermission("image:upload")
    public Result<ImageVO> upload(@RequestParam("file") MultipartFile file,
                                   @RequestParam(required = false) Long categoryId,
                                   @RequestParam(required = false) String description,
                                   @RequestParam(required = false) String tags,
                                   @RequestParam(required = false) String visibility,
                                   @RequestParam(required = false) String visibleUsernames) {
        return Result.ok(imageWriteService.upload(file, categoryId, description, tags, visibility, visibleUsernames));
    }

    @Operation(summary = "删除图片")
    @DeleteMapping("/{id}")
    @SaCheckPermission("image:delete")
    public Result<Void> delete(@PathVariable Long id) {
        imageWriteService.delete(id);
        return Result.ok();
    }

    @Operation(summary = "更新图片信息")
    @PutMapping("/{id}")
    @SaCheckPermission("image:edit")
    public Result<ImageVO> update(@PathVariable Long id, @RequestBody @Valid ImageUpdateDTO dto) {
        return Result.ok(imageWriteService.update(id, dto));
    }

    @Operation(summary = "查询当前用户图片列表")
    @GetMapping("/list")
    public Result<Page<ImageVO>> list(@Valid ImageQueryDTO dto) {
        return Result.ok(imageReadService.page(dto));
    }

    @Operation(summary = "下载图片")
    @GetMapping("/download/{id}")
    public ResponseEntity<byte[]> download(@PathVariable Long id) {
        var vo = imageReadService.getById(id);
        byte[] bytes = imageReadService.download(id);
        String encodedName = URLEncoder.encode(vo.getImageName(), StandardCharsets.UTF_8)
                .replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedName)
                .contentType(MediaType.IMAGE_JPEG)
                .body(bytes);
    }

    @Operation(summary = "获取图片详情")
    @GetMapping("/{id}")
    public Result<ImageVO> getById(@PathVariable Long id) {
        return Result.ok(imageReadService.getById(id));
    }

    @Operation(summary = "图片广场")
    @GetMapping("/square")
    public Result<Page<ImageVO>> square(@RequestParam(defaultValue = "1") Integer page,
                                        @RequestParam(defaultValue = "12") Integer limit) {
        return Result.ok(imageReadService.getSquare(page, limit));
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add backend/src/main/java/com/picmgmt/controller/ImageController.java
git commit -m "refactor: ImageController uses ImageWriteService and ImageReadService"
```

---

### Task 10.2: Update UserController for MinIO storage

**Files:**
- Modify: `backend/src/main/java/com/picmgmt/controller/UserController.java`

- [ ] **Step 1: Rewrite UserController**

```java
package com.picmgmt.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.io.FileUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.picmgmt.common.Result;
import com.picmgmt.dto.LoginDTO;
import com.picmgmt.dto.RegisterDTO;
import com.picmgmt.service.UserService;
import com.picmgmt.storage.StorageService;
import com.picmgmt.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Tag(name = "用户模块")
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final StorageService storageService;

    private static final Set<String> ALLOWED_EXT = Set.of("jpg", "jpeg", "png", "webp");

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public Result<UserVO> register(@Valid @RequestBody RegisterDTO dto) {
        return Result.ok(userService.register(dto));
    }

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Result<String> login(@Valid @RequestBody LoginDTO dto) {
        return Result.ok(userService.login(dto));
    }

    @Operation(summary = "用户退出")
    @PostMapping("/logout")
    public Result<Void> logout() {
        userService.logout();
        return Result.ok();
    }

    @Operation(summary = "获取当前用户信息")
    @GetMapping("/info")
    public Result<UserVO> info() {
        return Result.ok(userService.getUserVOById(StpUtil.getLoginIdAsLong()));
    }

    @Operation(summary = "获取用户公开信息")
    @GetMapping("/profile/{id}")
    public Result<UserVO> profile(@PathVariable Long id) {
        return Result.ok(userService.getUserVOById(id));
    }

    @Operation(summary = "更新个人资料")
    @PutMapping("/profile")
    public Result<UserVO> updateProfile(@RequestBody Map<String, String> body) {
        long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(userService.updateProfile(userId,
                body.get("displayName"), body.get("email"),
                body.get("phone"), body.get("bio")));
    }

    @Operation(summary = "上传头像")
    @PostMapping("/avatar")
    public Result<String> uploadAvatar(@RequestParam("file") MultipartFile file) throws IOException {
        long userId = StpUtil.getLoginIdAsLong();
        String ext = FileUtil.extName(file.getOriginalFilename()).toLowerCase();
        if (!ALLOWED_EXT.contains(ext)) throw new IllegalArgumentException("仅支持图片格式");
        String objectKey = "avatars/" + userId + "/" + UUID.randomUUID() + "." + ext;
        String mimeType = "image/" + (ext.equals("jpg") ? "jpeg" : ext);
        storageService.upload("avatars", objectKey, file.getBytes(), mimeType);
        userService.updateAvatar(userId, objectKey);
        return Result.ok(storageService.getAccessUrl("avatars", objectKey));
    }

    @Operation(summary = "上传背景")
    @PostMapping("/background")
    public Result<String> uploadBackground(@RequestParam("file") MultipartFile file) throws IOException {
        long userId = StpUtil.getLoginIdAsLong();
        String ext = FileUtil.extName(file.getOriginalFilename()).toLowerCase();
        if (!ALLOWED_EXT.contains(ext)) throw new IllegalArgumentException("仅支持图片格式");
        String objectKey = "backgrounds/" + userId + "/" + UUID.randomUUID() + "." + ext;
        String mimeType = "image/" + (ext.equals("jpg") ? "jpeg" : ext);
        storageService.upload("backgrounds", objectKey, file.getBytes(), mimeType);
        userService.updateBackground(userId, objectKey);
        return Result.ok(storageService.getAccessUrl("backgrounds", objectKey));
    }

    @Operation(summary = "管理员获取用户列表")
    @GetMapping("/list")
    @SaCheckPermission("user:manage")
    public Result<Page<UserVO>> list(@RequestParam(defaultValue = "1") Integer page,
                                      @RequestParam(defaultValue = "10") Integer limit) {
        return Result.ok(userService.getUserList(page, limit));
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add backend/src/main/java/com/picmgmt/controller/UserController.java
git commit -m "refactor: UserController uses StorageService instead of Base64 encoding"
```

---

### Task 10.3: Update CommentController for MinIO storage

**Files:**
- Modify: `backend/src/main/java/com/picmgmt/controller/CommentController.java`

- [ ] **Step 1: Rewrite CommentController**

```java
package com.picmgmt.controller;

import cn.hutool.core.io.FileUtil;
import com.picmgmt.common.Result;
import com.picmgmt.entity.Comment;
import com.picmgmt.service.CommentService;
import com.picmgmt.storage.StorageService;
import com.picmgmt.vo.CommentVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Tag(name = "评论模块")
@RestController
@RequestMapping("/comment")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;
    private final StorageService storageService;

    private static final Set<String> ALLOWED_EXT = Set.of("jpg", "jpeg", "png", "webp");

    @Operation(summary = "上传评论图片")
    @PostMapping("/upload-image")
    public Result<String> uploadImage(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) throw new IllegalArgumentException("文件不能为空");
        String ext = FileUtil.extName(file.getOriginalFilename()).toLowerCase();
        if (!ALLOWED_EXT.contains(ext)) throw new IllegalArgumentException("仅支持 JPG/PNG/WEBP 格式");

        String objectKey = "comments/" + UUID.randomUUID() + "." + ext;
        String mimeType = "image/" + (ext.equals("jpg") ? "jpeg" : ext);
        storageService.upload("comments", objectKey, file.getBytes(), mimeType);
        return Result.ok(storageService.getAccessUrl("comments", objectKey));
    }

    @Operation(summary = "添加评论")
    @PostMapping
    public Result<Comment> add(@RequestBody Map<String, String> body) {
        return Result.ok(commentService.add(
                Long.valueOf(body.get("imageId")), body.get("content"), body.get("imageKey")));
    }

    @Operation(summary = "删除评论")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        commentService.delete(id);
        return Result.ok();
    }

    @Operation(summary = "获取图片评论列表")
    @GetMapping("/list/{imageId}")
    public Result<List<CommentVO>> list(@PathVariable Long imageId) {
        return Result.ok(commentService.listByImage(imageId));
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add backend/src/main/java/com/picmgmt/controller/CommentController.java
git commit -m "refactor: CommentController uses StorageService for image uploads"
```

---

### Task 10.4: Update CategoryController with @Valid

**Files:**
- Modify: `backend/src/main/java/com/picmgmt/controller/CategoryController.java`

No major changes needed — just keep existing with minor import cleanup. Commit as-is.

```bash
git add backend/src/main/java/com/picmgmt/controller/CategoryController.java
git commit -m "refactor: CategoryController validation clean up"
```

---

## Phase 11: Data Migration

### Task 11.1: Create StorageMigrationRunner

**Files:**
- Create: `backend/src/main/java/com/picmgmt/config/StorageMigrationRunner.java`

- [ ] **Step 1: Write StorageMigrationRunner**

```java
package com.picmgmt.config;

import com.picmgmt.entity.Comment;
import com.picmgmt.entity.Image;
import com.picmgmt.entity.User;
import com.picmgmt.mapper.CommentMapper;
import com.picmgmt.mapper.ImageMapper;
import com.picmgmt.mapper.UserMapper;
import com.picmgmt.service.ImageCacheService;
import com.picmgmt.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.List;

@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class StorageMigrationRunner implements CommandLineRunner {

    private final ImageMapper imageMapper;
    private final UserMapper userMapper;
    private final CommentMapper commentMapper;
    private final StorageService storageService;

    private static final String MIGRATION_FLAG = "storage_migration_done";

    @Override
    public void run(String... args) {
        // Check if already done
        if ("1".equals(System.getProperty(MIGRATION_FLAG))) {
            log.info("数据迁移已完成，跳过");
            return;
        }

        log.info("===== 开始迁移 Base64 → MinIO =====");
        int imageCount = migrateImages();
        int avatarCount = migrateAvatars();
        int bgCount = migrateBackgrounds();
        int commentCount = migrateCommentImages();
        dropOldColumns();
        System.setProperty(MIGRATION_FLAG, "1");
        log.info("===== 迁移完成: 图片 {} 张, 头像 {} 个, 背景 {} 个, 评论 {} 条 =====",
                imageCount, avatarCount, bgCount, commentCount);
    }

    private int migrateImages() {
        List<Image> images = imageMapper.selectList(null);
        int count = 0;
        for (Image image : images) {
            if (image.getImagePath() == null || !image.getImagePath().startsWith("data:")) continue;
            try {
                String base64Data = image.getImagePath().substring(image.getImagePath().indexOf(',') + 1);
                byte[] bytes = Base64.getDecoder().decode(base64Data);
                String ext = image.getImageType().toLowerCase();
                String storageKey = "images/" + image.getUserId() + "/" + image.getId() + "." + ext;
                storageService.upload("images", storageKey, bytes,
                        "image/" + (ext.equals("jpg") ? "jpeg" : ext));
                image.setStorageKey(storageKey);
                image.setImagePath(null);
                imageMapper.updateById(image);
                count++;
            } catch (Exception e) {
                log.warn("迁移图片 {} 失败: {}", image.getId(), e.getMessage());
            }
        }
        return count;
    }

    private int migrateAvatars() {
        List<User> users = userMapper.selectList(null);
        int count = 0;
        for (User user : users) {
            if (user.getAvatar() == null || !user.getAvatar().startsWith("data:")) continue;
            try {
                String base64Data = user.getAvatar().substring(user.getAvatar().indexOf(',') + 1);
                byte[] bytes = Base64.getDecoder().decode(base64Data);
                String storageKey = "avatars/" + user.getId() + "/avatar";
                storageService.upload("avatars", storageKey, bytes, "image/png");
                user.setAvatarKey(storageKey);
                user.setAvatar(null);
                userMapper.updateById(user);
                count++;
            } catch (Exception e) {
                log.warn("迁移用户头像 {} 失败: {}", user.getId(), e.getMessage());
            }
        }
        return count;
    }

    private int migrateBackgrounds() {
        List<User> users = userMapper.selectList(null);
        int count = 0;
        for (User user : users) {
            if (user.getBackground() == null || !user.getBackground().startsWith("data:")) continue;
            try {
                String base64Data = user.getBackground().substring(user.getBackground().indexOf(',') + 1);
                byte[] bytes = Base64.getDecoder().decode(base64Data);
                String storageKey = "backgrounds/" + user.getId() + "/background";
                storageService.upload("backgrounds", storageKey, bytes, "image/jpeg");
                user.setBackgroundKey(storageKey);
                user.setBackground(null);
                userMapper.updateById(user);
                count++;
            } catch (Exception e) {
                log.warn("迁移用户背景 {} 失败: {}", user.getId(), e.getMessage());
            }
        }
        return count;
    }

    private int migrateCommentImages() {
        List<Comment> comments = commentMapper.selectList(null);
        int count = 0;
        for (Comment comment : comments) {
            if (comment.getImagePath() == null || !comment.getImagePath().startsWith("data:")) continue;
            try {
                String base64Data = comment.getImagePath().substring(comment.getImagePath().indexOf(',') + 1);
                byte[] bytes = Base64.getDecoder().decode(base64Data);
                String storageKey = "comments/" + comment.getId();
                storageService.upload("comments", storageKey, bytes, "image/png");
                comment.setImageKey(storageKey);
                comment.setImagePath(null);
                commentMapper.updateById(comment);
                count++;
            } catch (Exception e) {
                log.warn("迁移评论图片 {} 失败: {}", comment.getId(), e.getMessage());
            }
        }
        return count;
    }

    private void dropOldColumns() {
        log.info("Base64 数据迁移完成，请手动执行以下 SQL 删除旧列:");
        log.info("ALTER TABLE images DROP COLUMN image_path;");
        log.info("ALTER TABLE users DROP COLUMN avatar;");
        log.info("ALTER TABLE users DROP COLUMN background;");
        log.info("ALTER TABLE comments DROP COLUMN image_path;");
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add backend/src/main/java/com/picmgmt/config/StorageMigrationRunner.java
git commit -m "feat: add StorageMigrationRunner for Base64 to MinIO migration"
```

---

## Phase 12: Cleanup

### Task 12.1: Delete old files

**Files:**
- Delete: `backend/src/main/java/com/picmgmt/service/ImageCacheService.java`
- Delete: `backend/src/main/java/com/picmgmt/config/DataMigrationRunner.java`
- Delete: `backend/src/main/java/com/picmgmt/service/ImageService.java` (no longer used)
- Delete: `backend/src/main/java/com/picmgmt/service/impl/ImageServiceImpl.java` (replaced by ImageWriteService + ImageReadService)
- Delete: `backend/src/main/resources/db/migration_add_image_visibility.sql` (obsolete)
- Delete: `backend/src/main/resources/db/migration_base64.sql` (obsolete)

- [ ] **Step 1: Delete files**

```bash
git rm backend/src/main/java/com/picmgmt/service/ImageCacheService.java
git rm backend/src/main/java/com/picmgmt/config/DataMigrationRunner.java
git rm backend/src/main/java/com/picmgmt/service/ImageService.java
git rm backend/src/main/java/com/picmgmt/service/impl/ImageServiceImpl.java
git rm backend/src/main/resources/db/migration_add_image_visibility.sql
git rm backend/src/main/resources/db/migration_base64.sql
git commit -m "chore: remove obsolete ImageCacheService, DataMigrationRunner, ImageService/Impl, old migrations"
```

- [ ] **Step 2: Delete old test file**

The test `ImageServiceImplTest.java` tests methods that no longer exist. It should be removed and replaced with new tests.

```bash
git rm backend/src/test/java/com/picmgmt/service/impl/ImageServiceImplTest.java
git commit -m "chore: remove obsolete ImageServiceImplTest"
```

---

## Phase 13: Frontend Changes

### Task 13.1: Update ImageCard and ImageViewer to use imageUrl

**Files:**
- Modify: `frontend/src/components/ImageCard.vue:74-77`
- Modify: `frontend/src/components/ImageViewer.vue` (no change needed — receives `src` prop)

- [ ] **Step 1: Update ImageCard imageSrc**

In `ImageCard.vue`, replace line 74-77:

```javascript
// Before:
const imageSrc = computed(() => {
  if (imgFailed.value || !props.image.imagePath) return ''
  return props.image.imagePath
})

// After:
const imageSrc = computed(() => {
  if (imgFailed.value) return ''
  return props.image.imageUrl || props.image.imagePath || ''
})
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/components/ImageCard.vue
git commit -m "feat: ImageCard renders imageUrl (MinIO) with Base64 fallback"
```

---

### Task 13.2: Update Profile.vue to use avatarUrl/backgroundUrl

**Files:**
- Modify: `frontend/src/views/Profile.vue:15` (avatar src)
- Modify: `frontend/src/views/Profile.vue:243-248` (bannerStyle computed)

- [ ] **Step 1: Update avatar binding**

Line 15, change `:src="user.avatar"` to `:src="user.avatarUrl || user.avatar"`:

```html
<el-avatar :size="120" :src="user.avatarUrl || user.avatar" class="avatar">
```

- [ ] **Step 2: Update bannerStyle**

In `bannerStyle` computed (around line 243), add `backgroundImage` support for `user.backgroundUrl`:

```javascript
const bannerStyle = computed(() => {
  const bg = user.value.backgroundUrl || user.value.background
  if (!bg) return { background: 'linear-gradient(135deg, #111827 0%, #2563eb 58%, #38bdf8 100%)' }
  if (bg.startsWith('#') || bg.startsWith('rgb')) return { backgroundColor: bg }
  return { backgroundImage: `url(${bg})`, backgroundSize: 'cover', backgroundPosition: 'center' }
})
```

- [ ] **Step 3: Update miniBannerStyle (around line 509)**

```javascript
const miniBannerStyle = computed(() => ({
  background: !bgPreviewUrl.value && !user.value.backgroundUrl
    ? 'linear-gradient(135deg, #111827 0%, #2563eb 58%, #38bdf8 100%)' : undefined
}))
```

- [ ] **Step 4: Commit**

```bash
git add frontend/src/views/Profile.vue
git commit -m "feat: Profile uses avatarUrl/backgroundUrl with Base64 fallback"
```

---

## Phase 14: Build & Test Verification

### Task 14.1: Verify backend compiles

- [ ] **Step 1: Compile backend**

Run: `cd backend && mvn clean compile -DskipTests`
Expected: BUILD SUCCESS

- [ ] **Step 2: Fix any compilation errors and recommit**

---

### Task 14.2: Run existing tests

- [ ] **Step 1: Run backend tests**

Run: `cd backend && mvn test`
Expected: Review failures. Existing `ImageServiceImplTest` is deleted. `CategoryServiceImplTest` may need minor adjustments due to BusinessException.

---

### Task 14.3: E2E smoke test

- [ ] **Step 1: Start MinIO** (Docker)

```bash
# Already running from earlier setup
```

- [ ] **Step 2: Start Redis**

```bash
docker run -d --name redis -p 6379:6379 redis:7-alpine
```

- [ ] **Step 3: Start backend**

Run: `cd backend && mvn spring-boot:run`
Expected: App starts, migration runner logs, no errors

- [ ] **Step 4: Start frontend**

Run: `cd frontend && npm run dev`
Expected: Dev server on :3000

- [ ] **Step 5: Test flows**

1. Open `http://localhost:3000` — navigate to login
2. Register a new user → login
3. Upload an image → verify in list → imageUrl is MinIO presigned URL
4. Delete image → verify gone
5. Visit square → view public images
6. Upload avatar → verify rendered
7. Upload background → verify rendered
8. Check Redis: `docker exec redis redis-cli KEYS "image:*"` — should have cache entries
9. Check MinIO console at `http://localhost:9001` — buckets and objects present

---

## Summary

**Total files**: ~30 created, ~15 modified, ~6 deleted

**Order of execution**: Phases 1→14, tasks within each phase sequentially. Backend-only phases can be done without frontend.

**Critical dependencies**: MinIO must be running before backend starts. Redis is optional (cache will miss L2 but won't fail).
