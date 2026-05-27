---
name: minio-storage-migration
description: Migrated image storage from Base64/MySQL to MinIO object storage
metadata: 
  node_type: memory
  type: project
  originSessionId: 4c7f230e-5d9e-4453-ac75-d087daafd71c
---

All images (pictures, avatars, backgrounds, comments) were originally stored as Base64 Data URLs in MySQL LONGTEXT columns. Migrated to MinIO object storage for scalability.

**StorageService:** Interface with `upload(bucket, key, bytes, mimeType)` and `download(bucket, key)` methods.
**MinioStorageService:** Implementation using MinIO Java SDK. Creates buckets on demand.
**StorageMigrationRunner:** On startup, detects legacy Base64 data in database, converts to MinIO objects, updates records with new storage keys.

**Bucket structure:**
- `avatars/{userId}/...`
- `backgrounds/{userId}/...`
- `images/{imageId}/...`
- `comments/{commentId}/...`

**nginx:** `/minio/` path proxies to `minio:9000/`, `sub_filter` rewrites `http://minio:9000/` to `/minio/` in API responses.

**Why:** Base64 in MySQL was wasteful — 33% storage overhead, no CDN potential, slow queries. MinIO provides proper object storage.
**How to apply:** New file uploads go through `StorageService.upload()`. Downloads through `StorageService.download()`. Legacy migration runs automatically on first startup.
